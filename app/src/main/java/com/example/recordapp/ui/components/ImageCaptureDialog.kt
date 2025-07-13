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
import com.example.recordapp.util.ImageCropperHelper
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.util.UCropWrapper
import java.io.File

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
    var serialNumber by remember { mutableStateOf(ocrResult?.serialNumber ?: "") }
    var amount by remember { mutableStateOf(if (ocrResult?.amount ?: 0.0 > 0) ocrResult?.amount.toString() else "") }
    var description by remember { mutableStateOf(ocrResult?.description ?: "") }
    
    // Rotation state for the image (keeping for compatibility but not showing rotation controls)
    var imageRotation by remember { mutableStateOf(0f) }
    
    // Current image URI (may be updated after cropping)
    var currentImageUri by remember { mutableStateOf(imageUri) }
    
    // State for full screen viewing
    var showFullScreenViewer by remember { mutableStateOf(false) }
    
    // State for showing edit menu
    var showEditOptions by remember { mutableStateOf(false) }
    
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
    
    // Create a destination URI for cropped image using helper
    val destinationUri = remember {
        ImageCropperHelper.createCropDestinationUri(context)
    }
    
    // Launcher for image cropping
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val resultData = result.data
                if (resultData != null) {
                    val resultUri = UCropWrapper.getOutput(resultData)
                    if (resultUri != null) {
                        // Save the cropped image to the folder
                        val savedUri = ImageCropperHelper.saveCroppedImage(context, resultUri, folderName)
                        
                        // Update image URI to the saved version
                        if (savedUri != null) {
                            currentImageUri = savedUri
                            Log.d("ImageCaptureDialog", "Image cropped and saved successfully: $savedUri")
                            
                            // Since cropped image might have changed dimensions, reset rotation
                            imageRotation = 0f
                        } else {
                            // If saving failed, still show the cropped image from cache
                            currentImageUri = resultUri
                            Log.e("ImageCaptureDialog", "Failed to save cropped image to folder, using temporary URI")
                        }
                    } else {
                        Log.e("ImageCaptureDialog", "Failed to get cropped image result - output is null")
                    }
                } else {
                    Log.e("ImageCaptureDialog", "Failed to get cropped image result - data is null")
                }
            } catch (e: Exception) {
                Log.e("ImageCaptureDialog", "Error processing cropped image: ${e.message}", e)
            }
        } else if (result.resultCode == UCropWrapper.RESULT_ERROR) {
            try {
                val data = result.data
                if (data != null) {
                    val error = UCropWrapper.getError(data)
                    Log.e("ImageCaptureDialog", "Image cropping error: ${error?.message}")
                } else {
                    Log.e("ImageCaptureDialog", "Image cropping error: Unknown (data is null)")
                }
            } catch (e: Exception) {
                Log.e("ImageCaptureDialog", "Error getting crop error details: ${e.message}", e)
            }
        } else {
            Log.d("ImageCaptureDialog", "Image cropping canceled or unknown result code: ${result.resultCode}")
        }
    }
    
    // Function to start image cropping
    val startCropActivity = { uri: Uri? ->
        if (uri != null) {
            try {
                // Get options using helper class
                val options = ImageCropperHelper.getDefaultCropOptions(settingsManager.imageCompression)
                
                // Launch UCrop with the configured options
                val cropIntent = UCropWrapper.of(uri, destinationUri)
                    .withOptions(options)
                    .getIntent(context)
                    
                cropLauncher.launch(cropIntent)
                showEditOptions = false
                
                Log.d("ImageCaptureDialog", "Launched crop activity with source URI: $uri and destination URI: $destinationUri")
            } catch (e: Exception) {
                Log.e("ImageCaptureDialog", "Failed to launch crop activity: ${e.message}", e)
            }
        } else {
            Log.e("ImageCaptureDialog", "Cannot crop null image URI")
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
                        // Edit image button
                        IconButton(onClick = { showEditOptions = !showEditOptions }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Image"
                            )
                        }
                        
                        // Hidden Scan button - removed as per requirement
                        // We're keeping the OCR functionality but not showing the button in UI
                        
                        IconButton(
                            onClick = { 
                                val amountValue = try { amount.toDouble() } catch (e: Exception) { 0.0 }
                                // Use currentImageUri which might contain the cropped image
                                onSave(serialNumber, description, amountValue) 
                                Log.d("ImageCaptureDialog", "Saving expense to folder: $folderName")
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
                // Enhanced image preview - taller and with rotation controls
                if (currentImageUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp) // Increased height for better viewing
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = currentImageUri,
                            contentDescription = "Captured image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { showFullScreenViewer = true }
                                .graphicsLayer(rotationZ = imageRotation),
                            contentScale = ContentScale.Fit // Changed to Fit to show the entire image
                        )
                        
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
                        
                        // Image rotation controls at the bottom - Removed as per requirement to only keep crop
                    }
                    
                    // Image editing options dropdown - Modified to only show crop option
                    if (showEditOptions) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                // Only Crop option - keeping just this option
                                EditOption(
                                    title = "Crop Image",
                                    icon = Icons.Default.Crop,
                                    onClick = {
                                        // Launch crop activity
                                        val uri = currentImageUri // Store in a local val to prevent smart cast issues
                                        startCropActivity(uri)
                                    }
                                )
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

@Composable
private fun EditOption(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
} 