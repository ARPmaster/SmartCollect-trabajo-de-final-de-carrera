// Test unitario de NewPostViewModel: reconocimiento de fotos, gestión de la lista de fotos, validación de campos y publicación de un ítem (incluida la detección de duplicados).
package com.example.aicollect.presentation.newpost

import android.content.Context
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import com.example.aicollect.presentation.UiText
import com.example.aicollect.presentation.isOnline
import com.example.aicollect.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NewPostViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recognitionRepository = mockk<RecognitionRepository>()
    private val itemRepository = mockk<ItemRepository>()
    private val context = mockk<Context>()

    @Before
    fun setUpConnectivity() {
        mockkStatic("com.example.aicollect.presentation.NetworkUtilsKt")
        every { context.isOnline() } returns true
    }

    @After
    fun tearDownConnectivity() {
        unmockkStatic("com.example.aicollect.presentation.NetworkUtilsKt")
    }

    private fun buildViewModel(): NewPostViewModel =
        NewPostViewModel(recognitionRepository, itemRepository, context)

    private fun existingItem(nombre: String) = Item(
        id = "existing-1",
        nombre = nombre,
        descripcion = null,
        marca = "",
        modelo = "",
        edicion = null,
        procedencia = null,
        deporte = "Running",
        estado = "Nuevo",
        valoracionActual = null,
        valoracionMin = null,
        valoracionMax = null,
        valoracionMoneda = "EUR",
        fuenteValoracion = null,
        confianzaIdentificacion = null,
    )

    private fun noValuationFound() = Result.success(
        ValuationResult(precio = null, min = null, max = null, moneda = "EUR", fuentes = emptyList()),
    )

    @Test
    fun `recognize success publishes candidates`() {
        val candidates = listOf(
            RankedCandidate("Hoka Clifton 8", "Hoka", "Clifton 8", null, null, confianza = 0.9, numeroFuentes = 3, score = 0.85),
        )
        coEvery { recognitionRepository.recognizeItem(any()) } returns Result.success(candidates)

        val viewModel = buildViewModel()
        viewModel.recognize(ByteArray(4))
        assertEquals(RecognitionUiState.Loading, viewModel.recognitionState.value)

        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recognitionState.value
        assertTrue(state is RecognitionUiState.Success)
        assertEquals(candidates, (state as RecognitionUiState.Success).candidates)
        assertEquals(candidates, viewModel.candidates)
    }

    @Test
    fun `recognize failure publishes Error with the repository's message`() {
        coEvery { recognitionRepository.recognizeItem(any()) } returns Result.failure(Exception("Sin cuota"))

        val viewModel = buildViewModel()
        viewModel.recognize(ByteArray(4))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recognitionState.value
        assertTrue(state is RecognitionUiState.Error)
        assertEquals(UiText.DynamicString("Sin cuota"), (state as RecognitionUiState.Error).message)
    }

    @Test
    fun `acknowledgeRecognitionResult resets recognitionState to Idle`() {
        coEvery { recognitionRepository.recognizeItem(any()) } returns Result.success(emptyList())
        val viewModel = buildViewModel()
        viewModel.recognize(ByteArray(4))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.acknowledgeRecognitionResult()

        assertEquals(RecognitionUiState.Idle, viewModel.recognitionState.value)
    }

    @Test
    fun `addPhoto stops accepting photos past the 3-photo cap`() {
        val viewModel = buildViewModel()

        assertTrue(viewModel.addPhoto(ByteArray(1)))
        assertTrue(viewModel.addPhoto(ByteArray(1)))
        assertTrue(viewModel.addPhoto(ByteArray(1)))
        assertEquals(false, viewModel.canAddMorePhotos)
        assertEquals(false, viewModel.addPhoto(ByteArray(1)))
        assertEquals(3, viewModel.photos.size)
    }

    @Test
    fun `removePhotoAt removes only the targeted photo`() {
        val viewModel = buildViewModel()
        val first = byteArrayOf(1)
        val second = byteArrayOf(2)
        viewModel.addPhoto(first)
        viewModel.addPhoto(second)

        viewModel.removePhotoAt(0)

        assertEquals(listOf(second), viewModel.photos)
    }

    @Test
    fun `reset clears photos, candidates and state`() {
        coEvery { recognitionRepository.recognizeItem(any()) } returns Result.success(emptyList())
        val viewModel = buildViewModel()
        viewModel.addPhoto(ByteArray(1))
        viewModel.recognize(ByteArray(4))
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.reset()

        assertTrue(viewModel.photos.isEmpty())
        assertTrue(viewModel.candidates.isEmpty())
        assertEquals(RecognitionUiState.Idle, viewModel.recognitionState.value)
        assertEquals(SaveItemUiState.Idle, viewModel.saveState.value)
    }

    @Test
    fun `saveItem with blank name sets ValidationError NAME without touching the repository`() {
        val viewModel = buildViewModel()

        viewModel.saveItem("   ", null, "Running", "Nuevo")

        val state = viewModel.saveState.value
        assertTrue(state is SaveItemUiState.ValidationError)
        assertEquals(RequiredField.NAME, (state as SaveItemUiState.ValidationError).field)
        coVerify(exactly = 0) { itemRepository.createItem(any(), any()) }
    }

    @Test
    fun `saveItem with null sport sets ValidationError SPORT`() {
        val viewModel = buildViewModel()

        viewModel.saveItem("Hoka Clifton 8", null, null, "Nuevo")

        val state = viewModel.saveState.value
        assertTrue(state is SaveItemUiState.ValidationError)
        assertEquals(RequiredField.SPORT, (state as SaveItemUiState.ValidationError).field)
    }

    @Test
    fun `saveItem with null condition sets ValidationError CONDITION`() {
        val viewModel = buildViewModel()

        viewModel.saveItem("Hoka Clifton 8", null, "Running", null)

        val state = viewModel.saveState.value
        assertTrue(state is SaveItemUiState.ValidationError)
        assertEquals(RequiredField.CONDITION, (state as SaveItemUiState.ValidationError).field)
    }

    @Test
    fun `saveItem with no matching item publishes directly and builds an Item with the defaulted fields`() {
        every { itemRepository.observeItems() } returns flowOf(emptyList())
        coEvery { itemRepository.searchValuation(any(), any(), any(), any()) } returns noValuationFound()
        val itemSlot = slot<Item>()
        coEvery { itemRepository.createItem(capture(itemSlot), any()) } returns Result.success("new-id")

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", "Blancas", "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveItemUiState.Success, viewModel.saveState.value)
        val sent = itemSlot.captured
        assertEquals("Nike Air Force 1", sent.nombre)
        assertEquals("Blancas", sent.descripcion)
        assertEquals("Baloncesto", sent.deporte)
        assertEquals("Nuevo", sent.estado)
        assertEquals("", sent.marca)
        assertEquals("", sent.modelo)
        assertEquals("", sent.id)
        assertTrue(sent.imageUrls.isEmpty())
        assertTrue(sent.historialPrecios.isEmpty())
        assertEquals(0L, sent.createdAt)
        assertEquals(0L, sent.updatedAt)
    }

    @Test
    fun `saveItem still publishes when searchValuation fails, with null valuation fields`() {
        every { itemRepository.observeItems() } returns flowOf(emptyList())
        coEvery { itemRepository.searchValuation(any(), any(), any(), any()) } returns Result.failure(Exception("Sin red"))
        val itemSlot = slot<Item>()
        coEvery { itemRepository.createItem(capture(itemSlot), any()) } returns Result.success("new-id")

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveItemUiState.Success, viewModel.saveState.value)
        assertEquals(null, itemSlot.captured.valoracionActual)
        assertEquals("EUR", itemSlot.captured.valoracionMoneda)
    }

    @Test
    fun `saveItem surfaces a repository failure as Error`() {
        every { itemRepository.observeItems() } returns flowOf(emptyList())
        coEvery { itemRepository.searchValuation(any(), any(), any(), any()) } returns noValuationFound()
        coEvery { itemRepository.createItem(any(), any()) } returns Result.failure(Exception("Sin conexión"))

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveItemUiState.Error)
        assertEquals(UiText.DynamicString("Sin conexión"), (state as SaveItemUiState.Error).message)
    }

    @Test
    fun `saveItem reports NoConnection and does not publish when there is no network`() {
        every { context.isOnline() } returns false

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveItemUiState.NoConnection, viewModel.saveState.value)
        coVerify(exactly = 0) { itemRepository.observeItems() }
        coVerify(exactly = 0) { itemRepository.createItem(any(), any()) }
    }

    @Test
    fun `saveItem pauses with DuplicateWarning when a matching item already exists, without publishing`() {
        every { itemRepository.observeItems() } returns flowOf(listOf(existingItem("Nike Air Force 1")))

        val viewModel = buildViewModel()
        viewModel.saveItem("air force 1 NIKE", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.saveState.value
        assertTrue(state is SaveItemUiState.DuplicateWarning)
        assertEquals("Nike Air Force 1", (state as SaveItemUiState.DuplicateWarning).existingItemName)
        coVerify(exactly = 0) { itemRepository.createItem(any(), any()) }
    }

    @Test
    fun `confirmPublishDespiteDuplicate publishes the paused request`() {
        every { itemRepository.observeItems() } returns flowOf(listOf(existingItem("Nike Air Force 1")))
        coEvery { itemRepository.searchValuation(any(), any(), any(), any()) } returns noValuationFound()
        coEvery { itemRepository.createItem(any(), any()) } returns Result.success("new-id")

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.confirmPublishDespiteDuplicate()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveItemUiState.Success, viewModel.saveState.value)
        coVerify(exactly = 1) { itemRepository.createItem(any(), any()) }
    }

    @Test
    fun `confirmPublishDespiteDuplicate reports NoConnection when the network drops after the warning`() {
        every { itemRepository.observeItems() } returns flowOf(listOf(existingItem("Nike Air Force 1")))

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        every { context.isOnline() } returns false
        viewModel.confirmPublishDespiteDuplicate()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        assertEquals(SaveItemUiState.NoConnection, viewModel.saveState.value)
        coVerify(exactly = 0) { itemRepository.createItem(any(), any()) }
    }

    @Test
    fun `dismissDuplicateWarning returns to Idle without publishing`() {
        every { itemRepository.observeItems() } returns flowOf(listOf(existingItem("Nike Air Force 1")))

        val viewModel = buildViewModel()
        viewModel.saveItem("Nike Air Force 1", null, "Baloncesto", "Nuevo")
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()

        viewModel.dismissDuplicateWarning()

        assertEquals(SaveItemUiState.Idle, viewModel.saveState.value)
        coVerify(exactly = 0) { itemRepository.createItem(any(), any()) }
    }
}
