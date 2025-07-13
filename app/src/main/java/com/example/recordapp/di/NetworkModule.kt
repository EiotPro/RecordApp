package com.example.recordapp.di

import android.content.Context
import com.example.recordapp.network.InternetConnectionChecker
import com.example.recordapp.network.SupabaseClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Hilt module for providing network-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://api.recordapp.example.com/"
    private const val BACKUP_URL = "https://backup-api.recordapp.example.com/"
    
    /**
     * Provides OkHttpClient with logging and timeout configuration
     * Includes DNS fallback and connection error handling
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS) // Reduced timeout for faster fallback
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true) // Retry on connection failure
            .build()
    }
    
    /**
     * Provides primary Retrofit instance for API calls
     */
    @Provides
    @Singleton
    @Named("primary")
    fun providePrimaryRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides backup Retrofit instance for API calls
     */
    @Provides
    @Singleton
    @Named("backup")
    fun provideBackupRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BACKUP_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Provides InternetConnectionChecker for checking connectivity
     */
    @Provides
    @Singleton
    fun provideInternetConnectionChecker(
        @ApplicationContext context: Context
    ): InternetConnectionChecker {
        return InternetConnectionChecker(context)
    }
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClient()
    }
} 