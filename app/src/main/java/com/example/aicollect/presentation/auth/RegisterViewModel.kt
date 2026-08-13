package com.example.aicollect.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.auth.AuthRepository
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
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun signUp(email: String, password: String, confirmPassword: String) {
        when {
            email.isBlank() || password.isBlank() -> {
                _uiState.value = RegisterUiState.Error("Completa correo y contraseña.")
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
            authRepository.signUp(email, password)
                .onSuccess { _uiState.value = RegisterUiState.Success }
                .onFailure { _uiState.value = RegisterUiState.Error(it.message ?: "No se pudo crear la cuenta.") }
        }
    }
}
