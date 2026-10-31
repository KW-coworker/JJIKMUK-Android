package com.coworker.jjikmuk.di

import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.feature.auth.google.GoogleCredentialCleaner
import com.coworker.jjikmuk.feature.auth.google.AndroidGoogleCredentialCleaner
import android.content.Context
import android.content.pm.ApplicationInfo
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.Properties
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrl

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton
    fun provideGoogleCleaner(@ApplicationContext context: Context): GoogleCredentialCleaner = AndroidGoogleCredentialCleaner(context)

    @Provides @Singleton
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore =
        EncryptedSessionStore(File(context.noBackupFilesDir, "auth-session.bin"), AndroidSessionKeyProvider())

    @Provides @Singleton
    fun provideSessionManager(store: SessionStore): AuthSessionManager = AuthSessionManager(store)

    @Provides @Singleton
    fun provideAuthenticatedRequests(sessions: AuthSessionManager, repository: AuthRepository) = AuthenticatedRequests(sessions, repository)

    @Provides @Singleton
    fun provideAuthRepository(@ApplicationContext context: Context): AuthRepository {
        // Authentication's server address has not been confirmed. No fallback to OCR/product hosts.
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            return HttpAuthRepository.unconfigured()
        }
        return runCatching {
            val config = Properties().apply {
                File(context.noBackupFilesDir, "auth-api.properties").inputStream().use(::load)
            }
            val url = requireNotNull(config.getProperty("baseUrl")).trim().toHttpUrl()
            require(url.isHttps) // Production credentials must never be sent over cleartext HTTP.
            HttpAuthRepository.create(url)
        }.getOrElse { HttpAuthRepository.unconfigured() }
    }
}
