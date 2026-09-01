// Test unitario de SecurityViewModel: cambio de email/contraseña (por separado y combinados, con sus validaciones) y eliminación de cuenta con reautenticación previa.
package com.example.aicollect.presentation.settings

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.presentation.UiText
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

    private val validPassword = "Secret123!"

    @Before
    fun setUp() {
        every { authRepository.getCurrentUserEmail() } returns "old@gmail.com"
    }

    @Test
    fun `no changes sets Error without calling the repository`() {
        viewModel.saveChanges(email = "old@gmail.com", newPassword = "", confirmPassword = "")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updateEmail(any()) }
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `malformed new email sets Error without calling the repository`() {
        viewModel.saveChanges(email = "newgmail.com", newPassword = "", confirmPassword = "")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updateEmail(any()) }
    }

    @Test
    fun `new email with an institutional domain calls updateEmail`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "new@uvigo.es", newPassword = "", confirmPassword = "")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateEmail("new@uvigo.es") }
    }

    @Test
    fun `new password shorter than 8 characters sets Error without calling the repository`() {
        viewModel.saveChanges(email = "old@gmail.com", newPassword = "Abc12!", confirmPassword = "Abc12!")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `new password without a number sets Error without calling the repository`() {
        viewModel.saveChanges(email = "old@gmail.com", newPassword = "Secretpass!", confirmPassword = "Secretpass!")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `mismatched passwords set Error without calling the repository`() {
        viewModel.saveChanges(email = "old@gmail.com", newPassword = validPassword, confirmPassword = "Different123!")

        assertTrue(viewModel.uiState.value is SecurityUiState.Error)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `email-only change calls updateEmail but not updatePassword`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "new@gmail.com", newPassword = "", confirmPassword = "")
        assertEquals(SecurityUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateEmail("new@gmail.com") }
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `password-only change calls updatePassword but not updateEmail`() {
        coEvery { authRepository.updatePassword(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "old@gmail.com", newPassword = validPassword, confirmPassword = validPassword)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updatePassword(validPassword) }
        coVerify(exactly = 0) { authRepository.updateEmail(any()) }
    }

    @Test
    fun `changing both email and password calls both repository methods`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.success(Unit)
        coEvery { authRepository.updatePassword(any()) } returns Result.success(Unit)

        viewModel.saveChanges(email = "new@gmail.com", newPassword = validPassword, confirmPassword = validPassword)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SecurityUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateEmail("new@gmail.com") }
        coVerify(exactly = 1) { authRepository.updatePassword(validPassword) }
    }

    @Test
    fun `updateEmail failure stops before calling updatePassword`() {
        coEvery { authRepository.updateEmail(any()) } returns Result.failure(Exception("Requiere sesión reciente"))

        viewModel.saveChanges(email = "new@gmail.com", newPassword = validPassword, confirmPassword = validPassword)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SecurityUiState.Error)
        assertEquals(UiText.DynamicString("Requiere sesión reciente"), (state as SecurityUiState.Error).message)
        coVerify(exactly = 0) { authRepository.updatePassword(any()) }
    }

    @Test
    fun `updatePassword failure surfaces its message`() {
        coEvery { authRepository.updatePassword(any()) } returns Result.failure(Exception("Contraseña débil"))

        viewModel.saveChanges(email = "old@gmail.com", newPassword = validPassword, confirmPassword = validPassword)
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SecurityUiState.Error)
        assertEquals(UiText.DynamicString("Contraseña débil"), (state as SecurityUiState.Error).message)
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
        assertEquals(UiText.DynamicString("Sin conexión"), (state as DeleteAccountUiState.Error).message)
        coVerify(exactly = 0) { authRepository.deleteAccount() }
    }

    @Test
    fun `deleteAccount stops before calling deleteAccount when reauthenticate fails`() {
        coEvery { authRepository.reauthenticate(any()) } returns Result.failure(Exception("Contraseña incorrecta."))

        viewModel.deleteAccount("clave-mala")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.deleteAccountState.value
        assertTrue(state is DeleteAccountUiState.Error)
        assertEquals(UiText.DynamicString("Contraseña incorrecta."), (state as DeleteAccountUiState.Error).message)
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
