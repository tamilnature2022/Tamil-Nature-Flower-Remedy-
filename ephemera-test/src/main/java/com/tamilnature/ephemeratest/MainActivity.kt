
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
import java.util.Locale

class MainActivity : Activity() {

    private val rasiNames = arrayOf(
        "மேஷம்",
        "ரிஷபம்",
        "மிதுனம்",
        "கடகம்",
        "சிம்மம்",
        "கன்னி",
        "துலாம்",
        "விருச்சிகம்",
        "தனுசு",
        "மகரம்",
        "கும்பம்",
        "மீனம்"
    )

    private fun normalizeLongitude(value: Double): Double {
        return ((value % 360.0) + 360.0) % 360.0
    }

    private fun formatRasi(longitude: Double): String {
        val normalized = normalizeLongitude(longitude)

        val rasiIndex = (normalized / 30.0).toInt()
        val withinRasi = normalized % 30.0

        val totalSeconds = (withinRasi * 3600.0).toLong()

        val degrees = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(
            Locale.US,
            "%s  %02d° %02d′ %02d″",
            rasiNames[rasiIndex],
            degrees,
            minutes,
            seconds
        )
    }

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
                day + (hour * 3600.0 +
                minute * 60.0 + second) / 86400.0

            val jd = Ephemera.julianDate(
                year, month, fractionalDay
            )

            val sun = Ephemera.position(Body.SUN, jd)
                ?: throw Exception("Sun position unavailable")

            val moon = Ephemera.position(Body.MOON, jd)
                ?: throw Exception("Moon position unavailable")

            val tt = Ephemera.utToTt(jd)

            val lahiri = Ephemera.ayanamsa(
                Ayanamsa.LAHIRI, tt
            )

            // Tropical longitudes
            val sunTropical = sun.longitude
            val moonTropical = moon.longitude

            // Sidereal longitudes
            val sunSidereal = normalizeLongitude(
                sunTropical - lahiri
            )

            val moonSidereal = normalizeLongitude(
                moonTropical - lahiri
            )

            result.append("EPHEMERIS CALCULATION\n\n")

            result.append("Date (UTC): $year-$month-$day\n")
            result.append("Time (UTC): $hour:$minute:$second\n\n")

            result.append("JULIAN DAY\n")
            result.append("$jd\n\n")

            result.append("LAHIRI AYANAMSA\n")
            result.append("$lahiri\n\n")

            result.append("========================\n")
            result.append("SUN - சூரியன்\n")
            result.append("========================\n")

            result.append("Tropical Longitude:\n")
            result.append("$sunTropical°\n\n")

            result.append("Sidereal Longitude:\n")
            result.append("$sunSidereal°\n\n")

            result.append("D1 Rasi Position:\n")
            result.append(formatRasi(sunSidereal))
            result.append("\n\n")

            result.append("========================\n")
            result.append("MOON - சந்திரன்\n")
            result.append("========================\n")

            result.append("Tropical Longitude:\n")
            result.append("$moonTropical°\n\n")

            result.append("Sidereal Longitude:\n")
            result.append("$moonSidereal°\n\n")

            result.append("D1 Rasi Position:\n")
            result.append(formatRasi(moonSidereal))
            result.append("\n\n")

            result.append("========================\n")
            result.append("TIME CONVERSION\n")
            result.append("========================\n")

            result.append("Terrestrial Time:\n")
            result.append("$tt\n")

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
