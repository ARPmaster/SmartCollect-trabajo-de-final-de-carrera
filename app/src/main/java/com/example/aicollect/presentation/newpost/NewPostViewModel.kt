package com.example.aicollect.presentation.newpost

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.NewItem
import com.example.aicollect.application.items.ValuationResult
import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.application.recognition.RecognitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface RecognitionUiState {
    data object Idle : RecognitionUiState
    data object Loading : RecognitionUiState
    data class Success(val candidates: List<RankedCandidate>) : RecognitionUiState
    data class Error(val message: String) : RecognitionUiState
}

sealed interface SaveItemUiState {
    data object Idle : SaveItemUiState
    /** Covers both the market-price lookup and the actual Firestore write — from the user's point
     * of view it's one action ("Publicar"), 2026-08-24 pedido explícito: antes eran dos pasos
     * (crear vacío, luego `refreshValuation` en segundo plano) y el precio tardaba en aparecer. */
    data object Loading : SaveItemUiState
    data object Success : SaveItemUiState
    data class Error(val message: String) : SaveItemUiState
    /** A required field is missing — kept separate from [Error] so the Fragment (which owns
     * string resources, the ViewModel doesn't have a Context) picks the right localized message
     * per [field] instead of the ViewModel hardcoding UI text. */
    data class ValidationError(val field: RequiredField) : SaveItemUiState
    /** 2026-08-24, pedido explícito: ya hay un objeto muy parecido (misma marca+modelo+edición
     * normalizados) en la colección del usuario — se pausa antes de publicar para que decida
     * seguir o cancelar, en vez de crear un duplicado silencioso. */
    data class DuplicateWarning(val existingItemName: String) : SaveItemUiState
}

/** 2026-08-24 MVVM fix: which required field is missing was decided by `NewPostFragment` before
 * (an `if` chain in `onPublishClicked()`); that's a business rule ("this item isn't valid without
 * these fields"), so it now lives here — the Fragment only maps [RequiredField] to a string. */
enum class RequiredField {
    NAME,
    SPORT,
    CONDITION,
}

/**
 * Shared state for the whole "Nueva Publicación" flow (Captura → Desambiguación → Formulario).
 * Activity-scoped (`by activityViewModels()`, MainActivity is the single Activity) instead of a
 * nested nav-graph ViewModel, so the flow's state survives the 3 fragment destinations without
 * needing Parcelable models — [reset] is called explicitly from MainActivity's "+" button so a
 * later run of the flow never inherits a previous one's candidates/photo.
 */
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

    /** Up to [MAX_PHOTOS] compressed JPEGs, in upload order — carried through to the review form
     * so they can be shown and uploaded, without re-reading the original URIs. Only [photos]`[0]`
     * is ever sent to `recognizeItem` (2026-08-23 feedback: "solo se analiza la primera en subir"). */
    val photos: List<ByteArray> get() = _photos

    val canAddMorePhotos: Boolean get() = _photos.size < MAX_PHOTOS

    var candidates: List<RankedCandidate> = emptyList()
        private set

    /** The candidate the user tapped in disambiguation; null means "Ninguno de estos" (manual entry). */
    var selectedCandidate: RankedCandidate? = null

    fun reset() {
        _photos.clear()
        candidates = emptyList()
        selectedCandidate = null
        pendingPublish = null
        _recognitionState.value = RecognitionUiState.Idle
        _saveState.value = SaveItemUiState.Idle
    }

    /** Must be called right after [RecognitionUiState.Success]/[RecognitionUiState.Error] is
     * handled (navigated on / shown). Otherwise, since [recognitionState] is a StateFlow, popping
     * back from disambiguation re-subscribes it and immediately replays the stale Success value,
     * bouncing straight back to disambiguation before the form ever shows. */
    fun acknowledgeRecognitionResult() {
        _recognitionState.value = RecognitionUiState.Idle
    }

    /** Appends a photo (up to [MAX_PHOTOS]) without calling `recognizeItem` — either the
     * "Rellenar manualmente" choice, or any photo after the first one, which is never re-analyzed
     * (2026-08-23 feedback). Returns false if already at the cap, so the caller can tell the user. */
    fun addPhoto(imageBytes: ByteArray): Boolean {
        if (_photos.size >= MAX_PHOTOS) return false
        _photos.add(imageBytes)
        return true
    }

    /** The "-" badge on a photo thumbnail (2026-08-23 feedback). Only removes that photo — a
     * previously-selected candidate's text is left in the form fields, since the user may have
     * already edited them and removing a photo shouldn't silently wipe that out. */
    fun removePhotoAt(index: Int) {
        if (index in _photos.indices) _photos.removeAt(index)
    }

    /** Only ever called for the first photo (2026-08-23 feedback: "solo se analiza la primera en
     * subir") — the choice dialog in [NewPostFragment] only offers automatic analysis when the
     * photo list was empty before this one was added. */
    fun recognize(imageBytes: ByteArray) {
        _recognitionState.value = RecognitionUiState.Loading
        viewModelScope.launch {
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            recognitionRepository.recognizeItem(imageBase64)
                .onSuccess { result ->
                    candidates = result
                    _recognitionState.value = RecognitionUiState.Success(result)
                }
                .onFailure {
                    _recognitionState.value = RecognitionUiState.Error(
                        it.message ?: "No se pudo analizar la imagen. Inténtalo de nuevo.",
                    )
                }
        }
    }

    /** Datos ya validados de una publicación pendiente — se guardan aquí en vez de repasarlos por
     * parámetro cuando el usuario confirma [confirmPublishDespiteDuplicate] tras el aviso de
     * duplicado, para no repetir la validación de campos requeridos. */
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

    /** Validates and builds the publish request (2026-08-24 MVVM fix: this used to be
     * `NewPostFragment.onPublishClicked()`'s job — required-field checks and shaping the domain
     * model are business rules, not view rendering). The Fragment only forwards the raw text/
     * selections the user typed/picked.
     *
     * 2026-08-24, misma sesión: si ya hay algo muy parecido en la colección del usuario, se pausa
     * en [SaveItemUiState.DuplicateWarning] en vez de publicar directamente. */
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
            confianzaIdentificacion = candidate?.confianza,
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

    /** El usuario vio [SaveItemUiState.DuplicateWarning] y quiere publicar igualmente (puede ser
     * un segundo ejemplar real del mismo objeto). */
    fun confirmPublishDespiteDuplicate() {
        val request = pendingPublish ?: return
        pendingPublish = null
        viewModelScope.launch { publish(request) }
    }

    /** El usuario canceló tras ver el aviso de duplicado — vuelve al formulario sin publicar. */
    fun dismissDuplicateWarning() {
        pendingPublish = null
        _saveState.value = SaveItemUiState.Idle
    }

    /** Compara contra la colección ya sincronizada en Room (misma clave que products_cache en el
     * backend: marca+modelo+edición normalizados, o el nombre si el item se creó a mano) — lectura
     * local, no gasta ninguna llamada de red. */
    private suspend fun findSimilarExistingItem(request: PendingPublish): Item? {
        val key = productKey(request.marca, request.modelo, request.edicion, request.nombre)
        return itemRepository.observeItems().first()
            .firstOrNull { productKey(it.marca, it.modelo, it.edicion, it.nombre) == key }
    }

    private suspend fun publish(request: PendingPublish) {
        _saveState.value = SaveItemUiState.Loading

        // Búsqueda de precio ANTES de crear el item (2026-08-24, pedido explícito): así se escribe
        // en Firestore en una sola operación, ya con el precio puesto, en vez de crear vacío y
        // actualizar después (dejaba un hueco visible en "Sin valorar"). Si la búsqueda falla, no
        // se bloquea la publicación — el item se guarda sin valorar, igual que si el usuario lo
        // hubiera creado antes de que existiera esta búsqueda automática.
        val valuation = itemRepository
            .searchValuation(request.nombre, request.marca, request.modelo, request.edicion)
            .getOrElse { ValuationResult(precio = null, min = null, max = null, moneda = DEFAULT_CURRENCY, fuentes = emptyList()) }

        val newItem = NewItem(
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
                    it.message ?: "No se pudo guardar el artículo. Inténtalo de nuevo.",
                )
            }
    }

    private fun productKey(marca: String, modelo: String, edicion: String?, nombre: String): String {
        val structured = listOfNotNull(marca, modelo, edicion).filter { it.isNotBlank() }.joinToString(" ")
        return normalizeKey(structured.ifBlank { nombre })
    }

    /** Misma normalización que `normalizeKey` en refreshValuation.ts (Cloud Function) — palabras
     * ordenadas alfabéticamente para que "Nike Air Force 1" y "Air Force 1 Nike" sigan contando
     * como el mismo producto tanto para el precio compartido como para este aviso de duplicado. */
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
