// ViewModel de la pantalla de login: valida las credenciales, delega el inicio de sesión en AuthRepository y expone el resultado como estado de UI.
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

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: UiText) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun isAlreadySignedIn(): Boolean = authRepository.getCurrentUserId() != null

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error(UiText.StringResource(R.string.error_login_missing_fields))
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess { _uiState.value = LoginUiState.Success }
                .onFailure {
                    _uiState.value = LoginUiState.Error(
                        it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.error_login_generic),
                    )
                }
        }
    }
}
