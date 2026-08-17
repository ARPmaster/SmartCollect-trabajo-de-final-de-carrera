package com.example.aicollect.presentation.auth

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

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data object Success : RegisterUiState
    data class Error(val message: String) : RegisterUiState

    /** Distinct from [Error] because the Fragment reacts with a timed red border, not just a Snackbar. */
    data class UsernameTaken(val message: String) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun signUp(email: String, username: String, password: String, confirmPassword: String) {
        val trimmedUsername = username.trim()
        when {
            email.isBlank() || password.isBlank() -> {
                _uiState.value = RegisterUiState.Error("Completa correo y contraseña.")
                return
            }
            trimmedUsername.length < MIN_USERNAME_LENGTH || trimmedUsername.length > MAX_USERNAME_LENGTH -> {
                _uiState.value = RegisterUiState.Error(
                    "El nombre de usuario debe tener entre $MIN_USERNAME_LENGTH y $MAX_USERNAME_LENGTH caracteres.",
                )
                return
            }
            password.length < 6 -> {
                _uiState.value = RegisterUiState.Error("La contraseña debe tener al menos 6 caracteres.")
                return
            }
            password != confirmPassword -> {
                _uiState.value = RegisterUiState.Error("Las contraseñas no coinciden.")
                return
            }
        }
        _uiState.value = RegisterUiState.Loading
        viewModelScope.launch {
            authRepository.signUp(email, password, trimmedUsername)
                .onSuccess { _uiState.value = RegisterUiState.Success }
                .onFailure { error ->
                    _uiState.value = if (error is UsernameTakenException) {
                        RegisterUiState.UsernameTaken(error.message ?: "Ese nombre de usuario ya está en uso.")
                    } else {
                        RegisterUiState.Error(error.message ?: "No se pudo crear la cuenta.")
                    }
                }
        }
    }

    private companion object {
        const val MIN_USERNAME_LENGTH = 6
        const val MAX_USERNAME_LENGTH = 16
    }
}
