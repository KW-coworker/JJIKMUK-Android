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
import com.coworker.jjikmuk.data.remote.BackendServer
import okhttp3.RequestBody
import okio.BufferedSink
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {

        return OkHttpClient.Builder()
            .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .followRedirects(false).followSslRedirects(false)
            .addInterceptor { chain ->
                val request = chain.request()
                val body = request.body
                // Paid POSTs must not be replayed by OkHttp for 408/503 follow-up responses.
                val safeRequest = if (body == null || request.method in setOf("GET", "HEAD")) request else
                    request.newBuilder().method(request.method, object : RequestBody() {
                        override fun contentType() = body.contentType()
                        override fun contentLength() = body.contentLength()
                        override fun isOneShot() = true
                        override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
                    }).build()
                chain.proceed(safeRequest)
            }
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
            .client(okHttpClient.newBuilder().addInterceptor(auth).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideChatApi(
        @BackendRetrofit
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
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return BackendServer.BASE_URL
        val file = File(context.noBackupFilesDir, "backend.properties")
        if (!file.exists()) return BackendServer.BASE_URL
        val properties = Properties().apply { file.inputStream().use(::load) }
        val url = properties.getProperty("baseUrl")?.trim() ?: return BackendServer.BASE_URL
        require(url == "http://127.0.0.1:8080/" || url == "http://10.0.2.2:8080/") {
            "Debug backend override must point to the local test server."
        }
        return url
    }

    private const val NETWORK_TIMEOUT_SECONDS = 30L
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit
