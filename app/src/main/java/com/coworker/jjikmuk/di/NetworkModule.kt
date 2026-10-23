package com.coworker.jjikmuk.di

import android.content.Context
import android.content.pm.ApplicationInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Properties
import com.coworker.jjikmuk.data.remote.auth.BackendAuthInterceptor
import com.coworker.jjikmuk.data.remote.api.SafeRecommendationApi
import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.api.ProductApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @AiRetrofit
    fun provideAiRetrofit(
        okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AI_SERVER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @BackendRetrofit
    fun provideBackendRetrofit(
        okHttpClient: OkHttpClient,
        auth: BackendAuthInterceptor,
        @ApplicationContext context: Context,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(backendBaseUrl(context))
            .client(okHttpClient.newBuilder().addInterceptor(auth).followRedirects(false).followSslRedirects(false).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatApi(
        @AiRetrofit
        retrofit: Retrofit,
    ): ChatApi {
        return retrofit.create(ChatApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProductApi(
        @BackendRetrofit
        retrofit: Retrofit,
    ): ProductApi {
        return retrofit.create(ProductApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSafeRecommendationApi(@BackendRetrofit retrofit: Retrofit): SafeRecommendationApi =
        retrofit.create(SafeRecommendationApi::class.java)

    @Provides
    @Singleton
    fun provideProductOptionsApi(@BackendRetrofit retrofit: Retrofit): com.coworker.jjikmuk.data.remote.api.ProductOptionsApi =
        retrofit.create(com.coworker.jjikmuk.data.remote.api.ProductOptionsApi::class.java)

    private fun backendBaseUrl(context: Context): String {
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return BACKEND_SERVER_BASE_URL
        val file = File(context.noBackupFilesDir, "backend.properties")
        if (!file.exists()) return BACKEND_SERVER_BASE_URL
        val properties = Properties().apply { file.inputStream().use(::load) }
        val url = properties.getProperty("baseUrl")?.trim() ?: return BACKEND_SERVER_BASE_URL
        require(url == "http://127.0.0.1:8080/" || url == "http://10.0.2.2:8080/") {
            "Debug backend override must point to the local test server."
        }
        return url
    }

    private const val AI_SERVER_BASE_URL = "http://10.0.2.2:8000/"
    private const val BACKEND_REMOTE_SERVER_BASE_URL = "https://jjikmuk-backend-dev.tail198441.ts.net/"
    private const val BACKEND_SERVER_BASE_URL = BACKEND_REMOTE_SERVER_BASE_URL
    private const val NETWORK_TIMEOUT_SECONDS = 30L
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AiRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit
