package com.coworker.jjikmuk.feature.auth.google

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.coworker.jjikmuk.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.io.File
import java.util.Properties
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext

sealed interface GoogleCredentialResult {
    // No generated toString: never log the ID token.
    class Token(val value: String) : GoogleCredentialResult
    data object Cancelled : GoogleCredentialResult
    data class Failure(val message: String) : GoogleCredentialResult
}

/** The Activity is supplied only for the duration of the platform picker, never held by a ViewModel. */
fun interface GoogleCredentialSource {
    suspend fun get(context: Context): GoogleCredentialResult
}

class AndroidGoogleCredentialSource : GoogleCredentialSource {
    override suspend fun get(context: Context): GoogleCredentialResult {
        val clientId = withContext(Dispatchers.IO) { readClientId(context) }
        if (clientId.isNullOrBlank()) return GoogleCredentialResult.Failure("Google 로그인 설정이 필요합니다. Web OAuth Client ID를 확인해 주세요.")
        return try {
            val option = GetSignInWithGoogleOption.Builder(clientId).build()
            val response = CredentialManager.create(context).getCredential(context,
                GetCredentialRequest.Builder().addCredentialOption(option).build())
            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                if (token.isBlank()) GoogleCredentialResult.Failure("Google 로그인 정보를 확인하지 못했어요. 다시 시도해 주세요.")
                else GoogleCredentialResult.Token(token)
            } else GoogleCredentialResult.Failure("Google 로그인 정보를 확인하지 못했어요. 다시 시도해 주세요.")
        } catch (_: GetCredentialCancellationException) {
            GoogleCredentialResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleCredentialResult.Failure("기기에 Google 계정을 추가하고 Google Play 서비스를 확인해 주세요.")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: GetCredentialException) {
            GoogleCredentialResult.Failure("Google 로그인에 실패했어요. 연결 상태와 앱의 Google 로그인 설정을 확인해 주세요.")
        } catch (_: Exception) {
            GoogleCredentialResult.Failure("Google 로그인 응답 또는 설정을 확인하지 못했어요.")
        }
    }

    private fun readClientId(context: Context): String? {
        val configured = context.getString(R.string.google_web_client_id).trim()
        val local = if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) runCatching {
            Properties().apply { File(context.noBackupFilesDir, "auth-api.properties").inputStream().use(::load) }
                .getProperty("googleWebClientId")?.trim()
        }.getOrNull() else null
        return (local ?: configured).takeIf { it.endsWith(".apps.googleusercontent.com") && it.none(Char::isWhitespace) }
    }
}

fun interface GoogleCredentialCleaner {
    suspend fun clear()
    object None : GoogleCredentialCleaner { override suspend fun clear() = Unit }
}

class AndroidGoogleCredentialCleaner(private val context: Context) : GoogleCredentialCleaner {
    override suspend fun clear() {
        try { withTimeoutOrNull(3_000) { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Local logout remains valid even when the provider is unavailable. */ }
    }
}
