/** Bottom sheet de filtros de Home: recoge rango de precio, deporte, estado y orden elegidos
*por el usuario, los guarda en memoria mientras dure la visita a Home y devuelve el resultado a
*HomeFragment.*/
package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.aicollect.R
import com.example.aicollect.data.FilterSessionState
import com.example.aicollect.databinding.FragmentFiltersSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.math.roundToInt

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

    private val conditionFilterOptions: List<String> by lazy {
        listOf(getString(R.string.filter_any_condition)) + resources.getStringArray(R.array.filter_condition_options)
    }
    private val sportFilterOptions: List<String> by lazy { resources.getStringArray(R.array.filter_sport_options).toList() }
    private val sortOptions: List<String> by lazy { resources.getStringArray(R.array.filter_sort_options).toList() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val saved = FilterSessionState.current

        binding.tvFilterSportValue.text = saved.sport ?: sportFilterOptions.first()
        binding.tvFilterConditionValue.text = saved.condition ?: conditionFilterOptions.first()
        binding.tvFilterSortValue.text = sortOptions.getOrElse(saved.sortOrdinal) { sortOptions.first() }

        setUpValueRange(saved)
        setUpDropdown(binding.btnFilterSport, binding.tvFilterSportValue, sportFilterOptions.toTypedArray())
        setUpDropdown(binding.btnFilterCondition, binding.tvFilterConditionValue, conditionFilterOptions.toTypedArray())
        setUpDropdown(binding.btnFilterSort, binding.tvFilterSortValue, sortOptions.toTypedArray())

        binding.btnCloseFilters.setOnClickListener { dismiss() }
        binding.btnApplyFilters.setOnClickListener {
            val a = binding.etMinValue.text.toString().toIntOrNull().orDefaultMin()
            val b = binding.etMaxValue.text.toString().toIntOrNull().orDefaultMax()
            val minPrice = minOf(a, b)
            val maxPrice = maxOf(a, b)

            val selectedSport = binding.tvFilterSportValue.text.toString()
                .takeUnless { it == sportFilterOptions.first() }
            val selectedCondition = binding.tvFilterConditionValue.text.toString()
                .takeUnless { it == conditionFilterOptions.first() }
            val selectedSortOrdinal = sortOptions.indexOf(binding.tvFilterSortValue.text.toString()).coerceAtLeast(0)

            FilterSessionState.update(
                FilterSessionState.SavedFilters(
                    minPrice = minPrice,
                    maxPrice = maxPrice,
                    sport = selectedSport,
                    condition = selectedCondition,
                    sortOrdinal = selectedSortOrdinal,
                ),
            )

            requireActivity().supportFragmentManager.setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    KEY_MIN_PRICE to minPrice,
                    KEY_MAX_PRICE to maxPrice,
                    KEY_SPORT to selectedSport,
                    KEY_CONDITION to selectedCondition,
                    KEY_SORT_ORDINAL to selectedSortOrdinal,
                ),
            )
            dismiss()
        }
    }

    private fun setUpValueRange(saved: FilterSessionState.SavedFilters) {
        val slider = binding.rangeSliderValue
        val min = saved.minPrice.coerceIn(MIN_VALUE, MAX_VALUE).toFloat().roundToStep(slider.stepSize)
        val max = saved.maxPrice.coerceIn(MIN_VALUE, MAX_VALUE).toFloat().roundToStep(slider.stepSize)
        slider.setValues(minOf(min, max), maxOf(min, max))
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
        val min = (binding.etMinValue.text.toString().toIntOrNull()?.coerceIn(MIN_VALUE, MAX_VALUE) ?: slider.values[0].toInt())
            .toFloat().roundToStep(slider.stepSize)
        val max = (binding.etMaxValue.text.toString().toIntOrNull()?.coerceIn(MIN_VALUE, MAX_VALUE) ?: slider.values[1].toInt())
            .toFloat().roundToStep(slider.stepSize)
        val orderedMin = minOf(min, max)
        val orderedMax = maxOf(min, max)
        slider.setValues(orderedMin, orderedMax)
        binding.etMinValue.setText(orderedMin.toInt().toString())
        binding.etMaxValue.setText(orderedMax.toInt().toString())
    }

    private fun Float.roundToStep(step: Float): Float {
        if (step <= 0f) return this
        return ((this / step).roundToInt() * step).coerceIn(MIN_VALUE.toFloat(), MAX_VALUE.toFloat())
    }

    private fun setUpDropdown(button: View, valueLabel: TextView, options: Array<String>) {
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

    private fun Int?.orDefaultMin() = this ?: MIN_VALUE
    private fun Int?.orDefaultMax() = this ?: MAX_VALUE

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
        const val REQUEST_KEY = "filter_result"
        const val KEY_MIN_PRICE = "min_price"
        const val KEY_MAX_PRICE = "max_price"
        const val KEY_SPORT = "sport"
        const val KEY_CONDITION = "condition"
        const val KEY_SORT_ORDINAL = "sort_ordinal"

        private const val MIN_VALUE = FilterSessionState.DEFAULT_MIN_PRICE
        private const val MAX_VALUE = FilterSessionState.DEFAULT_MAX_PRICE
    }
}
