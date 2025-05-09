package com.example.recordapp.model

/**
 * User data class for authentication
 */
data class User(
    val id: String,
    val email: String,
    val password: String,
    val name: String = ""
) 