package com.coworker.jjikmuk.di

import com.coworker.jjikmuk.feature.scanner.domain.ScannerOcrService
import com.coworker.jjikmuk.feature.scanner.domain.UnavailableScannerOcrService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides
    @Singleton
    fun provideScannerOcrService(): ScannerOcrService = UnavailableScannerOcrService()
}
