/**ViewModel de la Activity principal: expone los datos del perfil para el drawer lateral y
*la operación de cerrar sesión, haciendo de intermediario entre MainActivity y AuthRepository.*/
package com.example.aicollect.presentation

import androidx.lifecycle.ViewModel
import com.example.aicollect.application.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class DrawerProfileUi(val displayNameOrFallback: String?, val photoUrl: String?)

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
