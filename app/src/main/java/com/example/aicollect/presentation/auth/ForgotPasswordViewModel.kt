/** ViewModel del bottom sheet de recuperación de contraseña: valida el correo introducido y pide
 * a AuthRepository el envío del email de restablecimiento, exponiendo el resultado como estado de UI.*/
package com.example.aicollect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.R
import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ForgotPasswordUiState {
    data object Idle : ForgotPasswordUiState
    data object Loading : ForgotPasswordUiState
    data object Success : ForgotPasswordUiState
    data class Error(val message: UiText) : ForgotPasswordUiState
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(UiText.StringResource(R.string.error_forgot_password_empty_email))
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            authRepository.sendPasswordResetEmail(email)
                .onSuccess { _uiState.value = ForgotPasswordUiState.Success }
                .onFailure {
                    _uiState.value = ForgotPasswordUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_forgot_password_generic),
                    )
                }
        }
    }
}
