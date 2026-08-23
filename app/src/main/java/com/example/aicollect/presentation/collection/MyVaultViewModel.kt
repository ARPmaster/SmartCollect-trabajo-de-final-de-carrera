package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.PortfolioAnalytics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TopValuedItemUi(
    val id: String,
    val imageUrl: String?,
    val nombre: String,
    val subtitle: String,
    val valueLabel: String,
    val changeLabel: String?,
    val isPositiveChange: Boolean,
)

sealed interface MyVaultUiState {
    data object Loading : MyVaultUiState
    data object Empty : MyVaultUiState
    data class Content(
        val totalValueLabel: String,
        val changeLabel: String?,
        val evolution: List<Float>,
        val monthLabels: List<String>,
        /** (nombre del deporte, porcentaje) — ya ordenado por frecuencia, el Fragment solo toma
         * los 3 primeros huecos que tiene el layout (limitación ya documentada, sin cambios). */
        val sportDistribution: List<Pair<String, Int>>,
        /** estado (string real del dominio, ej. "Nuevo") → porcentaje. El orden fijo y los
         * colores por estado siguen siendo del Fragment porque dependen de
         * `R.array.filter_condition_options` y de colores resueltos — eso sí es una decisión de
         * vista, no de negocio. */
        val conditionPercentByEstado: Map<String, Int>,
        val totalItemsLabel: String,
        val topValuedItems: List<TopValuedItemUi>,
    ) : MyVaultUiState
    data class Error(val message: String) : MyVaultUiState
}

/**
 * 2026-08-24 MVVM fix: todo el cálculo de "My Vault" (evolución, distribución por deporte/estado,
 * artículos más valorados) vivía en `MyVaultFragment`, llamando directo a `PortfolioAnalytics`/
 * `ItemFormatting`. El ViewModel se limitaba a reexponer `observeItems()` sin transformar nada —
 * la Vista decidía, no el ViewModel. Ahora [uiState] ya trae todo listo para pintar.
 */
@HiltViewModel
class MyVaultViewModel @Inject constructor(itemRepository: ItemRepository) : ViewModel() {

    val uiState: StateFlow<MyVaultUiState> = itemRepository.observeItems()
        .map<List<Item>, MyVaultUiState> { items -> if (items.isEmpty()) MyVaultUiState.Empty else buildContent(items) }
        .catch { emit(MyVaultUiState.Error(it.message ?: "No se pudo cargar tu cartera.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MyVaultUiState.Loading)

    private fun buildContent(items: List<Item>): MyVaultUiState.Content {
        val evolution = PortfolioAnalytics.monthlyEvolution(items)
        return MyVaultUiState.Content(
            totalValueLabel = ItemFormatting.formatValue(PortfolioAnalytics.totalValue(items), CURRENCY),
            changeLabel = ItemFormatting.formatChangePercent(PortfolioAnalytics.changePercent(evolution)),
            evolution = evolution,
            monthLabels = PortfolioAnalytics.monthLabels(),
            sportDistribution = PortfolioAnalytics.distributionBy(items) { it.deporte },
            conditionPercentByEstado = PortfolioAnalytics.distributionBy(items) { it.estado }.toMap(),
            totalItemsLabel = items.size.toString(),
            topValuedItems = PortfolioAnalytics.topValued(items).map { it.toTopValuedItemUi() },
        )
    }

    private fun Item.toTopValuedItemUi(): TopValuedItemUi {
        val changePercent = PortfolioAnalytics.itemChangePercent(this)
        return TopValuedItemUi(
            id = id,
            imageUrl = imageUrls.firstOrNull(),
            nombre = nombre,
            subtitle = listOfNotNull(marca.takeIf { it.isNotBlank() }, estado.takeIf { it.isNotBlank() })
                .joinToString(" • ")
                .ifEmpty { deporte },
            valueLabel = ItemFormatting.formatValue(valoracionActual, valoracionMoneda),
            changeLabel = ItemFormatting.formatChangePercent(changePercent),
            isPositiveChange = (changePercent ?: 0f) >= 0f,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CURRENCY = "EUR"
    }
}
