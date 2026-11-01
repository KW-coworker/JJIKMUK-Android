package com.coworker.jjikmuk.di

import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.feature.auth.google.GoogleCredentialCleaner
import com.coworker.jjikmuk.feature.auth.google.AndroidGoogleCredentialCleaner
import android.content.Context
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.AuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrl

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides @Singleton
    fun provideLoginRetryGate(@ApplicationContext context: Context): com.coworker.jjikmuk.feature.auth.presentation.login.LoginRetryGate {
        val prefs = context.getSharedPreferences("auth-login-retry", Context.MODE_PRIVATE)
        return com.coworker.jjikmuk.feature.auth.presentation.login.LoginRetryGate(
            readDeadline = { prefs.getLong("deadline", 0L) },
            writeDeadline = { prefs.edit().putLong("deadline", it).apply() },
        )
    }

    @Provides @Singleton
    fun providePendingSignupPhotoStore(@ApplicationContext context: Context): com.coworker.jjikmuk.feature.auth.photo.PendingSignupPhotoStore =
        com.coworker.jjikmuk.feature.auth.photo.FilePendingSignupPhotoStore(File(context.noBackupFilesDir, "signup-photos"))

    @Provides
    fun provideSignupPhotoPreparation(implementation: com.coworker.jjikmuk.feature.auth.photo.AndroidSignupPhotoPreparation):
        com.coworker.jjikmuk.feature.auth.photo.SignupPhotoPreparation = implementation

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
    fun provideAuthRepository(): AuthRepository =
        HttpAuthRepository.create(com.coworker.jjikmuk.data.remote.BackendServer.BASE_URL.toHttpUrl())
}
