package com.example.aicollect.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SecurityUiState {
    data object Idle : SecurityUiState
    data object Loading : SecurityUiState
    data object Success : SecurityUiState
    data class Error(val message: String) : SecurityUiState
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    fun currentEmail(): String = authRepository.getCurrentUserEmail().orEmpty()

    /** Applies whichever of email/password actually changed; both real Firebase Auth calls. */
    fun saveChanges(email: String, newPassword: String, confirmPassword: String) {
        val emailChanged = email.isNotBlank() && email != currentEmail()
        val passwordProvided = newPassword.isNotBlank() || confirmPassword.isNotBlank()

        if (!emailChanged && !passwordProvided) {
            _uiState.value = SecurityUiState.Error("No hay cambios que guardar.")
            return
        }
        if (passwordProvided) {
            if (newPassword.length < 6) {
                _uiState.value = SecurityUiState.Error("La nueva contraseña debe tener al menos 6 caracteres.")
                return
            }
            if (newPassword != confirmPassword) {
                _uiState.value = SecurityUiState.Error("Las contraseñas no coinciden.")
                return
            }
        }

        _uiState.value = SecurityUiState.Loading
        viewModelScope.launch {
            if (emailChanged) {
                val result = authRepository.updateEmail(email)
                if (result.isFailure) {
                    _uiState.value = SecurityUiState.Error(
                        result.exceptionOrNull()?.message ?: "No se pudo actualizar el correo.",
                    )
                    return@launch
                }
            }
            if (passwordProvided) {
                val result = authRepository.updatePassword(newPassword)
                if (result.isFailure) {
                    _uiState.value = SecurityUiState.Error(
                        result.exceptionOrNull()?.message ?: "No se pudo actualizar la contraseña.",
                    )
                    return@launch
                }
            }
            _uiState.value = SecurityUiState.Success
        }
    }
}
