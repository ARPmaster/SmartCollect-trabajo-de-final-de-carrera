// Activity vacía usada solo como anfitriona de los Fragments en las pruebas de instrumentación, no se usa en la app real.
package com.example.aicollect

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HiltTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = CONTAINER_ID })
    }

    companion object {
        val CONTAINER_ID: Int = View.generateViewId()
    }
}
