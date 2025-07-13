package com.example.recordapp.di

import android.content.Context
import com.example.recordapp.data.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Main application module providing general app dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    // Add additional app-wide dependencies here
    
    /**
     * Provides application context
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    /**
     * Provides the AppDatabase instance
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
} 