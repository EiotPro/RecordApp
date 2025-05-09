package com.example.recordapp.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.recordapp.R
import com.example.recordapp.ui.components.EnhancedSettingsItem
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.util.GridSize
import com.example.recordapp.util.SettingsManager
import com.example.recordapp.viewmodel.AuthViewModel
import com.example.recordapp.viewmodel.ExpenseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: ExpenseViewModel? = null,
    authViewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    val settings = SettingsManager.getInstance(context)
    
    // Get current user if available
    val currentUser = authViewModel?.currentUser?.collectAsState()
    
    // Get app version information safely
    val appVersion = remember { mutableStateOf("RecordApp 1.0.0") }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
            
            appVersion.value = "RecordApp ${packageInfo.versionName} ($versionCode)"
        } catch (e: Exception) {
            // Keep default version
        }
    }
    
    // State for dialog controls - reduced number of dialogs
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showGridDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    
    // State for settings - removed some unnecessary settings
    var currencySymbol by remember { mutableStateOf(settings.currencySymbol) }
    var defaultGrid by remember { mutableStateOf(getGridSizeFromString(settings.defaultGridSize)) }
    var pdfQuality by remember { mutableStateOf(settings.pdfQuality.toFloat()) }
    var theme by remember { mutableStateOf(settings.appTheme) }
    var storageLocation by remember { mutableStateOf(settings.storageLocation) }
    var sortOrder by remember { mutableStateOf(settings.expenseSortOrder) }
    var notificationsEnabled by remember { mutableStateOf(settings.notificationsEnabled) }
    var dateFormat by remember { mutableStateOf(settings.dateFormat) }
    
    // Map accent color to Color resource
    val accentColorResource = R.color.accent_blue
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(accentColorResource)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Account section (moved from ProfileScreen)
            if (authViewModel != null && currentUser != null) {
                AnimatedSettingsCategory(
                    title = "Account",
                    icon = Icons.Default.AccountCircle,
                    iconTint = colorResource(R.color.accent_blue)
                )
                
                // User profile information
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Profile picture
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // User name
                        Text(
                            text = currentUser.value?.name ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // User email
                        Text(
                            text = currentUser.value?.email ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Logout button
                        Button(
                            onClick = {
                                authViewModel.logout()
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Logout"
                            )
                            
                            Spacer(modifier = Modifier.size(8.dp))
                            
                            Text("Logout")
                        }
                    }
                }
            }
            
            // General settings
            AnimatedSettingsCategory(
                title = stringResource(R.string.settings_category_general),
                icon = Icons.Default.Settings,
                iconTint = colorResource(R.color.accent_blue)
            )
            
            // Currency setting
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_currency_symbol),
                description = stringResource(R.string.settings_currency_symbol_description),
                value = currencySymbol,
                icon = Icons.Default.AttachMoney,
                iconTint = colorResource(R.color.accent_green),
                onClick = { showCurrencyDialog = true }
            )
            
            // Sort order setting
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_expense_sort),
                description = stringResource(R.string.settings_expense_sort_description),
                value = when (sortOrder) {
                    "newest" -> stringResource(R.string.sort_newest)
                    "oldest" -> stringResource(R.string.sort_oldest)
                    "amount_high" -> stringResource(R.string.sort_amount_high)
                    "amount_low" -> stringResource(R.string.sort_amount_low)
                    else -> stringResource(R.string.sort_newest)
                },
                icon = Icons.AutoMirrored.Filled.Sort,
                iconTint = colorResource(R.color.accent_blue),
                onClick = { showSortDialog = true }
            )
            
            // Date format setting
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_date_format),
                description = stringResource(R.string.settings_date_format_description),
                value = dateFormat,
                icon = Icons.Default.DateRange,
                iconTint = colorResource(R.color.accent_orange),
                onClick = { showDateFormatDialog = true }
            )
            
            // Appearance settings - simplified
            AnimatedSettingsCategory(
                title = stringResource(R.string.settings_category_appearance),
                icon = Icons.Default.Palette,
                iconTint = colorResource(R.color.accent_purple)
            )
            
            // Theme setting
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_theme),
                description = stringResource(R.string.settings_theme_description),
                value = when (theme) {
                    "light" -> stringResource(R.string.theme_light)
                    "dark" -> stringResource(R.string.theme_dark)
                    else -> stringResource(R.string.theme_system)
                },
                icon = Icons.Default.DarkMode,
                iconTint = colorResource(R.color.accent_purple),
                onClick = { showThemeDialog = true }
            )
            
            // Notifications toggle
            EnhancedSettingsItem(
                title = "Notifications",
                description = "Enable or disable app notifications",
                icon = Icons.Default.Notifications,
                iconTint = colorResource(R.color.accent_red),
                isSwitch = true,
                isSwitchEnabled = notificationsEnabled,
                onClick = { /* Handled by switch */ },
                onSwitchChange = { newValue ->
                    notificationsEnabled = newValue
                    settings.notificationsEnabled = newValue
                }
            )
            
            // PDF & Export settings
            AnimatedSettingsCategory(
                title = stringResource(R.string.settings_category_export),
                icon = Icons.Default.PictureAsPdf,
                iconTint = colorResource(R.color.accent_red)
            )
            
            // Grid size setting
            EnhancedSettingsItem(
                title = "Grid Size",
                description = "Select the grid layout for PDF exports",
                value = defaultGrid.displayName,
                icon = Icons.Default.GridView,
                iconTint = colorResource(R.color.accent_blue),
                onClick = { showGridDialog = true }
            )
            
            // PDF Quality slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_pdf_quality),
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = stringResource(R.string.settings_pdf_quality_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Low",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Slider(
                        value = pdfQuality,
                        onValueChange = { newValue ->
                            pdfQuality = newValue
                            settings.pdfQuality = newValue.toInt()
                        },
                        modifier = Modifier.weight(1f),
                        valueRange = 50f..100f,
                        steps = 4
                    )
                    
                    Text(
                        text = "High",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${pdfQuality.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            // Storage settings
            AnimatedSettingsCategory(
                title = stringResource(R.string.settings_category_storage),
                icon = Icons.Default.Storage,
                iconTint = colorResource(R.color.accent_orange)
            )
            
            // Storage location
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_storage_location),
                description = stringResource(R.string.settings_storage_location_description),
                value = storageLocation,
                icon = Icons.Default.Folder,
                iconTint = colorResource(R.color.accent_orange),
                onClick = { showStorageDialog = true }
            )
            
            // Clear data button
            EnhancedSettingsItem(
                title = stringResource(R.string.settings_clear_data),
                description = stringResource(R.string.settings_clear_data_description),
                icon = Icons.Default.DeleteForever,
                iconTint = colorResource(R.color.accent_red),
                onClick = { showClearDataDialog = true }
            )
            
            // About section
            AnimatedSettingsCategory(
                title = stringResource(R.string.settings_category_about),
                icon = Icons.Default.Info,
                iconTint = colorResource(R.color.accent_blue)
            )
            
            // App version
            EnhancedSettingsItem(
                title = "App Version",
                description = appVersion.value,
                icon = Icons.Default.Android,
                iconTint = colorResource(R.color.accent_green),
                onClick = { /* No action */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    
    // Dialog implementations
    
    // Currency Dialog
    if (showCurrencyDialog) {
        SimpleSelectionDialog(
            title = "Currency Symbol",
            onDismiss = { showCurrencyDialog = false }
        ) {
            val currencies = listOf("$", "€", "£", "¥", "₹", "₽")
            currencies.forEach { symbol ->
                SimpleRadioOption(
                    text = symbol,
                    selected = currencySymbol == symbol,
                    onClick = {
                        currencySymbol = symbol
                        settings.currencySymbol = symbol
                        showCurrencyDialog = false
                    }
                )
            }
        }
    }
    
    // Theme Dialog
    if (showThemeDialog) {
        val themes = listOf(
            "light" to "Light",
            "dark" to "Dark",
            "system" to "System Default"
        )
        
        SimpleSelectionDialog(
            title = "Theme",
            onDismiss = { showThemeDialog = false }
        ) {
            themes.forEach { (value, label) ->
                SimpleRadioOption(
                    text = label,
                    selected = theme == value,
                    onClick = {
                        theme = value
                        settings.appTheme = value
                        showThemeDialog = false
                    }
                )
            }
        }
    }
    
    // Grid Size Dialog
    if (showGridDialog) {
        SimpleSelectionDialog(
            title = "Grid Layout",
            onDismiss = { showGridDialog = false }
        ) {
            GridSize.values().forEach { gridSize ->
                SimpleRadioOption(
                    text = gridSize.displayName,
                    selected = defaultGrid == gridSize,
                    onClick = {
                        defaultGrid = gridSize
                        settings.defaultGridSize = gridSize.name
                        showGridDialog = false
                    }
                )
            }
        }
    }
    
    // Sort Order Dialog
    if (showSortDialog) {
        val sortOptions = listOf(
            "newest" to stringResource(R.string.sort_newest),
            "oldest" to stringResource(R.string.sort_oldest),
            "amount_high" to stringResource(R.string.sort_amount_high),
            "amount_low" to stringResource(R.string.sort_amount_low)
        )
        
        SimpleSelectionDialog(
            title = "Sort Order",
            onDismiss = { showSortDialog = false }
        ) {
            sortOptions.forEach { (value, label) ->
                SimpleRadioOption(
                    text = label,
                    selected = sortOrder == value,
                    onClick = {
                        sortOrder = value
                        settings.expenseSortOrder = value
                        showSortDialog = false
                    }
                )
            }
        }
    }
    
    // Date Format Dialog
    if (showDateFormatDialog) {
        val formats = listOf(
            "yyyy-MM-dd" to "YYYY-MM-DD (2023-12-31)",
            "MM/dd/yyyy" to "MM/DD/YYYY (12/31/2023)",
            "dd/MM/yyyy" to "DD/MM/YYYY (31/12/2023)"
        )
        
        SimpleSelectionDialog(
            title = "Date Format",
            onDismiss = { showDateFormatDialog = false }
        ) {
            formats.forEach { (value, label) ->
                SimpleRadioOption(
                    text = label,
                    selected = dateFormat == value,
                    onClick = {
                        dateFormat = value
                        settings.dateFormat = value
                        showDateFormatDialog = false
                    }
                )
            }
        }
    }
    
    // Storage Location Dialog
    if (showStorageDialog) {
        val locations = listOf(
            "internal" to "Internal Storage",
            "external" to "External Storage"
        )
        
        SimpleSelectionDialog(
            title = "Storage Location",
            onDismiss = { showStorageDialog = false }
        ) {
            locations.forEach { (value, label) ->
                SimpleRadioOption(
                    text = label,
                    selected = storageLocation == value,
                    onClick = {
                        storageLocation = value
                        settings.storageLocation = value
                        showStorageDialog = false
                    }
                )
            }
        }
    }
    
    // Clear Data Confirmation Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { Text("This will delete all expense records and associated data. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Clear data action would go here
                        showClearDataDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Helper function to convert string to GridSize enum
 */
private fun getGridSizeFromString(gridSizeStr: String): GridSize {
    return try {
        GridSize.valueOf(gridSizeStr)
    } catch (e: Exception) {
        GridSize.ONE_BY_ONE // Default fallback
    }
}

@Composable
private fun AnimatedSettingsCategory(
    title: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
fun SimpleSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SimpleRadioOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(text)
    }
} 