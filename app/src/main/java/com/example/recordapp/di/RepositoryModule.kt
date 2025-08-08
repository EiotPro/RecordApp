package com.example.recordapp.di

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.recordapp.data.AppDatabase
import com.example.recordapp.network.InternetConnectionChecker
import com.example.recordapp.network.SupabaseClient
import com.example.recordapp.repository.AuthRepository
import com.example.recordapp.util.CsvUtils
import com.example.recordapp.util.PdfUtils
import com.example.recordapp.util.SignInHistoryManager

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module providing repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * Provides the AuthRepository as a singleton
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context,
        supabaseClient: SupabaseClient,
        signInHistoryManager: SignInHistoryManager
    ): AuthRepository {
        return AuthRepository(context, supabaseClient, signInHistoryManager)
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(@ApplicationContext context: Context): EncryptedSharedPreferences {
        // Create a MasterKey with default parameters
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        return EncryptedSharedPreferences.create(
            context,
            "auth_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
    
    /**
     * Provides CsvUtils as a singleton
     */
    @Provides
    @Singleton
    fun provideCsvUtils(@ApplicationContext context: Context): CsvUtils {
        return CsvUtils(context)
    }
    
    /**
     * Provides PdfUtils as a singleton
     */
    @Provides
    @Singleton
    fun providePdfUtils(@ApplicationContext context: Context): PdfUtils {
        return PdfUtils(context)
    }

    /**
     * Provides SignInHistoryManager
     */
    @Provides
    fun provideSignInHistoryManager(@ApplicationContext context: Context): SignInHistoryManager {
        return SignInHistoryManager(context)
    }
    

} 