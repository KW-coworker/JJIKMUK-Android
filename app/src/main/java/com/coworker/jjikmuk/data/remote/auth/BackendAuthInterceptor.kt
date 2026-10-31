package com.coworker.jjikmuk.data.remote.auth

import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

class BackendAuthInterceptor @Inject constructor(private val tokens: AccessTokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
        tokens.token.value?.let { request.header("Authorization", "Bearer $it") }
        return chain.proceed(request.build())
    }
}
