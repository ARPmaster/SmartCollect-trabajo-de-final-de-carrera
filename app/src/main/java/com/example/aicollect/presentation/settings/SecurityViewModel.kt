// ViewModel de "Seguridad": valida y aplica cambios de email/contraseña, y gestiona la
// eliminación de cuenta reautenticando al usuario antes de borrarla.
package com.example.aicollect.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.AuthValidation
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

sealed interface DeleteAccountUiState {
    data object Idle : DeleteAccountUiState
    data object Deleting : DeleteAccountUiState
    data object Success : DeleteAccountUiState
    data class Error(val message: String) : DeleteAccountUiState
}

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SecurityUiState>(SecurityUiState.Idle)
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Idle)
    val deleteAccountState: StateFlow<DeleteAccountUiState> = _deleteAccountState.asStateFlow()

    fun currentEmail(): String = authRepository.getCurrentUserEmail().orEmpty()

    fun deleteAccount(password: String) {
        if (password.isBlank()) {
            _deleteAccountState.value = DeleteAccountUiState.Error("Introduce tu contraseña para confirmar.")
            return
        }
        _deleteAccountState.value = DeleteAccountUiState.Deleting
        viewModelScope.launch {
            val reauthResult = authRepository.reauthenticate(password)
            if (reauthResult.isFailure) {
                _deleteAccountState.value = DeleteAccountUiState.Error(
                    reauthResult.exceptionOrNull()?.message ?: "No se pudo verificar tu contraseña. Inténtalo de nuevo.",
                )
                return@launch
            }
            authRepository.deleteAccount()
                .onSuccess { _deleteAccountState.value = DeleteAccountUiState.Success }
                .onFailure {
                    _deleteAccountState.value = DeleteAccountUiState.Error(
                        it.message ?: "No se pudo eliminar la cuenta. Inténtalo de nuevo.",
                    )
                }
        }
    }

    fun saveChanges(email: String, newPassword: String, confirmPassword: String) {
        val emailChanged = email.isNotBlank() && email != currentEmail()
        val passwordProvided = newPassword.isNotBlank() || confirmPassword.isNotBlank()

        if (!emailChanged && !passwordProvided) {
            _uiState.value = SecurityUiState.Error("No hay cambios que guardar.")
            return
        }
        if (emailChanged) {
            val emailError = AuthValidation.emailError(email)
            if (emailError != null) {
                _uiState.value = SecurityUiState.Error(emailError)
                return
            }
        }
        if (passwordProvided) {
            val passwordError = AuthValidation.passwordError(newPassword)
            if (passwordError != null) {
                _uiState.value = SecurityUiState.Error(passwordError)
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
