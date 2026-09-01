// ViewModel de "Editar Perfil": expone el nombre y foto actuales, valida y guarda el nuevo
// nombre (detectando si ya está en uso) y sube la nueva foto de perfil.
package com.example.aicollect.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.UsernameTakenException
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
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
    data class Error(val message: UiText) : EditProfileUiState
    data class NameTaken(val message: UiText) : EditProfileUiState
}

sealed interface PhotoUploadUiState {
    data object Idle : PhotoUploadUiState
    data object Loading : PhotoUploadUiState
    data object Success : PhotoUploadUiState
    data class Error(val message: UiText) : PhotoUploadUiState
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
                    _photoUploadState.value = PhotoUploadUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_edit_profile_photo_generic),
                    )
                }
        }
    }

    fun saveDisplayName(fullName: String) {
        val trimmedName = fullName.trim()
        when {
            trimmedName.isEmpty() -> {
                _uiState.value = EditProfileUiState.Error(UiText.StringResource(R.string.error_edit_profile_name_blank))
                return
            }
            trimmedName.length < MIN_NAME_LENGTH || trimmedName.length > MAX_NAME_LENGTH -> {
                _uiState.value = EditProfileUiState.Error(
                    UiText.StringResource(R.string.error_edit_profile_name_length, listOf(MIN_NAME_LENGTH, MAX_NAME_LENGTH)),
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
                        EditProfileUiState.NameTaken(
                            error.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_username_taken),
                        )
                    } else {
                        EditProfileUiState.Error(
                            error.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_edit_profile_generic),
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_NAME_LENGTH = 6
        const val MAX_NAME_LENGTH = 16
    }
}
