// Test unitario de ForgotPasswordViewModel: validación de email en blanco y envío feliz/con error del correo de restablecimiento de contraseña.
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

class ForgotPasswordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { ForgotPasswordViewModel(authRepository) }

    @Test
    fun `blank email sets Error without calling the repository`() {
        viewModel.sendResetEmail("   ")

        assertTrue(viewModel.uiState.value is ForgotPasswordUiState.Error)
        coVerify(exactly = 0) { authRepository.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `successful send transitions from Loading to Success`() {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.success(Unit)

        viewModel.sendResetEmail("user@example.com")
        assertEquals(ForgotPasswordUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ForgotPasswordUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.sendPasswordResetEmail("user@example.com") }
    }

    @Test
    fun `failed send transitions from Loading to Error with the repository's message`() {
        coEvery { authRepository.sendPasswordResetEmail(any()) } returns Result.failure(Exception("No existe esa cuenta"))

        viewModel.sendResetEmail("user@example.com")
        assertEquals(ForgotPasswordUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ForgotPasswordUiState.Error)
        assertEquals("No existe esa cuenta", (state as ForgotPasswordUiState.Error).message)
    }
}
