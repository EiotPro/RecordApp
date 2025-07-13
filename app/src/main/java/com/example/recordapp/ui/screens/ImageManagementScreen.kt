package com.example.recordapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.paging.PagingData
import com.example.recordapp.ui.components.FullScreenImageViewer
import com.example.recordapp.ui.components.SwitchablePaginatedLayout
import com.example.recordapp.ui.components.ViewModeToggle
import com.example.recordapp.viewmodel.ExpenseViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageManagementScreen(
    folderId: String,
    viewModel: ExpenseViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Snackbar host state
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Get the list of images in the folder
    val folderImages = viewModel.getFolderImages(folderId).collectAsState(initial = emptyList())
    
    // Get paginated images for efficient loading
    val paginatedImages: Flow<PagingData<Uri>> = remember(folderId) {
        viewModel.getPaginatedFolderImages(folderId)
    }
    
    // States for tracking images and their ordering
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentImageOrder by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    
    // Observe UI state for loading
    val uiState by viewModel.uiState.collectAsState()
    
    // State for view mode
    var isGridView by remember { mutableStateOf(true) }
    
    // State for full screen image viewing
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Initialize image URIs from folder when component loads or when folderImages changes
    LaunchedEffect(folderImages.value) {
        if (folderImages.value.isNotEmpty()) {
            imageUris = folderImages.value.toList()
            currentImageOrder = folderImages.value.toList()
        }
    }
    
    // Show error message if any
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            scope.launch {
            snackbarHostState.showSnackbar(
                message = error,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Long
            )
            viewModel.clearErrorMessage()
        }
    }
    }
    
    // Function to track image order changes without saving them
    val trackChanges: (List<Uri>) -> Unit = { newOrder ->
        currentImageOrder = newOrder
        hasUnsavedChanges = true
    }

    // Function to save changes
    val saveChanges: () -> Unit = {
        scope.launch {
            viewModel.reorderFolderImages(folderId, currentImageOrder)
            snackbarHostState.showSnackbar(
                message = "Image order saved successfully",
                duration = SnackbarDuration.Short
            )
            hasUnsavedChanges = false
        }
    }
    
    // Function to handle image click for viewing
    val handleImageClick: (Uri) -> Unit = { uri ->
        selectedImageUri = uri
    }
    
    // Show full screen image viewer if an image is selected
    selectedImageUri?.let { uri ->
        FullScreenImageViewer(
            uri = uri,
            onDismiss = { selectedImageUri = null }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Manage Images",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize * 0.9f
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Add view mode toggle in top bar
                    ViewModeToggle(
                        isGridView = isGridView,
                        onViewModeChange = { isGridView = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    IconButton(
                        onClick = saveChanges,
                        enabled = hasUnsavedChanges && !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Save Changes"
                        )
                    }
                },
                modifier = Modifier.height(52.dp) // Smaller TopAppBar height
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (folderImages.value.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No images in this folder",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                SwitchablePaginatedLayout(
                    images = paginatedImages,
                    onImageClick = handleImageClick,
                    onImagesReordered = trackChanges,
                    modifier = Modifier.fillMaxSize(),
                    showViewModeToggle = false, // Don't show toggle in the layout, we show it in the app bar
                    isGridView = isGridView
                )
            }
        }
    }
} 