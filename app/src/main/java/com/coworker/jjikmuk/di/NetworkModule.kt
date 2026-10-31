package com.coworker.jjikmuk.di

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
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BackendServer.BASE_URL)
            .client(okHttpClient)
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

    private const val NETWORK_TIMEOUT_SECONDS = 30L
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BackendRetrofit
