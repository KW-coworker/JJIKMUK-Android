package com.coworker.jjikmuk.feature.auth

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.auth.google.AndroidGoogleCredentialSource
import com.coworker.jjikmuk.feature.auth.google.GoogleCredentialResult
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.Properties
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GoogleStartupDiagnosticTest {
    @Test fun inspectMissingConfiguration() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("googleStartupCheck") == "true")
        val instrument = InstrumentationRegistry.getInstrumentation()
        val context = instrument.targetContext
        val builtIn = context.getString(R.string.google_web_client_id).trim()
        val local = runCatching { Properties().apply {
            File(context.noBackupFilesDir, "auth-api.properties").inputStream().use(::load)
        }.getProperty("googleWebClientId")?.trim() }.getOrNull()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'KST'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Seoul")
        }.format(java.util.Date())
        fun report(s: String) = instrument.sendStatus(0, Bundle().apply { putString("googleDiagnostic", s) })
        report("checkedAt=$time; builtInClientIdPresent=${builtIn.isNotEmpty()}; localClientIdPresent=${!local.isNullOrBlank()}")
        if (builtIn.isNotEmpty() || !local.isNullOrBlank()) { report("Skipped: configuration present; OAuth not invoked"); return@runBlocking }
        val result = AndroidGoogleCredentialSource().get(context)
        check(result is GoogleCredentialResult.Failure)
        report("stage=BEFORE_GOOGLE_ACCOUNT_PICKER; backendApiCalled=false; message=${result.message}")
    }
}
