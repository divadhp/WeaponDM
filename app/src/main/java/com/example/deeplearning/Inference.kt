package com.example.deeplearning

import android.graphics.*
import android.media.Image
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.HexagonDelegate
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.min


class Inference(file: File, labels: Vector<String>) {

    private lateinit var outputClasses: Array<FloatArray>
    private lateinit var outputLocations: Array<Array<FloatArray>>
    private lateinit var outputScores: Array<FloatArray>
    private lateinit var numDetections: FloatArray
    private lateinit var intValues: IntArray
    private lateinit var model : Interpreter
    private var imageX = -1
    private var imageY = -1
    private var IMAGE_MEAN = -1f
    private var IMAGE_STD = -1f
    private lateinit var imgData : ByteBuffer
    private val labels = labels
    private var quantized : Boolean = false

    var previewX = 0
    var previewY = 0
    var time = 0L


    init {
        setupModel(file)
    }

    protected fun setupModel(file: File) {
        Log.d("Model", "SetUp")

        val options = Interpreter.Options().apply {
            addDelegate(GpuDelegate())
            //addDelegate(NnApiDelegate())
        }

        model = Interpreter(file, options)
        model.outputTensorCount
        val imageShape = model.getInputTensor(0).shape()

        imageX = imageShape[1]
        imageY = imageShape[2]

        val outShape = model.getOutputTensor(0).shape()

        imgData = ByteBuffer.allocateDirect(model.getInputTensor(0).numBytes())
        imgData.order(ByteOrder.nativeOrder())


        Log.d("Shape", outShape.size.toString())
        Log.d("Shape", model.getInputTensor(0).dataType().toString())
        var size = 1
        for (i in imageShape) {
            size *= i
        }

        if (size / model.getInputTensor(0).numBytes() == 1) {
            quantized = true
        }
        Log.d("Quantized", quantized.toString())


        IMAGE_MEAN = 127.5f
        IMAGE_STD = 127.5f

        outputLocations = arrayOf(Array<FloatArray>(outShape[1]) {FloatArray(4)})
        outputClasses = arrayOf(FloatArray(outShape[1]))
        outputScores  = arrayOf(FloatArray(outShape[1]))
        numDetections = FloatArray(1)
        intValues = IntArray(imageY * imageX)


    }

    fun inference(bitmap: Bitmap) : Array<Recognition> {

        Log.d("Model", "Inference")
        var b = Bitmap.createScaledBitmap(bitmap, imageX, imageY, false)

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
                        if (gray > 160f)
                            imgData.putFloat( (0f - 33.791224489795916f) / 79.17246322228644f)
                        else
                            imgData.putFloat( ( (255f - gray) - 33.791224489795916f) / 79.17246322228644f )
                        Log.d("Hola", "MNIST")
                    } else {
                        imgData.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                        imgData.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    }
                }
            }
        }
        val outputMap: MutableMap<Int, Any> = HashMap()
        if (model.getOutputTensor(0).shape().size == 3) {
            outputMap[0] = outputLocations
            outputMap[1] = outputClasses
            outputMap[2] = outputScores
            outputMap[3] = numDetections
        } else {
            outputMap[0] = outputClasses
            outputMap[1] = outputScores
        }

        val inputArray = arrayOf<Any>(imgData)
        val tini = SystemClock.elapsedRealtime()

        if (model.getOutputTensor(0).shape().size == 3) {
            model.runForMultipleInputsOutputs(inputArray, outputMap)
        } else {
            return inferenceClassification()
        }
        time = SystemClock.elapsedRealtime() - tini
        val reconocimientos = Array(numDetections[0].toInt()) { i ->
            Recognition(labels[outputClasses[0][i].toInt()+ 1], outputScores[0][i], RectF(outputLocations[0][i][1] * previewX, outputLocations[0][i][0] * previewY, outputLocations[0][i][3]*previewX, outputLocations[0][i][2]*previewY))
        }

        return reconocimientos

    }

    private fun inferenceClassification(): Array<Recognition> {

        val tini = SystemClock.elapsedRealtime()

        val probability = TensorBuffer.createFixedSize(model.getOutputTensor(0).shape(), model.getOutputTensor(0).dataType())
        model.run(imgData, probability.buffer.rewind())
        time = SystemClock.elapsedRealtime() - tini

        val labeledProbability = TensorLabel(labels, probability).mapWithFloatValue


        val pq: PriorityQueue<Recognition> = PriorityQueue(
            3,
            object : Comparator<Recognition?> {
                override fun compare(lhs: Recognition?, rhs: Recognition?): Int {
                    // Intentionally reversed to put high confidence at the head of the queue.
                    return java.lang.Float.compare(rhs!!.score, lhs!!.score)
                }

            })


        for (entry in labeledProbability.entries) {
            Log.d("Label", entry.key + " " + entry.value)
            pq.add(Recognition(entry.key, entry.value, null))
        }


        val recognitions: Array<Recognition> = Array(min(pq.size, 3)) {i ->
            pq.poll()!!
        }
        return recognitions
    }

    fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }





    fun close() {
        model.close()
    }
}