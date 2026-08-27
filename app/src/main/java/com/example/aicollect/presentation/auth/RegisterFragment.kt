/** Pantalla de registro: recoge correo, usuario y contraseña, delega la creación de cuenta en el
 * ViewModel y muestra feedback específico si el nombre de usuario ya está en uso.*/
package com.example.aicollect.presentation.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.navigation.navOptions
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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

        binding.btnTogglePasswordVisibility.setOnClickListener { togglePasswordVisibility() }

        binding.btnCreateAccount.setOnClickListener {
            viewModel.signUp(
                email = binding.etEmail.text?.toString().orEmpty().trim(),
                username = binding.etUsername.text?.toString().orEmpty().trim(),
                password = binding.etPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
            )
        }

        binding.tvGoToLogin.setOnClickListener {
            findNavController().popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: RegisterUiState) {
        val isLoading = state is RegisterUiState.Loading
        binding.btnCreateAccount.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etUsername.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.btnCreateAccount.text = getString(
            if (isLoading) R.string.register_creating_account else R.string.register_create_account_button,
        )

        when (state) {
            is RegisterUiState.Success -> navigateToHome()
            is RegisterUiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            is RegisterUiState.UsernameTaken -> showUsernameTakenFeedback(state.message)
            else -> Unit
        }
    }

    private fun showUsernameTakenFeedback(message: String) {
        binding.boxUsername.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_auth_input_error)
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(requireContext(), R.color.vault_negative))
            .setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            .show()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(USERNAME_TAKEN_BORDER_MS)
            binding.boxUsername.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_auth_input)
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(
            R.id.homeFragment,
            null,
            navOptions { popUpTo(R.id.loginFragment) { inclusive = true } },
        )
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        val cursorPosition = binding.etPassword.selectionStart
        binding.etPassword.inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        binding.etPassword.setSelection(cursorPosition)
        binding.btnTogglePasswordVisibility.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val USERNAME_TAKEN_BORDER_MS = 2000L
    }
}
