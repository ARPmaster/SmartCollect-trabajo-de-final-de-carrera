package com.example.aicollect.presentation.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
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
import com.example.aicollect.databinding.FragmentSecurityBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** "Seguridad" screen (Figma 2076:132 claro / 2076:271 oscuro), opened from the drawer. */
@AndroidEntryPoint
class SecurityFragment : Fragment() {

    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SecurityViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
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

        binding.etEmail.setText(viewModel.currentEmail())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        var isNewPasswordVisible = false
        binding.btnToggleNewPasswordVisibility.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordVisibility(binding.etNewPassword, binding.btnToggleNewPasswordVisibility, isNewPasswordVisible)
        }

        var isConfirmPasswordVisible = false
        binding.btnToggleConfirmPasswordVisibility.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordVisibility(
                binding.etConfirmPassword,
                binding.btnToggleConfirmPasswordVisibility,
                isConfirmPasswordVisible,
            )
        }

        binding.btnSaveChanges.setOnClickListener {
            viewModel.saveChanges(
                email = binding.etEmail.text?.toString().orEmpty().trim(),
                newPassword = binding.etNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: SecurityUiState) {
        val isLoading = state is SecurityUiState.Loading
        binding.btnSaveChanges.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etNewPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.btnSaveChanges.text = getString(
            if (isLoading) R.string.security_saving else R.string.security_save_button,
        )

        when (state) {
            is SecurityUiState.Success -> {
                binding.etNewPassword.text?.clear()
                binding.etConfirmPassword.text?.clear()
                Snackbar.make(binding.root, R.string.security_success, Snackbar.LENGTH_LONG).show()
            }
            is SecurityUiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            else -> Unit
        }
    }

    private fun togglePasswordVisibility(field: EditText, toggleButton: ImageButton, isVisible: Boolean) {
        val cursorPosition = field.selectionStart
        field.inputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        field.setSelection(cursorPosition)
        toggleButton.setImageResource(if (isVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
