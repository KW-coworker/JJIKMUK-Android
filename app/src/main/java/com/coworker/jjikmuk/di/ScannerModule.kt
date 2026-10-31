package com.coworker.jjikmuk.di

import com.coworker.jjikmuk.data.remote.BackendServer
import com.coworker.jjikmuk.feature.auth.session.AuthorizedOperations
import com.coworker.jjikmuk.feature.scanner.data.AuthenticatedScannerOcrService
import com.coworker.jjikmuk.feature.scanner.data.HttpScannerOcrService
import com.coworker.jjikmuk.feature.scanner.domain.ScannerOcrService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides @Singleton
    fun provideScannerOcrService(operations: AuthorizedOperations): ScannerOcrService =
        AuthenticatedScannerOcrService(HttpScannerOcrService.create(BackendServer.BASE_URL.toHttpUrl()), operations)
}
