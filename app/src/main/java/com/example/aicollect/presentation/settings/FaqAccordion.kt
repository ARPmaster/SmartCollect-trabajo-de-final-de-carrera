// Extensión que convierte una fila `item_faq` en un acordeón (pregunta/respuesta) que se
// expande al tocarla, reutilizada por las pantallas de Ayuda y Sobre la aplicación.
package com.example.aicollect.presentation.settings

import android.view.View
import com.example.aicollect.databinding.ItemFaqBinding

fun ItemFaqBinding.bindAccordion(questionRes: Int, answerRes: Int) {
    tvQuestion.setText(questionRes)
    tvAnswer.setText(answerRes)
    root.setOnClickListener {
        val expanded = tvAnswer.visibility == View.VISIBLE
        tvAnswer.visibility = if (expanded) View.GONE else View.VISIBLE
        ivChevron.rotation = if (expanded) 0f else 180f
    }
}
