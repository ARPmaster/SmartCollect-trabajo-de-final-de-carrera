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
import com.example.aicollect.databinding.FragmentHomeBinding
import com.example.aicollect.presentation.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    /** Created once and reused across state emissions — [render] calls [CollectionFeedAdapter.submitList]
     * instead of replacing the adapter, so RecyclerView can diff+animate instead of a full rebind. */
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

        parentFragmentManager.setFragmentResultListener(
            FilterBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val minPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MIN_PRICE)
            val maxPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MAX_PRICE)
            viewModel.setPriceRange(minPrice, maxPrice)
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
                binding.rvFeed.visibility = if (state.isCollectionEmpty) View.GONE else View.VISIBLE
                binding.tvEmptyState.visibility = if (state.isCollectionEmpty) View.VISIBLE else View.GONE

                feedAdapter.submitList(buildFeedRows(state.visibleItems.map { it.toFeedItem() }, state.summary))
            }
            is HomeUiState.Error -> showSnackbar(binding.root, state.message).show()
        }
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
