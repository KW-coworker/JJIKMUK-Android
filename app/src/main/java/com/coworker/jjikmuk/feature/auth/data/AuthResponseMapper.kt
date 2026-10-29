package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonObject

/** Shared decoding for the next Google/refresh/profile adapters; it performs no requests or storage. */
internal object AuthResponseMapper {
    fun login(body: LoginResponse, requireRefresh: Boolean = false): AuthLogin {
        require(body.code == null && body.loginGranted != false)
        require(body.tokenType == "Bearer")
        val token = requireNotNull(body.token).also { require(it.isNotBlank()) }
        val expiresAt = requireNotNull(body.expiresAt)
        val expiresAtMillis = AuthApiTime.parseMillis(expiresAt)
        // Until the deployment switches, a wholly legacy response is explicit, not fabricated refresh data.
        val hasNewContract = body.refreshToken != null || body.refreshExpiresAt != null ||
            body.authProvider != null || body.hasPassword != null
        require(!requireRefresh || hasNewContract)
        val refresh = if (hasNewContract) RefreshCredentials(
            requireNotNull(body.refreshToken).also { require(it.isNotBlank()) },
            AuthApiTime.parseMillis(requireNotNull(body.refreshExpiresAt)),
        ) else null
        val provider = if (hasNewContract) AuthProvider.valueOf(requireNotNull(body.authProvider)) else null
        if (hasNewContract) {
            requireNotNull(body.isNewUser)
            require(requireNotNull(body.hasPassword) == (provider != AuthProvider.GOOGLE))
        }
        return AuthLogin(token, requireNotNull(body.userId).also { require(it > 0) },
            requireNotNull(body.email).also { require(it.isNotBlank()) }, expiresAt,
            requireNotNull(body.expiresInSeconds).also { require(it > 0) }, requireNotNull(body.profileCompleted),
            refresh, provider, body.hasPassword, body.isNewUser, expiresAtMillis)
    }

    fun google(body: JsonObject): GoogleAuthResult {
        if (body.get("code")?.asString == "GOOGLE_LINK_REQUIRED") {
            require(body.get("loginGranted")?.asBoolean == false)
            require(listOf("token", "refreshToken").all { !body.has(it) || body[it].isJsonNull })
            return GoogleAuthResult.LinkRequired(
                requireNotNull(body.get("linkToken")?.asString).also { require(it.isNotBlank()) },
                AuthApiTime.parseMillis(requireNotNull(body.get("expiresAt")?.asString)),
                requireNotNull(body.get("expiresInSeconds")?.asLong).also { require(it > 0) },
            )
        }
        require(!body.has("linkToken") || body["linkToken"].isJsonNull)
        return GoogleAuthResult.SignedIn(login(authJson().fromJson(body, LoginResponse::class.java), requireRefresh = true))
    }

    fun profile(body: ProfileBody): AuthProfile {
        val provider = AuthProvider.valueOf(requireNotNull(body.authProvider))
        val hasPassword = requireNotNull(body.hasPassword)
        require(hasPassword == (provider != AuthProvider.GOOGLE))
        require(body.photoUrl == null || body.photoUrl == "/api/users/me/photo")
        return AuthProfile(requireNotNull(body.id).also { require(it > 0) },
            requireNotNull(body.email).also { require(it.isNotBlank()) },
            requireNotNull(body.nickname).also { require(it.isNotBlank()) },
            body.allergies, body.diseases, body.specialDiet, body.dislikedIngredients,
            requireNotNull(body.role), provider, requireNotNull(body.profileCompleted), hasPassword, body.photoUrl)
    }
}
