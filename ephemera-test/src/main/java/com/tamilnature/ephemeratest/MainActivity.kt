package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
        }

        val title = TextView(this).apply {
            text = "Tamil Nature"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "Ephemera Test App\n\nHome Screen Working!"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 0)
        }

        layout.addView(title)
        layout.addView(message)

        setContentView(layout)
    }
}
