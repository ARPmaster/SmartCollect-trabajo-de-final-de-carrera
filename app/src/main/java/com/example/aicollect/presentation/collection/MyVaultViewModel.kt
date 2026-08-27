/**ViewModel de My Vault: transforma la colección observada en el contenido ya calculado de la
*pantalla (evolución, distribución por deporte/estado, artículos más valorados), aplicando el
* filtro de deporte elegido en los chips.*/
package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PortfolioAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TopValuedItemUi(
    val id: String,
    val imageUrl: String?,
    val nombre: String,
    val subtitle: String,
    val valueLabel: String,
)

sealed interface MyVaultUiState {
    data object Loading : MyVaultUiState
    data object Empty : MyVaultUiState
    data class Content(
        val totalValueLabel: String,
        val changeLabel: String?,
        val evolution: List<Float>,
        val monthLabels: List<String>,
        val sportDistribution: List<Pair<String, Int>>,
        val conditionPercentByEstado: Map<String, Int>,
        val hasItemsForSelectedSport: Boolean,
        val selectedSport: String?,
        val totalItemsLabel: String,
        val topValuedItems: List<TopValuedItemUi>,
    ) : MyVaultUiState
    data class Error(val message: String) : MyVaultUiState
}

@HiltViewModel
class MyVaultViewModel @Inject constructor(itemRepository: ItemRepository) : ViewModel() {

    private val _selectedSport = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MyVaultUiState> = combine(
        itemRepository.observeItems(),
        _selectedSport,
    ) { items, selectedSport ->
        if (items.isEmpty()) MyVaultUiState.Empty else buildContent(items, selectedSport)
    }
        .catch { emit(MyVaultUiState.Error(it.message ?: "No se pudo cargar tu cartera.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MyVaultUiState.Loading)

    fun selectSport(sport: String?) {
        _selectedSport.value = sport
    }

    private fun buildContent(items: List<Item>, selectedSport: String?): MyVaultUiState.Content {
        val evolution = PortfolioAnalytics.monthlyEvolution(items)
        val itemsForSelectedSport = selectedSport?.let { sport -> items.filter { it.deporte == sport } } ?: items
        return MyVaultUiState.Content(
            totalValueLabel = ItemFormatting.formatValue(PortfolioAnalytics.totalValue(items), CURRENCY),
            changeLabel = ItemFormatting.formatChangePercent(PortfolioAnalytics.changePercent(evolution)),
            evolution = evolution,
            monthLabels = PortfolioAnalytics.monthLabels(),
            sportDistribution = PortfolioAnalytics.distributionBy(items) { it.deporte },
            conditionPercentByEstado = PortfolioAnalytics.distributionBy(itemsForSelectedSport) { it.estado }.toMap(),
            hasItemsForSelectedSport = itemsForSelectedSport.isNotEmpty(),
            selectedSport = selectedSport,
            totalItemsLabel = items.size.toString(),
            topValuedItems = PortfolioAnalytics.topValued(itemsForSelectedSport).map { it.toTopValuedItemUi() },
        )
    }

    private fun Item.toTopValuedItemUi(): TopValuedItemUi = TopValuedItemUi(
        id = id,
        imageUrl = imageUrls.firstOrNull(),
        nombre = nombre,
        subtitle = listOfNotNull(marca.takeIf { it.isNotBlank() }, estado.takeIf { it.isNotBlank() })
            .joinToString(" • ")
            .ifEmpty { deporte },
        valueLabel = ItemFormatting.formatValue(valoracionActual, valoracionMoneda),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CURRENCY = "EUR"
    }
}
