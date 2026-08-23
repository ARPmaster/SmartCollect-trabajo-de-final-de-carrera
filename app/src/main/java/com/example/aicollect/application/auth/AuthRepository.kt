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

    /** Proves the user still knows their password right before a destructive action (account
     * deletion) — a UX/security gate, not strictly required by [deleteAccount] itself (that runs
     * server-side via the Admin SDK, which isn't subject to Firebase's client "recent login"
     * restriction). Fails with the underlying `FirebaseAuthInvalidCredentialsException` if
     * [password] is wrong. */
    suspend fun reauthenticate(password: String): Result<Unit>

    /** Borra, con el SDK de Android (sin Cloud Function — decisión explícita, 2026-08-24), cada
     * item del usuario (documento + fotos en Storage), su documento de perfil, la reserva de
     * `usernames/{nombre}` y por último la cuenta de Firebase Auth — ese orden importa: el token
     * de sesión deja de servir en cuanto se borra la cuenta, así que tiene que ser lo último.
     * Llamar a [reauthenticate] antes: `user.delete()` exige una sesión reciente igual que
     * [updateEmail]/[updatePassword]. */
    suspend fun deleteAccount(): Result<Unit>

    /** Uploads [imageBytes] as the user's profile photo and returns its public download URL. */
    suspend fun updateProfilePhoto(imageBytes: ByteArray): Result<String>
    fun observeAuthState(): Flow<Boolean>
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun getCurrentUserDisplayName(): String?
    fun getCurrentUserPhotoUrl(): String?
}
