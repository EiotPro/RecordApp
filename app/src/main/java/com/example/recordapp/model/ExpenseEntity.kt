package com.example.recordapp.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Room Entity for expense records
 */
@Entity(tableName = "expenses")
@TypeConverters(Converters::class)
data class ExpenseEntity(
    @PrimaryKey
    val id: String,
    val imagePathString: String?,
    val timestampString: String, // Creation timestamp
    val expenseDateTimeString: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), // Actual expense date/time
    val serialNumber: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val folderName: String = "default",
    val receiptType: String = "",
    val displayOrder: Int = 0,
    val imageModifiedTimestamp: Long = System.currentTimeMillis() // Track when image was last modified
) {
    /**
     * Convert to domain model
     */
    fun toExpense(): Expense {
        return Expense(
            id = id,
            imagePath = imagePathString?.let { Uri.parse(it) },
            timestamp = LocalDateTime.parse(timestampString, TIMESTAMP_FORMATTER),
            expenseDateTime = LocalDateTime.parse(expenseDateTimeString, TIMESTAMP_FORMATTER),
            serialNumber = serialNumber,
            amount = amount,
            description = description,
            folderName = folderName,
            receiptType = receiptType,
            displayOrder = displayOrder,
            imageModifiedTimestamp = imageModifiedTimestamp
        )
    }

    companion object {
        private val TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        /**
         * Create entity from domain model
         */
        fun fromExpense(expense: Expense): ExpenseEntity {
            return ExpenseEntity(
                id = expense.id,
                imagePathString = expense.imagePath?.toString(),
                timestampString = expense.timestamp.format(TIMESTAMP_FORMATTER),
                expenseDateTimeString = expense.expenseDateTime.format(TIMESTAMP_FORMATTER),
                serialNumber = expense.serialNumber,
                amount = expense.amount,
                description = expense.description,
                folderName = expense.folderName,
                receiptType = expense.receiptType,
                displayOrder = expense.displayOrder,
                imageModifiedTimestamp = expense.imageModifiedTimestamp
            )
        }
    }
}

/**
 * Type converters for Room
 */
class Converters {
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    }

    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return uriString?.let { Uri.parse(it) }
    }
} 