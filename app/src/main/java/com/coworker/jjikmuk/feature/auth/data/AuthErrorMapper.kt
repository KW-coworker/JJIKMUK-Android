package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonObject
import com.google.gson.JsonParser

internal object AuthErrorMapper {
    fun decode(status: Int, body: String?, retryAfter: String?, receivedAtMillis: Long): AuthFailure {
        val json = runCatching { body?.let { JsonParser.parseString(it).asJsonObject } }.getOrNull()
        // Bad optional fields must not erase a valid code; backend message/raw body never escapes here.
        return AuthFailure(AuthFailureKind.Server, status, json?.string("code"), json?.string("field"),
            json?.nonNegativeLong("retryAfterSeconds"),
            json?.nonNegativeLong("remainingAttempts")?.takeIf { it <= Int.MAX_VALUE }?.toInt(),
            AuthApiTime.retryAfterSeconds(retryAfter, receivedAtMillis))
    }

    private fun JsonObject.string(name: String): String? = get(name)?.takeIf {
        it.isJsonPrimitive && it.asJsonPrimitive.isString
    }?.asString?.takeIf { it.isNotBlank() }

    private fun JsonObject.nonNegativeLong(name: String): Long? = get(name)?.takeIf {
        it.isJsonPrimitive && it.asJsonPrimitive.isNumber
    }?.asString?.takeIf { it.matches(Regex("[0-9]+")) }?.toLongOrNull()
}
