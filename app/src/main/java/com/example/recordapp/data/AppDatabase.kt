package com.example.recordapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.recordapp.data.dao.ExpenseDao
import com.example.recordapp.model.Converters
import com.example.recordapp.model.ExpenseEntity

/**
 * Main database for the application
 */
@Database(
    entities = [ExpenseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    /**
     * Get DAO for expenses
     */
    abstract fun expenseDao(): ExpenseDao
    
    companion object {
        private const val DATABASE_NAME = "record_app.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get singleton instance of the database
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                
                INSTANCE = instance
                instance
            }
        }
    }
} 