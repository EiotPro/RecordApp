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
import com.example.recordapp.util.CsvUtils
import com.example.recordapp.util.FileUtils
import com.example.recordapp.util.OcrResult
import com.example.recordapp.util.OcrUtils
import com.example.recordapp.util.PdfUtils
import com.example.recordapp.model.GridSize
import com.example.recordapp.util.SettingsManager
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.recordapp.util.AppImageLoader
import com.example.recordapp.util.ZipUtils

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
        expenseDateTime: LocalDateTime = LocalDateTime.now(),
        compressionQuality: Int = FileUtils.DEFAULT_COMPRESSION_QUALITY,
        receiptType: String = "",
        generateRandomSerialIfBlank: Boolean = true
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
                
                // Handle serial number generation for blank entries
                val finalSerialNumber = if (serialNumber.isBlank() && generateRandomSerialIfBlank) {
                    val randomSerial = "GEN-" + UUID.randomUUID().toString().substring(0, 8)
                    Log.d(TAG, "Generated random serial number: $randomSerial for blank entry")
                    randomSerial
                } else {
                    serialNumber
                }
                
                // Create the expense with the current folder name and final serial number
                val expense = repository.addExpense(
                    imagePath = processedImageUri ?: imagePath,
                    timestamp = LocalDateTime.now(),
                    expenseDateTime = expenseDateTime,
                    serialNumber = finalSerialNumber,
                    amount = amount,
                    description = description,
                    folderName = folderName,
                    receiptType = receiptType
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
                
                // Debug log expenses before verification
                Log.d(TAG, "Generating PDF for ${allExpenses.size} expenses")
                allExpenses.forEachIndexed { index, expense ->
                    Log.d(TAG, "Pre-PDF Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                // Use original expenses without modifying serial numbers
                val verifiedExpenses = allExpenses
                

                
                val context = getApplication<Application>()
                // Using PdfUtils companion object method to generate PDF
                val pdfFile = PdfUtils.generateExpensesPdf(context, verifiedExpenses)
                
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
                // Using PdfUtils companion object method
                val pdfFile = PdfUtils.generateSingleExpensePdf(context, expense)
                
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
                
                // Log expenses for debugging
                Log.d(TAG, "Generating CSV for ${allExpenses.size} expenses")
                allExpenses.forEachIndexed { index, expense ->
                    Log.d(TAG, "Pre-CSV Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                val context = getApplication<Application>()
                // Using CsvUtils companion object method
                val csvFile = CsvUtils.generateExpensesCsv(context, allExpenses)
                
                if (csvFile != null && csvFile.exists() && csvFile.length() > 0) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )
                    onComplete(csvFile)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to generate CSV: Empty file created"
                    )
                    onComplete(null)
                }
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
                
                // Filter expenses for the specific folder
                val folderExpenses = if (folderName == "All") {
                    Log.d(TAG, "Using all expenses for 'All' folder CSV")
                    allExpenses  // For "All" folder, include all expenses
                } else {
                    val filteredExpenses = allExpenses.filter { it.folderName == folderName }
                    Log.d(TAG, "Found ${filteredExpenses.size} expenses in folder: $folderName for CSV")
                    filteredExpenses
                }
                
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found for specified folder")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No expenses found in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                val context = getApplication<Application>()
                
                // Using CsvUtils companion object method
                val csvFile = if (folderName == "All") {
                    // Use generateExpensesCsv for "All" folder
                    CsvUtils.generateExpensesCsv(context, folderExpenses)
                } else {
                    CsvUtils.generateCsvByFolder(context, allExpenses, folderName)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                
                onComplete(csvFile)
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
     * Get a specific expense by ID (reactive - updates when expense changes)
     */
    fun getExpenseById(id: String): Flow<Expense?> {
        return repository.getExpenseByIdAsFlow(id)
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
     * This is used for exporting all expenses regardless of folder
     */
    fun generateImageGridPdf(gridSize: GridSize, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val start = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    processingMessage = "Preparing grid PDF for all images"
                )
                
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
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
                        processingMessage = null,
                        errorMessage = "No expenses with images to export"
                    )
                    onComplete(null)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Processing ${expensesWithImages.size} images in grid layout"
                )
                
                // Use original expenses with images without modifying serial numbers
                val verifiedExpenses = expensesWithImages
                
                // Log expenses for image grid
                verifiedExpenses.forEachIndexed { index, expense ->
                    Log.d(TAG, "Image Grid Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                val context = getApplication<Application>()
                try {
                    // Use companion object method with withContext to run on IO thread
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfUtils.generateImageGridPdf(context, verifiedExpenses, gridSize)
                    }
                    
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        val end = System.currentTimeMillis()
                        val elapsedSeconds = (end - start) / 1000
                        
                        Log.d(TAG, "Generated grid PDF with ${verifiedExpenses.size} expenses. File: ${pdfFile.absolutePath}")
                        Log.d(TAG, "PDF generation completed in $elapsedSeconds seconds")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = null,
                            successMessage = "PDF generated successfully in $elapsedSeconds seconds"
                        )
                        
                        onComplete(pdfFile)
                    } else {
                        Log.e(TAG, "Grid PDF Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = "Failed to export grid PDF: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory error during PDF generation", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Not enough memory to generate PDF. Try reducing image quality in Settings."
                    )
                    onComplete(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error generating grid PDF", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Failed to generate grid PDF: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating grid PDF", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processingMessage = null,
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
     * This now uses the same grid layout system as folder grid PDFs
     */
    fun generatePdfByFolder(folderName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val start = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    processingMessage = "Preparing PDF for folder: $folderName"
                )
                
                Log.d(TAG, "Starting PDF generation for folder: $folderName")
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses to generate PDF - expenses list is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses to generate PDF"
                    )
                    onComplete(null)
                    return@launch
                }
                
                Log.d(TAG, "Found ${allExpenses.size} total expenses")
                // For "All" folder, include all expenses, otherwise filter by folderName
                val folderExpenses = if (folderName == "All") {
                    Log.d(TAG, "Using all expenses for 'All' folder")
                    allExpenses
                } else {
                    val filteredExpenses = allExpenses.filter { it.folderName == folderName }
                    Log.d(TAG, "Found ${filteredExpenses.size} expenses in folder: $folderName")
                    filteredExpenses
                }
                
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found for specified folder")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses found in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                // Filter expenses with images
                val expensesWithImages = folderExpenses.filter { it.imagePath != null }
                if (expensesWithImages.isEmpty()) {
                    Log.e(TAG, "No expenses with images found in folder: $folderName")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses with images in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Processing ${expensesWithImages.size} images for folder: $folderName"
                )
                
                // Use original folder expenses without modifying serial numbers
                val verifiedExpenses = folderExpenses
                
                // Get default grid size from settings
                val settings = SettingsManager.getInstance(getApplication<Application>())
                val gridSize = try {
                    GridSize.valueOf(settings.defaultGridSize)
                } catch (e: Exception) {
                    // Default to magazine layout for best presentation
                    GridSize.MAGAZINE_LAYOUT
                }
                
                // Log verified folder expenses for PDF
                Log.d(TAG, "Generating folder PDF for '$folderName' with ${verifiedExpenses.size} expenses using grid size: ${gridSize.name}")
                verifiedExpenses.forEachIndexed { index, expense ->
                    Log.d(TAG, "Folder PDF Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                val context = getApplication<Application>()
                
                try {
                    // Using PdfUtils companion object method with the grid layout
                    val pdfFile = withContext(Dispatchers.IO) {
                        if (folderName == "All") {
                            // For "All" folder, use the special grid layout that doesn't filter by folder name
                            PdfUtils.generateAllExpensesGridPdf(context, verifiedExpenses, gridSize)
                        } else {
                            // For specific folders, use the grid layout
                            PdfUtils.generateFolderGridPdf(context, verifiedExpenses, folderName, gridSize)
                        }
                    }
                    
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        val end = System.currentTimeMillis()
                        val elapsedSeconds = (end - start) / 1000
                        
                        Log.d(TAG, "PDF Export Success: ${pdfFile.absolutePath}")
                        Log.d(TAG, "PDF file exists: ${pdfFile.exists()}, size: ${pdfFile.length()} bytes")
                        Log.d(TAG, "PDF generation completed in $elapsedSeconds seconds")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = null,
                            successMessage = "PDF generated successfully in $elapsedSeconds seconds"
                        )
                        onComplete(pdfFile)
                    } else {
                        Log.e(TAG, "PDF Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = "Failed to export PDF: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory error during PDF generation", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Not enough memory to generate PDF. Try reducing image quality in Settings."
                    )
                    onComplete(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in PDF generation process for folder: $folderName", e)
                    Log.e(TAG, "Expense count: ${verifiedExpenses.size}, Grid size: ${gridSize.name}")
                    
                    // Log more details about the exception
                    e.printStackTrace()
                    
                    // Check if there are any specific issues with the expenses
                    try {
                        val hasExpensesWithNoImage = verifiedExpenses.any { it.imagePath == null }
                        val hasExpensesWithNoSerialNumber = verifiedExpenses.any { it.serialNumber.isBlank() }
                        
                        Log.e(TAG, "Diagnostics: hasExpensesWithNoImage=$hasExpensesWithNoImage, " +
                              "hasExpensesWithNoSerialNumber=$hasExpensesWithNoSerialNumber")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Error during diagnostics", e2)
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "PDF generation error: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating PDF for folder: $folderName", e)
                
                // Log more details about the setup process
                try {
                    val folderExpenses = if (folderName == "All") {
                        "All expenses count: ${repository.getAllExpenses().size}"
                    } else {
                        "Folder expenses count: ${repository.getAllExpenses().count { it.folderName == folderName }}"
                    }
                    Log.e(TAG, folderExpenses)
                } catch (e2: Exception) {
                    Log.e(TAG, "Error getting expense count", e2)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processingMessage = null,
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
     * Update the expense date/time for a specific expense
     */
    fun updateExpenseDateTime(expenseId: String, newDateTime: LocalDateTime) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val expense = repository.getExpenseById(expenseId)
                if (expense != null) {
                    val updatedExpense = expense.copy(expenseDateTime = newDateTime)
                    repository.updateExpense(updatedExpense)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null
                    )

                    // Note: No need to manually refresh - the Flow will automatically emit the updated expense
                } else {
                    Log.e(TAG, "Expense not found: $expenseId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating expense date/time", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update expense date/time: ${e.message}"
                )
            }
        }
    }

    /**
     * Add a new expense with an image
     */
    fun addExpenseWithImage(
        description: String = "",
        amount: Double = 0.0,
        folderName: String = "default",
        imageUri: Uri? = null,
        serialNumber: String = "",
        expenseDateTime: LocalDateTime = LocalDateTime.now(),
        compressionQuality: Int? = null,
        generateRandomSerialIfBlank: Boolean = true
    ) {
        // Get the receipt type from OCR result if available
        val receiptType = _ocrResult.value?.receiptType ?: ""
        
        // Use compression setting from settings manager if not specified
        val compQuality = compressionQuality ?: SettingsManager.getInstance(getApplication()).imageCompression
        
        addExpense(
            description = description,
            amount = amount,
            folderName = folderName,
            imagePath = imageUri,
            serialNumber = serialNumber,
            expenseDateTime = expenseDateTime,
            compressionQuality = compQuality,
            receiptType = receiptType,
            generateRandomSerialIfBlank = generateRandomSerialIfBlank
        )
    }
    
    /**
     * Format currency value based on locale settings
     */
    fun formatCurrency(amount: Double): String {
        val settingsManager = SettingsManager.getInstance(getApplication())
        return settingsManager.formatAmount(amount)
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
    
    /**
     * Get all images in a folder
     */
    fun getFolderImages(folderName: String): Flow<List<Uri>> = flow {
        try {
            val allExpenses = repository.getAllExpenses()
            val folderExpenses = allExpenses.filter { it.folderName == folderName }
            
            // Filter expenses with images and extract their image URIs
            val imageUris = folderExpenses
                .filter { it.imagePath != null }
                .sortedWith(compareBy { it.displayOrder })  // Ensure consistent ordering by displayOrder
                .map { it.imagePath!! }
            
            Log.d(TAG, "Retrieved ${imageUris.size} images from folder $folderName (sorted by displayOrder)")
            emit(imageUris)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting folder images", e)
            emit(emptyList())
        }
    }
    
    /**
     * Reorder images in a folder
     */
    fun reorderFolderImages(folderName: String, newOrder: List<Uri>) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                Log.d(TAG, "Reordering images in folder: $folderName, new order size: ${newOrder.size}")
                
                // Skip processing if new order is empty
                if (newOrder.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                val allExpenses = repository.getAllExpenses()
                val folderExpenses = allExpenses.filter { it.folderName == folderName }
                
                // Create a mapping of image URI to expense
                val imageToExpense = folderExpenses
                    .filter { it.imagePath != null }
                    .associateBy { it.imagePath!! }
                
                // Log info about items being reordered
                Log.d(TAG, "Found ${imageToExpense.size} expenses with images in folder")
                
                // Validate if all URIs in the new order exist in the folder
                val missingUris = newOrder.filter { !imageToExpense.containsKey(it) }
                if (missingUris.isNotEmpty()) {
                    Log.e(TAG, "Error reordering: ${missingUris.size} URIs not found in folder expenses")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Some images could not be reordered. Please try again."
                    )
                    return@launch
                }
                
                // Update display order for all images in the new order
                newOrder.forEachIndexed { index, uri ->
                    val expense = imageToExpense[uri]
                    if (expense != null) {
                        // Create updated expense with new display order
                        val updatedExpense = expense.copy(
                            displayOrder = index
                        )
                        
                        // Update in the repository
                        repository.updateExpense(updatedExpense)
                        Log.d(TAG, "Updated expense ${expense.id}, new order: $index")
                    }
                }
                
                // Also update displayOrder for expenses without images to put them at the end
                val nonImageExpenses = folderExpenses.filter { it.imagePath == null }
                val startOrderForNonImages = newOrder.size
                
                nonImageExpenses.forEachIndexed { index, expense ->
                    val updatedExpense = expense.copy(
                        displayOrder = startOrderForNonImages + index
                    )
                    repository.updateExpense(updatedExpense)
                }
                
                // Force refresh of the UI to show updated order
                refreshExpenseLists()
                
                Log.d(TAG, "Successfully reordered ${newOrder.size} images in folder: $folderName")
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error reordering folder images", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to reorder images: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Force refresh of expense lists
     */
    private fun refreshExpenseLists() {
        // Clear folder stats cache to force refresh
        folderStatsCache.clear()
        
        // Refresh folders
        refreshFolders()
        
        // Trigger a refresh of the paged expenses by briefly changing the selected folder
        val currentFolder = _selectedViewFolder.value
        viewModelScope.launch {
            // Toggle to null and back to force refresh
            _selectedViewFolder.value = null
            delay(100)  // Short delay
            _selectedViewFolder.value = currentFolder
        }
    }
    
    /**
     * Get paginated folder images for more efficient loading of large collections
     */
    fun getPaginatedFolderImages(folderName: String, pageSize: Int = 20): Flow<PagingData<Uri>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = true,
                maxSize = pageSize * 3
            )
        ) {
            object : PagingSource<Int, Uri>() {
                override suspend fun load(params: LoadParams<Int>): androidx.paging.PagingSource.LoadResult<Int, Uri> {
                    val page = params.key ?: 0
                    
                    return try {
                        // Get all expenses for the folder
                        val allExpenses = repository.getAllExpenses()
                        val folderExpenses = allExpenses.filter { it.folderName == folderName }
                        
                        // Filter expenses with images, extract URIs, and sort by display order
                        val imageUris = folderExpenses
                            .filter { it.imagePath != null }
                            .sortedWith(compareBy { it.displayOrder })  // Ensure consistent ordering
                            .map { it.imagePath!! }
                        
                        Log.d(TAG, "Paginated loading for folder $folderName: ${imageUris.size} images (sorted by displayOrder)")
                        
                        // Calculate page data
                        val startIndex = page * params.loadSize
                        val endIndex = min(startIndex + params.loadSize, imageUris.size)
                        
                        // Return paginated results
                        if (startIndex < imageUris.size) {
                            val pagedData = imageUris.subList(startIndex, endIndex)
                            val nextKey = if (endIndex < imageUris.size) page + 1 else null
                            Log.d(TAG, "Returning page $page with ${pagedData.size} images")
                            
                            androidx.paging.PagingSource.LoadResult.Page(
                                data = pagedData,
                                prevKey = if (page > 0) page - 1 else null,
                                nextKey = nextKey
                            )
                        } else {
                            Log.d(TAG, "No more pages available for folder $folderName")
                            androidx.paging.PagingSource.LoadResult.Page(
                                data = emptyList(),
                                prevKey = if (page > 0) page - 1 else null,
                                nextKey = null
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading paginated folder images", e)
                        androidx.paging.PagingSource.LoadResult.Error(e)
                    }
                }
                
                override fun getRefreshKey(state: PagingState<Int, Uri>): Int? {
                    return state.anchorPosition?.let { anchorPosition ->
                        val anchorPage = state.closestPageToPosition(anchorPosition)
                        anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
                    }
                }
            }
        }.flow
    }
    
    /**
     * Delete an image from a folder
     */
    fun deleteImageFromFolder(folderName: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                Log.d(TAG, "Deleting image from folder: $folderName, URI: $imageUri")
                
                val allExpenses = repository.getAllExpenses()
                val expenseToDelete = allExpenses.find { 
                    it.folderName == folderName && it.imagePath == imageUri 
                }
                
                if (expenseToDelete != null) {
                    // Delete the expense with this image
                    repository.deleteExpense(expenseToDelete.id)
                    
                    // Force refresh of the UI to show updated list
                    refreshExpenseLists()
                    
                    Log.d(TAG, "Successfully deleted image from folder: $folderName")
                } else {
                    Log.e(TAG, "Image not found in folder: $folderName, URI: $imageUri")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Image not found in folder"
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting image from folder", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete image: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Get paginated expenses (with images) for a folder
     */
    fun getPaginatedFolderExpenses(folderName: String, pageSize: Int = 20): Flow<PagingData<Expense>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = true,
                maxSize = pageSize * 3
            ),
            pagingSourceFactory = {
                object : PagingSource<Int, Expense>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Expense> {
                        val page = params.key ?: 0
                        return try {
                            val allExpenses = repository.getAllExpenses()
                            val folderExpenses = allExpenses.filter { it.folderName == folderName && it.imagePath != null }
                                .sortedWith(compareBy { it.displayOrder })
                            val startIndex = page * params.loadSize
                            val endIndex = kotlin.math.min(startIndex + params.loadSize, folderExpenses.size)
                            val pagedData = if (startIndex < folderExpenses.size) folderExpenses.subList(startIndex, endIndex) else emptyList()
                            val nextKey = if (endIndex < folderExpenses.size) page + 1 else null
                            LoadResult.Page(
                                data = pagedData,
                                prevKey = if (page > 0) page - 1 else null,
                                nextKey = nextKey
                            )
                        } catch (e: Exception) {
                            LoadResult.Error(e)
                        }
                    }
                    override fun getRefreshKey(state: PagingState<Int, Expense>): Int? {
                        return state.anchorPosition?.let { anchorPosition ->
                            val anchorPage = state.closestPageToPosition(anchorPosition)
                            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
                        }
                    }
                }
            }
        ).flow
    }
    
    /**
     * Generate PDF with all expenses in a specific folder with grid layout
     */
    fun generateFolderGridPdf(folderName: String, gridSize: GridSize, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val start = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    processingMessage = "Preparing grid PDF for folder: $folderName"
                )
                
                Log.d(TAG, "Starting Grid PDF generation for folder: $folderName with grid size: ${gridSize.name}")
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses to generate PDF - expenses list is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses to generate PDF"
                    )
                    onComplete(null)
                    return@launch
                }
                
                Log.d(TAG, "Found ${allExpenses.size} total expenses")
                val folderExpenses = allExpenses.filter { it.folderName == folderName }
                Log.d(TAG, "Found ${folderExpenses.size} expenses in folder: $folderName")
                
                // Filter expenses with images
                val expensesWithImages = folderExpenses.filter { it.imagePath != null }
                if (expensesWithImages.isEmpty()) {
                    Log.e(TAG, "No expenses with images found in folder: $folderName")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses with images in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Processing ${expensesWithImages.size} images in grid layout"
                )
                
                // Log verified folder expenses for PDF
                Log.d(TAG, "Generating folder grid PDF for '$folderName' with ${expensesWithImages.size} expenses")
                expensesWithImages.forEachIndexed { index, expense ->
                    Log.d(TAG, "Folder Grid PDF Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                val context = getApplication<Application>()
                
                try {
                    // Using PdfUtils companion object method
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfUtils.generateFolderGridPdf(context, allExpenses, folderName, gridSize)
                    }
                    
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        val end = System.currentTimeMillis()
                        val elapsedSeconds = (end - start) / 1000
                        
                        Log.d(TAG, "Grid PDF Export Success: ${pdfFile.absolutePath}")
                        Log.d(TAG, "Grid PDF file exists: ${pdfFile.exists()}, size: ${pdfFile.length()} bytes")
                        Log.d(TAG, "PDF generation completed in $elapsedSeconds seconds")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = null,
                            successMessage = "PDF generated successfully in $elapsedSeconds seconds"
                        )
                        onComplete(pdfFile)
                    } else {
                        Log.e(TAG, "Grid PDF Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = "Failed to export grid PDF: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory error during PDF generation", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Not enough memory to generate PDF. Try reducing image quality in Settings."
                    )
                    onComplete(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in grid PDF generation process", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Grid PDF generation error: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating grid PDF for folder: $folderName", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processingMessage = null,
                    errorMessage = "Failed to generate grid PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Generate PDF with all expenses in a specific folder with list layout
     */
    fun generateFolderListPdf(folderName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val start = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    processingMessage = "Preparing list PDF for folder: $folderName"
                )
                
                Log.d(TAG, "Starting List PDF generation for folder: $folderName")
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses to generate PDF - expenses list is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses to generate PDF"
                    )
                    onComplete(null)
                    return@launch
                }
                
                Log.d(TAG, "Found ${allExpenses.size} total expenses")
                val folderExpenses = allExpenses.filter { it.folderName == folderName }
                Log.d(TAG, "Found ${folderExpenses.size} expenses in folder: $folderName")
                
                // Filter expenses with images
                val expensesWithImages = folderExpenses.filter { it.imagePath != null }
                if (expensesWithImages.isEmpty()) {
                    Log.e(TAG, "No expenses with images found in folder: $folderName")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses with images in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Processing ${expensesWithImages.size} images in list format"
                )
                
                // Log folder expenses for PDF
                Log.d(TAG, "Generating folder list PDF for '$folderName' with ${expensesWithImages.size} expenses")
                expensesWithImages.forEachIndexed { index, expense ->
                    Log.d(TAG, "Folder List PDF Expense #$index: serialNumber=${expense.serialNumber}, id=${expense.id}")
                }
                
                val context = getApplication<Application>()
                
                try {
                    // Using PdfUtils companion object method
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfUtils.generateFolderListPdf(context, allExpenses, folderName)
                    }
                    
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0) {
                        val end = System.currentTimeMillis()
                        val elapsedSeconds = (end - start) / 1000
                        
                        Log.d(TAG, "List PDF Export Success: ${pdfFile.absolutePath}")
                        Log.d(TAG, "List PDF file exists: ${pdfFile.exists()}, size: ${pdfFile.length()} bytes")
                        Log.d(TAG, "PDF generation completed in $elapsedSeconds seconds")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = null,
                            successMessage = "List PDF generated successfully in $elapsedSeconds seconds"
                        )
                        onComplete(pdfFile)
                    } else {
                        Log.e(TAG, "List PDF Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = "Failed to export list PDF: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory error during PDF generation", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Not enough memory to generate PDF. Try reducing image quality in Settings."
                    )
                    onComplete(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in list PDF generation process", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "List PDF generation error: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating list PDF for folder: $folderName", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processingMessage = null,
                    errorMessage = "Failed to generate list PDF: ${e.message}"
                )
                onComplete(null)
            }
        }
    }
    
    /**
     * Rotate the image of an expense
     */
    fun rotateExpenseImage(expenseId: String, degrees: Float) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Get the expense
                val expense = repository.getExpenseById(expenseId)
                if (expense == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Expense not found"
                    )
                    return@launch
                }
                
                // Check if expense has an image
                val imagePath = expense.imagePath
                if (imagePath == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No image to rotate"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Rotating image for expense $expenseId by $degrees degrees")
                
                // Get settings manager for compression quality
                val settingsManager = SettingsManager.getInstance(getApplication())
                val compressionQuality = settingsManager.imageCompression
                
                // Rotate the image
                val rotatedImageUri = FileUtils.rotateImage(
                    getApplication(),
                    imagePath,
                    degrees,
                    compressionQuality
                )
                
                if (rotatedImageUri == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to rotate image"
                    )
                    return@launch
                }

                // CRITICAL: Update the expense with new imageModifiedTimestamp to force UI refresh
                Log.d(TAG, "Updating expense with new imageModifiedTimestamp...")
                val updatedExpense = expense.copy(
                    imageModifiedTimestamp = System.currentTimeMillis()
                )
                repository.updateExpense(updatedExpense)

                // ENHANCED CACHE CLEARING - Clear all caches to force reload
                Log.d(TAG, "Clearing image caches for rotated image...")

                // Clear cache for the specific URI
                AppImageLoader.clearCacheForUri(getApplication(), rotatedImageUri)

                // Also clear cache for the original image path (in case it's the same)
                AppImageLoader.clearCacheForUri(getApplication(), imagePath)

                // Clear all caches as a fallback
                AppImageLoader.clearCaches()

                // Force garbage collection to ensure memory is freed
                System.gc()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )

                // Force refresh of expense lists to show the rotated image
                refreshExpenseLists()

                // Additional refresh after a short delay to ensure UI updates
                viewModelScope.launch {
                    kotlinx.coroutines.delay(500) // Wait 500ms
                    refreshExpenseLists()
                }

                Log.d(TAG, "Successfully rotated image for expense $expenseId")
            } catch (e: Exception) {
                Log.e(TAG, "Error rotating expense image", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to rotate image: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Generate ZIP export with PDF, CSV, and Grid for all expenses or a specific folder
     * @param folderName Folder name or "All" for all expenses
     * @param onComplete Callback with the generated ZIP file or null if generation failed
     */
    fun generateZipExport(folderName: String, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val start = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    processingMessage = "Preparing ZIP export for folder: $folderName"
                )
                
                Log.d(TAG, "Starting ZIP export for folder: $folderName")
                val allExpenses = repository.getAllExpenses()
                
                if (allExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses to generate ZIP - expenses list is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses to generate ZIP export"
                    )
                    onComplete(null)
                    return@launch
                }
                
                Log.d(TAG, "Found ${allExpenses.size} total expenses")
                // For "All" folder, include all expenses, otherwise filter by folderName
                val folderExpenses = if (folderName == "All") {
                    Log.d(TAG, "Using all expenses for 'All' folder")
                    allExpenses
                } else {
                    val filteredExpenses = allExpenses.filter { it.folderName == folderName }
                    Log.d(TAG, "Found ${filteredExpenses.size} expenses in folder: $folderName")
                    filteredExpenses
                }
                
                if (folderExpenses.isEmpty()) {
                    Log.e(TAG, "No expenses found for specified folder")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "No expenses found in folder"
                    )
                    onComplete(null)
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    processingMessage = "Generating ZIP with PDF, CSV, and Grid exports for folder: $folderName"
                )
                
                val context = getApplication<Application>()
                
                try {
                    // Using ZipUtils to generate the ZIP file with all export types
                    val zipUri = withContext(Dispatchers.IO) {
                        ZipUtils.generateZipExport(context, folderExpenses, folderName)
                    }
                    
                    // Get the file from URI
                    val zipFile = FileUtils.getFileFromUri(context, zipUri)
                    
                    if (zipFile != null && zipFile.exists() && zipFile.length() > 0) {
                        val end = System.currentTimeMillis()
                        val elapsedSeconds = (end - start) / 1000
                        
                        Log.d(TAG, "ZIP Export Success: ${zipFile.absolutePath}")
                        Log.d(TAG, "ZIP file exists: ${zipFile.exists()}, size: ${zipFile.length()} bytes")
                        Log.d(TAG, "ZIP generation completed in $elapsedSeconds seconds")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = null,
                            successMessage = "ZIP export generated successfully in $elapsedSeconds seconds"
                        )
                        onComplete(zipFile)
                    } else {
                        Log.e(TAG, "ZIP Export Failed: File was null or empty")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            processingMessage = null,
                            errorMessage = "Failed to generate ZIP export: Empty file created"
                        )
                        onComplete(null)
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "Out of memory error during ZIP generation", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "Not enough memory to generate ZIP. Try reducing image quality in Settings."
                    )
                    onComplete(null)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ZIP generation process", e)
                    e.printStackTrace()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        processingMessage = null,
                        errorMessage = "ZIP generation error: ${e.message}"
                    )
                    onComplete(null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating ZIP for folder: $folderName", e)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    processingMessage = null,
                    errorMessage = "Failed to generate ZIP: ${e.message}"
                )
                onComplete(null)
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
    val errorMessage: String? = null,
    val processingMessage: String? = null,
    val successMessage: String? = null
) 