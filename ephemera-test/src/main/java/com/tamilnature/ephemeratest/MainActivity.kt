
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

    private val chartNames = listOf(
        "D1", "D2", "D3", "D4",
        "D7", "D9", "D10", "D12",
        "D16", "D20", "D24", "D27",
        "D30", "D40", "D45", "D60"
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
            (normalized / nakshatraSize).toInt().coerceIn(0, 26)

        val withinNakshatra = normalized % nakshatraSize
        val padaIndex =
            (withinNakshatra / padaSize).toInt().coerceIn(0, 3)

        return "${nakshatraNames[nakshatraIndex]} - பாதம் ${padaIndex + 1}"
    }

    private fun makeVarga(
        longitude: Double,
        divisions: Int,
        startingSign: Int
    ): VargaPosition {

        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0
        val size = 30.0 / divisions

        val division = (degreeInSign / size)
            .toInt()
            .coerceIn(0, divisions - 1)

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * divisions

        val targetSign = (startingSign + division) % 12
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D2 - HORA
    // Odd signs: first half Leo, second half Cancer
    // Even signs: first half Cancer, second half Leo

    private fun calculateHora(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degree = normalized % 30.0

        val firstHalf = degree < 15.0

        val targetSign = if (signIndex % 2 == 0) {
            if (firstHalf) 4 else 3
        } else {
            if (firstHalf) 3 else 4
        }

        val remaining = degree % 15.0
        val targetDegree = remaining * 2.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D3 - DREKKANA
    // Same sign, 5th sign, 9th sign

    private fun calculateDrekkana(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign = signIndex
        return makeVarga(
            normalized,
            3,
            startingSign
        ).let {
            val degree = normalized % 30.0
            val division = (degree / 10.0).toInt().coerceIn(0, 2)
            val targetSign = (signIndex + division * 4) % 12
            val remaining = degree - division * 10.0
            val targetDegree = remaining * 3.0
            val targetLongitude = targetSign * 30.0 + targetDegree

            VargaPosition(
                normalize(targetLongitude),
                rasiNames[targetSign],
                formatRasi(targetLongitude)
            )
        }
    }

    // D4 - CHATURTHAMSA
    // Same sign, 4th, 7th, 10th

    private fun calculateChaturthamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degree = normalized % 30.0

        val division = (degree / 7.5).toInt().coerceIn(0, 3)
        val targetSign = (signIndex + division * 3) % 12

        val remaining = degree - division * 7.5
        val targetDegree = remaining * 4.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D7 - SAPTAMSA

    private fun calculateSaptamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val startingSign =
            if (signIndex % 2 == 0) signIndex else (signIndex + 6) % 12

        return makeVarga(normalized, 7, startingSign)
    }

    // D9 - NAVAMSA

    private fun calculateNavamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val offset = when (signIndex % 3) {
            0 -> 0
            1 -> 8
            else -> 4
        }

        return makeVarga(
            normalized,
            9,
            (signIndex + offset) % 12
        )
    }

    // D10 - DASAMSA

    private fun calculateDasamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign =
            if (signIndex % 2 == 0) signIndex else (signIndex + 8) % 12

        return makeVarga(normalized, 10, startingSign)
    }

    // D12 - DWADASAMSA

    private fun calculateDwadasamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        return makeVarga(normalized, 12, signIndex)
    }

    // D16 - SHODASHAMSA

    private fun calculateShodashamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val offset = when (signIndex % 3) {
            0 -> 0
            1 -> 4
            else -> 8
        }

        return makeVarga(
            normalized,
            16,
            offset
        )
    }

    // D20 - VIMSHAMSA

    private fun calculateVimshamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign = when (signIndex % 3) {
            0 -> 0
            1 -> 8
            else -> 4
        }

        return makeVarga(normalized, 20, startingSign)
    }

    // D24 - CHATURVIMSHAMSA

    private fun calculateChaturvimshamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign =
            if (signIndex % 2 == 0) 4 else 3

        return makeVarga(normalized, 24, startingSign)
    }

    // D27 - BHAMSHA

    private fun calculateBhamsha(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign = when (signIndex % 3) {
            0 -> 0
            1 -> 3
            else -> 6
        }

        return makeVarga(normalized, 27, startingSign)
    }

    // D30 - TRIMSHAMSA

    private fun calculateTrimshamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degree = normalized % 30.0

        data class Division(
            val upperLimit: Double,
            val targetSign: Int,
            val segmentSize: Double
        )

        val divisions = if (signIndex % 2 == 0) {
            listOf(
                Division(5.0, 0, 5.0),
                Division(10.0, 10, 5.0),
                Division(18.0, 8, 8.0),
                Division(25.0, 2, 7.0),
                Division(30.0, 6, 5.0)
            )
        } else {
            listOf(
                Division(5.0, 1, 5.0),
                Division(12.0, 5, 7.0),
                Division(20.0, 11, 8.0),
                Division(25.0, 9, 5.0),
                Division(30.0, 7, 5.0)
            )
        }

        var lowerLimit = 0.0

        for (division in divisions) {
            if (degree < division.upperLimit) {
                val remaining = degree - lowerLimit
                val targetDegree =
                    remaining * (30.0 / division.segmentSize)

                val targetLongitude =
                    division.targetSign * 30.0 + targetDegree

                return VargaPosition(
                    normalize(targetLongitude),
                    rasiNames[division.targetSign],
                    formatRasi(targetLongitude)
                )
            }
            lowerLimit = division.upperLimit
        }

        return VargaPosition(
            normalized,
            rasiNames[signIndex],
            formatRasi(normalized)
        )
    }

    // D40 - KHAVEDAMSA

    private fun calculateKhavedamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign =
            if (signIndex % 2 == 0) 0 else 6

        return makeVarga(normalized, 40, startingSign)
    }

    // D45 - AKSHAVEDAMSA

    private fun calculateAkshavedamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign = when (signIndex % 3) {
            0 -> 0
            1 -> 4
            else -> 8
        }

        return makeVarga(normalized, 45, startingSign)
    }

    // D60 - SHASHTIAMSA

    private fun calculateShashtiamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()

        val startingSign =
            if (signIndex % 2 == 0) 0 else 6

        return makeVarga(normalized, 60, startingSign)
    }

    private fun calculateAll(longitude: Double): Map<String, VargaPosition> {
        return linkedMapOf(
            "D1" to VargaPosition(
                normalize(longitude),
                rasiNames[(normalize(longitude) / 30.0).toInt()],
                formatRasi(longitude)
            ),
            "D2" to calculateHora(longitude),
            "D3" to calculateDrekkana(longitude),
            "D4" to calculateChaturthamsa(longitude),
            "D7" to calculateSaptamsa(longitude),
            "D9" to calculateNavamsa(longitude),
            "D10" to calculateDasamsa(longitude),
            "D12" to calculateDwadasamsa(longitude),
            "D16" to calculateShodashamsa(longitude),
            "D20" to calculateVimshamsa(longitude),
            "D24" to calculateChaturvimshamsa(longitude),
            "D27" to calculateBhamsha(longitude),
            "D30" to calculateTrimshamsa(longitude),
            "D40" to calculateKhavedamsa(longitude),
            "D45" to calculateAkshavedamsa(longitude),
            "D60" to calculateShashtiamsa(longitude)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature Astrology - Shodashavarga 16 Charts"
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

            val latitude = 10.36896
            val longitude = 77.98036

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
            utcCalendar.timeInMillis = localCalendar.timeInMillis

            val year = utcCalendar.get(Calendar.YEAR)
            val month = utcCalendar.get(Calendar.MONTH) + 1
            val day = utcCalendar.get(Calendar.DAY_OF_MONTH)
            val hour = utcCalendar.get(Calendar.HOUR_OF_DAY)
            val minute = utcCalendar.get(Calendar.MINUTE)
            val second = utcCalendar.get(Calendar.SECOND)

            val fractionalDay =
                day + (hour * 3600.0 + minute * 60.0 + second) / 86400.0

            val jd = Ephemera.julianDate(year, month, fractionalDay)
            val tt = Ephemera.utToTt(jd)
            val lahiri = Ephemera.ayanamsa(Ayanamsa.LAHIRI, tt)

            val angles = Ephemera.ascMc(jd, longitude, latitude)

            val lagnaSidereal = normalize(angles.asc - lahiri)
            val mcSidereal = normalize(angles.mc - lahiri)

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")
            result.append("Date: $birthDay-$birthMonth-$birthYear\n")
            result.append("Birth Time: $birthHour:$birthMinute IST\n")
            result.append("Place: Dindigul Center\n")
            result.append("UTC Time: $hour:$minute:$second\n")
            result.append("Latitude: $latitude\n")
            result.append("Longitude: $longitude\n\n")

            result.append("=============================\n")
            result.append("AYANAMSA DIAGNOSTIC\n")
            result.append("=============================\n")
            result.append("JD UT: $jd\n")
            result.append("JD TT: $tt\n")
            result.append(
                "Lahiri Decimal: ${
                    String.format(Locale.US, "%.10f", lahiri)
                }°\n\n"
            )

            result.append("=============================\n")
            result.append("LAGNA - லக்னம்\n")
            result.append("=============================\n")

            val lagnaCharts = calculateAll(lagnaSidereal)

            for ((chart, position) in lagnaCharts) {
                result.append("$chart: ${position.degree}\n")
            }

            result.append("நட்சத்திரம்: ${getNakshatra(lagnaSidereal)}\n")
            result.append("MC: ${formatRasi(mcSidereal)}\n\n")

            result.append("=============================\n")
            result.append("PLANETARY POSITIONS - 16 CHARTS\n")
            result.append("=============================\n\n")

            for (graha in grahas) {
                val position = Ephemera.position(graha.body, jd)
                    ?: throw Exception("${graha.name} position unavailable")

                val sidereal = normalize(position.longitude - lahiri)
                val charts = calculateAll(sidereal)

                val motion = if (position.isRetrograde) {
                    "வக்கிரம் (வ)"
                } else {
                    "நேர்கதி"
                }

                result.append("${graha.name}\n")

                for ((chart, varga) in charts) {
                    result.append("$chart: ${varga.degree}\n")
                }

                result.append("நட்சத்திரம்: ${getNakshatra(sidereal)}\n")
                result.append("நிலை: $motion\n")
                result.append("--------------------------------\n\n")
            }

            // RAHU AND KETU - MEAN NODE

            val rahuPosition = Ephemera.position(Body.MEAN_NODE, jd)
                ?: throw Exception("Mean Node unavailable")

            val rahuSidereal = normalize(rahuPosition.longitude - lahiri)
            val ketuSidereal = normalize(rahuSidereal + 180.0)

            val rahuCharts = calculateAll(rahuSidereal)
            val ketuCharts = calculateAll(ketuSidereal)

            result.append("ராகு - MEAN NODE\n")

            for ((chart, varga) in rahuCharts) {
                result.append("$chart: ${varga.degree}\n")
            }

            result.append("நட்சத்திரம்: ${getNakshatra(rahuSidereal)}\n\n")

            result.append("கேது\n")

            for ((chart, varga) in ketuCharts) {
                result.append("$chart: ${varga.degree}\n")
            }

            result.append("நட்சத்திரம்: ${getNakshatra(ketuSidereal)}\n")

            result.append("\n=============================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Charts: 16 Shodashavarga\n")
            result.append(
                "D1 + D2 + D3 + D4 + D7 + D9 + D10 + D12 + " +
                "D16 + D20 + D24 + D27 + D30 + D40 + D45 + D60\n"
            )
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
