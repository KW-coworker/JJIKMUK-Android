package com.coworker.jjikmuk.feature.auth.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.KeyStoreException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidSessionKeyProvider(private val alias: String = "jjikmuk.auth.session.v1") : SessionKeyProvider {
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    override fun encryptionKey(): SecretKey {
        (store().getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build())
        }.generateKey()
    }

    override fun decryptionKey(): SecretKey = store().getKey(alias, null) as? SecretKey
        ?: throw KeyStoreException("Session key is missing")

    override fun deleteKey() { store().deleteEntry(alias) }
}
