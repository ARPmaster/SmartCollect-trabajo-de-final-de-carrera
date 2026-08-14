package com.example.aicollect.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentForgotPasswordSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * "¿Olvidaste tu contraseña?" as a bottom sheet over LoginFragment, matching the usual UX
 * pattern for password recovery (dialog/sheet over the login screen) instead of a full nav
 * destination. Figma has full-screen mocks (87:7 dark / 87:38 light) that this intentionally
 * does not replicate 1:1.
 */
@AndroidEntryPoint
class ForgotPasswordBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentForgotPasswordSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentForgotPasswordSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseForgotPassword.setOnClickListener { dismiss() }
        binding.btnSendResetEmail.setOnClickListener {
            viewModel.sendResetEmail(binding.etForgotPasswordEmail.text?.toString().orEmpty().trim())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ForgotPasswordUiState) {
        val isLoading = state is ForgotPasswordUiState.Loading
        binding.btnSendResetEmail.isEnabled = !isLoading
        binding.etForgotPasswordEmail.isEnabled = !isLoading
        binding.tvSendResetEmail.text = getString(
            if (isLoading) R.string.forgot_password_sending else R.string.forgot_password_submit_button,
        )

        when (state) {
            is ForgotPasswordUiState.Success -> {
                Snackbar.make(binding.root, R.string.forgot_password_success, Snackbar.LENGTH_LONG).show()
                dismiss()
            }
            is ForgotPasswordUiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            else -> Unit
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ForgotPasswordBottomSheetFragment"
    }
}
