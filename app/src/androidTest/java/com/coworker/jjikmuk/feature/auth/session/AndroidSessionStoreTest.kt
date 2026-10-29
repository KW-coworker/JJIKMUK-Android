package com.coworker.jjikmuk.feature.auth.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.domain.AuthProvider
import com.coworker.jjikmuk.feature.auth.domain.RefreshCredentials
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
            store.write(SessionCredentials("instrumented-test-token", 1, "test@example.com", Long.MAX_VALUE, true,
                RefreshCredentials("instrumented-refresh", Long.MAX_VALUE), AuthProvider.LOCAL, true))
            assertFalse(file.readText(Charsets.ISO_8859_1).contains("instrumented-test-token"))
            assertFalse(file.readText(Charsets.ISO_8859_1).contains("instrumented-refresh"))
            val restored = EncryptedSessionStore(file, AndroidSessionKeyProvider("jjikmuk.auth.test.$suffix")).read()!!
            assertEquals("instrumented-test-token", restored.accessToken)
            assertEquals("instrumented-refresh", restored.refresh!!.token)
            assertEquals(Long.MAX_VALUE, restored.refresh.expiresAtMillis)
            assertEquals(AuthProvider.LOCAL, restored.authProvider)
            store.clear()
            assertNull(store.read())
        } finally { store.clear() }
    }
}
