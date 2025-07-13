package com.example.recordapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.recordapp.R

/**
 * Navigation destinations for the app
 */
sealed class Screen(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    // Splash screen - entry point
    object Splash : Screen(
        route = "splash",
        titleResId = R.string.app_name,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    // Authentication screens
    object Login : Screen(
        route = "login",
        titleResId = R.string.login,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
    
    object Signup : Screen(
        route = "signup",
        titleResId = R.string.signup,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
    
    // Main navigation items
    object Home : Screen(
        route = "home",
        titleResId = R.string.home,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    object Expenses : Screen(
        route = "expenses",
        titleResId = R.string.expense_list,
        selectedIcon = Icons.AutoMirrored.Filled.List,
        unselectedIcon = Icons.AutoMirrored.Outlined.List
    )
    
    object Settings : Screen(
        route = "settings",
        titleResId = R.string.settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    // Detail screens
    object ExpenseDetail : Screen(
        route = "expense_detail/{expenseId}",
        titleResId = R.string.expense_details,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    ) {
        fun createRoute(expenseId: String) = "expense_detail/$expenseId"
    }
    
    // Image management screen
    object ImageManagement : Screen(
        route = "image_management/{folderId}",
        titleResId = R.string.image_management,
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections
    ) {
        fun createRoute(folderId: String) = "image_management/$folderId"
    }
    
    // Profile route for user account - now accessed from settings
    object Profile : Screen(
        route = "profile",
        titleResId = R.string.profile,
        selectedIcon = Icons.Filled.AccountCircle,
        unselectedIcon = Icons.Outlined.AccountCircle
    )
} 