package com.example.aicollect.presentation.settings

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SecurityViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { SecurityViewModel(authRepository) }

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserEmail() } returns "old@example.com"
    }

    @Test
    fun `no changes sets Error without calling the repository`() {
        viewModel.saveChanges(email = "old@example.com", newPassword = "", confirmPassword = "")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updateEmail(any()) }
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `new password shorter than 6 characters sets Error without calling the repository`() {
        viewModel.saveChanges(email = "old@example.com", newPassword = "abc12", confirmPassword = "abc12")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `mismatched passwords set Error without calling the repository`() {
        viewModel.saveChanges(email = "old@example.com", newPassword = "secret123", confirmPassword = "different123")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `email-only change calls updateEmail but not updatePassword`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "new@example.com", newPassword = "", confirmPassword = "")
        assertEquals(SecurityUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateEmail("new@example.com") }
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `password-only change calls updatePassword but not updateEmail`() {
        coEvery { authRepository.updatePassword(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "old@example.com", newPassword = "secret123", confirmPassword = "secret123")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updatePassword("secret123") }
        coVerify(exactly = 0) { authRepository.updateEmail(any()) }
    }

    @Test
    fun `changing both email and password calls both repository methods`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.success(Unit)
        coEvery { authRepository.updatePassword(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "new@example.com", newPassword = "secret123", confirmPassword = "secret123")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateEmail("new@example.com") }
        coVerify(exactly = 1) { authRepository.updatePassword("secret123") }
    }

    @Test
    fun `updateEmail failure stops before calling updatePassword`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.failure(Exception("Requiere sesión reciente"))

        viewModel.saveChanges(email = "new@example.com", newPassword = "secret123", confirmPassword = "secret123")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SecurityUiState.Error)
        assertEquals("Requiere sesión reciente", (state as SecurityUiState.Error).message)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `updatePassword failure surfaces its message`() {
        coEvery { authRepository.updatePassword(any()) } returns Result.failure(Exception("Contraseña débil"))

        viewModel.saveChanges(email = "old@example.com", newPassword = "secret123", confirmPassword = "secret123")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SecurityUiState.Error)
        assertEquals("Contraseña débil", (state as SecurityUiState.Error).message)
    }
}
