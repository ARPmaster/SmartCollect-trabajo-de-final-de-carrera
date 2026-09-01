/** ViewModel compartido del flujo "Nueva Publicación" (captura de fotos, reconocimiento automático,
* desambiguación y formulario): guarda las fotos y el candidato elegido, valida el formulario,
* detecta posibles duplicados en la colección y publica el ítem ya con su valoración de mercado.*/
package com.example.aicollect.presentation.newpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Base64
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Loading : RecognitionUiState
    data class Success(val candidates: List<RankedCandidate>) : RecognitionUiState
    data class Error(val message: UiText) : RecognitionUiState
}

sealed interface SaveItemUiState {
    data object Idle : SaveItemUiState
    data object Loading : SaveItemUiState
    data object Success : SaveItemUiState
    data class Error(val message: UiText) : SaveItemUiState
    data class ValidationError(val field: RequiredField) : SaveItemUiState
    data class DuplicateWarning(val existingItemName: String) : SaveItemUiState
}

enum class RequiredField {
    NAME,
    SPORT,
    CONDITION,
}

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val recognitionRepository: RecognitionRepository,
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private val _recognitionState = MutableStateFlow<RecognitionUiState>(RecognitionUiState.Idle)
    val recognitionState: StateFlow<RecognitionUiState> = _recognitionState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveItemUiState>(SaveItemUiState.Idle)
    val saveState: StateFlow<SaveItemUiState> = _saveState.asStateFlow()

    private val _photos = mutableListOf<ByteArray>()

    val photos: List<ByteArray> get() = _photos

    val canAddMorePhotos: Boolean get() = _photos.size < MAX_PHOTOS

    var candidates: List<RankedCandidate> = emptyList()
        private set

    var selectedCandidate: RankedCandidate? = null

    fun reset() {
        _photos.clear()
        candidates = emptyList()
        selectedCandidate = null
        pendingPublish = null
        _recognitionState.value = RecognitionUiState.Idle
        _saveState.value = SaveItemUiState.Idle
    }

    fun acknowledgeRecognitionResult() {
        _recognitionState.value = RecognitionUiState.Idle
    }

    fun addPhoto(imageBytes: ByteArray): Boolean {
        if (_photos.size >= MAX_PHOTOS) return false
        _photos.add(imageBytes)
        return true
    }

    fun removePhotoAt(index: Int) {
        if (index in _photos.indices) _photos.removeAt(index)
    }

    fun recognize(imageBytes: ByteArray) {
        _recognitionState.value = RecognitionUiState.Loading
        viewModelScope.launch {
            val imageBase64 = Base64.getEncoder().encodeToString(imageBytes)
            recognitionRepository.recognizeItem(imageBase64)
                .onSuccess { result ->
                    candidates = result
                    _recognitionState.value = RecognitionUiState.Success(result)
                }
                .onFailure {
                    _recognitionState.value = RecognitionUiState.Error(
                        it.message?.let(UiText::DynamicString)
                            ?: UiText.StringResource(R.string.error_new_post_analyze_generic),
                    )
                }
        }
    }

    private data class PendingPublish(
        val nombre: String,
        val descripcion: String?,
        val marca: String,
        val modelo: String,
        val edicion: String?,
        val procedencia: String?,
        val deporte: String,
        val estado: String,
        val confianzaIdentificacion: Double?,
    )

    private var pendingPublish: PendingPublish? = null

    fun saveItem(name: String, description: String?, sport: String?, condition: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _saveState.value = SaveItemUiState.ValidationError(RequiredField.NAME)
            return
        }
        if (sport == null) {
            _saveState.value = SaveItemUiState.ValidationError(RequiredField.SPORT)
            return
        }
        if (condition == null) {
            _saveState.value = SaveItemUiState.ValidationError(RequiredField.CONDITION)
            return
        }

        val candidate = selectedCandidate
        val request = PendingPublish(
            nombre = trimmedName,
            descripcion = description?.trim()?.takeIf { it.isNotEmpty() },
            marca = candidate?.marca.orEmpty(),
            modelo = candidate?.modelo.orEmpty(),
            edicion = candidate?.edicion,
            procedencia = candidate?.procedencia,
            deporte = sport,
            estado = condition,
            confianzaIdentificacion = candidate?.score,
        )

        viewModelScope.launch {
            val existing = findSimilarExistingItem(request)
            if (existing != null) {
                pendingPublish = request
                _saveState.value = SaveItemUiState.DuplicateWarning(existing.nombre)
            } else {
                publish(request)
            }
        }
    }

    fun confirmPublishDespiteDuplicate() {
        val request = pendingPublish ?: return
        pendingPublish = null
        viewModelScope.launch { publish(request) }
    }

    fun dismissDuplicateWarning() {
        pendingPublish = null
        _saveState.value = SaveItemUiState.Idle
    }

    private suspend fun findSimilarExistingItem(request: PendingPublish): Item? {
        val key = productKey(request.marca, request.modelo, request.edicion, request.nombre)
        return itemRepository.observeItems().firstOrNull()
            ?.firstOrNull { productKey(it.marca, it.modelo, it.edicion, it.nombre) == key }
    }

    private suspend fun publish(request: PendingPublish) {
        _saveState.value = SaveItemUiState.Loading

        val valuation = itemRepository
            .searchValuation(request.nombre, request.marca, request.modelo, request.edicion)
            .getOrElse { ValuationResult(precio = null, min = null, max = null, moneda = DEFAULT_CURRENCY, fuentes = emptyList()) }

        val newItem = Item(
            nombre = request.nombre,
            descripcion = request.descripcion,
            marca = request.marca,
            modelo = request.modelo,
            edicion = request.edicion,
            procedencia = request.procedencia,
            deporte = request.deporte,
            estado = request.estado,
            valoracionActual = valuation.precio,
            valoracionMin = valuation.min,
            valoracionMax = valuation.max,
            valoracionMoneda = valuation.moneda,
            fuenteValoracion = if (valuation.fuentes.isNotEmpty()) "gemini_grounded_search" else null,
            valoracionBusquedas = valuation.fuentes,
            confianzaIdentificacion = request.confianzaIdentificacion,
        )

        val photosSnapshot = _photos.toList()
        itemRepository.createItem(newItem, photosSnapshot)
            .onSuccess { _saveState.value = SaveItemUiState.Success }
            .onFailure {
                _saveState.value = SaveItemUiState.Error(
                    it.message?.let(UiText::DynamicString)
                        ?: UiText.StringResource(R.string.error_new_post_save_generic),
                )
            }
    }

    private fun productKey(marca: String, modelo: String, edicion: String?, nombre: String): String {
        val structured = listOfNotNull(marca, modelo, edicion).filter { it.isNotBlank() }.joinToString(" ")
        return normalizeKey(structured.ifBlank { nombre })
    }

    private fun normalizeKey(text: String): String =
        text.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .sorted()
            .joinToString("_")

    private companion object {
        const val MAX_PHOTOS = 3
        const val DEFAULT_CURRENCY = "EUR"
    }
}
