package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.intellecat.ephemera.*
import java.util.Calendar
import java.util.TimeZone

class MainActivity : Activity() {

    private lateinit var resultText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val title = TextView(this).apply {
            text = "Tamil Nature - Ephemera Test"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val button = Button(this).apply {
            text = "Calculate Planet Positions"
            setOnClickListener { calculatePositions() }
        }

        resultText = TextView(this).apply {
            textSize = 16f
            setPadding(0, 24, 0, 0)
            text = "Press the button to calculate."
        }

        layout.addView(title)
        layout.addView(button)
        layout.addView(resultText)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun calculatePositions() {
        try {
            val calendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            )

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1

            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            val fractionalDay =
                day + (hour + minute / 60.0 + second / 3600.0) / 24.0

            val jd = Ephemera.julianDate(
                year, month, fractionalDay
            )

            val sun = Ephemera.position(Body.SUN, jd)
            val moon = Ephemera.position(Body.MOON, jd)

            val tt = Ephemera.utToTt(jd)
            val lahiri = Ephemera.ayanamsa(
                Ayanamsa.LAHIRI, tt
            )

            resultText.text = """
                EPHEMERIS CALCULATION

                Date: $day-$month-$year
                Time: $hour:$minute:$second UTC

                Julian Day:
                $jd

                SUN POSITION:
                ${sun ?: "No data"}

                MOON POSITION:
                ${moon ?: "No data"}

                LAHIRI AYANAMSA:
                $lahiri

            """.trimIndent()

        } catch (e: Exception) {
            resultText.text =
                "Calculation Error:\n${e.message}"
        }
    }
}
