package com.example.weapondm.camera

import android.annotation.SuppressLint
import android.graphics.*
import android.media.Image
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.weapondm.model.Inference
import com.example.weapondm.utils.Prediction
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Camera(fragment: Fragment, modelFile: File, labels: Vector<String>, viewFinder: PreviewView, update: (preditcions: Array<Prediction>, time: Long, contador: Int, t: Long, bp: Bitmap) -> Unit) {

    val fragment = fragment
    val viewFinder = viewFinder
    lateinit var preview: Preview
    val update = update
    val model: Inference = Inference(modelFile, labels)
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    lateinit var cameraProvider: ProcessCameraProvider

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

    @SuppressLint("UnsafeExperimentalUsageError")
    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(fragment.context!!)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()

            var tiempo = 0L
            var contador = 0

            val imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetResolution(Size(900, 900))
                    .build().also {
                        it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->

                            val rotation = image.imageInfo.rotationDegrees
                            val matrix = Matrix()
                            matrix.postRotate(rotation.toFloat())
                            val bp = image.image!!.toBitmap()
                            val b = Bitmap.createBitmap(bp, 0, 0, bp.width, bp.height, matrix, true)
                            val tini = SystemClock.elapsedRealtime()
                            val reconocimientos = model.inference(b)
                            val t = SystemClock.elapsedRealtime() - tini
                            tiempo += model.time
                            contador++

                            model.previewY = viewFinder.height
                            model.previewX = viewFinder.width
                            Log.d("View", model.previewX.toString() + " " + model.previewY)
                            update(reconocimientos, tiempo, contador, t, b.copy(Bitmap.Config.RGB_565, false))
                            image.close()

                        })
                    }


            // Select back camera
            val cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    fragment, cameraSelector, preview, imageAnalyzer
                )
                preview?.setSurfaceProvider(viewFinder.createSurfaceProvider(camera!!.cameraInfo))

                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                    viewFinder.width.toFloat(), viewFinder.height.toFloat()
                )
                val centerWidth = viewFinder.width.toFloat() / 2
                val centerHeight = viewFinder.height.toFloat() / 2
                //create a point on the center of the view
                val autoFocusPoint = factory.createPoint(centerWidth, centerHeight)
                try {
                    camera!!.cameraControl.startFocusAndMetering(
                        FocusMeteringAction.Builder(
                            autoFocusPoint,
                            FocusMeteringAction.FLAG_AF
                        ).apply {
                            //auto-focus every 1 seconds
                            setAutoCancelDuration(1, TimeUnit.SECONDS)
                        }.build()
                    )
                } catch (e: CameraInfoUnavailableException) {
                    Log.d("ERROR", "cannot access camera", e)
                }

            } catch (exc: Exception) {
                Log.e("Camera", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(fragment.context))
    }

    fun close() {
        cameraProvider.unbindAll()
        //model.close()
        cameraExecutor.shutdown()
    }
}

