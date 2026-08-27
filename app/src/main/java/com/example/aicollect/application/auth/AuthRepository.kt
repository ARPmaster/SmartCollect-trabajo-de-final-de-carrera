/**Define el contrato de las operaciones de autenticación y gestión de cuenta (alta, baja,
 inicio/cierre de sesión, recuperación de contraseña y actualización de datos del usuario),
independiente de la implementación concreta (Firebase).*/
package com.example.aicollect.application.auth

import kotlinx.coroutines.flow.Flow

class UsernameTakenException : Exception("Ese nombre de usuario ya está en uso.")

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<Unit>


    suspend fun signUp(email: String, password: String, username: String): Result<Unit>
    suspend fun signOut()
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun updateDisplayName(displayName: String): Result<Unit>
    suspend fun updateEmail(newEmail: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun reauthenticate(password: String): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
    suspend fun updateProfilePhoto(imageBytes: ByteArray): Result<String>
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun getCurrentUserDisplayName(): String?
    fun getCurrentUserPhotoUrl(): String?
}
