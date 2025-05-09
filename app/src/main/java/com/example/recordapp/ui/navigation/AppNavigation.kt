package com.example.recordapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.recordapp.ui.screens.DashboardScreen
import com.example.recordapp.ui.screens.ExpenseDetailScreen
import com.example.recordapp.ui.screens.ExpensesScreen
import com.example.recordapp.ui.screens.HomeScreen
import com.example.recordapp.ui.screens.LoginScreen
import com.example.recordapp.ui.screens.ProfileScreen
import com.example.recordapp.ui.screens.SettingsScreen
import com.example.recordapp.ui.screens.SignupScreen
import com.example.recordapp.viewmodel.AuthViewModel
import com.example.recordapp.viewmodel.ExpenseViewModel

/**
 * List of main navigation items
 */
val navigationItems = listOf(
    Screen.Home,
    Screen.Expenses,
    Screen.Settings
)

/**
 * Main navigation graph for the app
 */
@Composable
fun AppNavigation(
    expenseViewModel: ExpenseViewModel,
    navController: NavHostController,
    startDestination: String = Screen.Login.route,
    modifier: Modifier = Modifier
) {
    // Create the auth view model
    val authViewModel: AuthViewModel = viewModel()
    
    // Create the navigation host
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Authentication screens
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
        
        // Main screens
        composable(Screen.Home.route) {
            DashboardScreen(
                viewModel = expenseViewModel,
                navController = navController
            )
        }
        
        composable(Screen.Expenses.route) {
            ExpensesScreen(
                viewModel = expenseViewModel,
                navController = navController
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                viewModel = expenseViewModel,
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = expenseViewModel,
                navController = navController
            )
        }
        
        // Profile screen - accessed from settings
        composable(Screen.Profile.route) {
            ProfileScreen(
                navController = navController,
                viewModel = authViewModel
            )
        }
    }
}

/**
 * Check if user is logged in and return the appropriate start destination
 */
@Composable
fun getStartDestination(authViewModel: AuthViewModel): String {
    val isLoggedIn by authViewModel.uiState.collectAsState()
    return if (isLoggedIn.isLoggedIn) Screen.Home.route else Screen.Login.route
}