package com.example.recordapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.recordapp.ui.components.AnimatedWelcomeText
import com.example.recordapp.ui.components.AppBottomNavigation
import com.example.recordapp.ui.navigation.AppNavigation
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.ui.navigation.getStartDestination
import com.example.recordapp.ui.theme.AppTheme
import com.example.recordapp.viewmodel.AuthViewModel
import com.example.recordapp.viewmodel.ExpenseViewModel
import com.example.recordapp.util.PermissionUtils

class MainActivity : ComponentActivity() {
    
    private val expenseViewModel: ExpenseViewModel by viewModels()
    
    // Add permission request constants
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request necessary permissions
        checkAndRequestPermissions()
        
        enableEdgeToEdge()
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent(expenseViewModel)
                }
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = PermissionUtils.getRequiredPermissions()
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE)
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (!allGranted) {
                // Show a toast message about missing permissions
                Toast.makeText(
                    this,
                    getString(R.string.storage_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

@Composable
fun MainContent(expenseViewModel: ExpenseViewModel) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    
    // Determine start destination based on auth state
    val startDestination = getStartDestination(authViewModel)
    
    // Is the user logged in?
    val uiState by authViewModel.uiState.collectAsState()
    val isAuthenticated = uiState.isLoggedIn
    
    Scaffold(
        bottomBar = {
            // Only show bottom navigation when user is authenticated
            if (isAuthenticated) {
                AppBottomNavigation(navController = navController)
            }
        }
    ) { innerPadding ->
        AppNavigation(
            expenseViewModel = expenseViewModel,
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        )
    }
}