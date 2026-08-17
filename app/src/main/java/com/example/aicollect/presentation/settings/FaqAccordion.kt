package com.example.aicollect.presentation.settings

import android.view.View
import com.example.aicollect.databinding.ItemFaqBinding

/** Wires an `item_faq` row as a tap-to-expand accordion, shared by [HelpFragment] and [AboutFragment]. */
fun ItemFaqBinding.bindAccordion(questionRes: Int, answerRes: Int) {
    tvQuestion.setText(questionRes)
    tvAnswer.setText(answerRes)
    root.setOnClickListener {
        val expanded = tvAnswer.visibility == View.VISIBLE
        tvAnswer.visibility = if (expanded) View.GONE else View.VISIBLE
        ivChevron.rotation = if (expanded) 0f else 180f
    }
}
