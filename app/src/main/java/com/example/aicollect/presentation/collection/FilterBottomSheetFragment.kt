package com.example.aicollect.presentation.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aicollect.databinding.FragmentFiltersSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Stub for the "Inicio: Filtros" bottom sheet (Figma nodes 2027:211 / 2027:332), not implemented yet. */
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FilterBottomSheetFragment"
    }
}
