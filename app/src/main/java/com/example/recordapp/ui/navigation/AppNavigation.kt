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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.recordapp.ui.screens.DashboardScreen
import com.example.recordapp.ui.screens.ExpenseDetailScreen
import com.example.recordapp.ui.screens.ExpensesScreen
import com.example.recordapp.ui.screens.HomeScreen
import com.example.recordapp.ui.screens.ImageManagementScreen
import com.example.recordapp.ui.screens.LoginScreen
import com.example.recordapp.ui.screens.ProfileScreen
import com.example.recordapp.ui.screens.SettingsScreen
import com.example.recordapp.ui.screens.SignupScreen
import com.example.recordapp.ui.screens.SplashScreen
import com.example.recordapp.viewmodel.AuthViewModel
import com.example.recordapp.viewmodel.ExpenseViewModel
import com.example.recordapp.network.InternetConnectionChecker
import javax.inject.Inject

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
    startDestination: String = Screen.Splash.route,
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
        // Splash screen
        composable(Screen.Splash.route) {
            val isLoggedIn by authViewModel.uiState.collectAsState()
            SplashScreen(
                navController = navController,
                isUserLoggedIn = isLoggedIn.isLoggedIn
            )
        }
        
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
        
        // Image Management screen
        composable(
            route = Screen.ImageManagement.route,
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            ImageManagementScreen(
                folderId = folderId,
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
 * Always returns Splash screen as the start destination
 */
@Composable
fun getStartDestination(authViewModel: AuthViewModel): String {
    return Screen.Splash.route
}