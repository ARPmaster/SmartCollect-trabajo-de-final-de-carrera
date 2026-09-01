// Test unitario de EditProfileViewModel: validación de nombre, guardado feliz/con nombre en uso, subida de foto de perfil y getters delegados en el repositorio.
package com.example.aicollect.presentation.settings

import com.example.aicollect.application.auth.AuthRepository
import com.example.aicollect.application.auth.UsernameTakenException
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val viewModel by lazy { EditProfileViewModel(authRepository) }

    @Test
    fun `blank name sets Error without calling the repository`() {
        viewModel.saveDisplayName("   ")

        assertTrue(viewModel.uiState.value is EditProfileUiState.Error)
        coVerify(exactly = 0) { authRepository.updateDisplayName(any()) }
    }

    @Test
    fun `name shorter than 6 characters sets Error without calling the repository`() {
        viewModel.saveDisplayName("abc")

        assertTrue(viewModel.uiState.value is EditProfileUiState.Error)
        coVerify(exactly = 0) { authRepository.updateDisplayName(any()) }
    }

    @Test
    fun `name longer than 16 characters sets Error without calling the repository`() {
        viewModel.saveDisplayName("a".repeat(17))

        assertTrue(viewModel.uiState.value is EditProfileUiState.Error)
        coVerify(exactly = 0) { authRepository.updateDisplayName(any()) }
    }

    @Test
    fun `successful save transitions from Loading to Success and trims the name`() {
        coEvery { authRepository.updateDisplayName(any()) } returns Result.success(Unit)

        viewModel.saveDisplayName("  collector1  ")
        assertEquals(EditProfileUiState.Loading, viewModel.uiState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(EditProfileUiState.Success, viewModel.uiState.value)
        coVerify(exactly = 1) { authRepository.updateDisplayName("collector1") }
    }

    @Test
    fun `save failing with UsernameTakenException transitions to NameTaken, not Error`() {
        coEvery { authRepository.updateDisplayName(any()) } returns Result.failure(UsernameTakenException())

        viewModel.saveDisplayName("collector1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditProfileUiState.NameTaken)
        assertEquals(
            UiText.StringResource(R.string.error_username_taken),
            (state as EditProfileUiState.NameTaken).message,
        )
    }

    @Test
    fun `save failing with a generic error transitions to Error`() {
        coEvery { authRepository.updateDisplayName(any()) } returns Result.failure(Exception("Fallo de red"))

        viewModel.saveDisplayName("collector1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditProfileUiState.Error)
        assertEquals(UiText.DynamicString("Fallo de red"), (state as EditProfileUiState.Error).message)
    }

    @Test
    fun `successful photo upload transitions photoUploadState from Loading to Success`() {
        coEvery { authRepository.updateProfilePhoto(any()) } returns Result.success("https://example.com/photo.jpg")
        val bytes = ByteArray(4)

        viewModel.uploadProfilePhoto(bytes)
        assertEquals(PhotoUploadUiState.Loading, viewModel.photoUploadState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(PhotoUploadUiState.Success, viewModel.photoUploadState.value)
        coVerify(exactly = 1) { authRepository.updateProfilePhoto(bytes) }
    }

    @Test
    fun `failed photo upload transitions photoUploadState to Error`() {
        coEvery { authRepository.updateProfilePhoto(any()) } returns Result.failure(Exception("No se pudo subir"))

        viewModel.uploadProfilePhoto(ByteArray(4))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.photoUploadState.value
        assertTrue(state is PhotoUploadUiState.Error)
        assertEquals(UiText.DynamicString("No se pudo subir"), (state as PhotoUploadUiState.Error).message)
    }

    @Test
    fun `currentDisplayName and currentPhotoUrl delegate to the repository`() {
        every { authRepository.getCurrentUserDisplayName() } returns "collector1"
        every { authRepository.getCurrentUserPhotoUrl() } returns "https://example.com/photo.jpg"

        assertEquals("collector1", viewModel.currentDisplayName())
        assertEquals("https://example.com/photo.jpg", viewModel.currentPhotoUrl())
    }
}
