package com.example.recordapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.recordapp.R
import com.example.recordapp.ui.navigation.Screen
import com.example.recordapp.ui.components.EnhancedSignInDialog
import com.example.recordapp.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val signInHistory by viewModel.signInHistory.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEnhancedDialog by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Check if user is logged in and navigate to home
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }
    
    // Show error message
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    Image(
                        painter = painterResource(id = R.drawable.logo2),
                        contentDescription = "App Logo",
                        modifier = Modifier.height(80.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "RecordApp",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(R.string.login_title),
                        fontSize = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Enhanced Sign-in Button
                    Button(
                        onClick = { showEnhancedDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Supabase")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick sign-in with most recent account
                    if (signInHistory.isNotEmpty()) {
                        val mostRecent = signInHistory.first()
                        OutlinedButton(
                            onClick = {
                                email = mostRecent.email
                                showEnhancedDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Continue as ${mostRecent.userName.ifBlank { mostRecent.email }}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mostRecent.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { navController.navigate(Screen.Signup.route) }
                    ) {
                        Text(text = "Don't have an account? Sign Up")
                    }
                }
            }
        }
    }

    // Enhanced Sign-in Dialog
    if (showEnhancedDialog) {
        EnhancedSignInDialog(
            onDismiss = { showEnhancedDialog = false },
            onSignIn = { emailInput, passwordInput ->
                viewModel.login(emailInput, passwordInput)
                showEnhancedDialog = false
            },
            onSignUp = {
                showEnhancedDialog = false
                navController.navigate(Screen.Signup.route)
            },
            signInHistory = signInHistory,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage
        )
    }
}