package com.example.weapondm.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.example.weapondm.R
import java.io.*
import java.util.*

class ModelListFragment(files : Array<String>, preffix: String) : Fragment() {

    val files = files
    val preffix = preffix

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.list_model, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.title = "Lista de modelos"
        val listAdapter = ArrayAdapter<String>(context!!,
            R.layout.list_item, files)

        val listView: ListView = view.findViewById(R.id.listview)
        listView.adapter = listAdapter


        listView.setOnItemClickListener { adapterView, view, i, l ->
            Log.d("List", adapterView.getItemAtPosition(i).toString())
            val inputStream = activity!!.assets.open(preffix + "/" + adapterView.getItemAtPosition(i).toString())
            val file = File.createTempFile("model", "tflite")
            org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, file)
            val model = adapterView.getItemAtPosition(i).toString().split(".")[0]
            val labelsInput = activity!!.assets.open(preffix + "/" + adapterView.getItemAtPosition(i).toString().split(".")[0] + ".label")
            val br = BufferedReader(InputStreamReader(labelsInput))
            val labels: Vector<String> = Vector<String>()
            var line = br.readLine()
            while (line != null) {
                labels.add(line)
                line = br.readLine()
            }
            if (preffix == "Detection")
                activity!!.supportFragmentManager.beginTransaction().replace(
                    R.id.container,
                    ObjectDetectionFragment(
                        file,
                        labels,
                        model
                    )
                ).addToBackStack(null).commit()
            else
                activity!!.supportFragmentManager.beginTransaction().replace(
                    R.id.container,
                    ClassificationCameraFragment(
                        file,
                        labels,
                        model
                    )
                ).addToBackStack(null).commit()
        }
    }

    private fun createFileFromInputStream(inputStream: InputStream): File? {

        try {
            val f = File("new FilePath")
            val outputStream: OutputStream = FileOutputStream(f)
            val buffer = ByteArray(1024)
            var length = 0
            while (inputStream.read(buffer).also({ length = it }) > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.close()
            inputStream.close()
            return f
        } catch (e: IOException) {
            Log.d("List", "Error")
        }
        return null
    }

}