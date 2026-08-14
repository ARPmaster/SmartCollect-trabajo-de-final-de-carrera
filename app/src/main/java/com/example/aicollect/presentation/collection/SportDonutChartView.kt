package com.example.aicollect.presentation.collection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** One slice of the donut chart: [percent] of the whole (0-100) rendered in [color]. */
data class DonutSegment(val percent: Int, val color: Int)

/**
 * Hand-drawn proportional donut/ring chart for the "Distribución" legend in My Vault
 * (Figma 2014:76). Segments are proportional to [segments], drawn with a small gap between
 * them — simplified vs. the decorative broken-ring artwork in the mock, prioritizing an
 * honest proportional representation over pixel-matching that specific SVG.
 */
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
