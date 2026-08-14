package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aicollect.R
import com.example.aicollect.application.collection.CollectionPriceFilter
import com.example.aicollect.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        binding.rvFeed.adapter = CollectionFeedAdapter(sampleCollectionFeedItems)

        parentFragmentManager.setFragmentResultListener(
            FilterBottomSheetFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val minPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MIN_PRICE)
            val maxPrice = bundle.getInt(FilterBottomSheetFragment.KEY_MAX_PRICE)
            val filtered = sampleCollectionFeedItems.filter { item ->
                CollectionPriceFilter.isWithinRange(item.price, minPrice, maxPrice)
            }
            binding.rvFeed.adapter = CollectionFeedAdapter(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Placeholder data from the Figma mock, standing in until items can be added for real.
     * Every real item added to the collection should render through this same
     * [CollectionFeedItem] shape (category, image, price, description, date).
     */
    private val sampleCollectionFeedItems: List<CollectionFeedItem>
        get() = listOf(
            CollectionFeedItem(
                category = "MEMORABILIA CURATOR",
                image = R.drawable.item_signed_jersey,
                price = "250€",
                description = "Finally tracked down this 1996 NBA Finals worn jersey. " +
                    "Signed by the GOAT himself. A cornerstone of the collection.",
                date = "15/10/2023",
            ),
            CollectionFeedItem(
                category = "SNEAKERHEAD ELITE",
                image = R.drawable.item_grail_sneaker,
                price = "250€",
                description = "Still the holy grail of 2005 collaborations. Deadstock Tiffany Dunk Lows, pristine box.",
                date = "12/10/2023",
            ),
        )
}
