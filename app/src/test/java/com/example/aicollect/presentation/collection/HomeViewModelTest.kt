// Test unitario de HomeViewModel: colección vacía, filtros por precio/deporte/estado, orden de resultados y fallo del repositorio al observar los ítems.
package com.example.aicollect.presentation.collection

import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.ItemSortOption
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
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val itemRepository = mockk<ItemRepository>()

    private fun buildViewModel(): HomeViewModel = HomeViewModel(itemRepository)

    private fun item(
        nombre: String,
        deporte: String = "Running",
        estado: String = "Nuevo",
        valoracionActual: Double? = null,
        createdAt: Long = 0L,
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
        createdAt = createdAt,
    )

    @Test
    fun `uiState exposes Content with every item visible when no filters are applied`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(item("Nike", deporte = "Baloncesto", valoracionActual = 100.0), item("Hoka", deporte = "Running", valoracionActual = 50.0))
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Content)
        state as HomeUiState.Content
        assertEquals(false, state.isCollectionEmpty)
        assertEquals(false, state.hasNoFilterResults)
        assertEquals(listOf("Nike", "Hoka"), state.visibleItems.map { it.nombre })
        job.cancel()
    }

    @Test
    fun `uiState marks isCollectionEmpty true for an empty collection`() = runTest(mainDispatcherRule.dispatcher) {
        every { itemRepository.observeItems() } returns flowOf(emptyList())
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertTrue(state.isCollectionEmpty)
        assertTrue(state.visibleItems.isEmpty())
        job.cancel()
    }

    @Test
    fun `setFilters filters visible items by price range`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(item("Barato", valoracionActual = 50.0), item("Caro", valoracionActual = 500.0))
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setFilters(minPrice = 0, maxPrice = 100, sport = null, condition = null, sort = ItemSortOption.DEFAULT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(listOf("Barato"), state.visibleItems.map { it.nombre })
        job.cancel()
    }

    @Test
    fun `setFilters filters visible items by sport and condition together`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(
            item("A", deporte = "Baloncesto", estado = "Nuevo"),
            item("B", deporte = "Baloncesto", estado = "Usado"),
            item("C", deporte = "Running", estado = "Nuevo"),
        )
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setFilters(minPrice = 0, maxPrice = Int.MAX_VALUE, sport = "Baloncesto", condition = "Nuevo", sort = ItemSortOption.DEFAULT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(listOf("A"), state.visibleItems.map { it.nombre })
        job.cancel()
    }

    @Test
    fun `hasNoFilterResults is true when a filter excludes everything from a non-empty collection`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(item("Nike", valoracionActual = 500.0))
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setFilters(minPrice = 0, maxPrice = 10, sport = null, condition = null, sort = ItemSortOption.DEFAULT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(false, state.isCollectionEmpty)
        assertTrue(state.hasNoFilterResults)
        assertTrue(state.visibleItems.isEmpty())
        job.cancel()
    }

    @Test
    fun `setFilters sorts visible items by the chosen option`() = runTest(mainDispatcherRule.dispatcher) {
        val items = listOf(item("Barato", valoracionActual = 10.0), item("Caro", valoracionActual = 200.0))
        every { itemRepository.observeItems() } returns flowOf(items)
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.setFilters(minPrice = 0, maxPrice = Int.MAX_VALUE, sport = null, condition = null, sort = ItemSortOption.PRICE_DESC)
        advanceUntilIdle()

        val state = viewModel.uiState.value as HomeUiState.Content
        assertEquals(listOf("Caro", "Barato"), state.visibleItems.map { it.nombre })
        job.cancel()
    }

    @Test
    fun `an upstream failure from observeItems surfaces as Error`() = runTest(mainDispatcherRule.dispatcher) {
        every { itemRepository.observeItems() } returns flow<List<Item>> { throw RuntimeException("Fallo de Firestore") }
        val viewModel = buildViewModel()
        val job = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertEquals("Fallo de Firestore", (state as HomeUiState.Error).message)
        job.cancel()
    }
}
