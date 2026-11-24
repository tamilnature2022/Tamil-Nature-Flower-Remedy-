
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

    private data class Navamsa(
        val longitude: Double,
        val rasi: String,
        val degree: String
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

    // D9 NAVAMSA CALCULATION

    private fun calculateNavamsa(longitude: Double): Navamsa {

        val normalized = normalize(longitude)

        val signIndex = (normalized / 30.0).toInt()

        val degreeInSign = normalized % 30.0

        val navamsaSize = 30.0 / 9.0

        val navamsaIndex =
            (degreeInSign / navamsaSize).toInt()
                .coerceIn(0, 8)

        // Movable, Fixed and Dual sign starting rules
        val startingOffset = when (signIndex % 3) {
            0 -> 0  // Movable
            1 -> 8  // Fixed
            else -> 4 // Dual
        }

        val navamsaSignIndex =
            (signIndex + startingOffset + navamsaIndex) % 12

        val degreeWithinNavamsa =
            degreeInSign - (navamsaIndex * navamsaSize)

        val d9Degree =
            degreeWithinNavamsa * 9.0

        val d9Longitude =
            navamsaSignIndex * 30.0 + d9Degree

        return Navamsa(
            longitude = normalize(d9Longitude),
            rasi = rasiNames[navamsaSignIndex],
            degree = formatRasi(d9Longitude)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology Test D1 + D9"
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

            // TEST BIRTH DETAILS

            val birthYear = 1984
            val birthMonth = 11
            val birthDay = 19
            val birthHour = 19
            val birthMinute = 58

            val latitude = 10.48013
            val longitude = 77.74977

            // IST TO UTC

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
                day + (
                    hour * 3600.0 +
                    minute * 60.0 +
                    second
                ) / 86400.0

            val jd = Ephemera.julianDate(
                year, month, fractionalDay
            )

            val tt = Ephemera.utToTt(jd)

            val lahiri = Ephemera.ayanamsa(
                Ayanamsa.LAHIRI, tt
            )

            // ASCENDANT

            val angles = Ephemera.ascMc(
                jd, longitude, latitude
            )

            val lagnaSidereal = normalize(
                angles.asc - lahiri
            )

            val mcSidereal = normalize(
                angles.mc - lahiri
            )

            val lagnaD9 = calculateNavamsa(lagnaSidereal)

            // BIRTH DETAILS

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")
            result.append("Date: $birthDay-$birthMonth-$birthYear\n")
            result.append("Birth Time: $birthHour:$birthMinute IST\n")
            result.append("UTC Time: $hour:$minute:$second\n")
            result.append("Latitude: $latitude\n")
            result.append("Longitude: $longitude\n\n")

            result.append("JULIAN DAY\n$jd\n\n")
            result.append("LAHIRI AYANAMSA\n$lahiri°\n\n")

            // LAGNA D1 + D9

            result.append("=============================\n")
            result.append("LAGNA - லக்னம்\n")
            result.append("=============================\n\n")

            result.append(
                "Tropical Ascendant:\n${formatDegree(angles.asc)}\n\n"
            )

            result.append(
                "D1 Lagna:\n${formatRasi(lagnaSidereal)}\n"
            )

            result.append(
                "Lagna Nakshatra: ${getNakshatra(lagnaSidereal)}\n\n"
            )

            result.append(
                "D9 Navamsa Lagna:\n${lagnaD9.degree}\n\n"
            )

            result.append(
                "MC:\n${formatRasi(mcSidereal)}\n\n"
            )

            // PLANETS D1 + D9

            result.append("=============================\n")
            result.append("PLANETARY POSITIONS D1 + D9\n")
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

                val d9 = calculateNavamsa(sidereal)

                val motion = if (position.isRetrograde) {
                    "வக்கிரம் (வ)"
                } else {
                    "நேர்கதி"
                }

                result.append("${graha.name}\n")

                result.append(
                    "D1: ${formatRasi(sidereal)}\n"
                )

                result.append(
                    "D9: ${d9.degree}\n"
                )

                result.append(
                    "நட்சத்திரம்: ${getNakshatra(sidereal)}\n"
                )

                result.append(
                    "நிலை: $motion\n"
                )

                result.append("--------------------------------\n\n")
            }

            // RAHU

            val rahuPosition = Ephemera.position(
                Body.MEAN_NODE, jd
            ) ?: throw Exception("Mean Node unavailable")

            val rahuSidereal = normalize(
                rahuPosition.longitude - lahiri
            )

            val ketuSidereal = normalize(
                rahuSidereal + 180.0
            )

            val rahuD9 = calculateNavamsa(rahuSidereal)
            val ketuD9 = calculateNavamsa(ketuSidereal)

            result.append("ராகு - MEAN NODE\n")

            result.append(
                "D1: ${formatRasi(rahuSidereal)}\n"
            )

            result.append(
                "D9: ${rahuD9.degree}\n"
            )

            result.append(
                "நட்சத்திரம்: ${getNakshatra(rahuSidereal)}\n\n"
            )

            // KETU

            result.append("கேது\n")

            result.append(
                "D1: ${formatRasi(ketuSidereal)}\n"
            )

            result.append(
                "D9: ${ketuD9.degree}\n"
            )

            result.append(
                "நட்சத்திரம்: ${getNakshatra(ketuSidereal)}\n"
            )

            result.append("\n=============================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Charts: D1 + D9\n")
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
