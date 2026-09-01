/** ViewModel del detalle de un ítem: carga sus datos, calcula el contenido listo para pintar
* (evolución de precio, rango de valoración, emoji de deporte), refresca la valoración en
* segundo plano si está desactualizada, y gestiona su eliminación.*/
package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PortfolioAnalytics
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ItemDetailUiState {
    data object Loading : ItemDetailUiState
    data class Content(
        val imageUrl: String?,
        val nombre: String,
        val priceLabel: UiText,
        val estado: String,
        val deporte: String,
        val sportEmoji: String,
        val evolution: List<Float>,
        val monthLabels: List<String>,
        val valuationRangeLabel: String?,
    ) : ItemDetailUiState
    data class Error(val message: UiText) : ItemDetailUiState
}

sealed interface DeleteItemUiState {
    data object Idle : DeleteItemUiState
    data object Deleting : DeleteItemUiState
    data object Success : DeleteItemUiState
    data class Error(val message: UiText) : DeleteItemUiState
}

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemDetailUiState>(ItemDetailUiState.Loading)
    val uiState: StateFlow<ItemDetailUiState> = _uiState.asStateFlow()

    private val _deleteState = MutableStateFlow<DeleteItemUiState>(DeleteItemUiState.Idle)
    val deleteState: StateFlow<DeleteItemUiState> = _deleteState.asStateFlow()

    private var currentItemId: String? = null

    private var loadJob: Job? = null

    fun load(itemId: String) {
        currentItemId = itemId
        _uiState.value = ItemDetailUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            itemRepository.getItem(itemId)
                .onSuccess { item ->
                    if (currentItemId != itemId) return@onSuccess
                    _uiState.value = item.toContent()
                    if (item.needsValuationRefresh()) refreshValuationSilently(itemId)
                }
                .onFailure {
                    if (currentItemId != itemId) return@onFailure
                    _uiState.value = ItemDetailUiState.Error(
                        it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.error_item_load_generic),
                    )
                }
        }
    }

    fun deleteItem() {
        val itemId = currentItemId ?: return
        _deleteState.value = DeleteItemUiState.Deleting
        viewModelScope.launch {
            itemRepository.deleteItem(itemId)
                .onSuccess { _deleteState.value = DeleteItemUiState.Success }
                .onFailure {
                    _deleteState.value = DeleteItemUiState.Error(
                        it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.item_detail_delete_error),
                    )
                }
        }
    }

    private fun refreshValuationSilently(itemId: String) {
        viewModelScope.launch {
            itemRepository.refreshValuation(itemId)
                .onSuccess { if (currentItemId == itemId) _uiState.value = it.toContent() }
                .onFailure { }
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
        return "${ItemFormatting.formatKnownValue(min, valoracionMoneda)} – ${ItemFormatting.formatKnownValue(max, valoracionMoneda)}"
    }

    private fun sportEmoji(deporte: String): String = when (deporte.lowercase(Locale("es", "ES"))) {
        "baloncesto" -> "🏀"
        "fútbol" -> "⚽"
        "fútbol americano" -> "🏈"
        "béisbol" -> "⚾"
        else -> "🏆"
    }

    private companion object {
        const val STALE_VALUATION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
