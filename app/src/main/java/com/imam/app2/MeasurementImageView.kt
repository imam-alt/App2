package com.imam.app2

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.hypot

class MeasurementImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        style = Paint.Style.FILL
    }

    private val userPoints = mutableListOf<PointF>()
    private var lastLengthPx: Double? = null

    init {
        isClickable = true
        scaleType = ScaleType.FIT_CENTER
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
            val bitmapPoint = viewPointToDrawablePoint(event.x, event.y) ?: return true
            if (userPoints.size >= 2) {
                userPoints.clear()
                lastLengthPx = null
            }
            userPoints.add(bitmapPoint)
            if (userPoints.size == 2) {
                lastLengthPx = distance(userPoints[0], userPoints[1])
            }
            invalidate()
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    fun clearMeasurement() {
        userPoints.clear()
        lastLengthPx = null
        invalidate()
    }

    fun getActiveLengthPx(): Double? = lastLengthPx

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawable == null || userPoints.isEmpty()) return

        val viewPoints = userPoints.mapNotNull { drawablePointToViewPoint(it) }
        for ((index, point) in viewPoints.withIndex()) {
            canvas.drawCircle(point.x, point.y, 12f, pointPaint)
            canvas.drawText("${index + 1}", point.x + 16f, point.y - 16f, textPaint)
        }
        if (viewPoints.size == 2) {
            canvas.drawLine(
                viewPoints[0].x,
                viewPoints[0].y,
                viewPoints[1].x,
                viewPoints[1].y,
                linePaint
            )
        }
    }

    private fun drawablePointToViewPoint(drawablePoint: PointF): PointF? {
        val d: Drawable = drawable ?: return null
        val matrixValues = FloatArray(9)
        imageMatrix.getValues(matrixValues)
        val scaleX = matrixValues[android.graphics.Matrix.MSCALE_X]
        val scaleY = matrixValues[android.graphics.Matrix.MSCALE_Y]
        val transX = matrixValues[android.graphics.Matrix.MTRANS_X]
        val transY = matrixValues[android.graphics.Matrix.MTRANS_Y]

        if (d.intrinsicWidth <= 0 || d.intrinsicHeight <= 0) return null
        return PointF(
            drawablePoint.x * scaleX + transX,
            drawablePoint.y * scaleY + transY
        )
    }

    private fun viewPointToDrawablePoint(x: Float, y: Float): PointF? {
        val d: Drawable = drawable ?: return null
        val matrixValues = FloatArray(9)
        imageMatrix.getValues(matrixValues)
        val scaleX = matrixValues[android.graphics.Matrix.MSCALE_X]
        val scaleY = matrixValues[android.graphics.Matrix.MSCALE_Y]
        val transX = matrixValues[android.graphics.Matrix.MTRANS_X]
        val transY = matrixValues[android.graphics.Matrix.MTRANS_Y]

        if (scaleX == 0f || scaleY == 0f) return null

        val drawableX = (x - transX) / scaleX
        val drawableY = (y - transY) / scaleY

        if (drawableX < 0 || drawableY < 0 || drawableX > d.intrinsicWidth || drawableY > d.intrinsicHeight) {
            return null
        }
        return PointF(drawableX, drawableY)
    }

    private fun distance(a: PointF, b: PointF): Double {
        return hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())
    }
}
