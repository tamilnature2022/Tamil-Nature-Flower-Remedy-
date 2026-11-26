
package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.GridLayout
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

    private fun signIndex(longitude: Double): Int {
        return (normalize(longitude) / 30.0).toInt()
    }

    private fun houseNumber(
        planetLongitude: Double,
        lagnaLongitude: Double
    ): Int {
        val planetSign = signIndex(planetLongitude)
        val lagnaSign = signIndex(lagnaLongitude)

        return ((planetSign - lagnaSign + 12) % 12) + 1
    }

    private fun formatRasi(longitude: Double): String {
        val normalized = normalize(longitude)
        val index = signIndex(normalized)
        val withinRasi = normalized % 30.0

        val totalSeconds = (withinRasi * 3600.0).toInt()
        val degrees = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return String.format(
            Locale.US,
            "%s %02d° %02d′ %02d″",
            rasiNames[index],
            degrees,
            minutes,
            seconds
        )
    }

    private fun formatDegree(value: Double): String {
        return String.format(Locale.US, "%.6f°", value)
    }

    private fun createCell(
        text: String,
        bold: Boolean = false,
        background: Int = Color.WHITE
    ): TextView {

        return TextView(this).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(5, 10, 5, 10)
            setBackgroundColor(background)
            setTextColor(Color.BLACK)

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }

            setBackgroundResource(android.R.drawable.editbox_background)
        }
    }

    private fun createChart(
        lagnaSign: Int,
        positions: Map<String, Double>
    ): GridLayout {

        val chart = GridLayout(this).apply {
            rowCount = 4
            columnCount = 4
            setPadding(4, 4, 4, 4)
        }

        // South Indian fixed Rasi positions

        val coordinates = arrayOf(
            intArrayOf(0, 1), // Aries
            intArrayOf(0, 2), // Taurus
            intArrayOf(0, 3), // Gemini
            intArrayOf(1, 3), // Cancer
            intArrayOf(2, 3), // Leo
            intArrayOf(3, 3), // Virgo
            intArrayOf(3, 2), // Libra
            intArrayOf(3, 1), // Scorpio
            intArrayOf(3, 0), // Sagittarius
            intArrayOf(2, 0), // Capricorn
            intArrayOf(1, 0), // Aquarius
            intArrayOf(0, 0)  // Pisces
        )

        val cells = Array(4) { Array(4) { "" } }

        for (sign in 0..11) {

            val row = coordinates[sign][0]
            val col = coordinates[sign][1]

            val bhava = ((sign - lagnaSign + 12) % 12) + 1

            val text = StringBuilder()

            text.append(rasiNames[sign])
            text.append("\n")
            text.append("பாவம் $bhava")
            text.append("\n")

            if (sign == lagnaSign) {
                text.append("Lagna (L)\n")
            }

            val occupants = positions.filter {
                signIndex(it.value) == sign
            }.keys

            if (occupants.isNotEmpty()) {
                text.append(occupants.joinToString(" "))
            }

            cells[row][col] = text.toString()
        }

        for (row in 0..3) {
            for (col in 0..3) {

                val text = cells[row][col]

                val cellText = if (
                    row in 1..2 && col in 1..2
                ) {
                    when {
                        row == 1 && col == 1 ->
                            "TAMIL NATURE\n\nD1 RASI CHART"

                        else -> ""
                    }
                } else {
                    text
                }

                val cell = createCell(
                    cellText,
                    bold = true
                )

                val params = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(row, 1, 1f)
                    columnSpec = GridLayout.spec(col, 1, 1f)
                    width = 0
                    height = 150
                    setMargins(3, 3, 3, 3)
                }

                chart.addView(cell, params)
            }
        }

        return chart
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology Test"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(10, 25, 10, 25)
        }

        mainLayout.addView(title)

        val result = StringBuilder()

        try {

            // TEST BIRTH DETAILS

            val birthYear = 1984
            val birthMonth = 11
            val birthDay = 19
            val birthHour = 19
            val birthMinute = 58

            // Oddanchatram coordinates

            val latitude = 10.48013
            val longitude = 77.74977

            // IST to UTC

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

            val lagnaSign = signIndex(lagnaSidereal)

            // Graha positions

            val planetPositions =
                mutableMapOf<String, Double>()

            for (graha in grahas) {

                val position = Ephemera.position(
                    graha.body, jd
                ) ?: throw Exception(
                    "${graha.name} position unavailable"
                )

                planetPositions[graha.name] =
                    normalize(position.longitude - lahiri)
            }

            // Rahu and Ketu

            val rahuPosition = Ephemera.position(
                Body.MEAN_NODE, jd
            ) ?: throw Exception("Mean Node unavailable")

            val rahuSidereal = normalize(
                rahuPosition.longitude - lahiri
            )

            val ketuSidereal = normalize(
                rahuSidereal + 180.0
            )

            planetPositions["ராகு"] = rahuSidereal
            planetPositions["கேது"] = ketuSidereal

            // D1 CHART

            val chartHeading = TextView(this).apply {
                text = "D1 - தென்னிந்திய ராசி கட்டம்"
                textSize = 19f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(8, 16, 8, 16)
            }

            mainLayout.addView(chartHeading)

            mainLayout.addView(
                createChart(
                    lagnaSign,
                    planetPositions
                )
            )

            // Detailed report

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")
            result.append(
                "Date: $birthDay-$birthMonth-$birthYear\n"
            )
            result.append(
                "Time: $birthHour:$birthMinute IST\n"
            )
            result.append(
                "UTC: $hour:$minute:$second\n"
            )
            result.append(
                "Latitude: $latitude\n"
            )
            result.append(
                "Longitude: $longitude\n\n"
            )

            result.append("JD: $jd\n")
            result.append("Lahiri: $lahiri°\n\n")

            result.append("=============================\n")
            result.append("LAGNA\n")
            result.append("=============================\n")
            result.append(
                "Tropical: ${formatDegree(angles.asc)}\n"
            )
            result.append(
                "Sidereal: ${formatDegree(lagnaSidereal)}\n"
            )
            result.append(
                "Lagna: ${formatRasi(lagnaSidereal)}\n\n"
            )

            result.append(
                "MC: ${formatRasi(mcSidereal)}\n\n"
            )

            result.append("=============================\n")
            result.append("GRAHA DETAILS\n")
            result.append("=============================\n\n")

            for (graha in grahas) {

                val position = Ephemera.position(
                    graha.body, jd
                )!!

                val sidereal =
                    planetPositions[graha.name]!!

                val motion = if (
                    position.isRetrograde
                ) {
                    "வக்கிரம்"
                } else {
                    "நேர்கதி"
                }

                val house = houseNumber(
                    sidereal,
                    lagnaSidereal
                )

                result.append("${graha.name}\n")
                result.append(
                    "Position: ${formatRasi(sidereal)}\n"
                )
                result.append(
                    "House: $house\n"
                )
                result.append(
                    "Motion: $motion\n"
                )
                result.append("-----------------------------\n")
            }

            result.append("\nராகு\n")
            result.append(
                formatRasi(rahuSidereal)
            )
            result.append(
                "\nHouse: ${
                    houseNumber(
                        rahuSidereal,
                        lagnaSidereal
                    )
                }\n\n"
            )

            result.append("கேது\n")
            result.append(
                formatRasi(ketuSidereal)
            )
            result.append(
                "\nHouse: ${
                    houseNumber(
                        ketuSidereal,
                        lagnaSidereal
                    )
                }\n\n"
            )

            // 12 HOUSE MAPPING

            result.append("=============================\n")
            result.append("12 BHAVA MAPPING\n")
            result.append("=============================\n\n")

            for (house in 1..12) {

                val currentSign =
                    (lagnaSign + house - 1) % 12

                result.append(
                    "HOUSE $house - ${rasiNames[currentSign]}\n"
                )

                val occupants = planetPositions.filter {
                    houseNumber(
                        it.value,
                        lagnaSidereal
                    ) == house
                }.keys

                if (occupants.isEmpty()) {
                    result.append("கிரகங்கள் இல்லை\n")
                } else {
                    result.append(
                        occupants.joinToString(" | ")
                    )
                    result.append("\n")
                }

                result.append("-----------------------------\n")
            }

            result.append("\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")

        } catch (e: Exception) {

            result.append("\nCALCULATION ERROR\n")
            result.append(e.toString())
        }

        val report = TextView(this).apply {
            text = result.toString()
            textSize = 16f
            typeface = Typeface.MONOSPACE
            setPadding(15, 25, 15, 25)
        }

        mainLayout.addView(report)

        val scroll = ScrollView(this)
        scroll.addView(mainLayout)

        setContentView(scroll)
    }
}
