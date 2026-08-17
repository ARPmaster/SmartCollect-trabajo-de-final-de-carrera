package com.example.aicollect.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.UsernameTakenException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EditProfileUiState {
    data object Idle : EditProfileUiState
    data object Loading : EditProfileUiState
    data object Success : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState

    /** Distinct from [Error] because the Fragment reacts with a timed red border, not just a Snackbar. */
    data class NameTaken(val message: String) : EditProfileUiState
}

sealed interface PhotoUploadUiState {
    data object Idle : PhotoUploadUiState
    data object Loading : PhotoUploadUiState
    data object Success : PhotoUploadUiState
    data class Error(val message: String) : PhotoUploadUiState
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _photoUploadState = MutableStateFlow<PhotoUploadUiState>(PhotoUploadUiState.Idle)
    val photoUploadState: StateFlow<PhotoUploadUiState> = _photoUploadState.asStateFlow()

    fun currentDisplayName(): String = authRepository.getCurrentUserDisplayName().orEmpty()

    fun currentPhotoUrl(): String? = authRepository.getCurrentUserPhotoUrl()

    fun uploadProfilePhoto(imageBytes: ByteArray) {
        _photoUploadState.value = PhotoUploadUiState.Loading
        viewModelScope.launch {
            authRepository.updateProfilePhoto(imageBytes)
                .onSuccess { _photoUploadState.value = PhotoUploadUiState.Success }
                .onFailure {
                    _photoUploadState.value =
                        PhotoUploadUiState.Error(it.message ?: "No se pudo actualizar la foto de perfil.")
                }
        }
    }

    /** The name shown across the drawer — persisted on the Firebase Auth user profile itself
     * (`displayName`), so it survives reinstalls: signing back in on any device pulls the same
     * value from the account, same as the profile photo. */
    fun saveDisplayName(fullName: String) {
        val trimmedName = fullName.trim()
        when {
            trimmedName.isEmpty() -> {
                _uiState.value = EditProfileUiState.Error("El nombre no puede estar vacío.")
                return
            }
            trimmedName.length < MIN_NAME_LENGTH || trimmedName.length > MAX_NAME_LENGTH -> {
                _uiState.value = EditProfileUiState.Error(
                    "El nombre debe tener entre $MIN_NAME_LENGTH y $MAX_NAME_LENGTH caracteres.",
                )
                return
            }
        }
        _uiState.value = EditProfileUiState.Loading
        viewModelScope.launch {
            authRepository.updateDisplayName(trimmedName)
                .onSuccess { _uiState.value = EditProfileUiState.Success }
                .onFailure { error ->
                    _uiState.value = if (error is UsernameTakenException) {
                        EditProfileUiState.NameTaken(error.message ?: "Ese nombre de usuario ya está en uso.")
                    } else {
                        EditProfileUiState.Error(error.message ?: "No se pudo actualizar el perfil.")
                    }
                }
        }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 6
        const val MAX_NAME_LENGTH = 16
    }
}
