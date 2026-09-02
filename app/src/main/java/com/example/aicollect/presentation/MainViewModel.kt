/**ViewModel de la Activity principal: expone los datos del perfil para el drawer lateral, la
*subida de la foto de perfil elegida desde ahí (en segundo plano, sin bloquear la app) y la
*operación de cerrar sesión, haciendo de intermediario entre MainActivity y AuthRepository.*/
package com.example.aicollect.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DrawerProfileUi(val displayNameOrFallback: String?, val photoUrl: String?)

sealed interface PhotoUploadUiState {
    data object Idle : PhotoUploadUiState
    data object Loading : PhotoUploadUiState
    data object Success : PhotoUploadUiState
    data class Error(val message: UiText) : PhotoUploadUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _photoUploadState = MutableStateFlow<PhotoUploadUiState>(PhotoUploadUiState.Idle)
    val photoUploadState: StateFlow<PhotoUploadUiState> = _photoUploadState.asStateFlow()

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

    fun uploadProfilePhoto(imageBytes: ByteArray) {
        _photoUploadState.value = PhotoUploadUiState.Loading
        viewModelScope.launch {
            authRepository.updateProfilePhoto(imageBytes)
                .onSuccess { _photoUploadState.value = PhotoUploadUiState.Success }
                .onFailure {
                    _photoUploadState.value = PhotoUploadUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_drawer_photo_generic),
                    )
                }
        }
    }

    suspend fun signOut() = authRepository.signOut()
}
