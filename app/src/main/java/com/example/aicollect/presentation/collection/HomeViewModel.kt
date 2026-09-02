/** ViewModel de Home: combina la colección observada con los filtros activos
 para producir la lista visible y el resumen de valor total. */
package com.example.aicollect.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aicollect.application.items.CollectionPriceFilter
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.ItemRepository
import com.example.aicollect.application.items.ItemSortOption
import com.example.aicollect.application.items.PortfolioAnalytics
import com.example.aicollect.application.items.sortedByOption
import com.example.aicollect.R
import com.example.aicollect.presentation.UiText
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
        val hasNoFilterResults: Boolean,
        val visibleItems: List<Item>,
        val summary: CollectionSummary,
    ) : HomeUiState
    data class Error(val message: UiText) : HomeUiState
}

private data class ActiveFilters(
    val priceRange: IntRange? = null,
    val sport: String? = null,
    val condition: String? = null,
    val sort: ItemSortOption = ItemSortOption.DEFAULT,
)

@HiltViewModel
class HomeViewModel @Inject constructor(itemRepository: ItemRepository) : ViewModel() {

    private val _filters = MutableStateFlow(ActiveFilters())

    val uiState: StateFlow<HomeUiState> = combine(
        itemRepository.observeItems(),
        _filters,
    ) { items, filters ->
        val visibleItems = items.filter { item ->
            val matchesPrice = filters.priceRange?.let { range ->
                CollectionPriceFilter.isWithinRange(item.valoracionActual ?: 0.0, range.first, range.last)
            } ?: true
            val matchesSport = filters.sport?.let { it == item.deporte } ?: true
            val matchesCondition = filters.condition?.let { it == item.estado } ?: true
            matchesPrice && matchesSport && matchesCondition
        }.sortedByOption(filters.sort)
        val content: HomeUiState = HomeUiState.Content(
            isCollectionEmpty = items.isEmpty(),
            hasNoFilterResults = items.isNotEmpty() && visibleItems.isEmpty(),
            visibleItems = visibleItems,
            summary = summaryFor(items),
        )
        content
    }
        .catch {
            emit(
                HomeUiState.Error(
                    it.message?.let(UiText::DynamicString) ?: UiText.StringResource(R.string.home_load_error),
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)

    fun setFilters(minPrice: Int, maxPrice: Int, sport: String?, condition: String?, sort: ItemSortOption) {
        _filters.value = ActiveFilters(priceRange = minPrice..maxPrice, sport = sport, condition = condition, sort = sort)
    }

    private fun summaryFor(items: List<Item>): CollectionSummary {
        val itemCount = items.size
        return CollectionSummary(
            totalValueLabel = ItemFormatting.formatKnownValue(PortfolioAnalytics.totalValue(items), CURRENCY),
            itemCountLabel = if (itemCount == 1) "1 artículo en tu colección" else "$itemCount artículos en tu colección",
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CURRENCY = "EUR"
    }
}
