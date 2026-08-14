package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentFiltersSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** "Inicio: Filtros" bottom sheet (Figma nodes 2027:211 dark / 2027:332 light). */
class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFiltersSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentFiltersSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvFilterSportValue.text = resources.getStringArray(R.array.filter_sport_options).first()
        binding.tvFilterItemTypeValue.text = resources.getStringArray(R.array.filter_item_type_options).first()
        binding.tvFilterConditionValue.text = resources.getStringArray(R.array.filter_condition_options).first()

        setUpValueRange()
        setUpDropdown(binding.btnFilterSport, binding.tvFilterSportValue, R.array.filter_sport_options)
        setUpDropdown(binding.btnFilterItemType, binding.tvFilterItemTypeValue, R.array.filter_item_type_options)
        setUpDropdown(binding.btnFilterCondition, binding.tvFilterConditionValue, R.array.filter_condition_options)

        binding.btnCloseFilters.setOnClickListener { dismiss() }
        binding.btnApplyFilters.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_MIN_PRICE to binding.etMinValue.text.toString().toIntOrNull().orDefaultMin(),
                    KEY_MAX_PRICE to binding.etMaxValue.text.toString().toIntOrNull().orDefaultMax(),
                ),
            )
            dismiss()
        }
    }

    private fun setUpValueRange() {
        val slider = binding.rangeSliderValue
        slider.setValues(0f, 15000f)
        binding.etMinValue.setText(slider.values[0].toInt().toString())
        binding.etMaxValue.setText(slider.values[1].toInt().toString())

        slider.addOnChangeListener { _, _, fromUser ->
            if (!fromUser) return@addOnChangeListener
            binding.etMinValue.setText(slider.values[0].toInt().toString())
            binding.etMaxValue.setText(slider.values[1].toInt().toString())
        }

        binding.etMinValue.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) syncSliderFromInputs() }
        binding.etMaxValue.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) syncSliderFromInputs() }
    }

    private fun syncSliderFromInputs() {
        val slider = binding.rangeSliderValue
        val min = binding.etMinValue.text.toString().toIntOrNull()?.coerceIn(0, 100_000) ?: slider.values[0].toInt()
        val max = binding.etMaxValue.text.toString().toIntOrNull()?.coerceIn(0, 100_000) ?: slider.values[1].toInt()
        if (min <= max) {
            slider.setValues(min.toFloat(), max.toFloat())
        }
    }

    private fun setUpDropdown(button: View, valueLabel: TextView, optionsRes: Int) {
        val options = resources.getStringArray(optionsRes)
        button.setOnClickListener {
            val popup = PopupMenu(requireContext(), button)
            options.forEachIndexed { index, option -> popup.menu.add(0, index, index, option) }
            popup.setOnMenuItemClickListener { item ->
                valueLabel.text = options[item.itemId]
                true
            }
            popup.show()
        }
    }

    private fun Int?.orDefaultMin() = this ?: 0
    private fun Int?.orDefaultMax() = this ?: 100_000

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
        const val REQUEST_KEY = "filter_result"
        const val KEY_MIN_PRICE = "min_price"
        const val KEY_MAX_PRICE = "max_price"
    }
}
