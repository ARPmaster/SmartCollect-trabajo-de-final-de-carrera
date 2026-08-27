// Vista custom que dibuja a mano el donut de distribución proporcional (por deporte o estado) usado en My Vault.
package com.example.aicollect.presentation.collection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

data class DonutSegment(val percent: Int, val color: Int)

class SportDonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var segments: List<DonutSegment> = emptyList()
        set(value) {
            field = value
            invalidate()
        }

    private val strokeWidthPx = 28f
    private val gapDegrees = 6f

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.ROUND
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val total = segments.sumOf { it.percent }
        if (total <= 0) return

        val inset = strokeWidthPx / 2f
        val rect = RectF(inset, inset, width - inset, height - inset)

        var startAngle = -90f
        for (segment in segments) {
            val sweep = (segment.percent.toFloat() / total) * 360f
            arcPaint.color = segment.color
            canvas.drawArc(rect, startAngle + gapDegrees / 2f, sweep - gapDegrees, false, arcPaint)
            startAngle += sweep
        }
    }
}
