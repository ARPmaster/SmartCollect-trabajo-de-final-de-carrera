// Pantalla de detalle de un ítem: carga sus datos por id, permite navegar a editarlo y borrarlo , y renderiza su evolución de precio.
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
import coil.load
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentItemDetailBinding
import com.example.aicollect.presentation.asString
import com.example.aicollect.presentation.showSnackbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        val id = arguments?.getString(ARG_ITEM_ID)
        if (id == null) {
            findNavController().popBackStack()
            return
        }
        viewModel.load(id)

        binding.btnEditItem.setOnClickListener {
            findNavController().navigate(R.id.editItemFragment, bundleOf(ARG_ITEM_ID to id))
        }
        binding.btnDeleteItem.setOnClickListener { showDeleteConfirmationDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { render(it) } }
                launch { viewModel.deleteState.collect { renderDelete(it) } }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.item_detail_delete_confirm_title)
            .setMessage(getString(R.string.item_detail_delete_confirm_message, binding.tvItemTitle.text))
            .setPositiveButton(R.string.item_detail_delete_confirm_button) { _, _ -> viewModel.deleteItem() }
            .setNegativeButton(R.string.item_detail_delete_cancel_button, null)
            .show()
    }

    private fun renderDelete(state: DeleteItemUiState) {
        val isDeleting = state is DeleteItemUiState.Deleting
        binding.btnDeleteItem.isEnabled = !isDeleting
        binding.btnEditItem.isEnabled = !isDeleting

        when (state) {
            is DeleteItemUiState.Success -> {
                showSnackbar(binding.root, R.string.item_detail_deleted_success).show()
                findNavController().popBackStack()
            }
            is DeleteItemUiState.Error -> showSnackbar(binding.root, state.message.asString(requireContext())).show()
            else -> Unit
        }
    }

    private fun render(state: ItemDetailUiState) {
        when (state) {
            is ItemDetailUiState.Loading -> Unit
            is ItemDetailUiState.Content -> bind(state)
            is ItemDetailUiState.Error -> {
                showSnackbar(binding.root, state.message.asString(requireContext())).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun bind(state: ItemDetailUiState.Content) {
        binding.ivItemImage.load(state.imageUrl)
        binding.tvItemTitle.text = state.nombre
        binding.tvItemPrice.text = state.priceLabel
        binding.tvItemCondition.text = state.estado
        binding.tvItemCategory.text = state.deporte
        binding.tvItemSportIcon.text = state.sportEmoji
        binding.chartPortfolio.values = state.evolution

        binding.tvValuationRange.visibility = if (state.valuationRangeLabel != null) View.VISIBLE else View.GONE
        binding.tvValuationRange.text = state.valuationRangeLabel.orEmpty()

        val monthViews = listOf(
            binding.tvMonth1, binding.tvMonth2, binding.tvMonth3,
            binding.tvMonth4, binding.tvMonth5, binding.tvMonth6,
        )
        monthViews.forEachIndexed { index, view -> view.text = state.monthLabels.getOrNull(index).orEmpty() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_ITEM_ID = "itemId"
    }
}
