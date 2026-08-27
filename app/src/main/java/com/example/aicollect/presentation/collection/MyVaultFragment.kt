/**Pantalla My Vault (estadísticas de la colección): pinta el resumen de valor total, la
 *distribución por deporte/estado y los artículos más valorados, y gestiona los chips de filtro por deporte.*/
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
import com.example.aicollect.databinding.ItemVaultDistributionRowBinding
import com.example.aicollect.databinding.ItemVaultTopValuedBinding
import com.example.aicollect.presentation.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        highlightSelectedChip(state.selectedSport)
        setUpSportDistribution(state.sportDistribution)
        setUpConditionDistribution(state.conditionPercentByEstado, state.hasItemsForSelectedSport)

        binding.tvTotalItemsValue.text = state.totalItemsLabel

        setUpTopValuedItems(state.topValuedItems)
    }

    private fun setUpSportDistribution(distribution: List<Pair<String, Int>>) {
        binding.rowsSportDistribution.removeAllViews()
        distribution.forEachIndexed { index, (sport, percent) ->
            val rowBinding = ItemVaultDistributionRowBinding.inflate(layoutInflater, binding.rowsSportDistribution, false)
            rowBinding.tvDistributionLabel.text = sport
            rowBinding.tvDistributionValue.text = "$percent%"
            rowBinding.trackDistribution.doOnLayout {
                rowBinding.fillDistribution.layoutParams =
                    rowBinding.fillDistribution.layoutParams.apply { width = (rowBinding.trackDistribution.width * percent / 100f).toInt() }
            }
            if (index > 0) {
                (rowBinding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                    resources.getDimensionPixelSize(R.dimen.vault_distribution_row_spacing)
            }
            binding.rowsSportDistribution.addView(rowBinding.root)
        }
    }

    private fun setUpConditionDistribution(percentByEstado: Map<String, Int>, hasResults: Boolean) {
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
        binding.chartDonut.segments = if (hasResults) {
            estadoOrder.indices.map { index ->
                DonutSegment(percent = percents[index], color = ContextCompat.getColor(requireContext(), colors[index]))
            }
        } else {
            listOf(DonutSegment(percent = 100, color = ContextCompat.getColor(requireContext(), R.color.vault_progress_track)))
        }
    }

    private var sportChips: List<Pair<TextView, String?>> = emptyList()

    private fun setUpSportChips() {
        val chipLabels = resources.getStringArray(R.array.filter_sport_options).toList()
        val allLabel = chipLabels.first()
        sportChips = chipLabels.map { label ->
            val view = (layoutInflater.inflate(R.layout.item_vault_sport_chip, binding.rowSportChips, false) as TextView).apply {
                text = label
            }
            view to label.takeUnless { it == allLabel }
        }
        sportChips.forEach { (chip, sport) ->
            binding.rowSportChips.addView(chip)
            chip.setOnClickListener { viewModel.selectSport(sport) }
        }
    }

    private fun highlightSelectedChip(selectedSport: String?) {
        sportChips.forEach { (chip, sport) ->
            val isSelected = sport == selectedSport
            chip.setTextColor(
                ContextCompat.getColor(requireContext(), if (isSelected) R.color.vault_chip_active_text else R.color.vault_text_secondary),
            )
            chip.setBackgroundResource(if (isSelected) R.drawable.bg_vault_chip_active else R.drawable.bg_vault_chip_inactive)
        }
    }

    private fun setUpTopValuedItems(topItems: List<TopValuedItemUi>) {
        binding.listTopItems.removeAllViews()
        topItems.forEachIndexed { index, item ->
            val itemBinding = ItemVaultTopValuedBinding.inflate(layoutInflater, binding.listTopItems, false)
            itemBinding.ivItemImage.load(item.imageUrl)
            itemBinding.tvItemName.text = item.nombre
            itemBinding.tvItemSubtitle.text = item.subtitle
            itemBinding.tvItemValue.text = item.valueLabel

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
