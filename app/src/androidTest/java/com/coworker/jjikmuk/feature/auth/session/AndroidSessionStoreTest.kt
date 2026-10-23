package com.coworker.jjikmuk.feature.auth.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSessionStoreTest {
    @Test fun keystoreRoundTripUsesAnIsolatedKeyAndFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val suffix = UUID.randomUUID().toString()
        val file = File(context.noBackupFilesDir, "test-auth-$suffix.bin")
        val keys = AndroidSessionKeyProvider("jjikmuk.auth.test.$suffix")
        val store = EncryptedSessionStore(file, keys)
        try {
            store.write(SessionCredentials("instrumented-test-token", 1, "test@example.com", Long.MAX_VALUE, true))
            assertFalse(file.readText(Charsets.ISO_8859_1).contains("instrumented-test-token"))
            assertEquals("instrumented-test-token", store.read()!!.accessToken)
            store.clear()
            assertNull(store.read())
        } finally { store.clear() }
    }
}
