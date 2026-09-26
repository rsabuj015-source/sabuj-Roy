package com.example.chatnova.domain.repository

import com.example.chatnova.domain.model.UserProfile

interface AuthRepository {
    fun isUserLoggedIn(): Boolean
    fun isFirebaseConfigured(): Boolean
    suspend fun getCurrentUser(): UserProfile?
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(username: String, displayName: String, email: String, password: String): Result<UserProfile>
    suspend fun updateProfile(userId: String, displayName: String, username: String, bio: String, photoPreset: String?): Result<UserProfile>
    suspend fun logout(): Result<Unit>
}
