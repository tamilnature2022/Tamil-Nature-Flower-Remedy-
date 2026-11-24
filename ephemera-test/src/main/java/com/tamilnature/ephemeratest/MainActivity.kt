
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

    private val nakshatraNames = arrayOf(
        "அசுவினி", "பரணி", "கார்த்திகை",
        "ரோகிணி", "மிருகசீரிடம்", "திருவாதிரை",
        "புனர்பூசம்", "பூசம்", "ஆயில்யம்",
        "மகம்", "பூரம்", "உத்திரம்",
        "அஸ்தம்", "சித்திரை", "சுவாதி",
        "விசாகம்", "அனுஷம்", "கேட்டை",
        "மூலம்", "பூராடம்", "உத்திராடம்",
        "திருவோணம்", "அவிட்டம்", "சதயம்",
        "பூரட்டாதி", "உத்திரட்டாதி", "ரேவதி"
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

    private fun getNakshatra(longitude: Double): String {
        val normalized = normalize(longitude)

        val nakshatraSize = 360.0 / 27.0
        val padaSize = nakshatraSize / 4.0

        val nakshatraIndex =
            (normalized / nakshatraSize).toInt()

        val withinNakshatra =
            normalized % nakshatraSize

        val padaIndex =
            (withinNakshatra / padaSize).toInt()

        return "${nakshatraNames[nakshatraIndex]} - பாதம் ${padaIndex + 1}"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology Test"
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

            val birthYear = 1984
            val birthMonth = 11
            val birthDay = 19
            val birthHour = 19
            val birthMinute = 58

            val latitude = 10.48013
            val longitude = 77.74977

            val localCalendar = Calendar.getInstance(
                TimeZone.getTimeZone("Asia/Kolkata")
            )

            localCalendar.isLenient = false
            localCalendar.set(
                birthYear,
                birthMonth - 1,
                birthDay,
                birthHour,
                birthMinute,
                0
            )
            localCalendar.set(Calendar.MILLISECOND, 0)

            val utcCalendar = Calendar.getInstance(
                TimeZone.getTimeZone("UTC")
            )

            utcCalendar.timeInMillis =
                localCalendar.timeInMillis

            val year = utcCalendar.get(Calendar.YEAR)
            val month = utcCalendar.get(Calendar.MONTH) + 1
            val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

            val hour = utcCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = utcCalendar.get(Calendar.MINUTE)
            val second = utcCalendar.get(Calendar.SECOND)

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

            val angles = Ephemera.ascMc(
                jd, longitude, latitude
            )

            val lagnaSidereal = normalize(
                angles.asc - lahiri
            )

            val mcSidereal = normalize(
                angles.mc - lahiri
            )

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")
            result.append("Date: $birthDay-$birthMonth-$birthYear\n")
            result.append("Birth Time: $birthHour:$birthMinute IST\n")
            result.append("UTC Time: $hour:$minute:$second\n")
            result.append("Latitude: $latitude\n")
            result.append("Longitude: $longitude\n\n")

            result.append("JULIAN DAY\n$jd\n\n")
            result.append("LAHIRI AYANAMSA\n$lahiri°\n\n")

            result.append("=============================\n")
            result.append("LAGNA - லக்னம்\n")
            result.append("=============================\n")

            result.append("Tropical Ascendant:\n")
            result.append("${formatDegree(angles.asc)}\n\n")

            result.append("Sidereal Ascendant:\n")
            result.append("${formatDegree(lagnaSidereal)}\n\n")

            result.append("லக்ன ராசி:\n")
            result.append("${formatRasi(lagnaSidereal)}\n")

            result.append(
                "லக்ன நட்சத்திரம்: ${getNakshatra(lagnaSidereal)}\n\n"
            )

            result.append("MC - மத்தியான புள்ளி:\n")
            result.append("${formatRasi(mcSidereal)}\n\n")

            result.append("=============================\n")
            result.append("9 GRAHA POSITIONS - D1\n")
            result.append("=============================\n\n")

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
                    "D1: ${formatRasi(sidereal)}\n"
                )
                result.append(
                    "நட்சத்திரம்: ${getNakshatra(sidereal)}\n"
                )
                result.append("நிலை: $motion\n")
                result.append("--------------------------------\n\n")
            }

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
                "D1: ${formatRasi(rahuSidereal)}\n"
            )
            result.append(
                "நட்சத்திரம்: ${getNakshatra(rahuSidereal)}\n\n"
            )

            result.append("கேது\n")
            result.append(
                "Sidereal: ${formatDegree(ketuSidereal)}\n"
            )
            result.append(
                "D1: ${formatRasi(ketuSidereal)}\n"
            )
            result.append(
                "நட்சத்திரம்: ${getNakshatra(ketuSidereal)}\n"
            )

            result.append("\n=============================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Nakshatra: 27\n")
            result.append("Pada: 108\n")

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
