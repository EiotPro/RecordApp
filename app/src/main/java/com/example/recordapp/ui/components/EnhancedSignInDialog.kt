package com.example.recordapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.recordapp.R
import com.example.recordapp.util.SignInHistoryManager
import com.example.recordapp.util.DateUtils

/**
 * Enhanced sign-in dialog with history, Supabase branding, and improved positioning
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSignInDialog(
    onDismiss: () -> Unit,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: () -> Unit,
    signInHistory: List<SignInHistoryManager.SignInRecord> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(false) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    val passwordFocusRequester = remember { FocusRequester() }
    
    // Auto-fill most recent sign-in if available
    LaunchedEffect(signInHistory) {
        if (email.isEmpty() && signInHistory.isNotEmpty()) {
            email = signInHistory.first().email
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Position dialog higher on screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp), // Move dialog upwards
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Supabase branding header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo2),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = "Sign in with Supabase",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Supabase logo/branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Powered by",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Supabase",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Email field with history dropdown
                    Column {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { 
                                email = it
                                showHistory = it.isNotEmpty() && signInHistory.isNotEmpty()
                            },
                            label = { Text("Email") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email")
                            },
                            trailingIcon = if (signInHistory.isNotEmpty()) {
                                {
                                    IconButton(
                                        onClick = { showHistory = !showHistory }
                                    ) {
                                        Icon(
                                            if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = "Show history"
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { passwordFocusRequester.requestFocus() }
                            )
                        )
                        
                        // Sign-in history dropdown
                        if (showHistory && signInHistory.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 200.dp)
                                ) {
                                    items(signInHistory) { record ->
                                        SignInHistoryItem(
                                            record = record,
                                            onClick = {
                                                email = record.email
                                                showHistory = false
                                                passwordFocusRequester.requestFocus()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Password")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(passwordFocusRequester),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                if (email.isNotBlank() && password.isNotBlank()) {
                                    onSignIn(email.trim(), password)
                                }
                            }
                        )
                    )
                    
                    // Error message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Sign in button
                    if (isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = { onSignIn(email.trim(), password) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = email.isNotBlank() && password.isNotBlank()
                        ) {
                            Text("Sign In")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Sign up link
                    TextButton(
                        onClick = onSignUp
                    ) {
                        Text("Don't have an account? Sign Up")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Cancel button
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun SignInHistoryItem(
    record: SignInHistoryManager.SignInRecord,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.email,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (record.userName.isNotBlank()) {
                Text(
                    text = record.userName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        
        Text(
            text = DateUtils.formatTimestamp(record.lastSignInTime, includeTime = false),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
