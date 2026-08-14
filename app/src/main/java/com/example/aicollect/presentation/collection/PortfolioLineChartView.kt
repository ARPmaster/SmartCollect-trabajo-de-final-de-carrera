package com.example.aicollect.presentation.collection

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.aicollect.R

/**
 * Hand-drawn area/line chart for "Evolución (6 meses)" (My Vault, Figma 2014:76), since there's
 * no charting library in the project. Values are plotted evenly spaced along the width,
 * normalized to the view's height.
 */
class PortfolioLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var values: List<Float> = emptyList()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val goldColor = ContextCompat.getColor(context, R.color.collect_gold)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = goldColor
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = goldColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = width / (values.size - 1).toFloat()
        val topPadding = 8f
        val bottomPadding = 8f
        val usableHeight = height - topPadding - bottomPadding

        fun xAt(index: Int) = index * stepX
        fun yAt(value: Float) = topPadding + usableHeight - ((value - min) / range) * usableHeight

        val linePath = Path().apply {
            moveTo(xAt(0), yAt(values[0]))
            for (i in 1 until values.size) {
                val prevX = xAt(i - 1)
                val prevY = yAt(values[i - 1])
                val currX = xAt(i)
                val currY = yAt(values[i])
                val midX = (prevX + currX) / 2f
                cubicTo(midX, prevY, midX, currY, currX, currY)
            }
        }

        val fillPath = Path(linePath).apply {
            lineTo(xAt(values.size - 1), height.toFloat())
            lineTo(xAt(0), height.toFloat())
            close()
        }

        fillPaint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            0x4DE5B543, 0x00E5B543,
            Shader.TileMode.CLAMP,
        )
        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(linePath, linePaint)
        canvas.drawCircle(xAt(values.size - 1), yAt(values.last()), 8f, dotPaint)
    }
}
