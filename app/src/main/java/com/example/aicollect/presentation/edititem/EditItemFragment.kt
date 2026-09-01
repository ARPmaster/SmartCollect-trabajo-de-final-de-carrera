// Pantalla de edición de un ítem ya publicado: formulario con los campos editables y su comunicación con EditItemViewModel.
package com.example.aicollect.presentation.edititem

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentEditItemBinding
import com.example.aicollect.presentation.asString
import com.example.aicollect.presentation.showSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditItemFragment : Fragment() {

    private var _binding: FragmentEditItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditItemViewModel by viewModels()

    private var selectedSport: String? = null
    private var selectedCondition: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(top = systemBars.top, bottom = maxOf(systemBars.bottom, ime.bottom))
            insets
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener {
            viewModel.save(
                name = binding.etName.text?.toString().orEmpty(),
                description = binding.etDescription.text?.toString(),
                sport = selectedSport,
                condition = selectedCondition,
            )
        }

        setUpDropdown(binding.btnSport, binding.tvSportValue, R.array.sport_options) { selectedSport = it }
        setUpDropdown(binding.btnCondition, binding.tvConditionValue, R.array.filter_condition_options) { selectedCondition = it }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.saveState.collect { renderSave(it) } }
            }
        }
    }

    private fun render(state: EditItemUiState) {
        when (state) {
            is EditItemUiState.Loading -> Unit
            is EditItemUiState.Content -> {
                if (binding.etName.text.isNullOrEmpty()) binding.etName.setText(state.nombre)
                if (binding.etDescription.text.isNullOrEmpty()) binding.etDescription.setText(state.descripcion.orEmpty())
                if (selectedSport == null) selectField(binding.tvSportValue, state.deporte).also { selectedSport = state.deporte }
                if (selectedCondition == null) selectField(binding.tvConditionValue, state.estado).also { selectedCondition = state.estado }
            }
            is EditItemUiState.Error -> {
                showSnackbar(binding.root, state.message.asString(requireContext())).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun selectField(valueLabel: TextView, value: String) {
        valueLabel.text = value
        valueLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.auth_input_text))
    }

    private fun renderSave(state: SaveEditUiState) {
        val isSaving = state is SaveEditUiState.Saving
        binding.overlayLoading.visibility = if (isSaving) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isSaving

        when (state) {
            is SaveEditUiState.Success -> {
                showSnackbar(binding.root, R.string.edit_item_success).show()
                findNavController().popBackStack()
            }
            is SaveEditUiState.Error -> showSnackbar(binding.root, state.message.asString(requireContext())).show()
            is SaveEditUiState.ValidationError -> {
                val messageRes = when (state.field) {
                    EditRequiredField.NAME -> R.string.edit_item_name_required_error
                    EditRequiredField.SPORT -> R.string.edit_item_sport_required_error
                    EditRequiredField.CONDITION -> R.string.edit_item_condition_required_error
                }
                showSnackbar(binding.root, messageRes).show()
            }
            else -> Unit
        }
    }

    private fun setUpDropdown(button: View, valueLabel: TextView, optionsRes: Int, onSelected: (String) -> Unit) {
        val options = resources.getStringArray(optionsRes)
        button.setOnClickListener {
            val popup = PopupMenu(requireContext(), button)
            options.forEachIndexed { index, option -> popup.menu.add(0, index, index, option) }
            popup.setOnMenuItemClickListener { item ->
                val option = options[item.itemId]
                selectField(valueLabel, option)
                onSelected(option)
                true
            }
            popup.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ITEM_ID = "itemId"
    }
}
