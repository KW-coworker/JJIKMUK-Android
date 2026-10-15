package com.coworker.jjikmuk.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.coworker.jjikmuk.feature.scanner.data.HttpScannerOcrService
import com.coworker.jjikmuk.feature.scanner.domain.ScannerOcrService
import com.coworker.jjikmuk.feature.scanner.domain.UnavailableScannerOcrService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Properties
import okhttp3.HttpUrl.Companion.toHttpUrl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScannerModule {
    @Provides
    @Singleton
    fun provideScannerOcrService(@ApplicationContext context: Context): ScannerOcrService {
        // Debug credentials live only in private, non-backed-up storage, never in APKs or logs.
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) {
            return UnavailableScannerOcrService()
        }
        return runCatching {
            val config = Properties().apply {
                File(context.noBackupFilesDir, "scanner-ocr.properties").inputStream().use(::load)
            }
            val token = config.getProperty("testToken")?.trim()
            require(!token.isNullOrBlank() && token.all { it.code in 33..126 })
            val url = config.getProperty("baseUrl",
                "https://jjikmuk-backend-dev.tail198441.ts.net/").toHttpUrl()
            HttpScannerOcrService.create(url) { token }
        }.getOrElse { UnavailableScannerOcrService() }
    }
}
