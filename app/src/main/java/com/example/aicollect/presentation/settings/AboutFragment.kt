// Pantalla "Sobre la aplicación": versión de la app y acordeones de términos/privacidad.
package com.example.aicollect.presentation.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aicollect.BuildConfig
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvVersion.text = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)

        binding.itemTerms.bindAccordion(R.string.about_terms, R.string.about_terms_answer)
        binding.itemPrivacy.bindAccordion(R.string.about_privacy, R.string.about_privacy_answer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
