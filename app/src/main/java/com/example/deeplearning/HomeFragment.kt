package com.example.deeplearning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.home_fragment.*

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       return inflater.inflate(R.layout.home_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity!!.title = "WeaponDM"

        detection.setOnClickListener {
            val f = activity!!.assets.list("Detection")
            val files = Array<String>(f!!.size / 2) {""}
            var i = 0
            for (file in f) {
                if (file.endsWith("tflite")) {
                    files[i] = file
                    i++
                }
            }
            activity!!.supportFragmentManager.beginTransaction().replace(R.id.container, ModelListFragment(files, "Detection")).addToBackStack(null).commit()

        }

        classification.setOnClickListener {

            val f = activity!!.assets.list("Classification")
            val files = Array<String>(f!!.size / 2) {""}
            var i = 0
            for (file in f) {
                if (file.endsWith("tflite")) {
                    files[i] = file
                    i++
                }
            }
            activity!!.supportFragmentManager.beginTransaction().replace(R.id.container, ModelListFragment(files, "Classification")).addToBackStack(null).commit()

        }

    }
}