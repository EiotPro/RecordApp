package com.example.recordapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility class for OCR (Optical Character Recognition) operations
 */
object OcrUtils {
    
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    /**
     * Process an image URI and extract text using OCR
     */
    suspend fun processImage(context: Context, imageUri: Uri?): OcrResult {
        if (imageUri == null) return OcrResult()
        
        val bitmap = getBitmapFromUri(context, imageUri) ?: throw Exception("Failed to load image")
        return processImage(bitmap)
    }
    
    /**
     * Process a bitmap and extract text using OCR
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizeText(image)
        
        return extractExpenseInfo(result)
    }
    
    /**
     * Recognize text from an input image
     */
    private suspend fun recognizeText(image: InputImage): Text = suspendCancellableCoroutine { continuation ->
        recognizer.process(image)
            .addOnSuccessListener { text ->
                if (continuation.isActive) {
                    continuation.resume(text)
                }
            }
            .addOnFailureListener { e ->
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
    }
    
    /**
     * Extract expense information from recognized text
     */
    private fun extractExpenseInfo(text: Text): OcrResult {
        val allText = text.text
        
        // Extract serial number
        val serialNumber = extractSerialNumber(allText)
        
        // Extract amount
        val amount = extractAmount(allText)
        
        // Extract description/details
        val description = extractDescription(text)
        
        return OcrResult(
            fullText = allText,
            serialNumber = serialNumber,
            amount = amount,
            description = description
        )
    }
    
    /**
     * Extract serial number from text
     */
    private fun extractSerialNumber(text: String): String {
        // Common patterns for serial numbers
        val patterns = listOf(
            Pattern.compile("(?i)(?:serial|s[/.]?n|no)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:bill|invoice)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:receipt)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:transaction)[.:#]?\\s*([A-Z0-9\\-]+)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return matcher.group(1) ?: ""
            }
        }
        
        return ""
    }
    
    /**
     * Extract amount from text
     */
    private fun extractAmount(text: String): Double {
        // Common patterns for rupees amounts
        val patterns = listOf(
            Pattern.compile("(?i)(?:rs|inr|₹|rupees)[.:]?\\s*(\\d+(?:[.,]\\d+)?)"),
            Pattern.compile("(?i)(?:total|amount|price)[.:]?\\s*(?:rs|inr|₹|rupees)?\\s*(\\d+(?:[.,]\\d+)?)"),
            Pattern.compile("(?i)(?:rs|inr|₹|rupees)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:/-|rs|only)")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: ""
                return try {
                    amountStr.toDouble()
                } catch (e: Exception) {
                    0.0
                }
            }
        }
        
        // Fallback: Look for numbers after Rs or ₹ symbols
        val simpleMatcher = Pattern.compile("[Rr][sS]|₹\\s*(\\d+(?:[.,]\\d+)?)").matcher(text)
        if (simpleMatcher.find()) {
            val amountStr = simpleMatcher.group(1)?.replace(",", "") ?: ""
            return try {
                amountStr.toDouble()
            } catch (e: Exception) {
                0.0
            }
        }
        
        return 0.0
    }
    
    /**
     * Extract description from text blocks
     */
    private fun extractDescription(text: Text): String {
        // Get the largest text block as it's likely to be the main content
        var largestBlock = ""
        var maxLength = 0
        
        for (block in text.textBlocks) {
            val blockText = block.text
            if (blockText.length > maxLength) {
                maxLength = blockText.length
                largestBlock = blockText
            }
        }
        
        return if (largestBlock.length > 100) {
            // If it's too long, truncate it
            largestBlock.substring(0, 100) + "..."
        } else {
            largestBlock
        }
    }
    
    /**
     * Get bitmap from Uri
     */
    private fun getBitmapFromUri(context: Context, uri: Uri?): Bitmap? {
        if (uri == null) return null
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Result class for OCR processing
 */
data class OcrResult(
    val fullText: String = "",
    val serialNumber: String = "",
    val amount: Double = 0.0,
    val description: String = ""
)