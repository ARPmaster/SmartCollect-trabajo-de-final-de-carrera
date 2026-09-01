/** Pantalla Home: muestra el feed de la colección con su cabecera de valor total,
*escucha el resultado del bottom sheet de filtros y navega al detalle de un ítem al tocarlo.*/
package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aicollect.R
import com.example.aicollect.application.items.ItemSortOption
import com.example.aicollect.databinding.FragmentHomeBinding
import com.example.aicollect.presentation.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private val feedAdapter = CollectionFeedAdapter(onItemClick = ::navigateToItemDetail)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvFeed.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.adapter = feedAdapter

        requireActivity().supportFragmentManager.setFragmentResultListener(
            FilterBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            applyFilters(
                minPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MIN_PRICE),
                maxPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MAX_PRICE),
                sport = bundle.getString(FilterBottomSheetFragment.KEY_SPORT),
                condition = bundle.getString(FilterBottomSheetFragment.KEY_CONDITION),
                sortOrdinal = bundle.getInt(FilterBottomSheetFragment.KEY_SORT_ORDINAL),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: HomeUiState) {
        when (state) {
            is HomeUiState.Loading -> Unit
            is HomeUiState.Content -> {
                val showEmptyState = state.isCollectionEmpty || state.hasNoFilterResults
                binding.rvFeed.visibility = if (showEmptyState) View.GONE else View.VISIBLE
                binding.tvEmptyState.visibility = if (showEmptyState) View.VISIBLE else View.GONE
                binding.tvEmptyState.text = getString(
                    if (state.isCollectionEmpty) R.string.home_empty_state else R.string.home_no_filter_results,
                )

                feedAdapter.submitList(buildFeedRows(state.visibleItems.map { it.toFeedItem() }, state.summary))
            }
            is HomeUiState.Error -> showSnackbar(binding.root, state.message).show()
        }
    }

    private fun applyFilters(minPrice: Int, maxPrice: Int, sport: String?, condition: String?, sortOrdinal: Int) {
        viewModel.setFilters(minPrice, maxPrice, sport, condition, ItemSortOption.fromOrdinal(sortOrdinal))
    }

    private fun navigateToItemDetail(item: CollectionFeedItem) {
        findNavController().navigate(
            R.id.itemDetailFragment,
            bundleOf(ItemDetailFragment.ARG_ITEM_ID to item.id),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
