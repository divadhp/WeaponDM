package com.example.weapondm.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.weapondm.R
import com.example.weapondm.utils.Prediction
import kotlinx.android.synthetic.main.class_layout.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.math.min


class ClassificationViewFragment(bp: Bitmap, file: File, labels: Vector<String>, modelName: String): Fragment() {

    private lateinit var imgData: ByteBuffer
    val bitmap = bp
    val file = file
    val labels = labels
    val modelName = modelName
    lateinit var model: Interpreter
    private var imageX = -1
    private var imageY = -1
    private lateinit var intValues: IntArray
    private var quantized = false


    private val IMAGE_STD = 1f
    private val IMAGE_MEAN = 0f

    private var time = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       return inflater.inflate(R.layout.class_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.title = "Clasificación con " + modelName

        image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 1080, 1440, false))

        back_button.setOnClickListener {
            activity!!.supportFragmentManager.popBackStack()
        }


        setup()
        inference()
    }

    private fun setup() {

        val options = Interpreter.Options().apply {
            //setUseNNAPI(true)
            //addDelegate(HexagonDelegate(context))
            addDelegate(NnApiDelegate())
        }

        model = Interpreter(file, options)

        val imageShape = model.getInputTensor(0).shape()

        imageX = imageShape[1]
        imageY = imageShape[2]

        val outShape = model.getOutputTensor(0).shape()

        imgData = ByteBuffer.allocateDirect(model.getInputTensor(0).numBytes())
        imgData.order(ByteOrder.nativeOrder())
        intValues = IntArray(imageX * imageY)

        var size = 1
        for (i in imageShape) {
            size *= i
        }

        if (size / model.getInputTensor(0).numBytes() == 1) {
            quantized = true
        }

    }

    fun inference() {
        var bp = Bitmap.createBitmap(bitmap, 0, bitmap.height / 2 - bitmap.width / 2, bitmap.width, bitmap.width)
        var b = Bitmap.createScaledBitmap(bp, imageX, imageY, false)

        b.getPixels(intValues, 0, b.width, 0, 0, b.width, b.height)

        imgData.rewind()
        var x = 0
        for (i in 0 until imageX) {
            for (j in 0 until imageY) {
                val pixelValue = intValues[i * imageX + j]
                if (quantized) {
                    // Quantized model
                    imgData.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData.put((pixelValue and 0xFF).toByte())
                } else { // Float model
                    x += 3 * 4
                    if (model.getInputTensor(0).shape()[3] == 1) {
                        val r = pixelValue shr 16 and 0xFF
                        val g = pixelValue shr 8 and 0xFF
                        val b = pixelValue and 0XFF
                        val gray = ((0.2989 * r + 0.5870 * g + 0.1140 * b)).toFloat()
                        if (gray < 90f)
                            imgData.putFloat( (255f - 33.791224489795916f) / 79.17246322228644f)
                        else
                            imgData.putFloat( (0f - 33.791224489795916f) / 79.17246322228644f )
                    } else {
                        imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    }
                }
            }
        }

        val recongnitions = inferenceClassification()

        activity!!.runOnUiThread {
            inf_time.text = "%.2f segundos".format(time / 1000f)
            clase.text = recongnitions[0].prediction
            confianza.text = "%.2f".format(recongnitions[0].score * 100) + "%"
            val imageShape = model.getInputTensor(0).shape()
            tam.text = "%dx%dx%dx%d".format(imageShape[0], imageShape[1], imageShape[2], imageShape[3])
        }
    }


    private fun inferenceClassification(): Array<Prediction> {

        val tini = SystemClock.elapsedRealtime()

        val probability = TensorBuffer.createFixedSize(model.getOutputTensor(0).shape(), model.getOutputTensor(0).dataType())
        model.run(imgData, probability.buffer.rewind())
        time = SystemClock.elapsedRealtime() - tini

        val labeledProbability = TensorLabel(labels, probability).mapWithFloatValue


        val pq: PriorityQueue<Prediction> = PriorityQueue(
            3,
            object : Comparator<Prediction?> {
                override fun compare(lhs: Prediction?, rhs: Prediction?): Int {
                    // Intentionally reversed to put high confidence at the head of the queue.
                    return java.lang.Float.compare(rhs!!.score, lhs!!.score)
                }

            })


        for (entry in labeledProbability.entries) {
            Log.d("Label", entry.key + " " + entry.value)
            pq.add(
                Prediction(
                    entry.key,
                    entry.value,
                    null
                )
            )
        }


        val recognitions: Array<Prediction> = Array(min(pq.size, 3)) { i ->
            pq.poll()!!
        }
        return recognitions
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

}