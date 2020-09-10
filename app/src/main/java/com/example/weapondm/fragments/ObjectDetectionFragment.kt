package com.example.weapondm.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.weapondm.R
import com.example.weapondm.camera.Camera
import com.example.weapondm.model.Inference
import com.example.weapondm.utils.Prediction
import kotlinx.android.synthetic.main.camera_layout.*
import java.io.File
import java.util.*

class ObjectDetectionFragment(modelFile: File, labels: Vector<String>, model:String) : Fragment() {

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    val modelFile = modelFile
    val labels = labels
    private val modelName = model
    lateinit var camera: Camera

    private lateinit var outputDirectory: File
    //private lateinit var cameraExecutor: ExecutorService
    private lateinit var model: Inference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity!!.title = modelName

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
        bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                bar_progress.text = "${p1}%"
                imageView.umbral = p1 / 100f
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
            }

        })


        //cameraExecutor = Executors.newSingleThreadExecutor()
    }


    private fun update(predictions: Array<Prediction>, tiempo: Long, contador: Int, t: Long, bp: Bitmap) {
        Log.d("Update", "Hola")
        activity?.runOnUiThread {
            viewFinder?.apply {
                imageView.reconocimientos = predictions
                imageView.invalidate()
                fps.text = "%.2f fps".format((1000f / (tiempo.toFloat() / contador.toFloat())))
                fps_total.text = "%.2f fps".format((1000f / t.toFloat()))
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {
        camera = Camera(
            this,
            modelFile,
            labels,
            viewFinder,
            this::update
        ).apply {
            startCamera()
        }
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
        camera.close()
        //model.close()
        //cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}