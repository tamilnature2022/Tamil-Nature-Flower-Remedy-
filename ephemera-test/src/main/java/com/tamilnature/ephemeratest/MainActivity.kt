
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

    private data class VargaPosition(
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

    // D9 NAVAMSA

    private fun calculateNavamsa(
        longitude: Double
    ): VargaPosition {

        val normalized = normalize(longitude)

        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val navamsaSize = 30.0 / 9.0

        val navamsaIndex =
            (degreeInSign / navamsaSize).toInt()
                .coerceIn(0, 8)

        val startingOffset = when (signIndex % 3) {
            0 -> 0
            1 -> 8
            else -> 4
        }

        val navamsaSignIndex =
            (signIndex + startingOffset + navamsaIndex) % 12

        val degreeWithinNavamsa =
            degreeInSign - (navamsaIndex * navamsaSize)

        val d9Degree = degreeWithinNavamsa * 9.0

        val d9Longitude =
            navamsaSignIndex * 30.0 + d9Degree

        return VargaPosition(
            longitude = normalize(d9Longitude),
            rasi = rasiNames[navamsaSignIndex],
            degree = formatRasi(d9Longitude)
        )
    }

    // D10 DASAMSA

    private fun calculateDasamsa(
        longitude: Double
    ): VargaPosition {

        val normalized = normalize(longitude)

        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val dasamsaSize = 3.0

        val dasamsaIndex =
            (degreeInSign / dasamsaSize).toInt()
                .coerceIn(0, 9)

        val startingOffset = if (signIndex % 2 == 0) {
            0
        } else {
            8
        }

        val dasamsaSignIndex =
            (signIndex + startingOffset + dasamsaIndex) % 12

        val degreeWithinDasamsa =
            degreeInSign - (dasamsaIndex * dasamsaSize)

        val d10Degree =
            degreeWithinDasamsa * 10.0

        val d10Longitude =
            dasamsaSignIndex * 30.0 + d10Degree

        return VargaPosition(
            longitude = normalize(d10Longitude),
            rasi = rasiNames[dasamsaSignIndex],
            degree = formatRasi(d10Longitude)
        )
    }

    // D7 SAPTAMSA

    private fun calculateSaptamsa(
        longitude: Double
    ): VargaPosition {

        val normalized = normalize(longitude)

        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val saptamsaSize = 30.0 / 7.0

        val saptamsaIndex =
            (degreeInSign / saptamsaSize).toInt()
                .coerceIn(0, 6)

        val startingOffset = if (signIndex % 2 == 0) {
            0
        } else {
            6
        }

        val saptamsaSignIndex =
            (signIndex + startingOffset + saptamsaIndex) % 12

        val degreeWithinSaptamsa =
            degreeInSign - (saptamsaIndex * saptamsaSize)

        val d7Degree =
            degreeWithinSaptamsa * 7.0

        val d7Longitude =
            saptamsaSignIndex * 30.0 + d7Degree

        return VargaPosition(
            longitude = normalize(d7Longitude),
            rasi = rasiNames[saptamsaSignIndex],
            degree = formatRasi(d7Longitude)
        )
    }

    // D12 DWADASAMSA

    private fun calculateDwadasamsa(
        longitude: Double
    ): VargaPosition {

        val normalized = normalize(longitude)

        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val dwadasamsaSize = 30.0 / 12.0

        val dwadasamsaIndex =
            (degreeInSign / dwadasamsaSize).toInt()
                .coerceIn(0, 11)

        // All signs start from the same sign

        val dwadasamsaSignIndex =
            (signIndex + dwadasamsaIndex) % 12

        val degreeWithinDwadasamsa =
            degreeInSign -
            (dwadasamsaIndex * dwadasamsaSize)

        val d12Degree =
            degreeWithinDwadasamsa * 12.0

        val d12Longitude =
            dwadasamsaSignIndex * 30.0 + d12Degree

        return VargaPosition(
            longitude = normalize(d12Longitude),
            rasi = rasiNames[dwadasamsaSignIndex],
            degree = formatRasi(d12Longitude)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology D1 + D9 + D10 + D7 + D12"
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

            // DINDIGUL CENTER COORDINATES

            val latitude = 10.36896
            val longitude = 77.98036

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

            val lagnaD9 =
                calculateNavamsa(lagnaSidereal)

            val lagnaD10 =
                calculateDasamsa(lagnaSidereal)

            val lagnaD7 =
                calculateSaptamsa(lagnaSidereal)

            val lagnaD12 =
                calculateDwadasamsa(lagnaSidereal)

            // BIRTH DETAILS

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")
            result.append("Date: $birthDay-$birthMonth-$birthYear\n")
            result.append("Birth Time: $birthHour:$birthMinute IST\n")
            result.append("Place: Dindigul Center\n")
            result.append("UTC Time: $hour:$minute:$second\n")
            result.append("Latitude: $latitude\n")
            result.append("Longitude: $longitude\n\n")

            // AYANAMSA DIAGNOSTIC

            result.append("=============================\n")
            result.append("AYANAMSA DIAGNOSTIC\n")
            result.append("=============================\n\n")

            result.append("JD UT: $jd\n")
            result.append("JD TT: $tt\n")

            result.append(
                "Lahiri Decimal: ${
                    String.format(Locale.US, "%.10f", lahiri)
                }°\n"
            )

            val totalArcSeconds = lahiri * 3600.0

            val deg = totalArcSeconds.toInt() / 3600
            val min = (totalArcSeconds.toInt() % 3600) / 60
            val sec = totalArcSeconds % 60

            result.append(
                "Lahiri DMS: $deg° $min′ ${
                    String.format(Locale.US, "%.4f", sec)
                }″\n\n"
            )

            // LAGNA D1 + D9 + D10 + D7 + D12

            result.append("=============================\n")
            result.append("LAGNA - லக்னம்\n")
            result.append("=============================\n\n")

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
                "D10 Dasamsa Lagna:\n${lagnaD10.degree}\n\n"
            )

            result.append(
                "D7 Saptamsa Lagna:\n${lagnaD7.degree}\n\n"
            )

            result.append(
                "D12 Dwadasamsa Lagna:\n${lagnaD12.degree}\n\n"
            )

            result.append(
                "MC:\n${formatRasi(mcSidereal)}\n\n"
            )

            // PLANETS D1 + D9 + D10 + D7 + D12

            result.append("=============================\n")
            result.append("PLANETARY POSITIONS D1 + D9 + D10 + D7 + D12\n")
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
                val d10 = calculateDasamsa(sidereal)
                val d7 = calculateSaptamsa(sidereal)
                val d12 = calculateDwadasamsa(sidereal)

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
                    "D10: ${d10.degree}\n"
                )

                result.append(
                    "D7: ${d7.degree}\n"
                )

                result.append(
                    "D12: ${d12.degree}\n"
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

            val rahuD9 =
                calculateNavamsa(rahuSidereal)

            val ketuD9 =
                calculateNavamsa(ketuSidereal)

            val rahuD10 =
                calculateDasamsa(rahuSidereal)

            val ketuD10 =
                calculateDasamsa(ketuSidereal)

            val rahuD7 =
                calculateSaptamsa(rahuSidereal)

            val ketuD7 =
                calculateSaptamsa(ketuSidereal)

            val rahuD12 =
                calculateDwadasamsa(rahuSidereal)

            val ketuD12 =
                calculateDwadasamsa(ketuSidereal)

            result.append("ராகு - MEAN NODE\n")

            result.append(
                "D1: ${formatRasi(rahuSidereal)}\n"
            )

            result.append(
                "D9: ${rahuD9.degree}\n"
            )

            result.append(
                "D10: ${rahuD10.degree}\n"
            )

            result.append(
                "D7: ${rahuD7.degree}\n"
            )

            result.append(
                "D12: ${rahuD12.degree}\n"
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
                "D10: ${ketuD10.degree}\n"
            )

            result.append(
                "D7: ${ketuD7.degree}\n"
            )

            result.append(
                "D12: ${ketuD12.degree}\n"
            )

            result.append(
                "நட்சத்திரம்: ${getNakshatra(ketuSidereal)}\n"
            )

            result.append("\n=============================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Charts: D1 + D9 + D10 + D7 + D12\n")
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
