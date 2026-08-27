// Pantalla "Seguridad": cambio de email/contraseña y eliminación de cuenta (con confirmación
// y reautenticación por contraseña antes de borrar).
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
import androidx.navigation.navOptions
import com.example.aicollect.R
import com.example.aicollect.databinding.DialogDeleteAccountBinding
import com.example.aicollect.databinding.FragmentSecurityBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect { state -> render(state) } }
                launch { viewModel.deleteAccountState.collect { state -> renderDeleteAccount(state) } }
            }
        }
    }

    private fun showDeleteAccountDialog() {
        val dialogBinding = DialogDeleteAccountBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.security_delete_account_dialog_title)
            .setMessage(R.string.security_delete_account_dialog_message)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.security_delete_account_confirm_button) { _, _ ->
                viewModel.deleteAccount(dialogBinding.etPassword.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.security_delete_account_cancel_button, null)
            .show()
    }

    private fun renderDeleteAccount(state: DeleteAccountUiState) {
        binding.btnDeleteAccount.isEnabled = state !is DeleteAccountUiState.Deleting
        binding.btnDeleteAccount.text = getString(
            if (state is DeleteAccountUiState.Deleting) {
                R.string.security_delete_account_deleting
            } else {
                R.string.security_delete_account_button
            },
        )

        when (state) {
            is DeleteAccountUiState.Success -> {
                findNavController().navigate(
                    R.id.loginFragment,
                    null,
                    navOptions { popUpTo(R.id.nav_graph) { inclusive = true } },
                )
            }
            is DeleteAccountUiState.Error ->
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            else -> Unit
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
