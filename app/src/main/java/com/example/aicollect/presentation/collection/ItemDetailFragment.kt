package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentItemDetailBinding

/**
 * "Detalle de objeto/carta" (Figma 73:55 oscuro / 74:180 claro) — reached from the Home feed or
 * My Vault's top-valued list. Both entry points only have 2 sample items today (no real items
 * repository yet), so [ARG_ITEM_INDEX] just indexes into [sampleItemDetails], same placeholder
 * pattern as [HomeFragment]/[MyVaultFragment]. Back navigation goes through the shared toolbar
 * (MainActivity swaps its hamburger icon for a back arrow on this destination), not a
 * screen-local button.
 */
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val item = sampleItemDetails.getOrElse(arguments?.getInt(ARG_ITEM_INDEX) ?: 0) { sampleItemDetails.first() }
        binding.ivItemImage.setImageResource(item.image)
        binding.tvItemTitle.text = item.title
        binding.tvItemPrice.text = item.price
        binding.tvItemCondition.text = item.condition
        binding.tvItemCategory.text = item.category
        binding.tvItemSportIcon.text = item.sportIcon
        binding.chartPortfolio.values = item.priceHistory
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class ItemDetail(
        val title: String,
        val image: Int,
        val price: String,
        val condition: String,
        val category: String,
        val sportIcon: String,
        val priceHistory: List<Float>,
    )

    private val sampleItemDetails = listOf(
        ItemDetail(
            title = "Michael Jordan 1998 Finals Jersey",
            image = R.drawable.item_signed_jersey,
            price = "$1,250,000",
            condition = "Excelente",
            category = "Memorabilia - Baloncesto",
            sportIcon = "🏀",
            priceHistory = listOf(0.5f, 0.35f, 0.55f, 0.75f, 0.68f, 0.95f),
        ),
        ItemDetail(
            title = "2005 Tiffany Dunk Low",
            image = R.drawable.item_grail_sneaker,
            price = "$210,000",
            condition = "Nuevo (Deadstock)",
            category = "Zapatillas - Baloncesto",
            sportIcon = "👟",
            priceHistory = listOf(0.6f, 0.5f, 0.65f, 0.6f, 0.8f, 0.72f),
        ),
    )

    companion object {
        const val ARG_ITEM_INDEX = "itemIndex"
    }
}
