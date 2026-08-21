package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.aicollect.R
import com.example.aicollect.application.items.Item
import com.example.aicollect.application.items.PortfolioAnalytics
import com.example.aicollect.databinding.FragmentItemDetailBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

/**
 * "Detalle de objeto/carta" (Figma 73:55 oscuro / 74:180 claro) — reached from the Home feed or
 * My Vault's top-valued list, both passing the real Firestore [Item.id] as [ARG_ITEM_ID]. Back
 * navigation goes through the shared toolbar (MainActivity swaps its hamburger icon for a back
 * arrow on this destination), not a screen-local button.
 */
@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels()

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

        val itemId = arguments?.getString(ARG_ITEM_ID)
        if (itemId == null) {
            findNavController().popBackStack()
            return
        }
        viewModel.load(itemId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: ItemDetailUiState) {
        when (state) {
            is ItemDetailUiState.Loading -> Unit
            is ItemDetailUiState.Content -> bind(state.item)
            is ItemDetailUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun bind(item: Item) {
        binding.ivItemImage.load(item.imageUrls.firstOrNull())
        binding.tvItemTitle.text = item.nombre
        binding.tvItemPrice.text = ItemFormatting.formatValue(item.valoracionActual, item.valoracionMoneda)
        binding.tvItemCondition.text = item.estado
        binding.tvItemCategory.text = item.deporte
        binding.tvItemSportIcon.text = sportEmoji(item.deporte)
        binding.chartPortfolio.values = PortfolioAnalytics.monthlyEvolution(listOf(item))

        val monthLabels = PortfolioAnalytics.monthLabels()
        val monthViews = listOf(
            binding.tvMonth1, binding.tvMonth2, binding.tvMonth3,
            binding.tvMonth4, binding.tvMonth5, binding.tvMonth6,
        )
        monthViews.forEachIndexed { index, view -> view.text = monthLabels.getOrNull(index).orEmpty() }
    }

    private fun sportEmoji(deporte: String): String = when (deporte.lowercase(Locale("es", "ES"))) {
        "baloncesto" -> "🏀"
        "fútbol" -> "⚽"
        "fútbol americano" -> "🏈"
        "béisbol" -> "⚾"
        else -> "🏆"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ITEM_ID = "itemId"
    }
}
