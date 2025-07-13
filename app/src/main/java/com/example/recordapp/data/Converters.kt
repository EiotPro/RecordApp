package com.example.recordapp.data

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Type converters for Room database
 */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
} 