
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

    private fun calculateNavamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 9.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 8)

        val offset = when (signIndex % 3) {
            0 -> 0
            1 -> 8
            else -> 4
        }

        val targetSign =
            (signIndex + offset + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 9.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D10 DASAMSA

    private fun calculateDasamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 3.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 9)

        val offset = if (signIndex % 2 == 0) 0 else 8
        val targetSign = (signIndex + offset + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 10.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D7 SAPTAMSA

    private fun calculateSaptamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 7.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 6)

        val offset = if (signIndex % 2 == 0) 0 else 6
        val targetSign = (signIndex + offset + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 7.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D12 DWADASAMSA

    private fun calculateDwadasamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 12.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 11)

        val targetSign = (signIndex + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 12.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D16 SHODASHAMSA

    private fun calculateShodashamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 16.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 15)

        val offset = when (signIndex % 3) {
            0 -> 0
            1 -> 4
            else -> 8
        }

        val targetSign = (offset + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 16.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D20 VIMSHAMSA

    private fun calculateVimshamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 20.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 19)

        val startingSign = when (signIndex % 3) {
            0 -> 0
            1 -> 8
            else -> 4
        }

        val targetSign = (startingSign + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 20.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D24 CHATURVIMSHAMSA

    private fun calculateChaturvimshamsa(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 24.0
        val division =
            (degreeInSign / size).toInt().coerceIn(0, 23)

        val startingSign = if (signIndex % 2 == 0) 4 else 3

        val targetSign = (startingSign + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 24.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    // D27 BHAMSHA / NAKSHATRAMSA

    private fun calculateBhamsha(longitude: Double): VargaPosition {
        val normalized = normalize(longitude)
        val signIndex = (normalized / 30.0).toInt()
        val degreeInSign = normalized % 30.0

        val size = 30.0 / 27.0

        val division =
            (degreeInSign / size).toInt().coerceIn(0, 26)

        // Movable: Aries
        // Fixed: Cancer
        // Dual: Libra

        val startingSign = when (signIndex % 3) {
            0 -> 0
            1 -> 3
            else -> 6
        }

        val targetSign =
            (startingSign + division) % 12

        val remaining = degreeInSign - division * size
        val targetDegree = remaining * 27.0
        val targetLongitude = targetSign * 30.0 + targetDegree

        return VargaPosition(
            normalize(targetLongitude),
            rasiNames[targetSign],
            formatRasi(targetLongitude)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology D1 + D9 + D10 + D7 + D12 + D16 + D20 + D24 + D27"
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

            // BIRTH DETAILS

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

            utcCalendar.timeInMillis = localCalendar.timeInMillis

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

            // LAGNA VARGAS

            val lagnaD9 = calculateNavamsa(lagnaSidereal)
            val lagnaD10 = calculateDasamsa(lagnaSidereal)
            val lagnaD7 = calculateSaptamsa(lagnaSidereal)
            val lagnaD12 = calculateDwadasamsa(lagnaSidereal)
            val lagnaD16 = calculateShodashamsa(lagnaSidereal)
            val lagnaD20 = calculateVimshamsa(lagnaSidereal)
            val lagnaD24 = calculateChaturvimshamsa(lagnaSidereal)
            val lagnaD27 = calculateBhamsha(lagnaSidereal)

            // BIRTH DETAILS OUTPUT

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

            // LAGNA OUTPUT

            result.append("=============================\n")
            result.append("LAGNA - லக்னம்\n")
            result.append("=============================\n\n")

            result.append("D1 Lagna:\n${formatRasi(lagnaSidereal)}\n")
            result.append("Lagna Nakshatra: ${getNakshatra(lagnaSidereal)}\n\n")
            result.append("D9 Navamsa Lagna:\n${lagnaD9.degree}\n\n")
            result.append("D10 Dasamsa Lagna:\n${lagnaD10.degree}\n\n")
            result.append("D7 Saptamsa Lagna:\n${lagnaD7.degree}\n\n")
            result.append("D12 Dwadasamsa Lagna:\n${lagnaD12.degree}\n\n")
            result.append("D16 Shodashamsa Lagna:\n${lagnaD16.degree}\n\n")
            result.append("D20 Vimshamsa Lagna:\n${lagnaD20.degree}\n\n")
            result.append("D24 Chaturvimshamsa Lagna:\n${lagnaD24.degree}\n\n")
            result.append("D27 Bhamsha Lagna:\n${lagnaD27.degree}\n\n")
            result.append("MC:\n${formatRasi(mcSidereal)}\n\n")

            // PLANETARY POSITIONS

            result.append("=============================\n")
            result.append("PLANETARY POSITIONS D1 + D9 + D10 + D7 + D12 + D16 + D20 + D24 + D27\n")
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
                val d16 = calculateShodashamsa(sidereal)
                val d20 = calculateVimshamsa(sidereal)
                val d24 = calculateChaturvimshamsa(sidereal)
                val d27 = calculateBhamsha(sidereal)

                val motion = if (position.isRetrograde) {
                    "வக்கிரம் (வ)"
                } else {
                    "நேர்கதி"
                }

                result.append("${graha.name}\n")
                result.append("D1: ${formatRasi(sidereal)}\n")
                result.append("D9: ${d9.degree}\n")
                result.append("D10: ${d10.degree}\n")
                result.append("D7: ${d7.degree}\n")
                result.append("D12: ${d12.degree}\n")
                result.append("D16: ${d16.degree}\n")
                result.append("D20: ${d20.degree}\n")
                result.append("D24: ${d24.degree}\n")
                result.append("D27: ${d27.degree}\n")
                result.append("நட்சத்திரம்: ${getNakshatra(sidereal)}\n")
                result.append("நிலை: $motion\n")
                result.append("--------------------------------\n\n")
            }

            // RAHU AND KETU

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

            val rahuD10 = calculateDasamsa(rahuSidereal)
            val ketuD10 = calculateDasamsa(ketuSidereal)

            val rahuD7 = calculateSaptamsa(rahuSidereal)
            val ketuD7 = calculateSaptamsa(ketuSidereal)

            val rahuD12 = calculateDwadasamsa(rahuSidereal)
            val ketuD12 = calculateDwadasamsa(ketuSidereal)

            val rahuD16 = calculateShodashamsa(rahuSidereal)
            val ketuD16 = calculateShodashamsa(ketuSidereal)

            val rahuD20 = calculateVimshamsa(rahuSidereal)
            val ketuD20 = calculateVimshamsa(ketuSidereal)

            val rahuD24 = calculateChaturvimshamsa(rahuSidereal)
            val ketuD24 = calculateChaturvimshamsa(ketuSidereal)

            val rahuD27 = calculateBhamsha(rahuSidereal)
            val ketuD27 = calculateBhamsha(ketuSidereal)

            result.append("ராகு - MEAN NODE\n")
            result.append("D1: ${formatRasi(rahuSidereal)}\n")
            result.append("D9: ${rahuD9.degree}\n")
            result.append("D10: ${rahuD10.degree}\n")
            result.append("D7: ${rahuD7.degree}\n")
            result.append("D12: ${rahuD12.degree}\n")
            result.append("D16: ${rahuD16.degree}\n")
            result.append("D20: ${rahuD20.degree}\n")
            result.append("D24: ${rahuD24.degree}\n")
            result.append("D27: ${rahuD27.degree}\n")
            result.append("நட்சத்திரம்: ${getNakshatra(rahuSidereal)}\n\n")

            result.append("கேது\n")
            result.append("D1: ${formatRasi(ketuSidereal)}\n")
            result.append("D9: ${ketuD9.degree}\n")
            result.append("D10: ${ketuD10.degree}\n")
            result.append("D7: ${ketuD7.degree}\n")
            result.append("D12: ${ketuD12.degree}\n")
            result.append("D16: ${ketuD16.degree}\n")
            result.append("D20: ${ketuD20.degree}\n")
            result.append("D24: ${ketuD24.degree}\n")
            result.append("D27: ${ketuD27.degree}\n")
            result.append("நட்சத்திரம்: ${getNakshatra(ketuSidereal)}\n")

            // COMPLETION

            result.append("\n=============================\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Charts: D1 + D9 + D10 + D7 + D12 + D16 + D20 + D24 + D27\n")
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
