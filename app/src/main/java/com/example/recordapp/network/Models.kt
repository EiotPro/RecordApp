package com.example.recordapp.network

import com.example.recordapp.model.User

/**
 * Request class for syncing data
 */
data class SyncDataRequest(
    val dataType: String,
    val data: Map<String, Any>
)

/**
 * Response class for data sync operations
 */
data class SyncDataResponse(
    val success: Boolean,
    val message: String,
    val syncedItems: Int
)

/**
 * Response class for user list requests
 */
data class UserListResponse(
    val success: Boolean,
    val message: String,
    val users: List<User>
)

/**
 * Response class for user update operations
 */
data class UserUpdateResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)

/**
 * Response class for credential reset operations
 */
data class CredentialsResetResponse(
    val success: Boolean,
    val message: String,
    val temporaryPassword: String?
)

/**
 * Request class for credential update operations
 */
data class CredentialsUpdateRequest(
    val currentUsername: String,
    val currentPassword: String,
    val newUsername: String,
    val newPassword: String
)

/**
 * Response class for credential update operations
 */
data class CredentialsUpdateResponse(
    val success: Boolean,
    val message: String
) 