package com.example.ocrjizhang.di

import com.example.ocrjizhang.BuildConfig
import com.example.ocrjizhang.data.remote.api.ApiConstants
import com.example.ocrjizhang.data.remote.service.AuthService
import com.example.ocrjizhang.data.remote.service.CategoryService
import com.example.ocrjizhang.data.remote.service.OcrService
import com.example.ocrjizhang.data.remote.service.SyncService
import com.example.ocrjizhang.data.remote.service.TransactionService
import com.example.ocrjizhang.data.repository.SessionManager
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor = Interceptor { chain ->
        val token = sessionManager.getTokenBlocking()
        val requestBuilder = chain.request().newBuilder()
        if (token.isNotBlank()) {
            requestBuilder.header(
                ApiConstants.AUTHORIZATION_HEADER,
                ApiConstants.BEARER_PREFIX + token,
            )
        }
        chain.proceed(requestBuilder.build())
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    @Named("backendClient")
    fun provideBackendOkHttpClient(
        authInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("ocrClient")
    fun provideOcrOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @Named("backendRetrofit")
    fun provideBackendRetrofit(
        @Named("backendClient") client: OkHttpClient,
        gson: Gson,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @Named("ocrRetrofit")
    fun provideOcrRetrofit(
        @Named("ocrClient") client: OkHttpClient,
        gson: Gson,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConstants.BAIDU_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAuthService(@Named("backendRetrofit") retrofit: Retrofit): AuthService =
        retrofit.create(AuthService::class.java)

    @Provides
    @Singleton
    fun provideCategoryService(@Named("backendRetrofit") retrofit: Retrofit): CategoryService =
        retrofit.create(CategoryService::class.java)

    @Provides
    @Singleton
    fun provideTransactionService(@Named("backendRetrofit") retrofit: Retrofit): TransactionService =
        retrofit.create(TransactionService::class.java)

    @Provides
    @Singleton
    fun provideSyncService(@Named("backendRetrofit") retrofit: Retrofit): SyncService =
        retrofit.create(SyncService::class.java)

    @Provides
    @Singleton
    fun provideOcrService(@Named("ocrRetrofit") retrofit: Retrofit): OcrService =
        retrofit.create(OcrService::class.java)
}
