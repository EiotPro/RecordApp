package com.example.recordapp.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.recordapp.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for expense operations
 */
@Dao
interface ExpenseDao {
    
    /**
     * Insert a new expense
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)
    
    /**
     * Update an expense
     */
    @Update
    suspend fun update(expense: ExpenseEntity)
    
    /**
     * Delete an expense
     */
    @Delete
    suspend fun delete(expense: ExpenseEntity)
    
    /**
     * Delete an expense by ID
     */
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)
    
    /**
     * Delete all expenses
     */
    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
    
    /**
     * Get an expense by ID
     */
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?
    
    /**
     * Get all expenses as Flow
     */
    @Query("SELECT * FROM expenses ORDER BY displayOrder ASC, timestampString DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>
    
    /**
     * Get all expenses as a list (not a Flow)
     */
    @Query("SELECT * FROM expenses ORDER BY displayOrder ASC, timestampString DESC")
    suspend fun getAllExpensesAsList(): List<ExpenseEntity>
    
    /**
     * Get paged expenses
     */
    @Query("SELECT * FROM expenses ORDER BY displayOrder ASC, timestampString DESC")
    fun getPagedExpenses(): PagingSource<Int, ExpenseEntity>
    
    /**
     * Get expenses by folder name
     */
    @Query("SELECT * FROM expenses WHERE folderName = :folderName ORDER BY displayOrder ASC, timestampString DESC")
    fun getExpensesByFolder(folderName: String): Flow<List<ExpenseEntity>>
} 