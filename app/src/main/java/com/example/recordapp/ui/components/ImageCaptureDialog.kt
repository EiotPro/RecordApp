package com.example.recordapp.ui.components

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.recordapp.R
import com.example.recordapp.util.OcrResult
import com.example.recordapp.util.FileUtils
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.util.UcropHelper
import java.io.File
import android.widget.Toast
import kotlinx.coroutines.launch
import android.content.Intent
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.core.content.FileProvider
import android.os.Build
import android.os.Bundle
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCaptureDialog(
    imageUri: Uri?,
    ocrResult: OcrResult?,
    folderName: String,
    onSave: (String, String, Double) -> Unit,
    onOcrRequest: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager.getInstance(context)
    val scope = rememberCoroutineScope()
    var serialNumber by remember { mutableStateOf(ocrResult?.serialNumber ?: "") }
    var amount by remember { mutableStateOf(if (ocrResult?.amount ?: 0.0 > 0) ocrResult?.amount.toString() else "") }
    var description by remember { mutableStateOf(ocrResult?.description ?: "") }
    
    // Rotation state for the image (keeping for compatibility but not showing rotation controls)
    var imageRotation by remember { mutableStateOf(0f) }
    
    // Current image URI (may be updated after cropping)
    var currentImageUri by remember { mutableStateOf(imageUri) }
    
    // State for full screen viewing
    var showFullScreenViewer by remember { mutableStateOf(false) }
    
    // Error state
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Receipt type for displaying appropriate icon and label - keep for backend functionality
    val receiptType = remember(ocrResult?.receiptType) {
        ocrResult?.receiptType ?: ""
    }
    
    // Update fields when OCR result changes
    LaunchedEffect(ocrResult) {
        ocrResult?.let {
            serialNumber = it.serialNumber
            if (it.amount > 0) {
                amount = it.amount.toString()
            }
            description = it.description
        }
    }
    
    // Update the crop launcher implementation
    // Launcher for image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val resultData = result.data
                if (resultData != null) {
                    // Log any extras for debugging
                    resultData.extras?.let { bundle ->
                        bundle.keySet().forEach { key ->
                            Log.d("ImageCaptureDialog", "Result extras - $key: ${bundle.get(key)}")
                        }
                    }
                    
                    // Get the output URI directly from UCrop
                    val resultUri = UCrop.getOutput(resultData)
                    
                    if (resultUri != null) {
                        Log.d("ImageCaptureDialog", "Successfully received cropped image URI: $resultUri")
                        
                        // Check if the URI is accessible
                        if (UcropHelper.isUriAccessible(context, resultUri)) {
                            // Update the UI immediately with the cropped image
                            currentImageUri = resultUri
                            
                            // Clear any previous error
                            errorMessage = null
                            
                            // Save the cropped image to the folder in the background
                            scope.launch {
                                try {
                                    // Show a toast to indicate saving
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Saving cropped image...", Toast.LENGTH_SHORT).show()
                                    }
                                    
                                    val savedUri = UcropHelper.saveCroppedImage(context, resultUri, folderName)
                                    
                                    // Update image URI to the saved version if successful
                                    if (savedUri != null) {
                                        withContext(Dispatchers.Main) {
                                            currentImageUri = savedUri
                                            // Since cropped image might have changed dimensions, reset rotation
                                            imageRotation = 0f
                                            
                                            // Show a confirmation toast
                                            Toast.makeText(context, "Image cropped successfully", Toast.LENGTH_SHORT).show()
                                            
                                            // Clear any error messages
                                            errorMessage = null
                                        }
                                        
                                        Log.d("ImageCaptureDialog", "Image cropped and saved successfully: $savedUri")
                                    } else {
                                        // If saving failed, still use the temporary URI but log the error
                                        Log.e("ImageCaptureDialog", "Failed to save cropped image to folder, using temporary URI")
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Image cropped but couldn't be saved permanently", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ImageCaptureDialog", "Exception when saving cropped image: ${e.message}", e)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error saving cropped image: ${e.message}", Toast.LENGTH_SHORT).show()
                                        errorMessage = "Error saving cropped image: ${e.message}"
                                    }
                                }
                            }
                        } else {
                            Log.e("ImageCaptureDialog", "Cropped image URI is not accessible: $resultUri")
                            errorMessage = "Cannot access cropped image. Please try again."
                            Toast.makeText(context, "Cannot access cropped image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("ImageCaptureDialog", "Failed to get cropped image result - output is null")
                        Toast.makeText(context, "Failed to crop image - no result returned", Toast.LENGTH_SHORT).show()
                        errorMessage = "Failed to get cropped image result - output is null"
                    }
                } else {
                    Log.e("ImageCaptureDialog", "Failed to get cropped image result - data is null")
                    Toast.makeText(context, "Failed to crop image - no data returned", Toast.LENGTH_SHORT).show()
                    errorMessage = "Failed to get cropped image result - data is null"
                }
            } catch (e: Exception) {
                Log.e("ImageCaptureDialog", "Error processing cropped image: ${e.message}", e)
                Toast.makeText(context, "Error processing cropped image", Toast.LENGTH_SHORT).show()
                errorMessage = "Error processing cropped image: ${e.message}"
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            try {
                val data = result.data
                if (data != null) {
                    val error = UcropHelper.getError(data)
                    Log.e("ImageCaptureDialog", "Image cropping error: ${error?.message}")
                    
                    // Log any extras for debugging
                    data.extras?.let { bundle ->
                        bundle.keySet().forEach { key ->
                            Log.d("ImageCaptureDialog", "Error extras - $key: ${bundle.get(key)}")
                        }
                    }
                    
                    Toast.makeText(context, "Crop error: ${error?.message ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
                    errorMessage = "Image cropping error: ${error?.message}"
                } else {
                    Log.e("ImageCaptureDialog", "Image cropping error: Unknown (data is null)")
                    Toast.makeText(context, "Unknown error during cropping", Toast.LENGTH_SHORT).show()
                    errorMessage = "Image cropping error: Unknown (data is null)"
                }
            } catch (e: Exception) {
                Log.e("ImageCaptureDialog", "Error getting crop error details: ${e.message}", e)
                Toast.makeText(context, "Error processing crop result", Toast.LENGTH_SHORT).show()
                errorMessage = "Error getting crop error details: ${e.message}"
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.d("ImageCaptureDialog", "Image cropping canceled by user")
            // User canceled, no need for a toast
        } else {
            Log.d("ImageCaptureDialog", "Image cropping returned unknown result code: ${result.resultCode}")
            Toast.makeText(context, "Unknown result from cropping", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Get appropriate icon for receipt type - keep for backend functionality
    val receiptIcon = remember(receiptType) {
        when (receiptType) {
            "PHYSICAL_RECEIPT" -> Icons.Default.Receipt
            "DIGITAL_PAYMENT" -> Icons.Default.CreditCard
            "UPI_PAYMENT" -> Icons.Default.QrCode
            else -> Icons.AutoMirrored.Filled.ReceiptLong
        }
    }
    
    // Get appropriate color for receipt type - keep for backend functionality
    val colorScheme = MaterialTheme.colorScheme
    val receiptColor = remember(receiptType, colorScheme) {
        when (receiptType) {
            "PHYSICAL_RECEIPT" -> colorScheme.tertiary
            "DIGITAL_PAYMENT" -> colorScheme.primary
            "UPI_PAYMENT" -> colorScheme.secondary
            else -> colorScheme.surfaceVariant
        }
    }
    
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        // Empty title - removed "Capture Expense Record" heading
                    },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cancel"
                            )
                        }
                    },
                    actions = {
                        // Crop image button
                        IconButton(onClick = {
                            val uri = currentImageUri
                            if (uri != null) {
                                try {
                                    // Clear any previous error
                                    errorMessage = null
                                    
                                    // Show a toast to indicate loading
                                    Toast.makeText(context, "Preparing image for cropping...", Toast.LENGTH_SHORT).show()
                                    
                                    // Log detailed information about the image URI
                                    try {
                                        val scheme = uri.scheme
                                        val authority = uri.authority
                                        val path = uri.path
                                        Log.d("ImageCaptureDialog", "Source image details - scheme: $scheme, authority: $authority, path: $path")
                                    } catch (e: Exception) {
                                        Log.e("ImageCaptureDialog", "Error logging URI details: ${e.message}")
                                    }
                                    
                                    // Check URI accessibility
                                    if (UcropHelper.isUriAccessible(context, uri)) {
                                        try {
                                            // Create crop intent and launch
                                            val cropIntent = UcropHelper.startCrop(context, uri)
                                            cropLauncher.launch(cropIntent)
                                        } catch (e: Exception) {
                                            Log.e("ImageCaptureDialog", "Error creating crop intent: ${e.message}", e)
                                            Toast.makeText(context, "Error starting crop: ${e.message}", Toast.LENGTH_SHORT).show()
                                            errorMessage = "Error launching crop: ${e.message}"
                                        }
                                    } else {
                                        Log.e("ImageCaptureDialog", "Cannot access image URI: $uri")
                                        Toast.makeText(context, "Cannot access the image", Toast.LENGTH_SHORT).show()
                                        errorMessage = "Cannot access image URI: $uri"
                                    }
                                } catch (e: Exception) {
                                    Log.e("ImageCaptureDialog", "Error launching crop: ${e.message}", e)
                                    Toast.makeText(context, "Error starting crop: ${e.message}", Toast.LENGTH_SHORT).show()
                                    errorMessage = "Error launching crop: ${e.message}"
                                }
                            } else {
                                Toast.makeText(context, "No image available to crop", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = "Crop Image"
                            )
                        }
                        
                        // Save button
                        IconButton(
                            onClick = { 
                                val amountValue = try { amount.toDouble() } catch (e: Exception) { 0.0 }
                                
                                // Check if we have a valid image URI
                                if (currentImageUri == null) {
                                    Toast.makeText(context, "No image available to save", Toast.LENGTH_SHORT).show()
                                    return@IconButton
                                }
                                
                                // Use currentImageUri which might contain the cropped image
                                onSave(serialNumber, description, amountValue)
                                Log.d("ImageCaptureDialog", "Saving expense to folder: $folderName with image: $currentImageUri")
                                Toast.makeText(context, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Display error message if any
                errorMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { errorMessage = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                // Enhanced image preview - taller and with rotation controls
                if (currentImageUri != null) {
                    // Track image loading state
                    var isImageError by remember { mutableStateOf(false) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp) // Increased height for better viewing
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (isImageError) {
                            // Display error state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Error loading image",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Failed to load image",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(currentImageUri)
                                    .crossfade(true)
                                    .diskCachePolicy(CachePolicy.DISABLED) // Disable disk cache to ensure fresh image
                                    .memoryCachePolicy(CachePolicy.DISABLED) // Disable memory cache to ensure fresh image
                                    .build(),
                                contentDescription = "Captured image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { showFullScreenViewer = true }
                                    .graphicsLayer(rotationZ = imageRotation),
                                contentScale = ContentScale.Fit, // Changed to Fit to show the entire image
                                onError = { 
                                    isImageError = true
                                    Log.e("ImageCaptureDialog", "Failed to load image: ${currentImageUri}")
                                }
                            )
                        }
                        
                        // Restore folder badge overlay
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(bottomEnd = 12.dp),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = folderName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        // Restore receipt type badge
                        if (receiptType.isNotEmpty()) {
                            Surface(
                                color = receiptColor.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(bottomStart = 12.dp),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = receiptIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(4.dp))
                                    
                                    val displayType = when(receiptType) {
                                        "PHYSICAL_RECEIPT" -> "Physical Receipt"
                                        "DIGITAL_PAYMENT" -> "Digital Payment"
                                        "UPI_PAYMENT" -> "UPI Transaction"
                                        else -> "Unknown Receipt"
                                    }
                                    
                                    Text(
                                        text = displayType,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                "No image selected",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Form inputs section
                Text(
                    text = "Expense Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Form inputs
                OutlinedTextField(
                    value = serialNumber,
                    onValueChange = { serialNumber = it },
                    label = { Text("Serial Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (${settingsManager.currencySymbol})") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CurrencyRupee,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.enter_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
    
    // Show full screen viewer if requested - Use currentImageUri which might have been updated after cropping
    if (showFullScreenViewer) {
        val displayUri = currentImageUri  // Renamed to avoid name shadowing with parameter imageUri
        if (displayUri != null) {
            FullScreenImageViewer(
                uri = displayUri,
                onDismiss = { showFullScreenViewer = false }
            )
        }
    }
} 