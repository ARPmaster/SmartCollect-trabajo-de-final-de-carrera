// Test unitario de MyVaultViewModel: estado vacío, agregados sobre la colección y el efecto de filtrar por deporte.
package com.example.aicollect.presentation.collection

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.presentation.UiText
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyVaultViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = mockk<ItemRepository>()

    private fun buildViewModel(): MyVaultViewModel = MyVaultViewModel(itemRepository)

    private fun item(
        nombre: String,
        deporte: String = "Running",
        estado: String = "Nuevo",
        valoracionActual: Double? = null,
    ) = Item(
        nombre = nombre,
        descripcion = null,
        marca = "",
        modelo = "",
        edicion = null,
        procedencia = null,
        deporte = deporte,
        estado = estado,
        valoracionActual = valoracionActual,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        confianzaIdentificacion = null,
    )

    @Test
    fun `uiState is Empty for an empty collection`() = runTest(mainDispatcherRule.dispatcher) {
        every { itemRepository.observeItems() } returns flowOf(emptyList())
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(MyVaultUiState.Empty, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `uiState with no sport selected computes totals and distribution over every item`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(
            item("A", deporte = "Baloncesto", valoracionActual = 100.0),
            item("B", deporte = "Running", valoracionActual = 50.0),
        )
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyVaultUiState.Content)
        state as MyVaultUiState.Content
        assertEquals(null, state.selectedSport)
        assertEquals("2", state.totalItemsLabel)
        assertTrue(state.hasItemsForSelectedSport)
        assertEquals(setOf("A", "B"), state.topValuedItems.map { it.nombre }.toSet())
        job.cancel()
    }

    @Test
    fun `selectSport restricts topValuedItems and conditionPercentByEstado but not sportDistribution or totals`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(
            item("A", deporte = "Baloncesto", estado = "Nuevo", valoracionActual = 100.0),
            item("B", deporte = "Running", estado = "Usado", valoracionActual = 50.0),
        )
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val contentBefore = viewModel.uiState.value as MyVaultUiState.Content

        viewModel.selectSport("Baloncesto")
        advanceUntilIdle()

        val state = viewModel.uiState.value as MyVaultUiState.Content
        assertEquals("Baloncesto", state.selectedSport)
        assertEquals(listOf("A"), state.topValuedItems.map { it.nombre })
        assertEquals(mapOf("Nuevo" to 100), state.conditionPercentByEstado)
        assertEquals(contentBefore.sportDistribution, state.sportDistribution)
        assertEquals(contentBefore.totalValueLabel, state.totalValueLabel)
        job.cancel()
    }

    @Test
    fun `selectSport with a sport that has no items reports hasItemsForSelectedSport false`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(item("A", deporte = "Running", valoracionActual = 50.0))
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectSport("Baloncesto")
        advanceUntilIdle()

        val state = viewModel.uiState.value as MyVaultUiState.Content
        assertEquals(false, state.hasItemsForSelectedSport)
        assertTrue(state.topValuedItems.isEmpty())
        job.cancel()
    }

    @Test
    fun `an upstream failure from observeItems surfaces as Error`() = runTest(mainDispatcherRule.dispatcher) {
        every { itemRepository.observeItems() } returns flow<List<Item>> { throw RuntimeException("Fallo de Firestore") }
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyVaultUiState.Error)
        assertEquals(UiText.DynamicString("Fallo de Firestore"), (state as MyVaultUiState.Error).message)
        job.cancel()
    }
}
