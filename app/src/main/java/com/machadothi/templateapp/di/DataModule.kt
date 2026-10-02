package com.machadothi.templateapp.di

import android.os.Build
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.machadothi.templateapp.BuildConfig
import com.machadothi.templateapp.data.network.DataService
import com.machadothi.templateapp.data.network.HeliostatService
import com.machadothi.templateapp.data.network.HostSelectionInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * The base URL is only a placeholder for the heliostat: HostSelectionInterceptor
     * rewrites every request to the address learned over BLE. SERVER_URL arrives as
     * the literal string "null" when local.properties has no server.url, and
     * Retrofit would throw on it while building the DI graph -- a crash on launch.
     */
    @Provides
    @Singleton
    fun providesRetrofit(
        json: Json,
        hostSelector: HostSelectionInterceptor,
    ): Retrofit {
        val baseUrl = BuildConfig.SERVER_URL.takeIf { it.startsWith("http") }
            ?: "http://heliostat.local/"
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                OkHttpClient.Builder()
                    .callTimeout(5L, TimeUnit.SECONDS)
                    .addInterceptor(hostSelector)
                    .addInterceptor(
                        HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY),
                    ).build(),
            )
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }


    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        return logging
    }

    @Provides
    fun providesService(retrofit: Retrofit): DataService {
        return retrofit.create(DataService::class.java)
    }

    @Provides
    @Singleton
    fun providesHeliostatService(retrofit: Retrofit): HeliostatService =
        retrofit.create(HeliostatService::class.java)

    @Provides
    @Singleton
    fun providesJson() = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }
}