package com.example.recordapp.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.recordapp.data.dao.ExpenseDao
import com.example.recordapp.data.Converters
import com.example.recordapp.model.ExpenseEntity

/**
 * Main database for the application
 */
@Database(
    entities = [ExpenseEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Get DAO for expenses
     */
    abstract fun expenseDao(): ExpenseDao
    
    companion object {
        const val DATABASE_NAME = "record_app.db"
        
        /**
         * Migration from version 1 to 2 to add receiptType field
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN receiptType TEXT NOT NULL DEFAULT ''")
            }
        }
        
        /**
         * Migration from version 3 to 4 to add displayOrder field for image reordering
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add displayOrder column to expenses table
                database.execSQL("ALTER TABLE expenses ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        /**
         * Migration from version 4 to 5 to add support for list converters
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This migration doesn't need to modify any tables
                // It's needed because we added new TypeConverters for List<String>
                Log.i("AppDatabase", "Migration 4 to 5 completed for TypeConverter changes")
            }
        }
        
        /**
         * Migration from version 5 to 4 to handle backup restore from versions that had admin functionality
         */
        private val MIGRATION_5_4 = object : Migration(5, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is a downgrade migration
                // Drop the admin_settings table if it exists
                try {
                    database.execSQL("DROP TABLE IF EXISTS admin_settings")
                    Log.i("AppDatabase", "Dropped admin_settings table during downgrade migration")
                } catch (e: Exception) {
                    Log.e("AppDatabase", "Error dropping admin_settings table", e)
                }
            }
        }
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get singleton instance of the database
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_4)
                .fallbackToDestructiveMigrationOnDowngrade() // Allow destructive downgrade as a last resort
                .build()
                .also { INSTANCE = it }
            }
        }
        
        /**
         * Clear the existing instance of the database
         * This is used during restore operations to ensure we get a fresh connection
         */
        fun clearInstance() {
            synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    if (instance.isOpen) {
                        instance.close()
                    }
                    INSTANCE = null
                    Log.d("AppDatabase", "Database instance cleared")
                }
            }
        }
        
        /**
         * Invalidate the current database instance to force reconnection
         * This is used during restore operations to ensure we reconnect to the restored database
         */
        fun invalidateInstance() {
            synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    if (instance.isOpen) {
                        instance.close()
                    }
                    INSTANCE = null
                    Log.d("AppDatabase", "Database instance invalidated")
                }
            }
        }
    }
} 