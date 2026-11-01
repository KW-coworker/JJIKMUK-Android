package com.coworker.jjikmuk.data.remote.auth

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class BackendAuthInterceptor @Inject constructor(private val tokens: AccessTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        // Explicit tokens (e.g. a captured chat session) must not be replaced by a newer account.
        if (chain.request().header("Authorization") == null &&
            !chain.request().url.encodedPath.startsWith("/api/auth/")) {
            tokens.currentToken()?.let { request.header("Authorization", "Bearer $it") }
        }
        val sent = request.build()
        val response = chain.proceed(sent)
        if (response.code == 403) {
            val code = runCatching { com.coworker.jjikmuk.feature.auth.data.AuthErrorMapper.decode(
                403, response.peekBody(4096).string(), null, System.currentTimeMillis()).code }.getOrNull()
            val access = sent.header("Authorization")?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
            if (code == "PROFILE_INCOMPLETE" && access != null)
                kotlinx.coroutines.runBlocking { tokens.profileIncomplete(access) }
        }
        return response
    }
}
