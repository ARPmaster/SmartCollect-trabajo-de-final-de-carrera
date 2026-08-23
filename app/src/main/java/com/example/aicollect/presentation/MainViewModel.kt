package com.example.aicollect.presentation

import androidx.lifecycle.ViewModel
import com.example.aicollect.application.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** [displayNameOrFallback] is already resolved (real name, else the email's local part) — null
 * only when neither exists, in which case [MainActivity] substitutes its own generic string
 * resource (ViewModels don't hold string resources, same split as
 * [com.example.aicollect.presentation.newpost.RequiredField]). */
data class DrawerProfileUi(val displayNameOrFallback: String?, val photoUrl: String?)

/**
 * 2026-08-24 MVVM fix: `MainActivity.refreshDrawerProfile()`/the logout button used to call
 * `AuthRepository` directly — the single Activity acting as its own View talking straight to the
 * Model. Thin on purpose: `MainActivity` still owns *when* to call [drawerProfile] (on every nav
 * destination change) and *when* to navigate after [signOut] completes, that's legitimate
 * Activity/navigation orchestration, not business logic.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    fun drawerProfile(): DrawerProfileUi {
        val displayName = authRepository.getCurrentUserDisplayName()
        val emailPrefix = authRepository.getCurrentUserEmail()?.substringBefore('@')
        val resolvedName = when {
            !displayName.isNullOrBlank() -> displayName
            !emailPrefix.isNullOrBlank() -> emailPrefix
            else -> null
        }
        return DrawerProfileUi(resolvedName, authRepository.getCurrentUserPhotoUrl())
    }

    suspend fun signOut() = authRepository.signOut()
}
