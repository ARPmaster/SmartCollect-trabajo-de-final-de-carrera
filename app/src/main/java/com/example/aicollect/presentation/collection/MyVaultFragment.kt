package com.example.aicollect.presentation.collection

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentMyVaultBinding
import com.example.aicollect.databinding.ItemVaultTopValuedBinding
import com.example.aicollect.presentation.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * "My Vault" / Estadísticas (Figma 2014:76 / 2015:376), wired to real Firestore items via
 * [MyVaultViewModel] — sport filter chips stay interactive-but-not-filtering (pre-existing,
 * documented limitation, same as [FilterBottomSheetFragment]'s sport/type/condition selectors).
 * The original item-type donut (Jerseys/Balones/Tarjetas/Otros) is repurposed to show
 * distribution by `estado` (Nuevo/Buen estado/Malas condiciones) — the real Item schema has no
 * "tipo de artículo" field, only deporte/estado (see PROJECT_CONTEXT.md).
 */
@AndroidEntryPoint
class MyVaultFragment : Fragment() {

    private var _binding: FragmentMyVaultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyVaultViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentMyVaultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpSportChips()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { render(it) }
            }
        }
    }

    private fun render(state: MyVaultUiState) {
        when (state) {
            is MyVaultUiState.Loading -> Unit
            is MyVaultUiState.Empty -> {
                binding.scrollContent.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            }
            is MyVaultUiState.Content -> bind(state)
            is MyVaultUiState.Error -> showSnackbar(binding.root, state.message).show()
        }
    }

    private fun bind(state: MyVaultUiState.Content) {
        binding.scrollContent.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE

        binding.tvVaultAmount.text = state.totalValueLabel
        binding.rowVaultChange.visibility = if (state.changeLabel != null) View.VISIBLE else View.GONE
        binding.tvVaultChange.text = state.changeLabel.orEmpty()
        binding.chartPortfolio.values = state.evolution

        listOf(binding.tvMonth1, binding.tvMonth2, binding.tvMonth3, binding.tvMonth4, binding.tvMonth5, binding.tvMonth6)
            .forEachIndexed { index, view -> view.text = state.monthLabels.getOrNull(index).orEmpty() }

        setUpSportDistribution(state.sportDistribution)
        setUpConditionDistribution(state.conditionPercentByEstado)

        binding.tvTotalItemsValue.text = state.totalItemsLabel

        setUpTopValuedItems(state.topValuedItems)
    }

    private fun setUpSportDistribution(distribution: List<Pair<String, Int>>) {
        val rows = listOf(
            Triple(binding.rowDistribution1, binding.tvDistribution1Label, binding.tvDistribution1Value) to
                (binding.trackDistribution1 to binding.fillDistribution1),
            Triple(binding.rowDistribution2, binding.tvDistribution2Label, binding.tvDistribution2Value) to
                (binding.trackDistribution2 to binding.fillDistribution2),
            Triple(binding.rowDistribution3, binding.tvDistribution3Label, binding.tvDistribution3Value) to
                (binding.trackDistribution3 to binding.fillDistribution3),
        )
        rows.forEachIndexed { index, (labels, track) ->
            val (row, label, value) = labels
            val (trackView, fillView) = track
            val entry = distribution.getOrNull(index)
            row.visibility = if (entry != null) View.VISIBLE else View.GONE
            if (entry != null) {
                label.text = entry.first
                value.text = "${entry.second}%"
                trackView.doOnLayout {
                    fillView.layoutParams = fillView.layoutParams.apply { width = (trackView.width * entry.second / 100f).toInt() }
                }
            }
        }
    }

    /** Unlike [setUpSportDistribution] (dynamic set of sports, sorted by frequency), estado has
     * exactly 3 fixed values with a meaningful order (Nuevo=gold, Buen estado=green, Malas
     * condiciones=red) — computed directly instead of through [PortfolioAnalytics.distributionBy]
     * so the color always matches the same estado, not whichever happens to be most common. */
    private fun setUpConditionDistribution(percentByEstado: Map<String, Int>) {
        val estadoOrder = resources.getStringArray(R.array.filter_condition_options)
        val colors = listOf(R.color.collect_gold, R.color.collect_green, R.color.vault_negative)
        val percents = estadoOrder.map { estado -> percentByEstado[estado] ?: 0 }
        val slots = listOf(
            Triple(binding.dotLegend1, binding.tvLegend1Label, binding.tvLegend1Value),
            Triple(binding.dotLegend2, binding.tvLegend2Label, binding.tvLegend2Value),
            Triple(binding.dotLegend3, binding.tvLegend3Label, binding.tvLegend3Value),
        )
        slots.forEachIndexed { index, (dot, label, value) ->
            val color = ContextCompat.getColor(requireContext(), colors[index])
            dot.backgroundTintList = ColorStateList.valueOf(color)
            value.setTextColor(color)
            label.text = estadoOrder.getOrNull(index).orEmpty()
            value.text = "${percents.getOrNull(index) ?: 0}%"
        }
        binding.chartDonut.segments = estadoOrder.indices.map { index ->
            DonutSegment(percent = percents[index], color = ContextCompat.getColor(requireContext(), colors[index]))
        }
    }

    private fun setUpSportChips() {
        val chipLabels = listOf(
            getString(R.string.vault_filter_futbol),
            getString(R.string.vault_filter_basquet),
            getString(R.string.vault_filter_beisbol),
            getString(R.string.vault_filter_tenis),
        )
        val chips = chipLabels.map { label ->
            (layoutInflater.inflate(R.layout.item_vault_sport_chip, binding.rowSportChips, false) as TextView).apply {
                text = label
            }
        }

        fun select(selected: TextView) {
            chips.forEach { chip ->
                val isSelected = chip === selected
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), if (isSelected) R.color.vault_chip_active_text else R.color.vault_text_secondary),
                )
                chip.setBackgroundResource(if (isSelected) R.drawable.bg_vault_chip_active else R.drawable.bg_vault_chip_inactive)
            }
        }

        chips.forEach { chip ->
            binding.rowSportChips.addView(chip)
            chip.setOnClickListener { select(chip) }
        }
        select(chips.first())
    }

    private fun setUpTopValuedItems(topItems: List<TopValuedItemUi>) {
        binding.listTopItems.removeAllViews()
        topItems.forEachIndexed { index, item ->
            val itemBinding = ItemVaultTopValuedBinding.inflate(layoutInflater, binding.listTopItems, false)
            itemBinding.ivItemImage.load(item.imageUrl)
            itemBinding.tvItemName.text = item.nombre
            itemBinding.tvItemSubtitle.text = item.subtitle
            itemBinding.tvItemValue.text = item.valueLabel

            itemBinding.tvItemChange.text = item.changeLabel.orEmpty()
            itemBinding.tvItemChange.visibility = if (item.changeLabel != null) View.VISIBLE else View.GONE
            itemBinding.tvItemChange.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (item.isPositiveChange) R.color.collect_green else R.color.vault_negative,
                ),
            )

            if (index != topItems.lastIndex) {
                (itemBinding.root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    resources.getDimensionPixelSize(R.dimen.vault_top_item_spacing)
            }
            itemBinding.root.setOnClickListener {
                findNavController().navigate(
                    R.id.itemDetailFragment,
                    bundleOf(ItemDetailFragment.ARG_ITEM_ID to item.id),
                )
            }
            binding.listTopItems.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
