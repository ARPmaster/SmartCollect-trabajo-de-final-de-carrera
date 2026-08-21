package com.example.aicollect.data.auth

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.UsernameTakenException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        Unit
    }
    /** Funcion con @param va*/
    override suspend fun signUp(email: String, password: String, username: String): Result<Unit> = runCatching {
        val trimmedUsername = username.trim()
        firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No se pudo crear la cuenta.")

        try {
            claimUsername(user.uid, trimmedUsername.lowercase(), previousNormalized = null)
        } catch (e: UsernameTakenException) {
            // Roll back the orphaned Auth account instead of leaving a user with no username —
            // the session is fresh right after createUser, so this delete won't need reauth.
            runCatching { user.delete().await() }
            throw e
        }

        val request = UserProfileChangeRequest.Builder().setDisplayName(trimmedUsername).build()
        user.updateProfile(request).await()
        Unit
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Unit
    }

    override suspend fun updateDisplayName(displayName: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No hay sesión activa.")
        val trimmedName = displayName.trim()
        claimUsername(user.uid, trimmedName.lowercase(), previousNormalized = user.displayName?.trim()?.lowercase())

        val request = UserProfileChangeRequest.Builder().setDisplayName(trimmedName).build()
        user.updateProfile(request).await()
        Unit
    }

    /** Atomically reserves [normalizedNew] in `usernames/` for [uid], releasing
     * [previousNormalized]'s reservation (if it actually has one) in the same transaction.
     * Throws [UsernameTakenException] if another uid already owns [normalizedNew]. */
    private suspend fun claimUsername(uid: String, normalizedNew: String, previousNormalized: String?) {
        if (normalizedNew == previousNormalized) return
        val newRef = firestore.collection(USERNAMES_COLLECTION).document(normalizedNew)
        val oldRef = previousNormalized
            ?.takeIf { it.isNotBlank() }
            ?.let { firestore.collection(USERNAMES_COLLECTION).document(it) }

        firestore.runTransaction { transaction ->
            // Firestore transactions require every read before any write, so both gets happen
            // first — the old-reservation read also lets us skip deleting a document that was
            // never created, which the `delete` rule can't evaluate (denied, not a no-op).
            val existingNew = transaction.get(newRef)
            val oldReservationExists = oldRef != null && transaction.get(oldRef).exists()

            if (existingNew.exists() && existingNew.getString(USERNAME_OWNER_FIELD) != uid) {
                throw UsernameTakenException()
            }

            transaction.set(newRef, mapOf(USERNAME_OWNER_FIELD to uid))
            if (oldReservationExists) {
                transaction.delete(oldRef!!)
            }
        }.await()
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No hay sesión activa.")
        try {
            user.updateEmail(newEmail).await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw IllegalStateException(RECENT_LOGIN_MESSAGE, e)
        }
        Unit
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No hay sesión activa.")
        try {
            user.updatePassword(newPassword).await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw IllegalStateException(RECENT_LOGIN_MESSAGE, e)
        }
        Unit
    }

    override suspend fun updateProfilePhoto(imageBytes: ByteArray): Result<String> = runCatching {
        val user = firebaseAuth.currentUser ?: throw IllegalStateException("No hay sesión activa.")
        val photoRef = firebaseStorage.reference.child("users/${user.uid}/profile.jpg")
        photoRef.putBytes(imageBytes).await()
        val downloadUrl = photoRef.downloadUrl.await()
        val request = UserProfileChangeRequest.Builder().setPhotoUri(downloadUrl).build()
        user.updateProfile(request).await()
        downloadUrl.toString()
    }

    override fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser != null)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid

    override fun getCurrentUserEmail(): String? = firebaseAuth.currentUser?.email

    override fun getCurrentUserDisplayName(): String? = firebaseAuth.currentUser?.displayName

    override fun getCurrentUserPhotoUrl(): String? = firebaseAuth.currentUser?.photoUrl?.toString()

    private companion object {
        const val RECENT_LOGIN_MESSAGE =
            "Por seguridad, cierra sesión y vuelve a iniciar sesión antes de cambiar estos datos."
        const val USERNAMES_COLLECTION = "usernames"
        const val USERNAME_OWNER_FIELD = "uid"
    }
}
