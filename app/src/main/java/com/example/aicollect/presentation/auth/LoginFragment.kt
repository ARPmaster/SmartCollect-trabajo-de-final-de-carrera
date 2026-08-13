package com.example.aicollect.presentation.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
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
import com.example.aicollect.databinding.FragmentLoginBinding
import com.example.aicollect.presentation.collection.PlaceholderFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Visual + functional implementation of "Iniciar Sesión" (Figma 81:7 dark / 82:350 light). */
@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.isAlreadySignedIn()) {
            navigateToHome()
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.updatePadding(top = systemBars.top, bottom = maxOf(systemBars.bottom, ime.bottom))
            insets
        }

        binding.btnTogglePasswordVisibility?.setOnClickListener { togglePasswordVisibility() }

        binding.btnSignIn.setOnClickListener {
            viewModel.signIn(
                email = binding.etEmail.text?.toString().orEmpty().trim(),
                password = binding.etPassword.text?.toString().orEmpty(),
            )
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(
                R.id.forgotPasswordFragment,
                bundleOf(PlaceholderFragment.ARG_TITLE to getString(R.string.forgot_password_placeholder_title)),
            )
        }

        binding.tvCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        val isLoading = state is LoginUiState.Loading
        binding.btnSignIn.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.btnSignIn.text = getString(
            if (isLoading) R.string.login_signing_in else R.string.login_sign_in_button,
        )

        when (state) {
            is LoginUiState.Success -> navigateToHome()
            is LoginUiState.Error -> Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            else -> Unit
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
        binding.btnTogglePasswordVisibility?.setImageResource(
            if (isPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
