/**Define el contrato de las operaciones de autenticación y gestión de cuenta (alta, baja,
 inicio/cierre de sesión, recuperación de contraseña y actualización de datos del usuario),
independiente de la implementación concreta (Firebase).*/
package com.example.aicollect.application.auth

import kotlinx.coroutines.flow.Flow

/** Sin mensaje de UI: el texto para el usuario se resuelve en presentación (patrón UiText),
 * nunca desde aquí — así una capa de dominio no decide en qué idioma habla la interfaz. */
class UsernameTakenException : Exception()

/** Errores de autenticación con significado propio para la UI, sin mensaje embebido (el texto se
 * resuelve en presentación con el patrón UiText). Mantiene la causa original para depuración. */
sealed class AuthError(cause: Throwable? = null) : Exception(null, cause) {
    /** Los métodos que lo lanzan solo deberían invocarse con sesión iniciada; si ocurre es un
     * estado inconsistente, no una validación de usuario. */
    object NoActiveSession : AuthError()
    object AccountCreationFailed : AuthError()
    class RecentLoginRequired(cause: Throwable? = null) : AuthError(cause)
    class WrongPassword(cause: Throwable? = null) : AuthError(cause)
}

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
