/** Pantalla de desambiguación de "Nueva Publicación": muestra los candidatos devueltos
*por el reconocimiento de imagen para que el usuario elija el correcto o indique que ninguno coincide.*/
package com.example.aicollect.presentation.newpost

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.aicollect.R
import com.example.aicollect.application.recognition.RankedCandidate
import com.example.aicollect.databinding.FragmentNewPostDisambiguationBinding
import com.example.aicollect.databinding.ItemCandidateCardBinding
import kotlin.math.roundToInt

class NewPostDisambiguationFragment : Fragment() {

    private var _binding: FragmentNewPostDisambiguationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewPostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentNewPostDisambiguationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
        }

        binding.cardNoneOfThese.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val candidates = viewModel.candidates
        binding.tvSubtitle.visibility = if (candidates.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = if (candidates.isEmpty()) View.VISIBLE else View.GONE

        candidates.forEachIndexed { index, candidate ->
            val cardBinding = ItemCandidateCardBinding.inflate(layoutInflater, binding.listCandidates, false)
            bindCandidate(cardBinding, candidate, isBestMatch = index == 0)
            if (index > 0) {
                (cardBinding.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                    resources.getDimensionPixelSize(R.dimen.candidate_card_spacing)
            }
            cardBinding.root.setOnClickListener { selectCandidate(candidate) }
            binding.listCandidates.addView(cardBinding.root)
        }

        binding.cardNoneOfThese.setOnClickListener { selectCandidate(null) }
    }

    private fun selectCandidate(candidate: RankedCandidate?) {
        viewModel.selectedCandidate = candidate
        findNavController().popBackStack()
    }

    private fun bindCandidate(binding: ItemCandidateCardBinding, candidate: RankedCandidate, isBestMatch: Boolean) {
        binding.root.background = ContextCompat.getDrawable(
            requireContext(),
            if (isBestMatch) R.drawable.bg_candidate_card_best_match else R.drawable.bg_candidate_card,
        )
        binding.tvBadgeBestMatch.visibility = if (isBestMatch) View.VISIBLE else View.GONE

        viewModel.photos.firstOrNull()?.let { bytes ->
            binding.ivCandidateThumbnail.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        }

        binding.tvCandidateName.text = candidate.nombre
        binding.tvCandidateSubtitle.text = listOfNotNull(candidate.marca, candidate.modelo, candidate.edicion, candidate.procedencia)
            .filter { it.isNotBlank() }
            .joinToString(" • ")

        val confidencePercent = (candidate.confianza * 100).roundToInt().coerceIn(0, 100)
        val isHighConfidence = candidate.confianza > HIGH_CONFIDENCE_THRESHOLD
        binding.tvCandidateConfidence.text = getString(R.string.disambiguation_confidence_percent_format, confidencePercent)
        binding.tvCandidateConfidence.setBackgroundResource(
            if (isHighConfidence) R.drawable.bg_badge_confidence_high else R.drawable.bg_badge_confidence_low,
        )
        binding.tvCandidateConfidence.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isHighConfidence) R.color.candidate_confidence_high_text else R.color.candidate_confidence_low_text,
            ),
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val HIGH_CONFIDENCE_THRESHOLD = 0.8
    }
}
