package com.example.deeplearning

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView

class PreviewView: PreviewView {

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        Log.d("Draw", "drawCanvas")
        canvas?.drawRect(RectF(0f, 0f, 200f, 200f), Paint().apply { style = Paint.Style.FILL })
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.d("Draw", "onDraw")
    }

    override fun createSurfaceProvider(cameraInfo: CameraInfo?): Preview.SurfaceProvider {

        return super.createSurfaceProvider(cameraInfo)
    }

}