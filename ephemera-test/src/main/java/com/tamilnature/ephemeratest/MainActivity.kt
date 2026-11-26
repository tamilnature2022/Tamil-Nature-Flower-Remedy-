package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.*
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

    private data class Graha(val name: String, val body: Body)

    private val grahas = listOf(
        Graha("சூரியன்", Body.SUN),
        Graha("சந்திரன்", Body.MOON),
        Graha("செவ்வாய்", Body.MARS),
        Graha("புதன்", Body.MERCURY),
        Graha("குரு", Body.JUPITER),
        Graha("சுக்கிரன்", Body.VENUS),
        Graha("சனி", Body.SATURN)
    )

    private fun normalize(value: Double): Double =
        ((value % 360.0) + 360.0) % 360.0

    private fun signIndex(longitude: Double): Int =
        (normalize(longitude) / 30.0).toInt()

    private fun houseNumber(
        planetLongitude: Double,
        lagnaLongitude: Double
    ): Int {
        return ((signIndex(planetLongitude) -
            signIndex(lagnaLongitude) + 12) % 12) + 1
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

    private fun formatDegree(value: Double): String =
        String.format(Locale.US, "%.6f°", value)

    private fun createCell(
        text: String,
        bold: Boolean = false
    ): TextView {

        return TextView(this).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(5, 10, 5, 10)
            setTextColor(Color.BLACK)

            if (bold) typeface = Typeface.DEFAULT_BOLD

            setBackgroundResource(
                android.R.drawable.editbox_background
            )
        }
    }

    private fun createChart(
        lagnaSign: Int,
        positions: Map<String, Double>,
        retrogradeNames: Set<String>
    ): GridLayout {

        val chart = GridLayout(this).apply {
            rowCount = 4
            columnCount = 4
            setPadding(4, 4, 4, 4)
        }

        val coordinates = arrayOf(
            intArrayOf(0, 1),
            intArrayOf(0, 2),
            intArrayOf(0, 3),
            intArrayOf(1, 3),
            intArrayOf(2, 3),
            intArrayOf(3, 3),
            intArrayOf(3, 2),
            intArrayOf(3, 1),
            intArrayOf(3, 0),
            intArrayOf(2, 0),
            intArrayOf(1, 0),
            intArrayOf(0, 0)
        )

        val cells = Array(4) { Array(4) { "" } }

        for (sign in 0..11) {

            val row = coordinates[sign][0]
            val col = coordinates[sign][1]

            val bhava =
                ((sign - lagnaSign + 12) % 12) + 1

            val text = StringBuilder()

            text.append(rasiNames[sign])
            text.append("\n")
            text.append("பாவம் $bhava\n")

            if (sign == lagnaSign) {
                text.append("Lagna (L)\n")
            }

            val occupants = positions.filter {
                signIndex(it.value) == sign
            }.keys

            if (occupants.isNotEmpty()) {

                val displayed = occupants.map { name ->
                    if (name in retrogradeNames) {
                        "$name (R)"
                    } else {
                        name
                    }
                }

                text.append(displayed.joinToString(" "))
            }

            cells[row][col] = text.toString()
        }

        for (row in 0..3) {
            for (col in 0..3) {

                val text = cells[row][col]

                val cellText = if (
                    row in 1..2 && col in 1..2
                ) {
                    if (row == 1 && col == 1) {
                        "TAMIL NATURE\n\nD1 RASI CHART"
                    } else {
                        ""
                    }
                } else {
                    text
                }

                val cell = createCell(cellText, true)

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

    private fun createInput(
        hint: String,
        value: String = ""
    ): EditText {

        return EditText(this).apply {
            this.hint = hint
            setText(value)
            textSize = 16f
            setSingleLine(true)
            setPadding(16, 12, 16, 12)
        }
    }

    private lateinit var outputLayout: LinearLayout

    private fun addLabel(
        text: String,
        size: Float = 16f
    ) {

        outputLayout.addView(TextView(this).apply {
            this.text = text
            textSize = size
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 8, 8, 8)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val title = TextView(this).apply {
            text = "Tamil Nature - Astrology Test"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(10, 20, 10, 20)
        }

        root.addView(title)

        val inputHeading = TextView(this).apply {
            text = "BIRTH DETAILS"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(8, 10, 8, 10)
        }

        root.addView(inputHeading)

        val dateInput = createInput(
            "Date DD-MM-YYYY",
            "19-11-1984"
        )

        val timeInput = createInput(
            "Time HH:MM AM/PM",
            "07:58 PM"
        )

        val placeInput = createInput(
            "Birth Place",
            "Oddanchatram, Tamil Nadu"
        )

        val latitudeInput = createInput(
            "Latitude",
            "10.48013"
        )

        val longitudeInput = createInput(
            "Longitude",
            "77.74977"
        )

        root.addView(dateInput)
        root.addView(timeInput)
        root.addView(placeInput)
        root.addView(latitudeInput)
        root.addView(longitudeInput)

        val calculateButton = Button(this).apply {
            text = "CALCULATE CHART"
            textSize = 18f
        }

        root.addView(calculateButton)

        outputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(outputLayout)

        calculateButton.setOnClickListener {

            outputLayout.removeAllViews()

            try {

                // STRICT DATE FORMAT

                val dateText = dateInput.text.toString().trim()

                if (!dateText.matches(
                        Regex("^\\d{2}-\\d{2}-\\d{4}$")
                    )
                ) {
                    throw Exception("Date format must be DD-MM-YYYY")
                }

                val dateParts = dateText.split("-")

                val birthDay = dateParts[0].toInt()
                val birthMonth = dateParts[1].toInt()
                val birthYear = dateParts[2].toInt()

                // STRICT CALENDAR DATE VALIDATION

                val validationCalendar = Calendar.getInstance(
                    TimeZone.getTimeZone("Asia/Kolkata")
                )

                validationCalendar.isLenient = false

                validationCalendar.set(
                    birthYear,
                    birthMonth - 1,
                    birthDay,
                    0,
                    0,
                    0
                )

                validationCalendar.set(
                    Calendar.MILLISECOND, 0
                )

                validationCalendar.time

                // STRICT TIME FORMAT

                val timeText = timeInput.text.toString().trim()

                val timeRegex = Regex(
                    "^(0?[1-9]|1[0-2]):([0-5][0-9])\\s+(AM|PM)$",
                    RegexOption.IGNORE_CASE
                )

                if (!timeRegex.matches(timeText)) {
                    throw Exception(
                        "Time format must be HH:MM AM/PM"
                    )
                }

                val timeParts = timeText.split(
                    Regex("\\s+")
                )

                val clockParts = timeParts[0].split(":")

                val hour12 = clockParts[0].toInt()
                val birthMinute = clockParts[1].toInt()

                val period = timeParts[1].uppercase(Locale.US)

                val birthHour = when {
                    period == "AM" && hour12 == 12 -> 0
                    period == "PM" && hour12 == 12 -> 12
                    period == "PM" -> hour12 + 12
                    else -> hour12
                }

                // PLACE VALIDATION

                val place = placeInput.text.toString().trim()

                if (place.isEmpty()) {
                    throw Exception("Birth place is required")
                }

                // LATITUDE VALIDATION

                val latitudeText =
                    latitudeInput.text.toString().trim()

                if (latitudeText.isEmpty()) {
                    throw Exception("Latitude is required")
                }

                val latitude = latitudeText.toDoubleOrNull()
                    ?: throw Exception("Invalid latitude")

                if (!latitude.isFinite() ||
                    latitude !in -90.0..90.0
                ) {
                    throw Exception(
                        "Latitude must be between -90 and 90"
                    )
                }

                // LONGITUDE VALIDATION

                val longitudeText =
                    longitudeInput.text.toString().trim()

                if (longitudeText.isEmpty()) {
                    throw Exception("Longitude is required")
                }

                val longitude = longitudeText.toDoubleOrNull()
                    ?: throw Exception("Invalid longitude")

                if (!longitude.isFinite() ||
                    longitude !in -180.0..180.0
                ) {
                    throw Exception(
                        "Longitude must be between -180 and 180"
                    )
                }

                calculate(
                    birthYear,
                    birthMonth,
                    birthDay,
                    birthHour,
                    birthMinute,
                    latitude,
                    longitude,
                    place,
                    timeText
                )

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    "Input Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val scroll = ScrollView(this)
        scroll.addView(root)

        setContentView(scroll)
    }

    private fun calculate(
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        birthHour: Int,
        birthMinute: Int,
        latitude: Double,
        longitude: Double,
        place: String,
        displayTime: String
    ) {

        outputLayout.removeAllViews()

        val result = StringBuilder()

        try {

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

            val planetPositions =
                mutableMapOf<String, Double>()

            val tropicalPositions =
                mutableMapOf<String, Double>()

            val retrogradeNames =
                mutableSetOf<String>()

            val retrogradeStatus =
                mutableMapOf<String, String>()

            for (graha in grahas) {

                val position = Ephemera.position(
                    graha.body, jd
                ) ?: throw Exception(
                    "${graha.name} position unavailable"
                )

                tropicalPositions[graha.name] =
                    normalize(position.longitude)

                planetPositions[graha.name] =
                    normalize(position.longitude - lahiri)

                retrogradeStatus[graha.name] =
                    if (position.isRetrograde) {
                        "வக்கிரம் (R)"
                    } else {
                        "நேர்கதி (D)"
                    }

                if (position.isRetrograde) {
                    retrogradeNames.add(graha.name)
                }
            }

            // RAHU & KETU

            val rahuPosition = Ephemera.position(
                Body.MEAN_NODE, jd
            ) ?: throw Exception("Mean Node unavailable")

            val rahuTropical = normalize(
                rahuPosition.longitude
            )

            val rahuSidereal = normalize(
                rahuTropical - lahiri
            )

            val ketuTropical = normalize(
                rahuTropical + 180.0
            )

            val ketuSidereal = normalize(
                rahuSidereal + 180.0
            )

            planetPositions["ராகு"] = rahuSidereal
            planetPositions["கேது"] = ketuSidereal

            tropicalPositions["ராகு"] = rahuTropical
            tropicalPositions["கேது"] = ketuTropical

            // CHART

            addLabel(
                "D1 - தென்னிந்திய ராசி கட்டம்",
                19f
            )

            outputLayout.addView(
                createChart(
                    lagnaSign,
                    planetPositions,
                    retrogradeNames
                )
            )

            // BIRTH DETAILS

            result.append("BIRTH DETAILS\n")
            result.append("-----------------------------\n")

            result.append(
                "Date: %02d-%02d-%04d\n".format(
                    birthDay, birthMonth, birthYear
                )
            )

            result.append("Time: $displayTime IST\n")
            result.append("Place: $place\n")

            result.append(
                "UTC: %02d:%02d:%02d\n".format(
                    hour, minute, second
                )
            )

            result.append("Latitude: $latitude\n")
            result.append("Longitude: $longitude\n\n")

            result.append("JD: $jd\n")
            result.append("TT: $tt\n")

            result.append(
                "Lahiri Ayanamsa: ${formatDegree(lahiri)}\n\n"
            )

            // LAGNA VALIDATION

            result.append("=============================\n")
            result.append("LAGNA VALIDATION\n")
            result.append("=============================\n")

            result.append(
                "Tropical Ascendant: ${formatDegree(angles.asc)}\n"
            )

            result.append(
                "Ayanamsa: ${formatDegree(lahiri)}\n"
            )

            result.append(
                "Sidereal Ascendant: ${formatDegree(lagnaSidereal)}\n"
            )

            result.append(
                "Final Lagna: ${formatRasi(lagnaSidereal)}\n\n"
            )

            result.append(
                "Tropical MC: ${formatDegree(angles.mc)}\n"
            )

            result.append(
                "Sidereal MC: ${formatDegree(mcSidereal)}\n"
            )

            result.append(
                "Final MC: ${formatRasi(mcSidereal)}\n\n"
            )

            // GRAHA VALIDATION

            result.append("=============================\n")
            result.append("GRAHA VALIDATION\n")
            result.append("=============================\n\n")

            for (graha in grahas) {

                val tropical =
                    tropicalPositions[graha.name]!!

                val sidereal =
                    planetPositions[graha.name]!!

                val house = houseNumber(
                    sidereal,
                    lagnaSidereal
                )

                result.append("${graha.name}\n")

                result.append(
                    "Tropical: ${formatDegree(tropical)}\n"
                )

                result.append(
                    "Lahiri: ${formatDegree(lahiri)}\n"
                )

                result.append(
                    "Sidereal: ${formatDegree(sidereal)}\n"
                )

                result.append(
                    "Final Position: ${formatRasi(sidereal)}\n"
                )

                result.append("House: $house\n")

                result.append(
                    "Motion: ${retrogradeStatus[graha.name]}\n"
                )

                result.append("-----------------------------\n")
            }

            // RAHU

            result.append("\nராகு\n")

            result.append(
                "Tropical: ${formatDegree(rahuTropical)}\n"
            )

            result.append(
                "Lahiri: ${formatDegree(lahiri)}\n"
            )

            result.append(
                "Sidereal: ${formatDegree(rahuSidereal)}\n"
            )

            result.append(
                "Final: ${formatRasi(rahuSidereal)}\n"
            )

            result.append(
                "House: ${houseNumber(rahuSidereal, lagnaSidereal)}\n\n"
            )

            // KETU

            result.append("கேது\n")

            result.append(
                "Tropical: ${formatDegree(ketuTropical)}\n"
            )

            result.append(
                "Lahiri: ${formatDegree(lahiri)}\n"
            )

            result.append(
                "Sidereal: ${formatDegree(ketuSidereal)}\n"
            )

            result.append(
                "Final: ${formatRasi(ketuSidereal)}\n"
            )

            result.append(
                "House: ${houseNumber(ketuSidereal, lagnaSidereal)}\n\n"
            )

            // 12 BHAVA MAPPING

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

            // =============================
            // 12 BHAVA LORD ANALYSIS
            // =============================

            val signLords = arrayOf(
                "செவ்வாய்",   // மேஷம்
                "சுக்கிரன்",   // ரிஷபம்
                "புதன்",      // மிதுனம்
                "சந்திரன்",   // கடகம்
                "சூரியன்",    // சிம்மம்
                "புதன்",      // கன்னி
                "சுக்கிரன்",  // துலாம்
                "செவ்வாய்",   // விருச்சிகம்
                "குரு",       // தனுசு
                "சனி",        // மகரம்
                "சனி",        // கும்பம்
                "குரு"        // மீனம்
            )

            result.append("\n")
            result.append("=============================\n")
            result.append("12 BHAVA LORD ANALYSIS\n")
            result.append("=============================\n\n")

            for (house in 1..12) {

                val currentSign =
                    (lagnaSign + house - 1) % 12

                val lord = signLords[currentSign]

                val lordPosition =
                    planetPositions[lord]

                result.append(
                    "HOUSE $house - ${rasiNames[currentSign]}\n"
                )

                result.append(
                    "Lord: $lord\n"
                )

                if (lordPosition != null) {

                    result.append(
                        "Lord Position: ${formatRasi(lordPosition)}\n"
                    )

                    result.append(
                        "Lord House: ${
                            houseNumber(
                                lordPosition,
                                lagnaSidereal
                            )
                        }\n"
                    )

                } else {

                    result.append(
                        "Lord Position unavailable\n"
                    )
                }

                result.append("-----------------------------\n")
            }

            // COMPLETION

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

        outputLayout.addView(report)
    }
}
