package com.example.aicollect.application.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signOut()
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
}
