// Test unitario de SecurityViewModel: cambio de email/contraseña (por separado y combinados, con sus validaciones) y eliminación de cuenta con reautenticación previa.
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

    @Test
    fun `deleteAccount with blank password sets Error without calling the repository`() {
        viewModel.deleteAccount("   ")

        assertTrue(viewModel.deleteAccountState.value is DeleteAccountUiState.Error)
        coVerify(exactly = 0) { authRepository.reauthenticate(any()) }
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `deleteAccount surfaces reauthenticate's real error message, not a hardcoded one`() {
        coEvery { authRepository.reauthenticate(any()) } returns Result.failure(Exception("Sin conexión"))

        viewModel.deleteAccount("clave-correcta")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.deleteAccountState.value
        assertTrue(state is DeleteAccountUiState.Error)
        assertEquals("Sin conexión", (state as DeleteAccountUiState.Error).message)
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `deleteAccount stops before calling deleteAccount when reauthenticate fails`() {
        coEvery { authRepository.reauthenticate(any()) } returns Result.failure(Exception("Contraseña incorrecta."))

        viewModel.deleteAccount("clave-mala")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.deleteAccountState.value
        assertTrue(state is DeleteAccountUiState.Error)
        assertEquals("Contraseña incorrecta.", (state as DeleteAccountUiState.Error).message)
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `deleteAccount success reauthenticates then deletes the account`() {
        coEvery { authRepository.reauthenticate(any()) } returns Result.success(Unit)
        coEvery { authRepository.deleteAccount() } returns Result.success(Unit)

        viewModel.deleteAccount("clave-correcta")
        assertEquals(DeleteAccountUiState.Deleting, viewModel.deleteAccountState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DeleteAccountUiState.Success, viewModel.deleteAccountState.value)
        coVerify(exactly = 1) { authRepository.reauthenticate("clave-correcta") }
        coVerify(exactly = 1) { authRepository.deleteAccount() }
    }
}
