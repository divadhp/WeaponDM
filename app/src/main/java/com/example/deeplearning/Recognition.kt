package com.example.deeplearning

import android.graphics.RectF

class Recognition(prediction: String, score: Float, box: RectF?) {

    val score = score
    val prediction = prediction
    val box = box
}

