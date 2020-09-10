package com.example.weapondm.utils

import android.graphics.RectF

class Prediction(prediction: String, score: Float, box: RectF?) {

    val score = score
    val prediction = prediction
    val box = box
}

