package com.example.aicollect.application.auth

import kotlinx.coroutines.flow.Flow

/** Thrown by [AuthRepository.updateDisplayName] when another account already owns that name. */
class UsernameTakenException : Exception("Ese nombre de usuario ya está en uso.")

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Fails with [UsernameTakenException] if another account already claimed [username]; the
     * freshly created Firebase Auth account is rolled back in that case, not left orphaned. */
    suspend fun signUp(email: String, password: String, username: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /** Fails with [UsernameTakenException] if another account already claimed [displayName]. */
    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>

    /** Uploads [imageBytes] as the user's profile photo and returns its public download URL. */
    suspend fun updateProfilePhoto(imageBytes: ByteArray): Result<String>
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun getCurrentUserDisplayName(): String?
    fun getCurrentUserPhotoUrl(): String?
}
