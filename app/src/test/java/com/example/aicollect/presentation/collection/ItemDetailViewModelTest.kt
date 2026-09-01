// Test unitario de ItemDetailViewModel: carga feliz/con error de un ítem, refresco silencioso de la valoración y eliminación del ítem.
package com.example.aicollect.presentation.collection

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PricePoint
import com.example.aicollect.presentation.UiText
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ItemDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = mockk<ItemRepository>()
    private val viewModel by lazy { ItemDetailViewModel(itemRepository) }

    private val ONE_DAY_MS = 24L * 60 * 60 * 1000
    private val THIRTY_ONE_DAYS_MS = 31L * ONE_DAY_MS

    private fun item(
        id: String = "item-1",
        nombre: String = "Hoka Clifton 8",
        historialPrecios: List<PricePoint> = emptyList(),
        valoracionActual: Double? = 120.0,
    ) = Item(
        id = id,
        nombre = nombre,
        descripcion = null,
        marca = "Hoka",
        modelo = "Clifton 8",
        edicion = null,
        procedencia = null,
        deporte = "Running",
        estado = "Nuevo",
        imageUrls = listOf("https://example.com/foto.jpg"),
        valoracionActual = valoracionActual,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        historialPrecios = historialPrecios,
        confianzaIdentificacion = null,
    )

    private fun freshHistorial() = listOf(PricePoint(fecha = System.currentTimeMillis() - ONE_DAY_MS, precio = 120.0))
    private fun staleHistorial() = listOf(PricePoint(fecha = System.currentTimeMillis() - THIRTY_ONE_DAYS_MS, precio = 100.0))

    @Test
    fun `load success exposes Content mapped from the item`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ItemDetailUiState.Content)
        state as ItemDetailUiState.Content
        assertEquals("Hoka Clifton 8", state.nombre)
        assertEquals("Running", state.deporte)
        assertEquals("Nuevo", state.estado)
        assertEquals("https://example.com/foto.jpg", state.imageUrl)
        assertEquals(UiText.DynamicString("120€"), state.priceLabel)
    }

    @Test
    fun `load failure exposes Error with the repository's message`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.failure(Exception("El artículo ya no existe."))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ItemDetailUiState.Error)
        assertEquals(UiText.DynamicString("El artículo ya no existe."), (state as ItemDetailUiState.Error).message)
    }

    @Test
    fun `load triggers a silent refresh when there is no price history yet`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = emptyList()))
        coEvery { itemRepository.refreshValuation("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { itemRepository.refreshValuation("item-1") }
    }

    @Test
    fun `load triggers a silent refresh when the last known price is older than 30 days`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = staleHistorial()))
        coEvery { itemRepository.refreshValuation("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { itemRepository.refreshValuation("item-1") }
    }

    @Test
    fun `load does not trigger a refresh when the last known price is recent`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { itemRepository.refreshValuation(any()) }
    }

    @Test
    fun `a failed silent refresh is swallowed and leaves the already-loaded content untouched`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = emptyList()))
        coEvery { itemRepository.refreshValuation("item-1") } returns Result.failure(Exception("Sin red"))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ItemDetailUiState.Content)
        assertEquals("Hoka Clifton 8", (state as ItemDetailUiState.Content).nombre)
    }

    @Test
    fun `a silent refresh superseded by a newer load does not overwrite the current item's state`() {
        val refreshDeferred = CompletableDeferred<Result<Item>>()
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(id = "item-1", nombre = "Item 1", historialPrecios = emptyList()))
        coEvery { itemRepository.refreshValuation("item-1") } coAnswers { refreshDeferred.await() }
        coEvery { itemRepository.getItem("item-2") } returns Result.success(item(id = "item-2", nombre = "Item 2", historialPrecios = freshHistorial()))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.load("item-2")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        refreshDeferred.complete(Result.success(item(id = "item-1", nombre = "Item 1 (refrescado)")))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ItemDetailUiState.Content)
        assertEquals("Item 2", (state as ItemDetailUiState.Content).nombre)
    }

    @Test
    fun `deleteItem before any load does nothing`() {
        viewModel.deleteItem()

        assertEquals(DeleteItemUiState.Idle, viewModel.deleteState.value)
        coVerify(exactly = 0) { itemRepository.deleteItem(any()) }
    }

    @Test
    fun `deleteItem success sets Success`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))
        coEvery { itemRepository.deleteItem("item-1") } returns Result.success(Unit)

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteItem()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(DeleteItemUiState.Success, viewModel.deleteState.value)
    }

    @Test
    fun `deleteItem failure sets Error with the repository's message`() {
        coEvery { itemRepository.getItem("item-1") } returns Result.success(item(historialPrecios = freshHistorial()))
        coEvery { itemRepository.deleteItem("item-1") } returns Result.failure(Exception("Sin conexión"))

        viewModel.load("item-1")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
        viewModel.deleteItem()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.deleteState.value
        assertTrue(state is DeleteItemUiState.Error)
        assertEquals(UiText.DynamicString("Sin conexión"), (state as DeleteItemUiState.Error).message)
    }
}
