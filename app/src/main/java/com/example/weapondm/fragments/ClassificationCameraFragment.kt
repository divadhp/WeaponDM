package com.example.weapondm.fragments


import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.weapondm.R
import com.example.weapondm.utils.Prediction
import kotlinx.android.synthetic.main.camera_class_layout.*
import kotlinx.android.synthetic.main.camera_layout.viewFinder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ClassificationCameraFragment(modelFile: File, labels: Vector<String>, modelName: String) : Fragment() {

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private val modelFile = modelFile
    private val modelName = modelName
    private val labels = labels
    private var lastBitmap: Bitmap? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private  var transition = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_class_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.title = "Cámara"
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                activity!!,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Setup the listener for take photo button
        camera_capture_button.setOnClickListener {
            transition = true

        }


        cameraExecutor = Executors.newSingleThreadExecutor()
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

    private fun update(predictions: Array<Prediction>, tiempo: Long, contador: Int, t: Long, bp: Bitmap) {
        if (transition) {
            transition = false
            activity?.supportFragmentManager?.beginTransaction()?.replace(
                R.id.container,
                ClassificationViewFragment(
                    bp,
                    modelFile,
                    labels,
                    modelName
                )
            )?.addToBackStack(null)?.commit()
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        com.example.weapondm.camera.Camera(
            this,
            modelFile,
            labels,
            viewFinder,
            this::update
        ).startCamera()
        /*
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()

            val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
            val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
            imageAnalyzer =
                ImageAnalysis.Builder()
                    .setTargetResolution(Size(900, 900))
                    //.setTargetResolution(Size(1080, (0.6 * 1920).toInt()))
                    .build().also {
                        it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->

                            val rotation = image.imageInfo.rotationDegrees
                            val matrix = Matrix()
                            matrix.postRotate(rotation.toFloat())
                            val bp = image.image!!.toBitmap()
                            val b = Bitmap.createBitmap(bp, 0, 0, bp.width, bp.height, matrix, true)
                            lastBitmap = b.copy(Bitmap.Config.RGB_565, false)
                            Log.d("Imagen", "Se actualiza el bitmap")
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
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
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
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))

         */
    }



    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity!!.baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    context,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                activity?.finish()
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}
