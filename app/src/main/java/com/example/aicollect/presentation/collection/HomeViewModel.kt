package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.CollectionPriceFilter
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

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val isCollectionEmpty: Boolean,
        val visibleItems: List<Item>,
        val summary: CollectionSummary,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * Owns the price-range filter and the resulting visible-items decision (2026-08-24, MVVM fix):
 * this used to live in HomeFragment (`activePriceRange`/`renderCurrentState()`), which meant the
 * View was deciding what to show instead of just rendering what the ViewModel gives it. The
 * Fragment now only forwards the filter-sheet result via [setPriceRange] and renders [uiState].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(itemRepository: ItemRepository) : ViewModel() {

    /** Null means "no filter active" — matches the Fragment's previous default. */
    private val _priceRange = MutableStateFlow<IntRange?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        itemRepository.observeItems(),
        _priceRange,
    ) { items, range ->
        val visibleItems = if (range != null) {
            items.filter { CollectionPriceFilter.isWithinRange(it.valoracionActual ?: 0.0, range.first, range.last) }
        } else {
            items
        }
        val content: HomeUiState = HomeUiState.Content(
            isCollectionEmpty = items.isEmpty(),
            visibleItems = visibleItems,
            summary = summaryFor(items),
        )
        content
    }
        .catch { emit(HomeUiState.Error(it.message ?: "No se pudo cargar tu colección.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

    /** Called from `FilterBottomSheetFragment`'s result, forwarded by `HomeFragment` — a plain
     * user-input event, not a decision, so this doesn't violate MVVM the way owning the filtered
     * list in the Fragment did. */
    fun setPriceRange(minPrice: Int, maxPrice: Int) {
        _priceRange.value = minPrice..maxPrice
    }

    private fun summaryFor(items: List<Item>): CollectionSummary {
        val evolution = PortfolioAnalytics.monthlyEvolution(items)
        val itemCount = items.size
        return CollectionSummary(
            totalValueLabel = ItemFormatting.formatValue(PortfolioAnalytics.totalValue(items), CURRENCY),
            changeLabel = ItemFormatting.formatChangePercent(PortfolioAnalytics.changePercent(evolution)),
            itemCountLabel = if (itemCount == 1) "1 artículo en tu colección" else "$itemCount artículos en tu colección",
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        // All items are created with EUR today (see NewPostViewModel) — once currency
        // becomes per-item/configurable, this total needs its own conversion strategy.
        const val CURRENCY = "EUR"
    }
}
