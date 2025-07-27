package com.example.recordapp.ui.components.datepicker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

/**
 * Combined Date and Time picker component
 * Allows user to select both date and time for expense entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePicker(
    selectedDateTime: LocalDateTime,
    onDateTimeSelected: (LocalDateTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember(selectedDateTime) { mutableStateOf(selectedDateTime.toLocalDate()) }
    var selectedTime by remember(selectedDateTime) { mutableStateOf(selectedDateTime.toLocalTime()) }

    // Update internal state when selectedDateTime parameter changes
    LaunchedEffect(selectedDateTime) {
        selectedDate = selectedDateTime.toLocalDate()
        selectedTime = selectedDateTime.toLocalTime()
    }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val displayText = "${selectedDate.format(dateFormatter)} ${selectedTime.format(timeFormatter)}"
    
    Column(modifier = modifier) {
        // Simple approach: Use a Button that looks like a text field
        OutlinedButton(
            onClick = {
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Select Time",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (showDialog) {
            DateTimePickerDialog(
                selectedDate = selectedDate,
                selectedTime = selectedTime,
                onDateSelected = { selectedDate = it },
                onTimeSelected = { selectedTime = it },
                onConfirm = {
                    val newDateTime = LocalDateTime.of(selectedDate, selectedTime)
                    onDateTimeSelected(newDateTime)
                    showDialog = false
                },
                onDismiss = { showDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(
    selectedDate: LocalDate,
    selectedTime: LocalTime,
    onDateSelected: (LocalDate) -> Unit,
    onTimeSelected: (LocalTime) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentTab by remember { mutableStateOf(0) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Title
                Text(
                    text = "Select Date & Time",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Tab Row
                TabRow(
                    selectedTabIndex = currentTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        text = { Text("Date") },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) }
                    )
                    Tab(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        text = { Text("Time") },
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Content based on selected tab
                when (currentTab) {
                    0 -> {
                        // Date Picker
                        val selectedMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
                        
                        DatePicker(
                            state = datePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Update selected date when picker changes
                        LaunchedEffect(datePickerState.selectedDateMillis) {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val newDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                onDateSelected(newDate)
                            }
                        }
                    }
                    1 -> {
                        // Time Picker
                        val timePickerState = rememberTimePickerState(
                            initialHour = selectedTime.hour,
                            initialMinute = selectedTime.minute
                        )
                        
                        TimePicker(
                            state = timePickerState,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Update selected time when picker changes
                        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                            val newTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            onTimeSelected(newTime)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
