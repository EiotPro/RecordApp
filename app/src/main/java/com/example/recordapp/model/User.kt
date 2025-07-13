package com.example.recordapp.model

import kotlinx.serialization.Serializable

/**
 * User data model that aligns with Supabase profiles table
 */
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String,
    val password: String = "", // Not stored in database, only for UI purposes
    val creationTime: Long = System.currentTimeMillis(),
    val lastLoginTime: Long = 0L,
    val profileImagePath: String? = null,
    val role: String = "user", // Role can be "user" or "admin"
    val isAdmin: Boolean = false // Flag for admin privileges
) 