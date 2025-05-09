package com.example.recordapp.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.recordapp.model.Expense
import com.example.recordapp.repository.ExpenseRepository
import com.example.recordapp.util.CsvGenerator
import com.example.recordapp.util.FileUtils
import com.example.recordapp.util.OcrResult
import com.example.recordapp.util.OcrUtils
import com.example.recordapp.util.PdfGenerator
import com.example.recordapp.util.GridSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import androidx.paging.filter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * ViewModel for expense operations
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = ExpenseRepository.getInstance(application)
    
    // UI state
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()
    
    // Current folder state for capturing new images
    private val _currentFolder = MutableStateFlow("default")
    val currentFolder: StateFlow<String> = _currentFolder.asStateFlow()
    
    // Selected folder for viewing expenses (null means view all folders)
    private val _selectedViewFolder = MutableStateFlow<String?>(null)
    val selectedViewFolder: StateFlow<String?> = _selectedViewFolder.asStateFlow()
    
    // OCR Result state
    private val _ocrResult = MutableStateFlow<OcrResult?>(null)
    val ocrResult: StateFlow<OcrResult?> = _ocrResult.asStateFlow()
    
    // Expenses (non-paged)
    val expenses = repository.expenses
    
    // Paged expenses using Paging 3 - filtered by selected folder
    val pagedExpenses: Flow<PagingData<Expense>> = 
        _selectedViewFolder.flatMapLatest { folderFilter ->
            if (folderFilter == null) {
                repository.getPagedExpenses()
            } else {
                // For filtered view, filter the paged data
                repository.getPagedExpenses().map { pagingData ->
                    pagingData.filter { expense ->
                        expense.folderName == folderFilter
                    }
                }
            }
        }.cachedIn(viewModelScope)
    
    // Available folders with expense counts
    private val _availableFolders = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val availableFolders: StateFlow<List<Pair<String, Int>>> = _availableFolders.asStateFlow()
    
    // Cache for folder statistics to avoid recalculation
    private val folderStatsCache = mutableMapOf<String, Pair<Int, Double>>() // folderName -> (count, total)
    
    init {
        Log.d(TAG, "ExpenseViewModel initialized")
        refreshFolders()
    }
    
    /**
     * Set the current folder for image captures
     */
    fun setCurrentFolder(folderName: String) {
        if (folderName.isNotBlank()) {
            _currentFolder.value = folderName
            Log.d(TAG, "Current folder set to: $folderName")
        }
    }
    
    /**
     * Set the folder filter for viewing expenses
     */
    fun setSelectedViewFolder(folderName: String?) {
        _selectedViewFolder.value = folderName
        Log.d(TAG, "View filter set to folder: ${folderName ?: "All"}")
    }
    
    /**
     * Refresh the list of available folders and their expense counts
     */
    fun refreshFolders() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get all expenses
                val allExpenses = repository.getAllExpenses()
                
                // Count expenses per folder
                val folderCounts = allExpenses.groupBy { it.folderName }
                    .map { (folder, expenses) -> 
                        folder to expenses.size
                    }
                
                // Update folder statistics cache
                folderCounts.forEach { (folder, count) ->
                    val totalAmount = allExpenses.filter { it.folderName == folder }.sumOf { it.amount }
                    folderStatsCache[folder] = count to totalAmount
                }
                
                // Update available folders
                _availableFolders.value = folderCounts
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing folders", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to refresh folders: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get statistics for a specific folder
     */
    fun getFolderStats(folderName: String): Flow<Pair<Int, Double>> = flow {
        // Check cache first
        folderStatsCache[folderName]?.let {
            emit(it)
            return@flow
        }
        
        // Calculate if not cached
        val expenses = repository.getAllExpenses()
        val folderExpenses = expenses.filter { it.folderName == folderName }
        val count = folderExpenses.size
        val total = folderExpenses.sumOf { it.amount }
        
        // Cache results
        folderStatsCache[folderName] = count to total
        
        emit(count to total)
    }
    
    /**
     * Move an expense to a different folder
     */
    fun moveExpenseToFolder(expenseId: String, targetFolder: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val expense = repository.getExpenseById(expenseId)
                if (expense == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                    return@launch
                }
                
                // Create the updated expense with the new folder name
                val updatedExpense = expense.copy(folderName = targetFolder)
                
                // Update the expense in the repository
                repository.updateExpense(updatedExpense)
                
                // Clear folder stats cache to force refresh
                folderStatsCache.clear()
                
                // Refresh folders
                refreshFolders()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Moved expense $expenseId from ${expense.folderName} to: $targetFolder")
            } catch (e: Exception) {
                Log.e(TAG, "Error moving expense to folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to move expense: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Process image with OCR and apply compression
     */
    fun processImageWithOcr(imageUri: Uri, compressionQuality: Int = FileUtils.DEFAULT_COMPRESSION_QUALITY) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Save the image to the current folder
                val savedImageUri = FileUtils.saveImageToFolder(
                    getApplication(),
                    imageUri,
                    _currentFolder.value,
                    compressionQuality
                )
                
                val result = OcrUtils.processImage(getApplication(), savedImageUri ?: imageUri)
                _ocrResult.value = result
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image with OCR", e)
                _ocrResult.value = OcrResult()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to process image: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Add a new expense
     */
    fun addExpense(
        description: String = "",
        amount: Double = 0.0,
        folderName: String = "default",
        imagePath: Uri? = null,
        serialNumber: String = "",
        compressionQuality: Int = FileUtils.DEFAULT_COMPRESSION_QUALITY
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                Log.d(TAG, "Adding expense to folder: $folderName with image: $imagePath")
                
                // Process the image with the specified compression quality if needed
                val processedImageUri = imagePath?.let {
                    // Save the image to the current folder with the specified compression quality
                    val savedUri = FileUtils.saveImageToFolder(
                        getApplication<Application>(),
                        it,
                        folderName,
                        compressionQuality
                    )
                    Log.d(TAG, "Image processed and saved to folder: $folderName, URI: $savedUri")
                    savedUri
                }
                
                // Create the expense with the current folder name
                val expense = repository.addExpense(
                    imagePath = processedImageUri ?: imagePath,
                    timestamp = LocalDateTime.now(),
                    serialNumber = serialNumber,
                    amount = amount,
                    description = description,
                    folderName = folderName
                )
                
                Log.d(TAG, "Expense added successfully with ID: ${expense.id} in folder: ${expense.folderName}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                // Clear OCR result after adding
                _ocrResult.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error adding expense", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to add expense: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Delete an expense
     */
    fun deleteExpense(id: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                repository.deleteExpense(id)
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting expense", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete expense: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Undo the deletion of an expense by re-adding it
     */
    fun undoDelete(expense: Expense) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Re-add the expense with its original ID and data
                repository.addExpenseWithId(expense)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error undoing expense deletion", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to restore expense: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Generate PDF with all expenses
     */
    fun generatePdf(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses to generate PDF"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                val pdfFile = PdfGenerator.generatePdf(context, allExpenses)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(pdfFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Generate PDF for a single expense
     */
    fun generateSingleExpensePdf(expenseId: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val expense = repository.getExpenseById(expenseId)
                
                if (expense == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                val pdfFile = PdfGenerator.generateSingleExpensePdf(context, expense)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(pdfFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating single expense PDF", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Generate CSV with all expenses
     */
    fun generateCsv(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses to generate CSV"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                val csvFile = CsvGenerator.generateCsv(context, allExpenses)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(csvFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating CSV", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate CSV: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Generate CSV with expenses in a specific folder
     */
    fun generateCsvByFolder(folderName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses to generate CSV"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                val csvFile = CsvGenerator.generateCsvByFolder(context, allExpenses, folderName)
                
                if (csvFile != null && csvFile.exists() && csvFile.length() > 0) {
                    Log.d(TAG, "CSV Export Success: ${csvFile.absolutePath}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    onComplete(csvFile)
                } else {
                    Log.e(TAG, "CSV Export Failed: File was null or empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to export CSV: Empty file created"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating CSV for folder: $folderName", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate CSV: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Get intent to share the generated file
     */
    fun getShareFileIntent(file: File): Intent {
        return FileUtils.getShareFileIntent(getApplication<Application>(), file)
    }
    
    /**
     * Get intent to view the generated PDF
     */
    fun getViewPdfIntent(file: File): Intent {
        return FileUtils.getViewPdfIntent(getApplication<Application>(), file)
    }
    
    /**
     * Create temporary image file for camera
     */
    fun createImageFile(): Pair<File, Uri> {
        return FileUtils.createImageFile(getApplication<Application>(), _currentFolder.value)
    }
    
    /**
     * Create temporary image file URI for camera
     */
    fun createImageFileUri(): Uri? {
        val (_, uri) = FileUtils.createImageFile(getApplication<Application>(), _currentFolder.value)
        return uri
    }
    
    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Clear OCR result
     */
    fun clearOcrResult() {
        _ocrResult.value = null
    }
    
    /**
     * Get a specific expense by ID
     */
    fun getExpenseById(id: String): Flow<Expense?> = flow {
        emit(repository.getExpenseById(id))
    }
    
    /**
     * Get the most recent expense for displaying on the home screen
     */
    fun getMostRecentExpense(): Flow<Expense?> = flow {
        val allExpenses = repository.getAllExpenses()
        val mostRecent = allExpenses.maxByOrNull { it.timestamp }
        emit(mostRecent)
    }
    
    /**
     * Export original image of a single expense
     */
    fun shareOriginalImage(expenseId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val expense = repository.getExpenseById(expenseId)
                
                if (expense?.imagePath == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found or no image available"
                    )
                    onComplete(false)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing original image", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to share image: ${e.message}"
                )
                onComplete(false)
            }
        }
    }
    
    /**
     * Generate PDF with images in grid layout
     */
    fun generateImageGridPdf(gridSize: GridSize, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses with images to export"
                    )
                    onComplete(null)
                    return@launch
                }
                
                // Filter expenses that have images
                val expensesWithImages = allExpenses.filter { it.imagePath != null }
                
                if (expensesWithImages.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses with images to export"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                val pdfFile = PdfGenerator.generateImageGridPdf(context, expensesWithImages, gridSize)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(pdfFile)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating grid PDF", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate grid PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Clear all expenses
     */
    fun clearAllExpenses() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                repository.clearAllExpenses()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing all expenses", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to clear expenses: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Generate PDF with all expenses in a specific folder
     */
    fun generatePdfByFolder(folderName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                Log.d(TAG, "Starting PDF generation for folder: $folderName")
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses to generate PDF - expenses list is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses to generate PDF"
                    )
                    onComplete(null)
                    return@launch
                }
                
                Log.d(TAG, "Found ${allExpenses.size} total expenses")
                val folderExpenses = allExpenses.filter { it.folderName == folderName }
                Log.d(TAG, "Found ${folderExpenses.size} expenses in folder: $folderName")
                
                val context = getApplication<Application>()
                try {
                    val pdfFile = PdfGenerator.generatePdfByFolder(context, allExpenses, folderName)
                    
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        Log.d(TAG, "PDF Export Success: ${pdfFile.absolutePath}")
                        Log.d(TAG, "PDF file exists: ${pdfFile.exists()}, size: ${pdfFile.length()} bytes")
                        Log.d(TAG, "PDF file path: ${pdfFile.absolutePath}")
                        Log.d(TAG, "PDF file can read: ${pdfFile.canRead()}")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = null
                        )
                        onComplete(pdfFile)
                    } else {
                        Log.e(TAG, "PDF Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to export PDF: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in PDF generation process", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "PDF generation error: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF for folder: $folderName", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to generate PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Get all expenses in a specific folder
     */
    fun getExpensesByFolder(folderName: String): Flow<List<Expense>> {
        return repository.getExpensesByFolder(folderName)
    }
    
    /**
     * Get the count of expenses in a specific folder
     */
    suspend fun getExpenseCountInFolder(folderName: String): Int {
        return repository.getAllExpenses().count { it.folderName == folderName }
    }
    
    /**
     * Get the total amount of expenses in a specific folder
     */
    suspend fun getTotalAmountInFolder(folderName: String): Double {
        return repository.getAllExpenses()
            .filter { it.folderName == folderName }
            .sumOf { it.amount }
    }
    
    /**
     * Update the folder of an expense
     */
    fun updateExpenseFolder(expenseId: String, newFolderName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val expense = repository.getExpenseById(expenseId)
                if (expense == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                    return@launch
                }
                
                // Create the updated expense with the new folder name
                val updatedExpense = expense.copy(folderName = newFolderName)
                
                // Update the expense in the repository
                repository.updateExpense(updatedExpense)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Updated expense $expenseId folder to: $newFolderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating expense folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update expense folder: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Add a new expense with an image
     */
    fun addExpenseWithImage(
        description: String,
        amount: Double,
        folderName: String,
        imageUri: Uri?
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                if (imageUri == null) {
                    // Create without image
                    addExpense(
                        description = description,
                        amount = amount,
                        folderName = folderName
                    )
                    return@launch
                }
                
                // Create expense with the image URI
                val expense = Expense(
                    description = description,
                    amount = amount,
                    folderName = folderName,
                    imagePath = imageUri,
                    timestamp = LocalDateTime.now()
                )
                
                // Add to repository
                repository.addExpense(expense)
                
                // Refresh folders to update counts
                refreshFolders()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Added expense with image: $description, $amount, $folderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding expense with image", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to add expense: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Format currency value based on locale settings
     */
    fun formatCurrency(amount: Double): String {
        val currencyFormatter = NumberFormat.getCurrencyInstance().apply {
            currency = Currency.getInstance(Locale.getDefault())
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return currencyFormatter.format(amount)
    }
    
    /**
     * Create a new folder
     */
    fun createFolder(folderName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                if (folderName.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Folder name cannot be empty"
                    )
                    return@launch
                }
                
                // Check if folder already exists
                val allFolders = _availableFolders.value.map { it.first }
                if (allFolders.contains(folderName)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Folder already exists"
                    )
                    return@launch
                }
                
                // Create folder by adding it to the available folders
                // Since folders are virtual (just tags on expenses),
                // we just need to update the available folders list
                val updatedFolders = _availableFolders.value.toMutableList()
                updatedFolders.add(Pair(folderName, 0))
                _availableFolders.value = updatedFolders
                
                // Set it as the current folder
                setCurrentFolder(folderName)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Created new folder: $folderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to create folder: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Rename a folder and update all associated expenses
     */
    fun renameFolder(oldFolderName: String, newFolderName: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                if (newFolderName.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "New folder name cannot be empty"
                    )
                    return@launch
                }
                
                // Check if new folder name already exists
                val allFolders = _availableFolders.value.map { it.first }
                if (allFolders.contains(newFolderName)) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Folder name already exists"
                    )
                    return@launch
                }
                
                // Don't allow renaming the default folder
                if (oldFolderName == "default") {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Cannot rename the default folder"
                    )
                    return@launch
                }
                
                // Get all expenses in the old folder
                val allExpenses = repository.getAllExpenses()
                val folderExpenses = allExpenses.filter { it.folderName == oldFolderName }
                
                // Update each expense with the new folder name
                folderExpenses.forEach { expense ->
                    val updatedExpense = expense.copy(folderName = newFolderName)
                    repository.updateExpense(updatedExpense)
                }
                
                // If current folder is the renamed one, update it
                if (_currentFolder.value == oldFolderName) {
                    _currentFolder.value = newFolderName
                }
                
                // Clear folder stats cache
                folderStatsCache.clear()
                
                // Refresh folders
                refreshFolders()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Renamed folder from $oldFolderName to $newFolderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to rename folder: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Delete a folder and optionally move or delete its contents
     */
    fun deleteFolder(folderName: String, moveContentsToFolder: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Don't allow deleting the default folder
                if (folderName == "default") {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Cannot delete the default folder"
                    )
                    return@launch
                }
                
                // Get all expenses in the folder
                val allExpenses = repository.getAllExpenses()
                val folderExpenses = allExpenses.filter { it.folderName == folderName }
                
                if (moveContentsToFolder != null) {
                    // Move expenses to another folder
                    folderExpenses.forEach { expense ->
                        val updatedExpense = expense.copy(folderName = moveContentsToFolder)
                        repository.updateExpense(updatedExpense)
                    }
                    Log.d(TAG, "Moved ${folderExpenses.size} expenses from $folderName to $moveContentsToFolder")
                } else {
                    // Delete all expenses in the folder
                    folderExpenses.forEach { expense ->
                        repository.deleteExpense(expense.id)
                    }
                    Log.d(TAG, "Deleted ${folderExpenses.size} expenses from folder: $folderName")
                }
                
                // If current folder is the deleted one, reset to default
                if (_currentFolder.value == folderName) {
                    _currentFolder.value = "default"
                }
                
                // Clear folder stats cache
                folderStatsCache.clear()
                
                // Refresh folders
                refreshFolders()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                Log.d(TAG, "Deleted folder: $folderName")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete folder: ${e.message}"
                )
            }
        }
    }
    
    companion object {
        private const val TAG = "ExpenseViewModel"
    }
}

/**
 * UI state for expenses
 */
data class ExpenseUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) 