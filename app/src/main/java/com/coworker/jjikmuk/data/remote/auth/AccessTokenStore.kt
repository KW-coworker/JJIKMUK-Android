package com.coworker.jjikmuk.data.remote.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Login owner supplies the raw access token; this class does not issue or persist credentials. */
@Singleton
class AccessTokenStore @Inject constructor() {
    private val mutableToken = MutableStateFlow<String?>(null)
    val token = mutableToken.asStateFlow()
    fun setAccessToken(value: String?) {
        val token = value?.trim()?.takeIf { it.isNotEmpty() }
        require(token == null || token.none { it.isWhitespace() }) { "Supply the raw access token." }
        mutableToken.value = token
    }
    fun clear() { mutableToken.value = null }
}
