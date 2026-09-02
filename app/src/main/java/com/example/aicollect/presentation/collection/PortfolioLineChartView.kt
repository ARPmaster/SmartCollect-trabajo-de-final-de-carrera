// Vista custom que dibuja a mano la gráfica de área/línea de evolución de valor (6 meses), con
// etiquetas y gridlines en el eje Y, ya que el proyecto no usa ninguna librería de gráficos.
package com.example.aicollect.presentation.collection

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.aicollect.R

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
    private val gridColor = ContextCompat.getColor(context, R.color.collect_gold_10)
    private val labelColor = ContextCompat.getColor(context, R.color.drawer_text_muted)

    private val labelTextSizePx = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        11f,
        context.resources.displayMetrics,
    )

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

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = gridColor
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = labelColor
        textSize = labelTextSizePx
        textAlign = Paint.Align.LEFT
    }

    private fun formatLabel(value: Float): String = ItemFormatting.formatKnownValue(value.toDouble(), "EUR")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        // La etiqueta superior del eje Y se centra sobre esta línea; sin margen suficiente sus
        // píxeles más altos quedan por encima de y=0 y se recortan contra el borde de la vista.
        val topPadding = labelTextSizePx / 2f + 4f
        val bottomPadding = 8f
        val labelGap = 8f

        // Escala realista: el eje Y siempre arranca en 0, con un techo base de 1000 € que
        // salta a 5000 € y luego a 10 000 € (y sigue creciendo en tramos de 5000 €) según
        // haga falta, para no recortar datos legítimos de la cartera.
        val min = 0f
        val dataMax = values.max()
        val max = when {
            dataMax <= AXIS_MAX_TIER_1 -> AXIS_MAX_TIER_1
            dataMax <= AXIS_MAX_TIER_2 -> AXIS_MAX_TIER_2
            dataMax <= AXIS_MAX_TIER_3 -> AXIS_MAX_TIER_3
            else -> kotlin.math.ceil(dataMax / AXIS_MAX_TIER_2) * AXIS_MAX_TIER_2
        }

        val range = max - min
        val midValue = min + range / 2f
        val labels = listOf(formatLabel(max), formatLabel(midValue), formatLabel(min))
        val leftPadding = labels.maxOf { labelPaint.measureText(it) } + labelGap
        val usableHeight = height - topPadding - bottomPadding

        fun yAt(value: Float) = topPadding + usableHeight - ((value - min) / range) * usableHeight

        listOf(max to labels[0], midValue to labels[1], min to labels[2]).forEach { (value, label) ->
            val y = yAt(value)
            canvas.drawLine(leftPadding, y, width.toFloat(), y, gridPaint)
            drawYLabel(canvas, label, y)
        }

        drawChartBody(canvas, leftPadding, topPadding, bottomPadding, min, range)
    }

    private fun drawYLabel(canvas: Canvas, label: String, y: Float) {
        val textOffset = -(labelPaint.ascent() + labelPaint.descent()) / 2f
        canvas.drawText(label, 0f, y + textOffset, labelPaint)
    }

    private fun drawChartBody(
        canvas: Canvas,
        leftPadding: Float,
        topPadding: Float,
        bottomPadding: Float,
        min: Float,
        range: Float,
    ) {
        val stepX = (width - leftPadding) / (values.size - 1).toFloat()
        val usableHeight = height - topPadding - bottomPadding

        fun xAt(index: Int) = leftPadding + index * stepX
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

    private companion object {
        const val AXIS_MAX_TIER_1 = 1_000f
        const val AXIS_MAX_TIER_2 = 5_000f
        const val AXIS_MAX_TIER_3 = 10_000f
    }
}
