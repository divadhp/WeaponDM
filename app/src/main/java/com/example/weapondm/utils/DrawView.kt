package com.example.weapondm.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView

internal class DrawView : ImageView {
    constructor(context: Context?) : super(context) {}
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
    }

    var reconocimientos: Array<Prediction>? = null
    var umbral = 0.5f
    var color = Color.RED

    override fun onDraw(canvas: Canvas) {
        Log.d("Draw", "onDraw")
        super.onDraw(canvas)
        val paint = Paint()
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 10f
        paint.textSize = 60f

        val textPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 2f
            textSize = 60f
        }

        val bgPaint = Paint(paint).apply {
            alpha = 160
            style = Paint.Style.FILL
        }

        reconocimientos?.let {
            for (rec in it) {
                if (rec.score >= umbral) {
                    rec.box?.let {
                        Log.d("Box", it.toShortString() + rec.score)
                        canvas.drawRect(it, paint)
                        val cornerSize: Float =
                            Math.min(it.width(), it.height()) / 8.0f
                        val text = rec.prediction + " " + "%.2f".format(rec.score * 100)  + "%"
                        canvas.drawRect(it.left, it.top + 60, it.left + bgPaint.measureText(text), it.top, bgPaint)
                        canvas.drawText(rec.prediction +" " + "%.2f".format(rec.score * 100) + "%", it.left, it.top+60, textPaint)
                    }
                }
            }
        }
    }
}

