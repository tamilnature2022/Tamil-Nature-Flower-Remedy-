
package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.intellecat.ephemera.*
import java.util.Calendar
import java.util.TimeZone

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val output = TextView(this).apply {
            textSize = 17f
            setPadding(24, 24, 24, 24)
        }

        val title = TextView(this).apply {
            text = "Tamil Nature - Ephemera Test"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 20)
        }

        val result = StringBuilder()

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
                day + (hour * 3600.0 + minute * 60.0 + second) / 86400.0

            val jd = Ephemera.julianDate(
                year, month, fractionalDay
            )

            val sun = Ephemera.position(Body.SUN, jd)
            val moon = Ephemera.position(Body.MOON, jd)

            val tt = Ephemera.utToTt(jd)
            val lahiri = Ephemera.ayanamsa(
                Ayanamsa.LAHIRI, tt
            )

            result.append("EPHEMERIS CALCULATION\n\n")
            result.append("Date (UTC): $year-$month-$day\n")
            result.append("Time (UTC): $hour:$minute:$second\n\n")
            result.append("Julian Day:\n$jd\n\n")
            result.append("SUN POSITION:\n$sun\n\n")
            result.append("MOON POSITION:\n$moon\n\n")
            result.append("LAHIRI AYANAMSA:\n$lahiri\n\n")
            result.append("Terrestrial Time:\n$tt\n")

        } catch (e: Exception) {
            result.append("CALCULATION ERROR\n\n")
            result.append(e.toString())
        }

        output.text = result.toString()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(output)
        }

        val scroll = ScrollView(this)
        scroll.addView(layout)
        setContentView(scroll)
    }
}
