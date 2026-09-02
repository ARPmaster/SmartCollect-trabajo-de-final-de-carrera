// Test unitario de RegisterViewModel: validaciones de email/usuario/contraseña, registro feliz, error genérico y nombre de usuario ya en uso.
package com.example.aicollect.presentation.auth

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.UsernameTakenException
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { RegisterViewModel(authRepository) }

    private val validPassword = "Secret123!"

    @Test
    fun `blank email sets Error without calling the repository`() {
        viewModel.signUp(email = "", username = "collector1", password = validPassword, confirmPassword = validPassword)

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `malformed email sets Error without calling the repository`() {
        viewModel.signUp(
            email = "usergmail.com",
            username = "collector1",
            password = validPassword,
            confirmPassword = validPassword,
        )

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `institutional email domain is accepted, not just generic providers`() {
        coEvery { authRepository.signUp(any(), any(), any()) } returns Result.success(Unit)

        viewModel.signUp(
            email = "user@uvigo.es",
            username = "collector1",
            password = validPassword,
            confirmPassword = validPassword,
        )
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RegisterUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.signUp("user@uvigo.es", validPassword, "collector1") }
    }

    @Test
    fun `blank password sets Error without calling the repository`() {
        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = "", confirmPassword = "")

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `username shorter than 6 characters sets Error without calling the repository`() {
        viewModel.signUp(email = "user@gmail.com", username = "abc", password = validPassword, confirmPassword = validPassword)

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `username longer than 16 characters sets Error without calling the repository`() {
        viewModel.signUp(
            email = "user@gmail.com",
            username = "a".repeat(17),
            password = validPassword,
            confirmPassword = validPassword,
        )

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `password shorter than 8 characters sets Error without calling the repository`() {
        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = "Abc12!", confirmPassword = "Abc12!")

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `password without an uppercase letter sets Error without calling the repository`() {
        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = "secret123!", confirmPassword = "secret123!")

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `password without a symbol sets Error without calling the repository`() {
        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = "Secret123", confirmPassword = "Secret123")

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `mismatched passwords set Error without calling the repository`() {
        viewModel.signUp(
            email = "user@gmail.com",
            username = "collector1",
            password = validPassword,
            confirmPassword = "Different123!",
        )

        assertTrue(viewModel.uiState.value is RegisterUiState.Error)
        coVerify(exactly = 0) { authRepository.signUp(any(), any(), any()) }
    }

    @Test
    fun `successful sign up transitions from Loading to Success`() {
        coEvery { authRepository.signUp(any(), any(), any()) } returns Result.success(Unit)

        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = validPassword, confirmPassword = validPassword)
        assertEquals(RegisterUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(RegisterUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.signUp("user@gmail.com", validPassword, "collector1") }
    }

    @Test
    fun `failed sign up transitions from Loading to Error`() {
        coEvery { authRepository.signUp(any(), any(), any()) } returns Result.failure(Exception("El correo ya está en uso"))

        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = validPassword, confirmPassword = validPassword)
        assertEquals(RegisterUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals(UiText.DynamicString("El correo ya está en uso"), (state as RegisterUiState.Error).message)
    }

    @Test
    fun `sign up failing with UsernameTakenException transitions to UsernameTaken, not Error`() {
        coEvery { authRepository.signUp(any(), any(), any()) } returns Result.failure(UsernameTakenException())

        viewModel.signUp(email = "user@gmail.com", username = "collector1", password = validPassword, confirmPassword = validPassword)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.UsernameTaken)
        assertEquals(
            UiText.StringResource(R.string.error_username_taken),
            (state as RegisterUiState.UsernameTaken).message,
        )
    }
}
