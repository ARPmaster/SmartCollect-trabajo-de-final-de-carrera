// Doble de prueba de AuthRepository para las pruebas de instrumentación: simula sesión, registro y gestión de cuenta en memoria, sin llamar a Firebase.
package com.example.aicollect.testutil

import com.example.aicollect.application.auth.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

@Singleton
class FakeAuthRepository @Inject constructor() : AuthRepository {

    var fakeUserId: String? = "fake-uid"
    var fakeUserEmail: String? = "fake@example.com"
    var fakeDisplayName: String? = "Usuario de prueba"
    var fakePhotoUrl: String? = null

    private val authState = MutableStateFlow(true)

    var signInResult: Result<Unit> = Result.success(Unit)
    var signInCallCount = 0
        private set
    var lastSignInEmail: String? = null
        private set
    var lastSignInPassword: String? = null
        private set

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        signInCallCount++
        lastSignInEmail = email
        lastSignInPassword = password
        return signInResult
    }
    override suspend fun signUp(email: String, password: String, username: String): Result<Unit> = Result.success(Unit)
    override suspend fun signOut() = Unit
    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateDisplayName(displayName: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateEmail(newEmail: String): Result<Unit> = Result.success(Unit)
    override suspend fun updatePassword(newPassword: String): Result<Unit> = Result.success(Unit)
    override suspend fun reauthenticate(password: String): Result<Unit> = Result.success(Unit)
    override suspend fun deleteAccount(): Result<Unit> = Result.success(Unit)
    override suspend fun updateProfilePhoto(imageBytes: ByteArray): Result<String> = Result.success("https://example.com/photo.jpg")
    override fun observeAuthState(): Flow<Boolean> = authState
    override fun getCurrentUserId(): String? = fakeUserId
    override fun getCurrentUserEmail(): String? = fakeUserEmail
    override fun getCurrentUserDisplayName(): String? = fakeDisplayName
    override fun getCurrentUserPhotoUrl(): String? = fakePhotoUrl
}
