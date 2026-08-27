// Test unitario de EditItemViewModel: carga feliz/con error del ítem, validación de campos obligatorios y guardado de la edición.
package com.example.aicollect.presentation.edititem

import androidx.lifecycle.SavedStateHandle
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditItemViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = mockk<ItemRepository>()

    private fun sampleItem() = Item(
        id = "item-1",
        nombre = "Hoka Clifton 8",
        descripcion = "Zapatillas de running",
        marca = "Hoka",
        modelo = "Clifton 8",
        edicion = null,
        procedencia = null,
        deporte = "Running",
        estado = "Nuevo",
        imageUrls = listOf("https://example.com/foto1.jpg"),
        valoracionActual = 120.0,
        valoracionMin = 108.0,
        valoracionMax = 120.0,
        valoracionMoneda = "EUR",
        fuenteValoracion = "gemini_grounded_search",
        confianzaIdentificacion = 0.9,
        createdAt = 1000L,
        updatedAt = 1000L,
    )

    private fun buildViewModel(): EditItemViewModel =
        EditItemViewModel(itemRepository, SavedStateHandle(mapOf("itemId" to "item-1")))

    @Test
    fun `successful load exposes Content with the item's editable fields`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())

        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditItemUiState.Content)
        state as EditItemUiState.Content
        assertEquals("Hoka Clifton 8", state.nombre)
        assertEquals("Zapatillas de running", state.descripcion)
        assertEquals("Running", state.deporte)
        assertEquals("Nuevo", state.estado)
    }

    @Test
    fun `failed load exposes Error`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.failure(Exception("Sin conexión"))

        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is EditItemUiState.Error)
        assertEquals("Sin conexión", (state as EditItemUiState.Error).message)
    }

    @Test
    fun `blank name sets ValidationError without calling the repository`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("   ", "desc", "Running", "Nuevo")

        val state = viewModel.saveState.value
        assertTrue(state is SaveEditUiState.ValidationError)
        assertEquals(EditRequiredField.NAME, (state as SaveEditUiState.ValidationError).field)
        coVerify(exactly = 0) { itemRepository.updateItem(any(), any()) }
    }

    @Test
    fun `null sport sets ValidationError without calling the repository`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("Hoka Clifton 8", "desc", null, "Nuevo")

        val state = viewModel.saveState.value
        assertTrue(state is SaveEditUiState.ValidationError)
        assertEquals(EditRequiredField.SPORT, (state as SaveEditUiState.ValidationError).field)
    }

    @Test
    fun `null condition sets ValidationError without calling the repository`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("Hoka Clifton 8", "desc", "Running", null)

        val state = viewModel.saveState.value
        assertTrue(state is SaveEditUiState.ValidationError)
        assertEquals(EditRequiredField.CONDITION, (state as SaveEditUiState.ValidationError).field)
    }

    @Test
    fun `saving before the item finished loading sets Error without calling the repository`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val viewModel = buildViewModel()

        viewModel.save("Hoka Clifton 8", "desc", "Running", "Nuevo")

        assertTrue(viewModel.saveState.value is SaveEditUiState.Error)
        coVerify(exactly = 0) { itemRepository.updateItem(any(), any()) }
    }

    @Test
    fun `successful save sends a copy of the loaded item with only the 4 editable fields changed`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val itemSlot = slot<Item>()
        coEvery { itemRepository.updateItem("item-1", capture(itemSlot)) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("  Hoka Clifton 8 X  ", "  Nueva descripción  ", "Baloncesto", "Buen estado")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveEditUiState.Success, viewModel.saveState.value)
        val sent = itemSlot.captured
        assertEquals("Hoka Clifton 8 X", sent.nombre)
        assertEquals("Nueva descripción", sent.descripcion)
        assertEquals("Baloncesto", sent.deporte)
        assertEquals("Buen estado", sent.estado)
        assertEquals("Hoka", sent.marca)
        assertEquals("Clifton 8", sent.modelo)
        assertEquals(120.0, sent.valoracionActual)
        assertEquals(listOf("https://example.com/foto1.jpg"), sent.imageUrls)
    }

    @Test
    fun `blank description is sent as null, same trimming rule as the rest of the app`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        val itemSlot = slot<Item>()
        coEvery { itemRepository.updateItem("item-1", capture(itemSlot)) } returns Result.success(Unit)

        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("Hoka Clifton 8", "   ", "Running", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, itemSlot.captured.descripcion)
    }

    @Test
    fun `failed save sets Error with the repository's message`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(sampleItem())
        coEvery { itemRepository.updateItem(any(), any()) } returns Result.failure(Exception("Sin conexión"))

        val viewModel = buildViewModel()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.save("Hoka Clifton 8", "desc", "Running", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveEditUiState.Error)
        assertEquals("Sin conexión", (state as SaveEditUiState.Error).message)
    }
}
