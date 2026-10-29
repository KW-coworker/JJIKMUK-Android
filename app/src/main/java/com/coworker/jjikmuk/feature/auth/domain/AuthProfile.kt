package com.coworker.jjikmuk.feature.auth.domain

/** Omission is not clearing. Only optional profile fields allow Clear. */
sealed interface ProfileField {
    data object Unchanged : ProfileField
    data object Clear : ProfileField
    class Value(val value: String) : ProfileField
}

class ProfilePatch(
    val nickname: String? = null,
    val allergies: ProfileField = ProfileField.Unchanged,
    val diseases: ProfileField = ProfileField.Unchanged,
    val specialDiet: ProfileField = ProfileField.Unchanged,
    val dislikedIngredients: ProfileField = ProfileField.Unchanged,
) {
    init { require(nickname == null || normalizeAuthNickname(nickname).isNotEmpty()) }
}

class AuthProfile(
    val id: Long, val email: String, val nickname: String,
    val allergies: String?, val diseases: String?, val specialDiet: String?, val dislikedIngredients: String?,
    val role: String, val authProvider: AuthProvider, val profileCompleted: Boolean,
    val hasPassword: Boolean, val photoUrl: String?,
)
