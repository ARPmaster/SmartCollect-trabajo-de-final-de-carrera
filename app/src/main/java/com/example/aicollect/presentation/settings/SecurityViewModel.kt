// ViewModel de "Seguridad": valida y aplica cambios de email/contraseña, y gestiona la
// eliminación de cuenta reautenticando al usuario antes de borrarla.
package com.example.aicollect.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthError
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.AuthValidation
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import com.example.aicollect.presentation.auth.asUiText
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
    data class Error(val message: UiText) : SecurityUiState
}

sealed interface DeleteAccountUiState {
    data object Idle : DeleteAccountUiState
    data object Deleting : DeleteAccountUiState
    data object Success : DeleteAccountUiState
    data class Error(val message: UiText) : DeleteAccountUiState
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
            _deleteAccountState.value =
                DeleteAccountUiState.Error(UiText.StringResource(R.string.error_security_confirm_password_required))
            return
        }
        _deleteAccountState.value = DeleteAccountUiState.Deleting
        viewModelScope.launch {
            val reauthResult = authRepository.reauthenticate(password)
            if (reauthResult.isFailure) {
                val error = reauthResult.exceptionOrNull()
                _deleteAccountState.value = DeleteAccountUiState.Error(
                    if (error is AuthError.WrongPassword) {
                        UiText.StringResource(R.string.error_security_wrong_password)
                    } else {
                        error?.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_security_reauth_generic)
                    },
                )
                return@launch
            }
            authRepository.deleteAccount()
                .onSuccess { _deleteAccountState.value = DeleteAccountUiState.Success }
                .onFailure {
                    _deleteAccountState.value = DeleteAccountUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_security_delete_account_generic),
                    )
                }
        }
    }

    fun saveChanges(email: String, newPassword: String, confirmPassword: String) {
        val emailChanged = email.isNotBlank() && email != currentEmail()
        val passwordProvided = newPassword.isNotBlank() || confirmPassword.isNotBlank()

        if (!emailChanged && !passwordProvided) {
            _uiState.value = SecurityUiState.Error(UiText.StringResource(R.string.error_security_no_changes))
            return
        }
        if (emailChanged) {
            val emailError = AuthValidation.emailError(email)
            if (emailError != null) {
                _uiState.value = SecurityUiState.Error(emailError.asUiText())
                return
            }
        }
        if (passwordProvided) {
            val passwordError = AuthValidation.passwordError(newPassword)
            if (passwordError != null) {
                _uiState.value = SecurityUiState.Error(passwordError.asUiText())
                return
            }
            if (newPassword != confirmPassword) {
                _uiState.value = SecurityUiState.Error(UiText.StringResource(R.string.error_passwords_dont_match))
                return
            }
        }

        _uiState.value = SecurityUiState.Loading
        viewModelScope.launch {
            if (emailChanged) {
                val result = authRepository.updateEmail(email)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    _uiState.value = SecurityUiState.Error(
                        if (error is AuthError.RecentLoginRequired) {
                            UiText.StringResource(R.string.error_security_recent_login_required)
                        } else {
                            error?.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_security_update_email_generic)
                        },
                    )
                    return@launch
                }
            }
            if (passwordProvided) {
                val result = authRepository.updatePassword(newPassword)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    _uiState.value = SecurityUiState.Error(
                        if (error is AuthError.RecentLoginRequired) {
                            UiText.StringResource(R.string.error_security_recent_login_required)
                        } else {
                            error?.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_security_update_password_generic)
                        },
                    )
                    return@launch
                }
            }
            _uiState.value = SecurityUiState.Success
        }
    }
}
