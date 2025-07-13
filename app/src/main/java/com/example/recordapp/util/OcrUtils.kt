package com.example.recordapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

/**
 * Utility class for OCR (Optical Character Recognition) operations
 */
object OcrUtils {
    private const val TAG = "OcrUtils"
    
    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    
    /**
     * Receipt type classification
     */
    enum class ReceiptType {
        PHYSICAL_RECEIPT,
        DIGITAL_PAYMENT,
        UPI_PAYMENT,
        UNKNOWN
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
        val lines = text.textBlocks.flatMap { it.lines }.map { it.text }
        
        Log.d(TAG, "Extracted full text: $allText")
        
        // Determine receipt type for tailored extraction
        val receiptType = detectReceiptType(allText, lines)
        Log.d(TAG, "Detected receipt type: $receiptType")
        
        // Extract serial number/bill number based on receipt type
        val serialNumber = extractSerialNumber(allText, lines, receiptType)
        Log.d(TAG, "Extracted serial number: $serialNumber")
        
        // Extract amount with improved currency recognition
        val (amount, confidence) = extractAmountWithConfidence(allText, lines, receiptType)
        Log.d(TAG, "Extracted amount: $amount (confidence: $confidence)")
        
        // Extract merchant/description with improved detection
        val description = extractDescription(text, lines, receiptType)
        Log.d(TAG, "Extracted description: $description")
        
        return OcrResult(
            fullText = allText,
            serialNumber = serialNumber,
            amount = amount,
            description = description,
            receiptType = receiptType.name
        )
    }
    
    /**
     * Detect receipt type using score-based system
     */
    private fun detectReceiptType(text: String, lines: List<String>): ReceiptType {
        val normalizedText = text.lowercase()
        
        // Score-based classification
        var physicalScore = 0
        var digitalScore = 0
        var upiScore = 0
        
        // Keywords for physical receipts
        val physicalKeywords = mapOf(
            "cash memo" to 3,
            "bill" to 2,
            "invoice" to 2,
            "receipt" to 2,
            "store" to 1,
            "gst" to 3,
            "tax invoice" to 3,
            "cash counter" to 3,
            "customer" to 1,
            "thank you" to 1,
            "total" to 1,
            "subtotal" to 2,
            "qty" to 2,
            "price" to 1
        )
        
        // Keywords for digital payments
        val digitalKeywords = mapOf(
            "transaction" to 3,
            "payment successful" to 3,
            "payment complete" to 3,
            "digital receipt" to 3,
            "confirmation" to 2,
            "reference" to 2,
            "transaction id" to 3,
            "paid to" to 3,
            "payment mode" to 3,
            "date & time" to 2,
            "paid from" to 2
        )
        
        // Keywords for UPI payments
        val upiKeywords = mapOf(
            "upi" to 3,
            "upi id" to 3,
            "upi reference" to 3,
            "bhim" to 3,
            "gpay" to 3,
            "google pay" to 3,
            "phonepe" to 3,
            "paytm" to 3,
            "vpa" to 3,
            "upi transaction" to 3
        )
        
        // Score calculation
        for ((keyword, value) in physicalKeywords) {
            if (normalizedText.contains(keyword)) {
                physicalScore += value
            }
        }
        
        for ((keyword, value) in digitalKeywords) {
            if (normalizedText.contains(keyword)) {
                digitalScore += value
            }
        }
        
        for ((keyword, value) in upiKeywords) {
            if (normalizedText.contains(keyword)) {
                upiScore += value
                digitalScore += 1 // UPI is also digital
            }
        }
        
        // Pattern-based detection for layout characteristics
        // Check for item listing patterns (common in physical receipts)
        if (lines.any { it.matches(Regex(".*\\d+\\s*x\\s*\\d+.*")) }) {
            physicalScore += 2
        }
        
        // Check for digital payment patterns
        if (text.contains(Regex("(?i)payment\\s+id\\s*:\\s*[a-zA-Z0-9]+"))) {
            digitalScore += 2
        }
        
        // Return the highest scoring type
        return when {
            upiScore >= 3 && upiScore >= digitalScore -> ReceiptType.UPI_PAYMENT
            digitalScore > physicalScore -> ReceiptType.DIGITAL_PAYMENT
            physicalScore > 0 -> ReceiptType.PHYSICAL_RECEIPT
            else -> ReceiptType.UNKNOWN
        }
    }
    
    /**
     * Extract serial number from text with enhanced patterns
     */
    private fun extractSerialNumber(text: String, lines: List<String>, receiptType: ReceiptType): String {
        val normalizedText = text.replace("\n", " ")
        
        // Type-specific extraction patterns
        when (receiptType) {
            ReceiptType.DIGITAL_PAYMENT, ReceiptType.UPI_PAYMENT -> {
                // Digital payment transaction IDs and UPI references
                val digitalPatterns = listOf(
                    // Transaction IDs (various formats)
                    Pattern.compile("(?i)(?:order|txn|transaction|payment)[\\s.:#_-]*id[\\s.:#_-]*(\\w{6,}|\\w{2,}[-]\\w{2,}[-]\\w{2,})"),
                    Pattern.compile("(?i)(?:reference|ref|utr)[\\s.:#_-]*(?:no|num|number)?[\\s.:#_-]*(\\w{6,}|\\w{2,}[-]\\w{2,}[-]\\w{2,})"),
                    Pattern.compile("(?i)(?:upi|payment)[\\s.:#_-]*ref[\\s.:#_-]*(?:no)?[\\s.:#_-]*(\\w{6,}|\\w{2,}[-]\\w{2,}[-]\\w{2,})"),
                    Pattern.compile("(?i)(?:id|txnid)[\\s.:#_-]*(\\w{6,}|\\w{2,}[-]\\w{2,}[-]\\w{2,})"),
                    
                    // Look for patterns like "Txn ID: ABCD1234" or "Reference #12345678"
                    Pattern.compile("(?i)(?:txn|transaction)[\\s.:#_-]*(?:id|ref)[\\s.:#_-]*[:#]?\\s*([A-Za-z0-9][-A-Za-z0-9]{5,})")
                )
                
                for (pattern in digitalPatterns) {
                    val matcher = pattern.matcher(normalizedText)
                    if (matcher.find()) {
                        return matcher.group(1)?.trim() ?: ""
                    }
                }
                
                // Attempt to find a standalone id in the first few lines
                for (line in lines.take(5)) {
                    val alphanumericOnly = line.trim().replace(Regex("[^A-Za-z0-9]"), "")
                    if (alphanumericOnly.length >= 6 && alphanumericOnly.length <= 20 && 
                        !alphanumericOnly.matches(Regex("\\d+")) && // Not just digits
                        !line.contains(Regex("(?i)(total|amount|rs|inr|rupee)"))) {
                        return alphanumericOnly
                    }
                }
            }
            
            ReceiptType.PHYSICAL_RECEIPT -> {
                // Physical receipt bill/invoice numbers
                val physicalPatterns = listOf(
                    // Common invoice and bill number formats
                    Pattern.compile("(?i)(?:bill|invoice|receipt)[\\s.:#_-]*(?:no|num|number)?[\\s.:#_-]*([A-Za-z0-9][-A-Za-z0-9/]{3,})"),
                    Pattern.compile("(?i)(?:serial|s[/.]?n)[\\s.:#_-]*([A-Za-z0-9][-A-Za-z0-9/]{3,})"),
                    
                    // Hash-prefixed bill numbers (common in retail)
                    Pattern.compile("(?i)(?:bill|invoice)[\\s.:#_-]*(?:no|num|number)?[\\s.:#_-]*#\\s*([A-Za-z0-9][-A-Za-z0-9/]{3,})"),
                    Pattern.compile("(?i)#\\s*([A-Za-z0-9][-A-Za-z0-9/]{3,})\\s*(?=bill|invoice)"),
                    
                    // Look for just the number pattern
                    Pattern.compile("(?i)(?:gst|cin|tin|bill)[\\s.:#_-]*([A-Za-z0-9][-A-Za-z0-9/]{3,})")
                )
                
                for (pattern in physicalPatterns) {
                    val matcher = pattern.matcher(normalizedText)
                    if (matcher.find()) {
                        return matcher.group(1)?.trim() ?: ""
                    }
                }
                
                // Check first few lines for standalone numbers (often bill numbers appear at the top)
                for (i in 0 until minOf(4, lines.size)) {
                    val line = lines[i].trim()
                    // Look for patterns like "#1234", "No. 1234", "B12345" at the beginning of receipts
                    val match = Regex("^[#]?\\s*(?:no[.:]?\\s*)?(\\w{4,})$").find(line)
                    if (match != null) {
                        return match.groupValues[1].trim()
                    }
                }
            }
            
            else -> {
                // Fallback to generic patterns for unknown receipt types
        val patterns = listOf(
            Pattern.compile("(?i)(?:serial|s[/.]?n|no)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:bill|invoice)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:receipt)[.:#]?\\s*([A-Z0-9\\-]+)"),
            Pattern.compile("(?i)(?:transaction)[.:#]?\\s*([A-Z0-9\\-]+)")
        )
        
        for (pattern in patterns) {
                    val matcher = pattern.matcher(normalizedText)
            if (matcher.find()) {
                        return matcher.group(1)?.trim() ?: ""
                    }
                }
            }
        }
        
        return ""
    }
    
    /**
     * Extract amount from text with improved currency recognition and confidence scoring
     */
    private fun extractAmountWithConfidence(text: String, lines: List<String>, receiptType: ReceiptType): Pair<Double, Float> {
        val normalizedText = text.replace("\n", " ")
        data class AmountMatch(val amount: Double, val confidence: Float, val context: String)
        val matches = mutableListOf<AmountMatch>()
        
        // Indian Rupee currency patterns with variations
        val currencyPatterns = listOf(
            // ₹ symbol patterns
            Pattern.compile("(?:₹|Rs\\.?|INR)\\s*(\\d+(?:[.,]\\d+)?)"),
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:₹|Rs\\.?|INR)"),
            
            // Total amount patterns with different currency formats
            Pattern.compile("(?i)(?:total|grand total|amount|sum|net amount)[\\s:]*(?:₹|Rs\\.?|INR)?\\s*(\\d+(?:[.,]\\d+)?)"),
            Pattern.compile("(?i)(?:total|grand total|amount|sum|net amount)[\\s:]*(?:₹|Rs\\.?|INR|Rupees)\\s*(\\d+(?:[.,]\\d+)?)"),
            Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:₹|Rs\\.?|INR|Rupees)\\s*(?:total|only)"),
            
            // With special characters and spacing variations
            Pattern.compile("(?i)(?:₹|Rs\\.?|INR)\\s*([\\d,]+\\.?\\d*)"),
            Pattern.compile("(?i)([\\d,]+\\.?\\d*)\\s*(?:₹|Rs\\.?|INR)"),
            
            // "Rupees" spelled out
            Pattern.compile("(?i)(?:rupees|rs)\\s+([\\d,]+\\.?\\d*)")
        )
        
        // Process each pattern and collect matches with confidence scores
        for (pattern in currencyPatterns) {
            val matcher = pattern.matcher(normalizedText)
            while (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                try {
                    val amount = amountStr.toDouble()
                    // Calculate a base confidence score
                    var confidence = 0.5f
                    
                    // Add confidence based on context
                    val matchContext = normalizedText.substring(
                        maxOf(0, matcher.start() - 20), 
                        minOf(normalizedText.length, matcher.end() + 20)
                    )
                    
                    // Higher confidence for amounts with "total" keywords
                    if (matchContext.contains(Regex("(?i)total|grand total|sum total|net"))) {
                        confidence += 0.3f
                    }
                    
                    // Higher confidence for amounts at the end of the text (often the final amount)
                    if (matcher.end() > normalizedText.length * 0.7) {
                        confidence += 0.1f
                    }
                    
                    // Higher confidence for larger amounts (typically the total vs. individual items)
                    if (amount > 100) {
                        confidence += 0.1f
                    }
                    
                    matches.add(AmountMatch(amount, confidence, matchContext))
                } catch (e: Exception) {
                    // Skip invalid numbers
                }
            }
        }
        
        // Type-specific extraction logic
        when (receiptType) {
            ReceiptType.PHYSICAL_RECEIPT -> {
                // For physical receipts, look for keywords that typically indicate the total amount
                val totalKeywords = listOf("grand total", "total amount", "net amount", "total payable", "amount payable")
                
                // Search for lines with these keywords
                for (line in lines) {
                    for (keyword in totalKeywords) {
                        if (line.lowercase().contains(keyword)) {
                            // Extract the number from this line
                            val numberMatch = Regex("(\\d+(?:[.,]\\d+)?)").find(line)
                            if (numberMatch != null) {
                                try {
                                    val amount = numberMatch.groupValues[1].replace(",", "").toDouble()
                                    matches.add(AmountMatch(amount, 0.9f, line)) // High confidence for total keywords
                                } catch (e: Exception) {
                                    // Skip invalid numbers
                                }
                            }
                        }
                    }
                }
                
                // Also check the last few lines of a physical receipt, which often contain the total
                for (line in lines.takeLast(5)) {
                    if (line.lowercase().contains(Regex("(?i)total|amount|pay|paid"))) {
                        val numberMatch = Regex("(\\d+(?:[.,]\\d+)?)").find(line)
                        if (numberMatch != null) {
                            try {
                                val amount = numberMatch.groupValues[1].replace(",", "").toDouble()
                                matches.add(AmountMatch(amount, 0.8f, line))
                            } catch (e: Exception) {
                                // Skip invalid numbers
                            }
                        }
                    }
                }
            }
            
            ReceiptType.DIGITAL_PAYMENT, ReceiptType.UPI_PAYMENT -> {
                // For digital payments, look for transaction amount patterns
                val digitalPatterns = listOf(
                    Pattern.compile("(?i)(?:amount|transaction amount)[\\s:]*(?:₹|Rs\\.?|INR)?\\s*(\\d+(?:[.,]\\d+)?)"),
                    Pattern.compile("(?i)paid[\\s:]*(?:₹|Rs\\.?|INR)?\\s*(\\d+(?:[.,]\\d+)?)")
                )
                
                for (pattern in digitalPatterns) {
                    val matcher = pattern.matcher(normalizedText)
                    while (matcher.find()) {
                        val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                        try {
                            val amount = amountStr.toDouble()
                            matches.add(AmountMatch(amount, 0.8f, 
                                normalizedText.substring(maxOf(0, matcher.start() - 10), 
                                minOf(normalizedText.length, matcher.end() + 10))))
                        } catch (e: Exception) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
            
            else -> {
                // Additional fallback patterns for unknown receipt types
                val fallbackPatterns = listOf(
                    Pattern.compile("(?i)(?:amount|total|price)[.:]?\\s*(?:rs|inr|₹|rupees)?\\s*(\\d+(?:[.,]\\d+)?)"),
                    Pattern.compile("(?i)(?:rs|inr|₹|rupees)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:/-|rs|only)")
                )
                
                for (pattern in fallbackPatterns) {
                    val matcher = pattern.matcher(normalizedText)
                    while (matcher.find()) {
                        val amountStr = matcher.group(1)?.replace(",", "") ?: continue
                        try {
                            val amount = amountStr.toDouble()
                            matches.add(AmountMatch(amount, 0.5f, 
                                normalizedText.substring(maxOf(0, matcher.start() - 10), 
                                minOf(normalizedText.length, matcher.end() + 10))))
            } catch (e: Exception) {
                            // Skip invalid numbers
                        }
                    }
                }
            }
        }
        
        // Find the match with the highest confidence
        val bestMatch = matches.maxByOrNull { it.confidence }
        
        // If we have multiple matches with similar confidence, prefer the larger amount
        // (assuming it's more likely to be the total)
        val closeConfidenceMatches = matches.filter { 
            bestMatch != null && (bestMatch.confidence - it.confidence <= 0.1f) 
        }
        
        val finalMatch = if (closeConfidenceMatches.size > 1) {
            closeConfidenceMatches.maxByOrNull { it.amount } ?: bestMatch
        } else {
            bestMatch
        }
        
        return if (finalMatch != null) {
            Pair(finalMatch.amount, finalMatch.confidence)
        } else {
            // Final fallback - try to find any number after Rs or ₹ symbols
            val simpleMatcher = Pattern.compile("[Rr][sS]|₹\\s*(\\d+(?:[.,]\\d+)?)").matcher(normalizedText)
            if (simpleMatcher.find()) {
                val amountStr = simpleMatcher.group(1)?.replace(",", "") ?: ""
                try {
                    Pair(amountStr.toDouble(), 0.3f)
                } catch (e: Exception) {
                    Pair(0.0, 0.0f)
                }
            } else {
                Pair(0.0, 0.0f)
            }
        }
    }
    
    /**
     * Extract description/merchant name from text with improved detection
     */
    private fun extractDescription(text: Text, lines: List<String>, receiptType: ReceiptType): String {
        when (receiptType) {
            ReceiptType.DIGITAL_PAYMENT, ReceiptType.UPI_PAYMENT -> {
                // For digital payments, look for paid to/merchant information
                val paymentPatterns = listOf(
                    Pattern.compile("(?i)paid to[:\\s]*(.+?)(?=\\n|$|paid from|upi id|date)"),
                    Pattern.compile("(?i)(?:recipient|merchant|to|payee)[:\\s]*(.+?)(?=\\n|$|transaction|id|date)"),
                    Pattern.compile("(?i)(?:paid)[\\s]*(?:to)[\\s]*(.+?)(?=\\n|$|on|at)")
                )
                
                for (line in lines) {
                    for (pattern in paymentPatterns) {
                        val matcher = pattern.matcher(line)
                        if (matcher.find()) {
                            val merchant = matcher.group(1)?.trim() ?: ""
                            if (merchant.isNotEmpty() && merchant.length < 50) {
                                return cleanupText(merchant)
                            }
                        }
                    }
                }
                
                // Look for UPI ID patterns
                val upiMatch = lines.firstOrNull { it.contains(Regex("(?i)@|upi id|vpa")) }
                if (upiMatch != null) {
                    // Try to extract a name from the UPI ID
                    val nameMatch = Regex("(?i)([a-zA-Z.]+)@").find(upiMatch)
                    if (nameMatch != null) {
                        val name = nameMatch.groupValues[1].replace(".", " ").trim()
                        if (name.length > 2) {
                            return cleanupText(name)
                        }
                    }
                    return cleanupText(upiMatch)
                }
            }
            
            ReceiptType.PHYSICAL_RECEIPT -> {
                // For physical receipts, try to extract store name from the top
                
                // First, check for store name at the top (usually first 1-3 lines contain store name)
                for (i in 0 until minOf(3, lines.size)) {
                    val line = lines[i].trim()
                    if (line.length > 3 && !line.contains(Regex("(?i)(bill|invoice|receipt|#|date|time)"))) {
                        return cleanupText(line)
                    }
                }
                
                // If no store name found, try to find item information (common in receipts)
                val items = mutableListOf<String>()
                var collectingItems = false
                
                for (line in lines) {
                    val lowercaseLine = line.lowercase()
                    
                    // Start collecting items after keywords like "item", "description", "qty"
                    if (!collectingItems && lowercaseLine.matches(Regex(".*\\b(item|description|qty|quantity|product)\\b.*"))) {
                        collectingItems = true
                        continue
                    }
                    
                    // Stop collecting items at keywords indicating the end of the item list
                    if (collectingItems && lowercaseLine.matches(Regex(".*\\b(total|subtotal|sum|amount|tax|gst)\\b.*"))) {
                        break
                    }
                    
                    // Collect potential items
                    if (collectingItems && line.trim().isNotEmpty() && line.length < 50) {
                        // Filter out lines that are likely not items
                        if (!lowercaseLine.matches(Regex(".*\\b(date|time|payment|card|cash)\\b.*"))) {
                            items.add(line.trim())
                        }
                    }
                }
                
                // If we found items, return the first few as the description
                if (items.isNotEmpty()) {
                    val combinedItems = items.take(2).joinToString(", ")
                    return cleanupText(combinedItems)
                }
            }
            
            else -> {
                // Fallback to the original method for unknown types
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
                    cleanupText(largestBlock.substring(0, 100) + "...")
        } else {
                    cleanupText(largestBlock)
                }
            }
        }
        
        // If nothing suitable found yet, return the first non-empty line
        return cleanupText(lines.firstOrNull { it.trim().isNotEmpty() } ?: "")
    }
    
    /**
     * Clean up text by removing unwanted characters and normalizing spacing
     */
    private fun cleanupText(text: String): String {
        return text
            .replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "") // Keep only letters, numbers, punctuation, and whitespace
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
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
    val description: String = "",
    val receiptType: String = ""
)