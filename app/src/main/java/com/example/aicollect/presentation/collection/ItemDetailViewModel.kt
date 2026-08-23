package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PortfolioAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Content(
        val imageUrl: String?,
        val nombre: String,
        val priceLabel: String,
        val estado: String,
        val deporte: String,
        val sportEmoji: String,
        val evolution: List<Float>,
        val monthLabels: List<String>,
        /** null oculta la fila del rango en el Fragment — no hay rango hasta que el refresco
         * automático en segundo plano ([refreshValuationSilently]) encuentra datos fiables. */
        val valuationRangeLabel: String?,
    ) : ItemDetailUiState
    data class Error(val message: String) : ItemDetailUiState
}

sealed interface DeleteItemUiState {
    data object Idle : DeleteItemUiState
    data object Deleting : DeleteItemUiState
    data object Success : DeleteItemUiState
    data class Error(val message: String) : DeleteItemUiState
}

/**
 * 2026-08-24 MVVM fix: `ItemDetailFragment.bind()` llamaba directo a `PortfolioAnalytics`/
 * `ItemFormatting`/el mapeo de emoji de deporte. Ahora [ItemDetailUiState.Content] ya trae todo
 * calculado, el Fragment solo pinta.
 *
 * 2026-08-24, misma sesión: añadida la valoración de mercado (Gemini + grounding en Google
 * Search, sustituye a la idea original de eBay Browse API), calculada en segundo plano al crear
 * el item y refrescada sola si tiene más de 30 días — sin botón manual ni fuentes visibles: los
 * "chips" de fuente resultaron ser enlaces a una búsqueda de Google, no al anuncio real (esta API
 * no expone eso), así que se decidió no mostrarlos y quitar también el botón "Actualizar valor"
 * para no prometer control manual sobre algo que igualmente se recalcula solo.
 */
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteItemUiState>(DeleteItemUiState.Idle)
    val deleteState: StateFlow<DeleteItemUiState> = _deleteState.asStateFlow()

    private var currentItemId: String? = null

    fun load(itemId: String) {
        currentItemId = itemId
        _uiState.value = ItemDetailUiState.Loading
        viewModelScope.launch {
            itemRepository.getItem(itemId)
                .onSuccess { item ->
                    _uiState.value = item.toContent()
                    // Auto-refresco silencioso (2026-08-24, pedido explícito, sustituye a un
                    // Cloud Scheduler real por coste/tiempo — ver PROJECT_CONTEXT.md): si nunca se
                    // valoró o el último precio tiene más de 30 días, se comprueba solo al abrir
                    // esta pantalla, sin botón ni Snackbar — no hay UI manual para esto.
                    if (item.needsValuationRefresh()) refreshValuationSilently(itemId)
                }
                .onFailure {
                    _uiState.value = ItemDetailUiState.Error(it.message ?: "No se pudo cargar el artículo.")
                }
        }
    }

    /** Completa el CRUD (roadmap: "Eliminar" seguía sin construir) — el propio [ItemRepository]
     * ya borraba también las fotos de Storage, solo faltaba una pantalla que lo llamara. */
    fun deleteItem() {
        val itemId = currentItemId ?: return
        _deleteState.value = DeleteItemUiState.Deleting
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
                .onSuccess { _deleteState.value = DeleteItemUiState.Success }
                .onFailure {
                    _deleteState.value = DeleteItemUiState.Error(
                        it.message ?: "No se pudo eliminar el artículo. Inténtalo de nuevo.",
                    )
                }
        }
    }

    private fun refreshValuationSilently(itemId: String) {
        viewModelScope.launch {
            itemRepository.refreshValuation(itemId)
                .onSuccess { _uiState.value = it.toContent() }
                .onFailure { /* silencioso a propósito: es un chequeo automático, no una acción del usuario */ }
        }
    }

    private fun Item.needsValuationRefresh(): Boolean {
        val lastPriced = historialPrecios.maxOfOrNull { it.fecha } ?: return true
        return System.currentTimeMillis() - lastPriced > STALE_VALUATION_MS
    }

    private fun Item.toContent() = ItemDetailUiState.Content(
        imageUrl = imageUrls.firstOrNull(),
        nombre = nombre,
        priceLabel = ItemFormatting.formatValue(valoracionActual, valoracionMoneda),
        estado = estado,
        deporte = deporte,
        sportEmoji = sportEmoji(deporte),
        evolution = PortfolioAnalytics.monthlyEvolution(listOf(this)),
        monthLabels = PortfolioAnalytics.monthLabels(),
        valuationRangeLabel = valuationRangeLabel(),
    )

    private fun Item.valuationRangeLabel(): String? {
        val min = valoracionMin ?: return null
        val max = valoracionMax ?: return null
        return "${ItemFormatting.formatValue(min, valoracionMoneda)} – ${ItemFormatting.formatValue(max, valoracionMoneda)}"
    }

    private fun sportEmoji(deporte: String): String = when (deporte.lowercase(Locale("es", "ES"))) {
        "baloncesto" -> "🏀"
        "fútbol" -> "⚽"
        "fútbol americano" -> "🏈"
        "béisbol" -> "⚾"
        else -> "🏆"
    }

    private companion object {
        // Mismo umbral que products_cache en refreshValuation.ts (Cloud Function), para que "está
        // viejo" signifique lo mismo en los dos sitios.
        const val STALE_VALUATION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
