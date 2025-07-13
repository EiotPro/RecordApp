package com.example.recordapp.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.recordapp.data.AppDatabase
import com.example.recordapp.model.Expense
import com.example.recordapp.model.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

/**
 * Repository for managing expense records
 */
class ExpenseRepository private constructor(private val context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val expenseDao = database.expenseDao()
    
    /**
     * Flow of all expenses for observing changes
     */
    val expenses: Flow<List<Expense>> = expenseDao.getAllExpenses().map { entityList ->
        entityList.map { it.toExpense() }
    }
    
    /**
     * Paged expenses for Paging 3
     */
    fun getPagedExpenses(pageSize: Int = DEFAULT_PAGE_SIZE): Flow<PagingData<Expense>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                maxSize = MAX_PAGING_ITEMS
            ),
            pagingSourceFactory = { expenseDao.getPagedExpenses() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toExpense() }
        }
    }
    
    /**
     * Add a new expense record
     */
    suspend fun addExpense(
        imagePath: Uri? = null, 
        timestamp: LocalDateTime = LocalDateTime.now(), 
        serialNumber: String = "",
        amount: Double = 0.0,
        description: String = "",
        folderName: String = "default",
        receiptType: String = ""
    ): Expense {
        return withContext(Dispatchers.IO) {
            val expense = Expense(
                id = UUID.randomUUID().toString(),
                imagePath = imagePath,
                timestamp = timestamp,
                serialNumber = serialNumber,
                amount = amount,
                description = description,
                folderName = folderName,
                receiptType = receiptType
            )
            val entity = ExpenseEntity.fromExpense(expense)
            expenseDao.insert(entity)
            Log.d(TAG, "Added expense: $expense")
            return@withContext expense
        }
    }
    
    /**
     * Add an expense directly from Expense object
     */
    suspend fun addExpense(expense: Expense): Expense {
        return withContext(Dispatchers.IO) {
            val entity = ExpenseEntity.fromExpense(expense)
            expenseDao.insert(entity)
            Log.d(TAG, "Added expense from object: $expense")
            return@withContext expense
        }
    }
    
    /**
     * Get all expenses
     */
    suspend fun getAllExpenses(): List<Expense> {
        return withContext(Dispatchers.IO) {
            expenseDao.getAllExpensesAsList().map { it.toExpense() }
        }
    }
    
    /**
     * Get expense by id
     */
    suspend fun getExpenseById(id: String): Expense? {
        return withContext(Dispatchers.IO) {
            expenseDao.getById(id)?.toExpense()
        }
    }
    
    /**
     * Delete an expense by id
     */
    suspend fun deleteExpense(id: String) {
        withContext(Dispatchers.IO) {
            expenseDao.deleteById(id)
        }
        Log.d(TAG, "Deleted expense with id: $id")
    }
    
    /**
     * Add an expense with its original ID (for undo functionality)
     */
    suspend fun addExpenseWithId(expense: Expense) {
        withContext(Dispatchers.IO) {
            val entity = ExpenseEntity.fromExpense(expense)
            expenseDao.insert(entity)
        }
        Log.d(TAG, "Restored expense with id: ${expense.id}")
    }
    
    /**
     * Clear all expenses
     */
    suspend fun clearAllExpenses() {
        withContext(Dispatchers.IO) {
            expenseDao.deleteAll()
        }
        Log.d(TAG, "Cleared all expenses")
    }
    
    /**
     * Update an expense
     */
    suspend fun updateExpense(expense: Expense) {
        withContext(Dispatchers.IO) {
            val entity = ExpenseEntity.fromExpense(expense)
            expenseDao.update(entity)
        }
    }
    
    /**
     * Get expenses filtered by folder name
     */
    fun getExpensesByFolder(folderName: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByFolder(folderName).map { entities ->
            entities.map { it.toExpense() }
        }
    }
    
    companion object {
        private const val TAG = "ExpenseRepository"
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGING_ITEMS = 100
        
        @Volatile
        private var INSTANCE: ExpenseRepository? = null
        
        fun getInstance(context: Context): ExpenseRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ExpenseRepository(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
}
