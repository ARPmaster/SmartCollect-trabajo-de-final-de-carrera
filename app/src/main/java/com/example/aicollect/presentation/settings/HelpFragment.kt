package com.example.aicollect.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aicollect.R
import com.example.aicollect.databinding.FragmentHelpBinding
import com.google.android.material.snackbar.Snackbar

/** "Ayuda y Soporte" / FAQ screen (Figma 2074:9 claro / 2076:163 oscuro), opened from the drawer. */
class HelpFragment : Fragment() {

    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
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

        binding.faqItem1.bindAccordion(R.string.help_question_1, R.string.help_answer_1)
        binding.faqItem2.bindAccordion(R.string.help_question_2, R.string.help_answer_2)
        binding.faqItem3.bindAccordion(R.string.help_question_3, R.string.help_answer_3)
        binding.faqItem4.bindAccordion(R.string.help_question_4, R.string.help_answer_4)
        binding.faqItem5.bindAccordion(R.string.help_question_5, R.string.help_answer_5)
        binding.faqItem6.bindAccordion(R.string.help_question_6, R.string.help_answer_6)

        binding.btnContactEmail.setOnClickListener { openEmailClient() }
    }

    private fun openEmailClient() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.help_support_email)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.help_support_email_subject))
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.help_no_email_app_error, Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
