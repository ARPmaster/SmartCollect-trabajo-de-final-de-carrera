package com.example.aicollect.presentation.collection

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentMyVaultBinding
import com.example.aicollect.databinding.ItemVaultTopValuedBinding

/**
 * "My Vault" / Estadísticas (Figma 2014:76 / 2015:376). Chart data, distribution and top items
 * are sample data (same placeholder items as [HomeFragment]) — there is no items repository yet,
 * this screen doesn't depend on the recognizeItem pipeline. Sport filter chips are interactive
 * but don't filter anything yet, same conscious limitation as [FilterBottomSheetFragment].
 */
class MyVaultFragment : Fragment() {

    private var _binding: FragmentMyVaultBinding? = null
    private val binding get() = _binding!!

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

        binding.chartPortfolio.values = samplePortfolioEvolution
        setUpDistributionBar(binding.trackDistributionMlb, binding.fillDistributionMlb, binding.tvDistributionMlbValue, 45)
        setUpDistributionBar(binding.trackDistributionNba, binding.fillDistributionNba, binding.tvDistributionNbaValue, 30)
        setUpDistributionBar(binding.trackDistributionSoccer, binding.fillDistributionSoccer, binding.tvDistributionSoccerValue, 25)

        binding.tvLegendJerseysValue.text = "50%"
        binding.tvLegendBalonesValue.text = "30%"
        binding.tvLegendTarjetasValue.text = "15%"
        binding.tvLegendOtrosValue.text = "5%"
        binding.chartDonut.segments = sampleDonutSegments(requireContext())

        setUpSportChips()
        setUpTopValuedItems()
    }

    private fun setUpDistributionBar(track: View, fill: View, valueLabel: TextView, percent: Int) {
        valueLabel.text = "$percent%"
        track.doOnLayout { fill.layoutParams = fill.layoutParams.apply { width = (track.width * percent / 100f).toInt() } }
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

    private fun setUpTopValuedItems() {
        sampleTopValuedItems.forEach { item ->
            val itemBinding = ItemVaultTopValuedBinding.inflate(layoutInflater, binding.listTopItems, false)
            itemBinding.ivItemImage.setImageResource(item.image)
            itemBinding.tvItemName.text = item.name
            itemBinding.tvItemSubtitle.text = item.subtitle
            itemBinding.tvItemValue.text = item.value
            itemBinding.tvItemChange.text = item.changeLabel
            itemBinding.tvItemChange.setTextColor(
                ContextCompat.getColor(requireContext(), if (item.isPositive) R.color.collect_green else R.color.vault_negative),
            )
            if (item !== sampleTopValuedItems.last()) {
                (itemBinding.root.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    resources.getDimensionPixelSize(R.dimen.vault_top_item_spacing)
            }
            binding.listTopItems.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val samplePortfolioEvolution = listOf(0.55f, 0.42f, 0.5f, 0.68f, 0.6f, 0.78f, 0.9f, 1f)

    private data class VaultTopItem(
        val name: String,
        val subtitle: String,
        val image: Int,
        val value: String,
        val changeLabel: String,
        val isPositive: Boolean,
    )

    private val sampleTopValuedItems = listOf(
        VaultTopItem(
            name = "1996 NBA Finals Jersey",
            subtitle = "Firmada • Memorabilia",
            image = R.drawable.item_signed_jersey,
            value = "$425,000",
            changeLabel = "+4.2%",
            isPositive = true,
        ),
        VaultTopItem(
            name = "2005 Tiffany Dunk Low",
            subtitle = "Deadstock • Sneaker",
            image = R.drawable.item_grail_sneaker,
            value = "$210,000",
            changeLabel = "+1.8%",
            isPositive = true,
        ),
    )

    private fun sampleDonutSegments(context: Context) = listOf(
        DonutSegment(percent = 50, color = ContextCompat.getColor(context, R.color.collect_gold)),
        DonutSegment(percent = 30, color = withAlpha(ContextCompat.getColor(context, R.color.collect_gold), 0.7f)),
        DonutSegment(percent = 15, color = withAlpha(ContextCompat.getColor(context, R.color.collect_gold), 0.4f)),
        DonutSegment(percent = 5, color = ContextCompat.getColor(context, R.color.vault_text_secondary)),
    )

    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (Color.alpha(color) * alpha).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
