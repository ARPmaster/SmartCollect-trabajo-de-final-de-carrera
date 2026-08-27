// Test unitario de LoginViewModel: validación de email/contraseña en blanco y el inicio de sesión feliz y con error del repositorio.
package com.example.aicollect.presentation.auth

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { LoginViewModel(authRepository) }

    @Test
    fun `blank email sets Error without calling the repository`() {
        viewModel.signIn(email = "", password = "secret123")

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `blank password sets Error without calling the repository`() {
        viewModel.signIn(email = "user@example.com", password = "")

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
        coVerify(exactly = 0) { authRepository.signIn(any(), any()) }
    }

    @Test
    fun `successful sign in transitions from Loading to Success`() {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)

        viewModel.signIn(email = "user@example.com", password = "secret123")
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(LoginUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.signIn("user@example.com", "secret123") }
    }

    @Test
    fun `failed sign in transitions from Loading to Error`() {
        coEvery { authRepository.signIn(any(), any()) } returns Result.failure(Exception("Credenciales inválidas"))

        viewModel.signIn(email = "user@example.com", password = "secret123")
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("Credenciales inválidas", (state as LoginUiState.Error).message)
    }
}
