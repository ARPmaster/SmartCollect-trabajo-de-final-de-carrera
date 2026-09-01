/** ViewModel de la pantalla de registro: valida correo, usuario y contraseñas, delega la creación
 * de cuenta en AuthRepository y expone el resultado como estado de UI.*/
package com.example.aicollect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.R
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.AuthValidation
import com.example.aicollect.application.auth.UsernameTakenException
import com.example.aicollect.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data object Success : RegisterUiState
    data class Error(val message: UiText) : RegisterUiState

    data class UsernameTaken(val message: UiText) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun signUp(email: String, username: String, password: String, confirmPassword: String) {
        val trimmedUsername = username.trim()
        val emailError = AuthValidation.emailError(email)
        val passwordError = AuthValidation.passwordError(password)
        when {
            emailError != null -> {
                _uiState.value = RegisterUiState.Error(UiText.DynamicString(emailError))
                return
            }
            trimmedUsername.length < MIN_USERNAME_LENGTH || trimmedUsername.length > MAX_USERNAME_LENGTH -> {
                _uiState.value = RegisterUiState.Error(
                    UiText.StringResource(
                        R.string.error_register_username_length,
                        listOf(MIN_USERNAME_LENGTH, MAX_USERNAME_LENGTH),
                    ),
                )
                return
            }
            passwordError != null -> {
                _uiState.value = RegisterUiState.Error(UiText.DynamicString(passwordError))
                return
            }
            password != confirmPassword -> {
                _uiState.value = RegisterUiState.Error(UiText.StringResource(R.string.error_passwords_dont_match))
                return
            }
        }
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            authRepository.signUp(email, password, trimmedUsername)
                .onSuccess { _uiState.value = RegisterUiState.Success }
                .onFailure { error ->
                    _uiState.value = if (error is UsernameTakenException) {
                        RegisterUiState.UsernameTaken(
                            error.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_username_taken),
                        )
                    } else {
                        RegisterUiState.Error(
                            error.message?.let(UiText::DynamicString)
                                ?: UiText.StringResource(R.string.error_register_generic),
                        )
                    }
                }
        }
    }

    private companion object {
        const val MIN_USERNAME_LENGTH = 6
        const val MAX_USERNAME_LENGTH = 16
    }
}
