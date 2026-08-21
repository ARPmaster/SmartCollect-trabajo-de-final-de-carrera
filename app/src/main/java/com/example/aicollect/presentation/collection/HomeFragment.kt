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
import com.example.aicollect.application.collection.CollectionPriceFilter
import com.example.aicollect.application.items.Item
import com.example.aicollect.databinding.FragmentHomeBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    /** Set while the price filter sheet has an active range; re-applied every time [render]
     * runs so a live Firestore update doesn't silently drop the current filter. */
    private var activePriceRange: IntRange? = null
    private var latestItems: List<Item> = emptyList()

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

        parentFragmentManager.setFragmentResultListener(
            FilterBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val minPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MIN_PRICE)
            val maxPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MAX_PRICE)
            activePriceRange = minPrice..maxPrice
            renderCurrentState()
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
                latestItems = state.items
                renderCurrentState()
            }
            is HomeUiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderCurrentState() {
        val range = activePriceRange
        val visibleItems = if (range != null) {
            latestItems.filter { CollectionPriceFilter.isWithinRange(it.valoracionActual ?: 0.0, range.first, range.last) }
        } else {
            latestItems
        }

        binding.rvFeed.visibility = if (latestItems.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (latestItems.isEmpty()) View.VISIBLE else View.GONE

        binding.rvFeed.adapter = CollectionFeedAdapter(
            items = visibleItems.map { it.toFeedItem() },
            summary = viewModel.summaryFor(latestItems),
            onItemClick = ::navigateToItemDetail,
        )
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
