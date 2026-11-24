
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
        "மேஷம்", "ரிஷபம்", "மிதுனம்", "கடகம்",
        "சிம்மம்", "கன்னி", "துலாம்", "விருச்சிகம்",
        "தனுசு", "மகரம்", "கும்பம்", "மீனம்"
    )

    private data class Graha(
        val name: String,
        val body: Body
    )

    private val grahas = listOf(
        Graha("சூரியன்", Body.SUN),
        Graha("சந்திரன்", Body.MOON),
        Graha("செவ்வாய்", Body.MARS),
        Graha("புதன்", Body.MERCURY),
        Graha("குரு", Body.JUPITER),
        Graha("சுக்கிரன்", Body.VENUS),
        Graha("சனி", Body.SATURN)
    )

    private fun normalize(value: Double): Double {
        return ((value % 360.0) + 360.0) % 360.0
    }

    private fun formatRasi(longitude: Double): String {
        val normalized = normalize(longitude)
        val index = (normalized / 30.0).toInt()
        val withinRasi = normalized % 30.0

        val totalSeconds = (withinRasi * 3600.0).toInt()
        val degrees = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(
            Locale.US,
            "%s  %02d° %02d′ %02d″",
            rasiNames[index],
            degrees,
            minutes,
            seconds
        )
    }

    private fun formatDegree(value: Double): String {
        return String.format(Locale.US, "%.8f°", value)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature - 9 Graha Test"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(16, 24, 16, 20)
        }

        val output = TextView(this).apply {
            textSize = 17f
            setPadding(24, 24, 24, 24)
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

            val tt = Ephemera.utToTt(jd)

            val lahiri = Ephemera.ayanamsa(
                Ayanamsa.LAHIRI, tt
            )

            result.append("9 GRAHA SIDEREAL CALCULATION\n\n")
            result.append("Date (UTC): $year-$month-$day\n")
            result.append("Time (UTC): $hour:$minute:$second\n")
            result.append("Julian Day: $jd\n")
            result.append("Lahiri Ayanamsa: $lahiri°\n\n")

            result.append("================================\n")
            result.append("GRAHA POSITIONS - D1\n")
            result.append("================================\n\n")

            for (graha in grahas) {

                val position = Ephemera.position(
                    graha.body, jd
                ) ?: throw Exception(
                    "${graha.name} position unavailable"
                )

                val sidereal = normalize(
                    position.longitude - lahiri
                )

                val motion = if (position.isRetrograde) {
                    "வக்கிரம் (வ)"
                } else {
                    "நேர்கதி"
                }

                result.append("${graha.name}\n")
                result.append(
                    "Sidereal: ${formatDegree(sidereal)}\n"
                )
                result.append(
                    "D1 Position: ${formatRasi(sidereal)}\n"
                )
                result.append("நிலை: $motion\n")
                result.append(
                    "Longitude Speed: ${position.longitudeSpeed}\n"
                )
                result.append("--------------------------------\n\n")
            }

            // Mean Rahu and opposite Ketu
            val rahuPosition = Ephemera.position(
                Body.MEAN_NODE, jd
            ) ?: throw Exception("Mean Node unavailable")

            val rahuSidereal = normalize(
                rahuPosition.longitude - lahiri
            )

            val ketuSidereal = normalize(
                rahuSidereal + 180.0
            )

            result.append("ராகு - MEAN NODE\n")
            result.append(
                "Sidereal: ${formatDegree(rahuSidereal)}\n"
            )
            result.append(
                "D1 Position: ${formatRasi(rahuSidereal)}\n"
            )
            result.append("--------------------------------\n\n")

            result.append("கேது - OPPOSITE OF RAHU\n")
            result.append(
                "Sidereal: ${formatDegree(ketuSidereal)}\n"
            )
            result.append(
                "D1 Position: ${formatRasi(ketuSidereal)}\n"
            )

            result.append("\n================================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node Method: Mean Node\n")

        } catch (e: Exception) {
            result.append("\nCALCULATION ERROR\n\n")
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
