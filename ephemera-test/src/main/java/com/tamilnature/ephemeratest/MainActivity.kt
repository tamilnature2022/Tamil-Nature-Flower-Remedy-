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
    ): Int =
        ((signIndex(planetLongitude) -
            signIndex(lagnaLongitude) + 12) % 12) + 1

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
            rasiNames[index], degrees, minutes, seconds
        )
    }

    private fun formatDegree(value: Double): String =
        String.format(Locale.US, "%.6f°", value)

    // STEP 4: LORD DIGNITY STATUS

    private fun getLordStatus(
        lord: String,
        sign: Int
    ): String {

        val ownSigns = when (lord) {
            "சூரியன்" -> listOf(4)
            "சந்திரன்" -> listOf(3)
            "செவ்வாய்" -> listOf(0, 7)
            "புதன்" -> listOf(2, 5)
            "குரு" -> listOf(8, 11)
            "சுக்கிரன்" -> listOf(1, 6)
            "சனி" -> listOf(9, 10)
            else -> emptyList()
        }

        val exaltationSigns = mapOf(
            "சூரியன்" to 0,
            "சந்திரன்" to 1,
            "செவ்வாய்" to 9,
            "புதன்" to 5,
            "குரு" to 3,
            "சுக்கிரன்" to 11,
            "சனி" to 6
        )

        val debilitationSigns = mapOf(
            "சூரியன்" to 6,
            "சந்திரன்" to 7,
            "செவ்வாய்" to 3,
            "புதன்" to 11,
            "குரு" to 9,
            "சுக்கிரன்" to 5,
            "சனி" to 0
        )

        return when {
            sign in ownSigns ->
                "ஆட்சி (Own Sign)"

            exaltationSigns[lord] == sign ->
                "உச்சம் (Exaltation)"

            debilitationSigns[lord] == sign ->
                "நீசம் (Debilitation)"

            else ->
                "மற்ற ராசி (Other Sign)"
        }
    }

    // STEP 6: GRAHA DRISHTI OFFSETS
    private fun getDrishtiOffsets(graha: String): List<Int> {
        return when (graha) {
            "செவ்வாய்" -> listOf(4, 7, 8)
            "குரு" -> listOf(5, 7, 9)
            "சனி" -> listOf(3, 7, 10)
            "ராகு", "கேது" -> listOf(3, 7, 11)
            else -> listOf(7)
        }
    }

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
                    if (name in retrogradeNames)
                        "$name (R)"
                    else
                        name
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
                    if (row == 1 && col == 1)
                        "TAMIL NATURE\n\nD1 RASI CHART"
                    else
                        ""
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

                val dateText = dateInput.text.toString().trim()

                if (!dateText.matches(
                        Regex("^\\d{2}-\\d{2}-\\d{4}$")
                    )
                ) {
                    throw Exception(
                        "Date format must be DD-MM-YYYY"
                    )
                }

                val dateParts = dateText.split("-")

                val birthDay = dateParts[0].toInt()
                val birthMonth = dateParts[1].toInt()
                val birthYear = dateParts[2].toInt()

                val validationCalendar = Calendar.getInstance(
                    TimeZone.getTimeZone("Asia/Kolkata")
                )

                validationCalendar.isLenient = false

                validationCalendar.set(
                    birthYear,
                    birthMonth - 1,
                    birthDay,
                    0, 0, 0
                )

                validationCalendar.set(
                    Calendar.MILLISECOND, 0
                )

                validationCalendar.time

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

                val place = placeInput.text.toString().trim()

                if (place.isEmpty()) {
                    throw Exception("Birth place is required")
                }

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
                    if (position.isRetrograde)
                        "வக்கிரம் (R)"
                    else
                        "நேர்கதி (D)"

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

            // STEP 5: GRAHA VALIDATION + DIGNITY

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

                val grahaSign = signIndex(sidereal)

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

                result.append(
                    "Graha Status: ${
                        getLordStatus(graha.name, grahaSign)
                    }\n"
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

            // 12 BHAVA LORD ANALYSIS + STATUS

            val signLords = arrayOf(
                "செவ்வாய்",
                "சுக்கிரன்",
                "புதன்",
                "சந்திரன்",
                "சூரியன்",
                "புதன்",
                "சுக்கிரன்",
                "செவ்வாய்",
                "குரு",
                "சனி",
                "சனி",
                "குரு"
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

                result.append("Lord: $lord\n")

                if (lordPosition != null) {

                    val lordSign = signIndex(lordPosition)

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

                    result.append(
                        "Lord Status: ${
                            getLordStatus(lord, lordSign)
                        }\n"
                    )

                } else {

                    result.append(
                        "Lord Position unavailable\n"
                    )
                }

                result.append("-----------------------------\n")
            }

            // STEP 6: GRAHA DRISHTI REPORT
            result.append("\n")
            result.append("=============================\n")
            result.append("STEP 6 - GRAHA DRISHTI\n")
            result.append("=============================\n\n")

            for (grahaName in planetPositions.keys) {
                val sourceLongitude = planetPositions[grahaName]!!
                val sourceHouse = houseNumber(sourceLongitude, lagnaSidereal)
                val offsets = getDrishtiOffsets(grahaName)

                result.append("$grahaName\n")
                result.append("Source House: $sourceHouse - ${rasiNames[signIndex(sourceLongitude)]}\n")
                result.append("Drishti Offsets: ${offsets.joinToString(", ")}\n")

                for (offset in offsets) {
                    val targetHouse = ((sourceHouse + offset - 2) % 12) + 1
                    val targetSign = (lagnaSign + targetHouse - 1) % 12
                    val occupants = planetPositions.filter {
                        houseNumber(it.value, lagnaSidereal) == targetHouse &&
                            it.key != grahaName
                    }.keys

                    result.append("  -> House $targetHouse - ${rasiNames[targetSign]}\n")
                    if (occupants.isEmpty()) {
                        result.append("     Target occupants: இல்லை\n")
                    } else {
                        result.append("     Target occupants: ${occupants.joinToString(" | ")}\n")
                    }
                }
                result.append("-----------------------------\n")
            }

            // STEP 7: BHAVA DRISHTI RECEIVED ANALYSIS
            result.append("\n")
            result.append("=============================\n")
            result.append("STEP 7 - BHAVA DRISHTI RECEIVED\n")
            result.append("=============================\n\n")

            val drishtiSources = planetPositions.keys.toList()
            val bhavaScores = mutableMapOf<Int, Int>()

            for (targetHouse in 1..12) {
                val targetSign = (lagnaSign + targetHouse - 1) % 12
                val receivedFrom = mutableListOf<String>()

                for (sourceName in drishtiSources) {
                    val sourceLongitude = planetPositions[sourceName]!!
                    val sourceHouse = houseNumber(sourceLongitude, lagnaSidereal)
                    val offsets = getDrishtiOffsets(sourceName)

                    for (offset in offsets) {
                        val calculatedTarget = ((sourceHouse + offset - 2) % 12) + 1
                        if (calculatedTarget == targetHouse) {
                            receivedFrom.add(sourceName)
                            break
                        }
                    }
                }

                val occupants = planetPositions.filter {
                    houseNumber(it.value, lagnaSidereal) == targetHouse
                }.keys

                result.append("HOUSE $targetHouse - ${rasiNames[targetSign]}\n")
                result.append("Occupants: ${if (occupants.isEmpty()) "இல்லை" else occupants.joinToString(" | ")}\n")
                result.append("Drishti received from: ${if (receivedFrom.isEmpty()) "எந்த கிரக பார்வையும் இல்லை" else receivedFrom.joinToString(" | ")}\n")
                result.append("Aspect count: ${receivedFrom.size}\n")
                result.append("-----------------------------\n")
            }

// STEP 8: PRELIMINARY BHAVA STRENGTH MODEL
            result.append("\n")
            result.append("=============================\n")
            result.append("STEP 8 - PRELIMINARY BHAVA STRENGTH\n")
            result.append("=============================\n")
            result.append("Note: This is a transparent prototype scoring model, not a traditional Shadbala calculation.\n\n")

            val signLordsForStrength = arrayOf(
                "செவ்வாய்", "சுக்கிரன்", "புதன்", "சந்திரன்",
                "சூரியன்", "புதன்", "சுக்கிரன்", "செவ்வாய்",
                "குரு", "சனி", "சனி", "குரு"
            )

            for (targetHouse in 1..12) {
                val targetSign = (lagnaSign + targetHouse - 1) % 12
                val lordName = signLordsForStrength[targetSign]
                val lordLongitude = planetPositions[lordName]
                var score = 50
                val factorLines = mutableListOf<String>()

                val occupants = planetPositions.filter {
                    houseNumber(it.value, lagnaSidereal) == targetHouse
                }.keys.toList()

                // Lord dignity contribution
                if (lordLongitude != null) {
                    val lordSign = signIndex(lordLongitude)
                    val dignity = getLordStatus(lordName, lordSign)
                    val dignityPoints = when {
                        dignity.contains("Exaltation") -> 20
                        dignity.contains("Own Sign") -> 15
                        dignity.contains("Debilitation") -> -15
                        else -> 0
                    }
                    score += dignityPoints
                    factorLines.add("Lord dignity: $dignity ($dignityPoints)")

                    // Lord placement contribution: kendras and trikonas
                    val lordHouse = houseNumber(lordLongitude, lagnaSidereal)
                    val placementPoints = when (lordHouse) {
                        1, 4, 7, 10 -> 10
                        5, 9 -> 8
                        2, 11 -> 5
                        3, 6, 8, 12 -> -5
                        else -> 0
                    }
                    score += placementPoints
                    factorLines.add("Lord placement: House $lordHouse ($placementPoints)")
                } else {
                    factorLines.add("Lord position unavailable (0)")
                }

                // Occupancy contribution (simple prototype adjustment)
                val occupancyPoints = occupants.size * 3
                score += occupancyPoints
                factorLines.add("Occupants: ${occupants.size} (+$occupancyPoints)")

                // Received aspects: Jupiter/Venus/Mercury and Moon treated as supportive
                // in this prototype; Mars/Saturn/Rahu/Ketu treated as challenging;
                // Sun is neutral. This is deliberately simplified.
                val aspectSources = mutableListOf<String>()
                var aspectAdjustment = 0
                for (sourceName in planetPositions.keys) {
                    val sourceLongitude = planetPositions[sourceName]!!
                    val sourceHouse = houseNumber(sourceLongitude, lagnaSidereal)
                    for (offset in getDrishtiOffsets(sourceName)) {
                        val calculatedTarget = ((sourceHouse + offset - 2) % 12) + 1
                        if (calculatedTarget == targetHouse) {
                            aspectSources.add(sourceName)
                            val adjustment = when (sourceName) {
                                "குரு", "சுக்கிரன்", "புதன்", "சந்திரன்" -> 3
                                "செவ்வாய்", "சனி", "ராகு", "கேது" -> -3
                                else -> 0
                            }
                            aspectAdjustment += adjustment
                            break
                        }
                    }
                }
                score += aspectAdjustment
                factorLines.add("Aspect influence: ${if (aspectSources.isEmpty()) "None" else aspectSources.joinToString()} ($aspectAdjustment)")

                val finalScore = score.coerceIn(0, 100)
                bhavaScores[targetHouse] = finalScore

                val strengthLabel = when {
                    finalScore >= 75 -> "High"
                    finalScore >= 60 -> "Above average"
                    finalScore >= 40 -> "Moderate"
                    finalScore >= 25 -> "Low"
                    else -> "Very low"
                }

                result.append("HOUSE $targetHouse - ${rasiNames[targetSign]}\n")
                result.append("Bhava Lord: $lordName\n")
                result.append("Occupants: ${if (occupants.isEmpty()) "இல்லை" else occupants.joinToString(" | ")}\n")
                result.append("Aspects: ${if (aspectSources.isEmpty()) "None" else aspectSources.joinToString(" | ")}\n")
                result.append("Factors:\n")
                factorLines.forEach { result.append(" - $it\n") }
                result.append("Raw score: $score\n")
                result.append("Final score (0-100): $finalScore\n")
                result.append("Strength level: $strengthLabel\n")
                result.append("-----------------------------\n")
            }

            // STEP 11: BHAVA SCORE SUMMARY
            result.append("\n=============================\n")
            result.append("STEP 11 - BHAVA SCORE SUMMARY\n")
            result.append("=============================\n\n")
            val sortedBhavas = bhavaScores.entries.sortedByDescending { it.value }
            result.append("Top 3 Bhavas (prototype scores):\n")
            sortedBhavas.take(3).forEach { entry ->
                val sign = (lagnaSign + entry.key - 1) % 12
                result.append("House ${entry.key} - ${rasiNames[sign]}: ${entry.value}/100\n")
            }
            result.append("\nLowest 3 Bhavas (prototype scores):\n")
            sortedBhavas.takeLast(3).sortedBy { it.value }.forEach { entry ->
                val sign = (lagnaSign + entry.key - 1) % 12
                result.append("House ${entry.key} - ${rasiNames[sign]}: ${entry.value}/100\n")
            }
            result.append("\nNote: Ranking is based only on the current heuristic Bhava score model.\n")

                                    // STEP 9: BASIC YOGA DETECTION
            result.append("\n=============================\n")
            result.append("STEP 9 - YOGA DETECTION\n")
            result.append("=============================\n\n")

            val yogaSignLords = arrayOf(
                "செவ்வாய்", "சுக்கிரன்", "புதன்", "சந்திரன்",
                "சூரியன்", "புதன்", "சுக்கிரன்", "செவ்வாய்",
                "குரு", "சனி", "சனி", "குரு"
            )
            val yogaPlanetHouses = planetPositions.mapValues { houseNumber(it.value, lagnaSidereal) }
            val yogaPlanetSigns = planetPositions.mapValues { signIndex(it.value) }
            val yogaDetected = mutableListOf<String>()

            fun yogaLordOfHouse(house: Int): String {
                val sign = (lagnaSign + house - 1) % 12
                return yogaSignLords[sign]
            }
            fun yogaIsKendra(house: Int) = house in listOf(1, 4, 7, 10)
            fun yogaHasAspect(from: String, to: String): Boolean {
                val sourceHouse = yogaPlanetHouses[from] ?: return false
                val targetHouse = yogaPlanetHouses[to] ?: return false
                return getDrishtiOffsets(from).any { ((sourceHouse + it - 2) % 12) + 1 == targetHouse }
            }
            fun yogaLinked(a: String, b: String): Boolean {
                if (yogaPlanetSigns[a] == yogaPlanetSigns[b]) return true
                return yogaHasAspect(a, b) || yogaHasAspect(b, a)
            }

            // Raja Yoga: Kendra lord linked with Trikona lord
            val kendraLords = listOf(1, 4, 7, 10).map { yogaLordOfHouse(it) }.distinct()
            val trikonaLords = listOf(5, 9).map { yogaLordOfHouse(it) }.distinct()
            for (kl in kendraLords) for (tl in trikonaLords) if (kl != tl && yogaLinked(kl, tl)) yogaDetected.add("Raja Yoga: $kl + $tl")

            // Dhana Yoga: 2nd/11th lord linked with 5th/9th/Kendra lord
            val wealthLords = listOf(2, 11).map { yogaLordOfHouse(it) }.distinct()
            val prosperityLords = listOf(1, 4, 5, 7, 9, 10).map { yogaLordOfHouse(it) }.distinct()
            for (wl in wealthLords) for (pl in prosperityLords) if (wl != pl && yogaLinked(wl, pl)) yogaDetected.add("Dhana Yoga: $wl + $pl")

            // Gaja Kesari: Jupiter in Kendra from Moon
            val moonHouse = yogaPlanetHouses["சந்திரன்"]
            val jupiterHouse = yogaPlanetHouses["குரு"]
            if (moonHouse != null && jupiterHouse != null) {
                val relativeHouse = ((jupiterHouse - moonHouse + 12) % 12) + 1
                if (relativeHouse in listOf(1, 4, 7, 10)) yogaDetected.add("Gaja Kesari Yoga")
            }
            // Budha-Aditya: Sun and Mercury same sign
            if (yogaPlanetSigns["சூரியன்"] == yogaPlanetSigns["புதன்"]) yogaDetected.add("Budha-Aditya Yoga")

            // Neecha Bhanga prototype candidate
            val debilitationSigns = mapOf("சூரியன்" to 6, "சந்திரன்" to 7, "செவ்வாய்" to 3, "புதன்" to 11, "குரு" to 9, "சுக்கிரன்" to 5, "சனி" to 0)
            val signLordsForNeecha = mapOf(0 to "செவ்வாய்", 1 to "சுக்கிரன்", 2 to "புதன்", 3 to "சந்திரன்", 4 to "சூரியன்", 5 to "புதன்", 6 to "சுக்கிரன்", 7 to "செவ்வாய்", 8 to "குரு", 9 to "சனி", 10 to "சனி", 11 to "குரு")
            for ((planet, sign) in yogaPlanetSigns) if (debilitationSigns[planet] == sign) {
                val debSignLord = signLordsForNeecha[sign]
                val debLordHouse = debSignLord?.let { yogaPlanetHouses[it] }
                val cancellation = (debLordHouse != null && yogaIsKendra(debLordHouse)) || (yogaPlanetHouses[planet]?.let { yogaIsKendra(it) } == true)
                yogaDetected.add(if (cancellation) "Neecha Bhanga candidate: $planet (prototype condition met)" else "Debilitated planet: $planet (no prototype cancellation condition)")
            }
            if (yogaDetected.isEmpty()) result.append("No listed Yoga detected by current prototype rules.\n")
            else yogaDetected.distinct().forEach { result.append("• $it\n") }
            result.append("Note: Rule-based prototype; traditional variants may differ.\n")


            // STEP 10: YOGA STRENGTH & VALIDATION (prototype)
            result.append("\n=============================\n")
            result.append("STEP 10 - YOGA STRENGTH & VALIDATION\n")
            result.append("=============================\n\n")

            val yogaPlanetNames = listOf("சூரியன்", "சந்திரன்", "செவ்வாய்", "புதன்", "குரு", "சுக்கிரன்", "சனி", "ராகு", "கேது")
            fun involvedPlanetsForYoga(yoga: String): List<String> {
                val found = yogaPlanetNames.filter { yoga.contains(it) }
                if (found.isNotEmpty()) return found.distinct()
                return when {
                    yoga.contains("Gaja Kesari") -> listOf("சந்திரன்", "குரு")
                    yoga.contains("Budha-Aditya") -> listOf("சூரியன்", "புதன்")
                    else -> emptyList()
                }
            }
            if (yogaDetected.isEmpty()) {
                result.append("No detected Yoga to score.\n")
            } else {
                for (yoga in yogaDetected.distinct()) {
                    val involved = involvedPlanetsForYoga(yoga)
                    var score = 50
                    val factors = mutableListOf<String>()
                    for (planet in involved) {
                        val sign = yogaPlanetSigns[planet]
                        val house = yogaPlanetHouses[planet]
                        if (sign != null) {
                            when (getLordStatus(planet, sign)) {
                                "ஆட்சி (Own Sign)" -> { score += 10; factors.add("$planet: own sign +10") }
                                "உச்சம் (Exaltation)" -> { score += 15; factors.add("$planet: exalted +15") }
                                "நீசம் (Debilitation)" -> { score -= 15; factors.add("$planet: debilitated -15") }
                                else -> factors.add("$planet: neutral dignity")
                            }
                        }
                        if (house != null) {
                            when (house) {
                                in listOf(1, 4, 7, 10) -> { score += 8; factors.add("$planet: Kendra placement +8") }
                                in listOf(5, 9) -> { score += 8; factors.add("$planet: Trikona placement +8") }
                                in listOf(3, 6, 8, 12) -> { score -= 5; factors.add("$planet: challenging house -5") }
                            }
                        }
                    }
                    if (involved.isEmpty()) factors.add("Planet-level validation unavailable for this rule")
                    val finalScore = score.coerceIn(0, 100)
                    val label = when {
                        finalScore >= 80 -> "Strong (prototype)"
                        finalScore >= 65 -> "Above average (prototype)"
                        finalScore >= 45 -> "Moderate (prototype)"
                        finalScore >= 25 -> "Low (prototype)"
                        else -> "Very low (prototype)"
                    }
                    result.append("Yoga: $yoga\n")
                    result.append("Involved planets: ${if (involved.isEmpty()) "Not parsed" else involved.joinToString(" | ")}\n")
                    result.append("Factors: ${if (factors.isEmpty()) "None" else factors.joinToString("; ")}\n")
                    result.append("Yoga strength score: $finalScore / 100\n")
                    result.append("Strength label: $label\n")
                    result.append("-----------------------------\n")
                }
            }
            result.append("Note: Preliminary heuristic scoring only; not classical Shadbala or a definitive traditional Yoga evaluation.\n")

            // STEP 12: PLANET STRENGTH RANKING (heuristic prototype)
            result.append("\n=============================\n")
            result.append("STEP 12 - PLANET STRENGTH RANKING\n")
            result.append("=============================\n\n")
            val planetStrengthRows = mutableListOf<Pair<String, Int>>()
            for (planetName in planetPositions.keys) {
                val longitude = planetPositions[planetName]!!
                val sign = signIndex(longitude)
                val house = houseNumber(longitude, lagnaSidereal)
                var score = 50
                val factors = mutableListOf<String>()
                val dignity = getLordStatus(planetName, sign)
                val dignityPoints = when {
                    dignity.contains("Exaltation") -> 20
                    dignity.contains("Own Sign") -> 15
                    dignity.contains("Debilitation") -> -15
                    else -> 0
                }
                score += dignityPoints
                factors.add("Dignity: $dignity ($dignityPoints)")
                val housePoints = when (house) {
                    1, 4, 7, 10 -> 10
                    5, 9 -> 8
                    2, 11 -> 5
                    3, 6, 8, 12 -> -5
                    else -> 0
                }
                score += housePoints
                factors.add("House placement: $house ($housePoints)")
                if (retrogradeNames.contains(planetName)) {
                    score -= 3
                    factors.add("Retrograde: -3 (prototype adjustment)")
                }
                val finalPlanetScore = score.coerceIn(0, 100)
                planetStrengthRows.add(planetName to finalPlanetScore)
                result.append("$planetName - ${rasiNames[sign]} - House $house\n")
                result.append("$factors\n")
                result.append("Score: $finalPlanetScore/100\n")
                result.append("-----------------------------\n")
            }
            val rankedPlanets = planetStrengthRows.sortedByDescending { it.second }
            result.append("\nPlanet ranking (highest to lowest):\n")
            rankedPlanets.forEachIndexed { index, entry ->
                result.append("${index + 1}. ${entry.first}: ${entry.second}/100\n")
            }
            result.append("\nNote: Prototype comparative score only; not classical Shadbala or a definitive planetary strength calculation.\n")


            // STEP 13: INTEGRATED HOROSCOPE ANALYSIS (prototype)
            result.append("\n=============================\n")
            result.append("STEP 13 - INTEGRATED HOROSCOPE ANALYSIS\n")
            result.append("=============================\n\n")

            result.append("Strongest Bhava themes (top 3):\n")
            sortedBhavas.take(3).forEach { entry ->
                result.append("House ${entry.key}: ${entry.value}/100\n")
            }
            result.append("\nPlanetary highlights (top 3):\n")
            rankedPlanets.take(3).forEachIndexed { index, entry ->
                result.append("${index + 1}. ${entry.first}: ${entry.second}/100\n")
            }
            result.append("\nYoga findings:\n")
            if (yogaDetected.isEmpty()) result.append("No Yoga detected by current prototype rules.\n")
            else yogaDetected.distinct().forEach { result.append("• $it\n") }

            result.append("\nIntegrated overview: This section consolidates the current prototype's Bhava ranking, planetary ranking, and rule-based Yoga findings. It is a technical summary, not a definitive traditional horoscope judgment.\n")


            // STEP 14: INTEGRATED BHAVA PREDICTION SCORE (heuristic prototype)
            result.append("\n=============================\n")
            result.append("STEP 14 - INTEGRATED BHAVA PREDICTION SCORE\n")
            result.append("=============================\n\n")
            result.append("Weighted prototype model: Bhava base 65%, house-lord strength 25%, occupancy signal 10%.\n")
            result.append("This is a research heuristic and not a classical prediction formula.\n\n")

            val planetScoresForIntegrated = rankedPlanets.associate { it.first to it.second }
            val integratedBhavaRows = mutableListOf<Pair<Int, Int>>()
            for (targetHouse in 1..12) {
                val targetSign = (lagnaSign + targetHouse - 1) % 12
                val lordName = signLordsForStrength[targetSign]
                val baseBhava = bhavaScores[targetHouse] ?: 50
                val lordScore = planetScoresForIntegrated[lordName] ?: 50
                val occupants = planetPositions.filter { houseNumber(it.value, lagnaSidereal) == targetHouse }.size
                val occupancySignal = (50 + occupants * 10).coerceIn(0, 100)
                val integratedScore = (baseBhava * 0.65 + lordScore * 0.25 + occupancySignal * 0.10).toInt().coerceIn(0, 100)
                val label = when {
                    integratedScore >= 75 -> "Strong"
                    integratedScore >= 60 -> "Above average"
                    integratedScore >= 40 -> "Moderate"
                    integratedScore >= 25 -> "Below average"
                    else -> "Low"
                }
                integratedBhavaRows.add(targetHouse to integratedScore)
                result.append("House $targetHouse - ${rasiNames[targetSign]}\n")
                result.append("Bhava base: $baseBhava | Lord ($lordName): $lordScore | Occupancy signal: $occupancySignal\n")
                result.append("Integrated score: $integratedScore/100 - $label\n")
                result.append("-----------------------------\n")
            }
            result.append("\nIntegrated Bhava ranking (highest to lowest):\n")
            integratedBhavaRows.sortedByDescending { it.second }.forEachIndexed { index, row ->
                result.append("${index + 1}. House ${row.first}: ${row.second}/100\n")
            }
            result.append("\nInterpretation caution: score indicates comparative prototype weighting only; use traditional context and practitioner judgment.\n")


            // STEP 15: PREDICTION INTERPRETATION LAYER (prototype)
            result.append("\n=============================\n")
            result.append("STEP 15 - PREDICTION INTERPRETATION LAYER\n")
            result.append("=============================\n\n")
            result.append("Interpretations are score-based research prompts, not fixed predictions.\n\n")

            val houseThemes = mapOf(
                1 to "Self, vitality, approach and personal direction",
                2 to "Resources, family, speech and accumulated values",
                3 to "Initiative, communication, skills and siblings",
                4 to "Home, emotional foundations, education and property",
                5 to "Creativity, learning, children and discernment",
                6 to "Daily work, service, routines and challenges",
                7 to "Partnerships, agreements and one-to-one relationships",
                8 to "Shared resources, transitions, research and deeper matters",
                9 to "Higher learning, beliefs, teachers and long-distance themes",
                10 to "Career, responsibility, public role and achievement",
                11 to "Networks, gains, aspirations and community",
                12 to "Rest, retreat, expenses, reflection and distant places"
            )

            integratedBhavaRows.sortedByDescending { it.second }.forEach { row ->
                val house = row.first
                val score = row.second
                val theme = houseThemes[house] ?: "General house themes"
                val interpretation = when {
                    score >= 75 -> "Strong score: consider this area a comparatively supportive focus; examine its lord, occupants and aspects for context."
                    score >= 60 -> "Above-average score: potentially constructive indicators; contextual factors may shape how they express."
                    score >= 40 -> "Moderate score: mixed or balanced signals; avoid drawing conclusions from this score alone."
                    score >= 25 -> "Below-average score: review contributing factors and consider this area for mindful effort and support."
                    else -> "Low score: this is a relative prototype signal, not a prediction of failure; inspect the underlying factors carefully."
                }
                result.append("House $house (${rasiNames[(lagnaSign + house - 1) % 12]}) - $score/100\n")
                result.append("Theme: $theme\n")
                result.append("Interpretation: $interpretation\n")
                result.append("-----------------------------\n")
            }
            result.append("\nStep 15 note: interpretations are generated from the Step 14 score bands and general house themes only; they do not yet integrate sign-specific, planetary, Yoga or transit-based narrative rules.\n")

            // STEP 16: PLANET-SPECIFIC BHAVA INTERPRETATION (prototype)
            result.append("\n=============================\n")
            result.append("STEP 16 - PLANET-SPECIFIC BHAVA INTERPRETATION\n")
            result.append("=============================\n\n")
            result.append("Planet placement and house-lord links are descriptive research signals; avoid deterministic conclusions.\n\n")

            val step16SignLords = arrayOf(
                "செவ்வாய்", "சுக்கிரன்", "புதன்", "சந்திரன்", "சூரியன்", "புதன்",
                "சுக்கிரன்", "செவ்வாய்", "குரு", "சனி", "சனி", "குரு"
            )
            val step16PlanetThemes = mapOf(
                "சூரியன்" to "identity, authority, visibility and responsibility",
                "சந்திரன்" to "emotional responses, care, adaptability and public connection",
                "செவ்வாய்" to "initiative, effort, assertiveness and conflict management",
                "புதன்" to "communication, learning, analysis and trade",
                "குரு" to "growth, guidance, meaning, teaching and judgment",
                "சுக்கிரன்" to "relationships, harmony, aesthetics and negotiation",
                "சனி" to "discipline, persistence, boundaries and long-term duties",
                "ராகு" to "novelty, ambition, amplification and unconventional approaches",
                "கேது" to "detachment, specialization, introspection and simplification"
            )
            for (targetHouse in 1..12) {
                val targetSign = (lagnaSign + targetHouse - 1) % 12
                val targetLord = step16SignLords[targetSign]
                val occupants = planetPositions.filter { houseNumber(it.value, lagnaSidereal) == targetHouse }.keys.toList()
                val lordLongitude = planetPositions[targetLord]
                val lordHouse = lordLongitude?.let { houseNumber(it, lagnaSidereal) }
                result.append("House $targetHouse (${rasiNames[targetSign]})\n")
                result.append("Occupying planets: ${if (occupants.isEmpty()) "None" else occupants.joinToString(" | ")}\n")
                if (occupants.isNotEmpty()) {
                    result.append("Planet signals: ")
                    result.append(occupants.joinToString("; ") { planet -> "$planet: ${step16PlanetThemes[planet] ?: "planetary influence"}" })
                    result.append("\n")
                } else {
                    result.append("Planet signals: No direct occupant; consider house lord, aspects and broader chart context.\n")
                }
                result.append("House lord: $targetLord${if (lordHouse != null) " placed in House $lordHouse" else " (position unavailable)"}\n")
                if (lordHouse != null) {
                    result.append("Lord-link prompt: House $targetHouse themes may connect with House $lordHouse topics; use the actual lord condition and chart context before interpreting.\n")
                }
                result.append("Score context: ${integratedBhavaRows.firstOrNull { it.first == targetHouse }?.second ?: "N/A"}/100\n")
                result.append("-----------------------------\n")
            }
            result.append("Step 16 note: this layer lists occupants and lord-house links using the prototype chart data. It is not a predictive guarantee.\n")

            // STEP 17: PLANETARY INFLUENCE INTEGRATION (simplified research layer)
            result.append("\n=============================\n")
            result.append("STEP 17 - PLANETARY INFLUENCE INTEGRATION\n")
            result.append("=============================\n\n")
            result.append("This section combines house occupants, received graha drishti, and house-lord placement into a descriptive signal. Classifications are simplified research heuristics.\n\n")

            val step17Supportive = setOf("குரு", "சுக்கிரன்", "புதன்", "சந்திரன்")
            val step17Challenging = setOf("செவ்வாய்", "சனி", "ராகு", "கேது")
            val step17Rows = mutableListOf<Pair<Int, Int>>()

            for (targetHouse in 1..12) {
                val targetSign = (lagnaSign + targetHouse - 1) % 12
                val targetLord = step16SignLords[targetSign]
                val occupants = planetPositions.filter { houseNumber(it.value, lagnaSidereal) == targetHouse }.keys.toList()
                val receivedFrom = mutableListOf<String>()
                for (sourceName in planetPositions.keys) {
                    val sourceLongitude = planetPositions[sourceName] ?: continue
                    val sourceHouse = houseNumber(sourceLongitude, lagnaSidereal)
                    for (offset in getDrishtiOffsets(sourceName)) {
                        val calculatedTarget = ((sourceHouse + offset - 2) % 12) + 1
                        if (calculatedTarget == targetHouse) {
                            receivedFrom.add(sourceName)
                            break
                        }
                    }
                }
                val lordHouse = planetPositions[targetLord]?.let { houseNumber(it, lagnaSidereal) }
                val supportive = (occupants + receivedFrom).count { it in step17Supportive }
                val challenging = (occupants + receivedFrom).count { it in step17Challenging }
                val balance = (supportive - challenging).coerceIn(-5, 5)
                val combinedScore = ((integratedBhavaRows.firstOrNull { it.first == targetHouse }?.second ?: 50) + balance * 3).coerceIn(0, 100)
                val signal = when {
                    balance >= 2 -> "Supportive influence balance"
                    balance <= -2 -> "Challenging influence balance"
                    else -> "Mixed / neutral influence balance"
                }
                step17Rows.add(targetHouse to combinedScore)

                result.append("House $targetHouse (${rasiNames[targetSign]})\n")
                result.append("Occupants: ${if (occupants.isEmpty()) "None" else occupants.joinToString()}\n")
                result.append("Received drishti: ${if (receivedFrom.isEmpty()) "None" else receivedFrom.joinToString()}\n")
                result.append("Supportive signal count: $supportive | Challenging signal count: $challenging\n")
                result.append("Lord: $targetLord${if (lordHouse != null) " in House $lordHouse" else " (position unavailable)"}\n")
                result.append("Influence balance: $signal | Integrated influence score: $combinedScore/100\n")
                result.append("\n")
            }
            result.append("Step 17 ranking (influence score):\n")
            step17Rows.sortedByDescending { it.second }.forEachIndexed { index, row ->
                result.append("${index + 1}. House ${row.first}: ${row.second}/100\n")
            }
            result.append("\nStep 17 note: this simplified layer uses broad supportive/challenging groupings, not full classical benefic/malefic judgement. Functional lordship, dignity, avastha, exact aspect strength and context remain to be developed.\n")

            // STEP 18: BADHAKA LORD IDENTIFICATION (research layer)
            result.append("\n=============================\n")
            result.append("STEP 18 - BADHAKA LORD ANALYSIS\n")
            result.append("=============================\n\n")

            val step18LagnaType = when (lagnaSign) {
                0, 3, 6, 9 -> "சர லக்னம் (Movable)"
                1, 4, 7, 10 -> "ஸ்திர லக்னம் (Fixed)"
                2, 5, 8, 11 -> "உபய லக்னம் (Dual)"
                else -> "Unknown"
            }
            val step18BadhakaHouse = when (lagnaSign) {
                0, 3, 6, 9 -> 11  // Movable signs
                1, 4, 7, 10 -> 9   // Fixed signs
                2, 5, 8, 11 -> 7   // Dual signs
                else -> 0
            }
            val step18BadhakaSign = if (step18BadhakaHouse in 1..12)
                (lagnaSign + step18BadhakaHouse - 1) % 12 else -1
            val step18BadhakaLord = if (step18BadhakaSign >= 0)
                step16SignLords[step18BadhakaSign] else "Unavailable"
            val step18BadhakaLordLongitude = planetPositions[step18BadhakaLord]
            val step18BadhakaLordHouse = step18BadhakaLordLongitude?.let {
                houseNumber(it, lagnaSidereal)
            }
            val step18BadhakaLordSign = step18BadhakaLordLongitude?.let { signIndex(it) }

            result.append("Lagna: ${rasiNames[lagnaSign]}\n")
            result.append("Lagna type: $step18LagnaType\n")
            result.append("Badhaka sthana: House $step18BadhakaHouse\n")
            if (step18BadhakaSign >= 0) {
                result.append("Badhaka sign: ${rasiNames[step18BadhakaSign]}\n")
            }
            result.append("Badhaka lord: $step18BadhakaLord\n")
            if (step18BadhakaLordHouse != null && step18BadhakaLordSign != null) {
                result.append("Badhaka lord position: ${rasiNames[step18BadhakaLordSign]} (House $step18BadhakaLordHouse)\n")
            } else {
                result.append("Badhaka lord position: unavailable in current planetary-position data\n")
            }
            result.append("Interpretation prompt: assess the Badhaka lord's dignity, conjunctions, aspects, strength and chart context separately; this module identifies the classical Badhaka lord only.\n")
            result.append("Step 18 note: Badhaka identification follows the movable/fixed/dual ascendant rule. It does not automatically label the lord or its house as harmful.\n")


            // STEP 19: TITHI SUNYA IDENTIFICATION (traditional table, research layer)
            result.append("\n=============================\n")
            result.append("STEP 19 - TITHI SUNYA ANALYSIS\n")
            result.append("=============================\n\n")

            val step19SunLongitude = planetPositions["சூரியன்"]!!
            val step19MoonLongitude = planetPositions["சந்திரன்"]!!
            val step19PhaseAngle = normalize(step19MoonLongitude - step19SunLongitude)
            val step19TithiNumber = (step19PhaseAngle / 12.0).toInt() + 1
            val step19Paksha = if (step19TithiNumber <= 15) "வளர்பிறை (Shukla Paksha)" else "தேய்பிறை (Krishna Paksha)"
            val step19TithiWithinPaksha = if (step19TithiNumber <= 15) step19TithiNumber else step19TithiNumber - 15
            val step19TithiNames = listOf("", "பிரதமை", "துவிதியை", "திரிதியை", "சதுர்த்தி", "பஞ்சமி", "ஷஷ்டி", "சப்தமி", "அஷ்டமி", "நவமி", "தசமி", "ஏகாதசி", "துவாதசி", "திரயோதசி", "சதுர்த்தசி", "பௌர்ணமி")
            val step19TithiName = when {
                step19TithiNumber == 15 -> "பௌர்ணமி"
                step19TithiNumber == 30 -> "அமாவாசை"
                else -> step19TithiNames[step19TithiWithinPaksha]
            }

            val step19SunyaTable = mapOf(
                1 to (listOf("மகரம்", "துலாம்") to listOf("சனி", "சுக்கிரன்")),
                2 to (listOf("தனுசு", "மீனம்") to listOf("குரு")),
                3 to (listOf("மகரம்", "சிம்மம்") to listOf("சனி", "சூரியன்")),
                4 to (listOf("கும்பம்", "ரிஷபம்") to listOf("சனி", "சுக்கிரன்")),
                5 to (listOf("மிதுனம்", "கன்னி") to listOf("புதன்")),
                6 to (listOf("மேஷம்", "சிம்மம்") to listOf("செவ்வாய்", "சூரியன்")),
                7 to (listOf("தனுசு", "கடகம்") to listOf("குரு", "சந்திரன்")),
                8 to (listOf("மிதுனம்", "கன்னி") to listOf("புதன்")),
                9 to (listOf("சிம்மம்", "விருச்சிகம்") to listOf("சூரியன்", "செவ்வாய்")),
                10 to (listOf("சிம்மம்", "விருச்சிகம்") to listOf("சூரியன்", "செவ்வாய்")),
                11 to (listOf("தனுசு", "மீனம்") to listOf("குரு")),
                12 to (listOf("மகரம்", "துலாம்") to listOf("சனி", "சுக்கிரன்")),
                13 to (listOf("ரிஷபம்", "சிம்மம்") to listOf("சுக்கிரன்", "சூரியன்")),
                14 to (listOf("மிதுனம்", "கன்னி", "தனுசு", "மீனம்") to listOf("புதன்", "குரு"))
            )
            val step19SunyaEntry = if (step19TithiWithinPaksha in 1..14) step19SunyaTable[step19TithiWithinPaksha] else null
            val step19SunyaSigns = step19SunyaEntry?.first ?: emptyList()
            val step19SunyaPlanets = step19SunyaEntry?.second ?: emptyList()
            val step19ChartPlanetsInSunyaSigns = planetPositions.filter { rasiNames[signIndex(it.value)] in step19SunyaSigns }.keys
            val step19SunyaPlanetPositions = step19SunyaPlanets.mapNotNull { name -> planetPositions[name]?.let { name to rasiNames[signIndex(it)] } }

            result.append("Moon-Sun phase angle: ${String.format(Locale.US, "%.2f", step19PhaseAngle)} degrees\n")
            result.append("Tithi: $step19TithiName | $step19Paksha | Paksha tithi #$step19TithiWithinPaksha\n")
            if (step19SunyaEntry == null) {
                result.append("Tithi Sunya table entry: Not applied for Purnima/Amavasya in this table.\n")
            } else {
                result.append("Tithi Sunya signs: ${step19SunyaSigns.joinToString()}\n")
                result.append("Tithi Sunya planets: ${step19SunyaPlanets.joinToString()}\n")
                result.append("Chart planets placed in Tithi Sunya signs: ${if (step19ChartPlanetsInSunyaSigns.isEmpty()) "None" else step19ChartPlanetsInSunyaSigns.joinToString()}\n")
                result.append("Tithi Sunya planet positions: ${if (step19SunyaPlanetPositions.isEmpty()) "None" else step19SunyaPlanetPositions.joinToString { "${it.first} in ${it.second}" }}\n")
            }
            result.append("Note: This is a table-based traditional interpretation prototype. It identifies listed signs/planets; it does not automatically reduce scores or assert effects. Validate the table tradition before using for predictions.\n")

            // STEP 20: MUDakku RASI (experimental counting method)
            result.append("\n=============================\n")
            result.append("STEP 20 - MUDakku RASI ANALYSIS (EXPERIMENTAL)\n")
            result.append("=============================\n\n")

            val step20SunSignIndex = signIndex(planetPositions["சூரியன்"]!!)
            val step20SagittariusIndex = 8 // தனுசு, zodiac index 8
            val step20CountFromSunToSagittarius = ((step20SagittariusIndex - step20SunSignIndex + 12) % 12) + 1
            val step20MudakkuSignIndex = (step20SagittariusIndex + step20CountFromSunToSagittarius - 1) % 12
            val step20MudakkuSign = rasiNames[step20MudakkuSignIndex]
            val step20MudakkuLord = step16SignLords[step20MudakkuSignIndex]
            val step20PlanetsInMudakkuSign = planetPositions.filter { signIndex(it.value) == step20MudakkuSignIndex }.keys
            val step20MudakkuLordLongitude = planetPositions[step20MudakkuLord]
            val step20MudakkuLordSign = step20MudakkuLordLongitude?.let { rasiNames[signIndex(it)] } ?: "Unavailable"
            val step20MudakkuLordHouse = step20MudakkuLordLongitude?.let { houseNumber(it, lagnaSidereal) }

            result.append("Sun sign: ${rasiNames[step20SunSignIndex]}\n")
            result.append("Inclusive count from Sun sign to Sagittarius: $step20CountFromSunToSagittarius\n")
            result.append("Experimental Mudakku sign: $step20MudakkuSign\n")
            result.append("Mudakku sign lord: $step20MudakkuLord\n")
            result.append("Planets in Mudakku sign: ${if (step20PlanetsInMudakkuSign.isEmpty()) "None" else step20PlanetsInMudakkuSign.joinToString()}\n")
            result.append("Mudakku lord sign: $step20MudakkuLordSign\n")
            result.append("Mudakku lord house: ${step20MudakkuLordHouse ?: "Unavailable"}\n")
            result.append("Note: This is an experimental sign-counting method based on the previously discussed rule; validate against the intended tradition before interpretive use.\n")

            // STEP 21: KARANA (PANCHANGA) CALCULATION
            result.append("\nSTEP 21 - KARANA ANALYSIS\n")
            result.append("-----------------------------\n")

            val step21SunLongitude = tropicalPositions["சூரியன்"]
                ?: throw Exception("Sun longitude unavailable for Karana")
            val step21MoonLongitude = tropicalPositions["சந்திரன்"]
                ?: throw Exception("Moon longitude unavailable for Karana")
            val step21Elongation = normalize(step21MoonLongitude - step21SunLongitude)
            val step21KaranaIndex = (step21Elongation / 6.0).toInt().coerceIn(0, 59)
            val step21RepeatingKaranas = listOf(
                "Bava", "Balava", "Kaulava", "Taitila", "Garaja", "Vanija", "Vishti"
            )
            val step21KaranaName = when (step21KaranaIndex) {
                0 -> "Kimstughna"
                in 1..56 -> step21RepeatingKaranas[(step21KaranaIndex - 1) % 7]
                57 -> "Shakuni"
                58 -> "Chatushpada"
                59 -> "Nagava (Naga)"
                else -> "Unavailable"
            }
            result.append("Sun-Moon elongation: %.3f degrees\n".format(Locale.US, step21Elongation))
            result.append("Karana interval index (0-59): $step21KaranaIndex\n")
            result.append("Karana: $step21KaranaName\n")
            result.append("Note: Karana is calculated from tropical Sun-Moon elongation; this is a Panchanga name identification layer, not an interpretive score.\n")


            // STEP 22: COMBINED FACTOR SUMMARY (DESCRIPTIVE ONLY)
            result.append("\n=============================\n")
            result.append("STEP 22 - COMBINED ASTROLOGICAL FACTOR SUMMARY\n")
            result.append("=============================\n\n")
            result.append("This summary consolidates the previously calculated factors without applying a combined penalty or prediction score.\n\n")

            result.append("1. BADHAKA FACTOR\n")
            result.append("Lagna type: $step18LagnaType\n")
            result.append("Badhaka house: $step18BadhakaHouse\n")
            result.append("Badhaka sign: ${if (step18BadhakaSign >= 0) rasiNames[step18BadhakaSign] else "Unavailable"}\n")
            result.append("Badhaka lord: $step18BadhakaLord\n")
            result.append("Badhaka lord house: ${step18BadhakaLordHouse ?: "Unavailable"}\n\n")

            result.append("2. TITHI SUNYA FACTOR\n")
            result.append("Tithi: $step19TithiName ($step19Paksha)\n")
            result.append("Sunya signs: ${if (step19SunyaSigns.isEmpty()) "Not applied / unavailable" else step19SunyaSigns.joinToString()}\n")
            result.append("Planets in Sunya signs: ${if (step19ChartPlanetsInSunyaSigns.isEmpty()) "None detected" else step19ChartPlanetsInSunyaSigns.joinToString()}\n")
            result.append("Sunya planets and positions: ${if (step19SunyaPlanetPositions.isEmpty()) "None / not applicable" else step19SunyaPlanetPositions.joinToString { "${it.first} in ${it.second}" }}\n\n")

            result.append("3. MUDakku FACTOR (EXPERIMENTAL)\n")
            result.append("Mudakku sign: $step20MudakkuSign\n")
            result.append("Mudakku sign lord: $step20MudakkuLord\n")
            result.append("Planets in Mudakku sign: ${if (step20PlanetsInMudakkuSign.isEmpty()) "None" else step20PlanetsInMudakkuSign.joinToString()}\n")
            result.append("Mudakku lord sign: $step20MudakkuLordSign\n")
            result.append("Mudakku lord house: ${step20MudakkuLordHouse ?: "Unavailable"}\n\n")

            result.append("4. KARANA FACTOR\n")
            result.append("Karana: $step21KaranaName (index $step21KaranaIndex)\n")
            result.append("Sun-Moon elongation: %.3f degrees\n".format(Locale.US, step21Elongation))
            result.append("\nCombined summary note: these are separate descriptive factors. No combined score or automatic adverse interpretation is applied. Validate tradition-specific rules before predictive use.\n")

            // STEP 23: 12 BHAVA FACTOR ASSOCIATION (DESCRIPTIVE, NO SCORE)
            result.append("\n=============================\n")
            result.append("STEP 23 - 12 BHAVA FACTOR ASSOCIATION\n")
            result.append("=============================\n\n")
            result.append("This layer maps sign-based factors to houses from the current Lagna. It does not alter scores.\n\n")

            val step23HouseBySignName = rasiNames.indices.associate { rasiNames[it] to ((it - lagnaSign + 12) % 12 + 1) }

            result.append("1. BADHAKA HOUSE ASSOCIATION\n")
            result.append("Badhaka house: $step18BadhakaHouse | Sign: ${if (step18BadhakaSign >= 0) rasiNames[step18BadhakaSign] else "Unavailable"}\n")
            result.append("Mapped bhava: ${step23HouseBySignName.entries.firstOrNull { it.value == step18BadhakaHouse }?.key ?: "Unavailable"} (House $step18BadhakaHouse)\n\n")

            result.append("2. TITHI SUNYA SIGN ASSOCIATIONS\n")
            if (step19SunyaSigns.isEmpty()) {
                result.append("No Sunya signs available from the selected table.\n")
            } else {
                for (sunyaSign in step19SunyaSigns) {
                    val mappedHouse = step23HouseBySignName[sunyaSign]
                    val occupantsInSign = planetPositions.filter { rasiNames[signIndex(it.value)] == sunyaSign }.keys
                    result.append("$sunyaSign -> House ${mappedHouse ?: "Unavailable"} | Occupants: ${if (occupantsInSign.isEmpty()) "None" else occupantsInSign.joinToString()}\n")
                }
            }
            result.append("\n")

            result.append("3. MUDakku SIGN ASSOCIATION\n")
            val step23MudakkuHouse = step23HouseBySignName[step20MudakkuSign]
            result.append("Mudakku sign: $step20MudakkuSign -> House ${step23MudakkuHouse ?: "Unavailable"}\n")
            result.append("Planets in Mudakku sign: ${if (step20PlanetsInMudakkuSign.isEmpty()) "None" else step20PlanetsInMudakkuSign.joinToString()}\n")
            result.append("Mudakku lord: $step20MudakkuLord | Lord house: ${step20MudakkuLordHouse ?: "Unavailable"}\n\n")

            result.append("4. KARANA ASSOCIATION\n")
            result.append("Karana: $step21KaranaName | Panchanga factor recorded as a general factor; no bhava mapping is applied without a defined tradition-specific rule.\n\n")

            result.append("5. ASSOCIATED BHAVAS - SUMMARY\n")
            val step23AssociatedHouseSet = linkedSetOf<Int>()
            if (step18BadhakaHouse in 1..12) step23AssociatedHouseSet.add(step18BadhakaHouse)
            for (sunyaSign in step19SunyaSigns) {
                step23HouseBySignName[sunyaSign]?.let { step23AssociatedHouseSet.add(it) }
            }
            step23MudakkuHouse?.let { step23AssociatedHouseSet.add(it) }
            if (step23AssociatedHouseSet.isEmpty()) {
                result.append("No house associations could be mapped.\n")
            } else {
                result.append(step23AssociatedHouseSet.sorted().joinToString { "House $it" })
                result.append("\n")
                result.append("These are factor associations only, not adverse or beneficial judgments.\n")
            }

            // STEP 24: BHAVA FACTOR CONSOLIDATION (COUNT DISTINCT FACTORS)
            result.append("\n=============================\n")
            result.append("STEP 24 - BHAVA FACTOR CONSOLIDATION\n")
            result.append("=============================\n\n")
            result.append("This layer consolidates distinct factors by bhava. It does not apply scores or predictive judgments.\n\n")

            val step24FactorsByHouse = mutableMapOf<Int, MutableSet<String>>()
            if (step18BadhakaHouse in 1..12) {
                step24FactorsByHouse.getOrPut(step18BadhakaHouse) { linkedSetOf() }.add("Badhaka")
            }
            for (sunyaSign in step19SunyaSigns) {
                step23HouseBySignName[sunyaSign]?.let { house ->
                    step24FactorsByHouse.getOrPut(house) { linkedSetOf() }.add("Tithi Sunya")
                }
            }
            step23MudakkuHouse?.let { house ->
                step24FactorsByHouse.getOrPut(house) { linkedSetOf() }.add("Mudakku")
            }

            for (house in 1..12) {
                val factors = step24FactorsByHouse[house] ?: emptySet()
                result.append("House $house: ")
                result.append(if (factors.isEmpty()) "None" else factors.joinToString())
                result.append("\nDistinct factor count: ${factors.size}\n\n")
            }
            result.append("Karana: ${step21KaranaName} (recorded separately; not assigned to a bhava in this prototype).\n")
            result.append("Step 24 note: factor counts represent distinct factor types only. They are not severity, strength, or prediction scores.\n")

            // STEP 25: BHAVA FACTOR CONTEXT MATRIX
            result.append("\n=============================\n")
            result.append("STEP 25 - BHAVA FACTOR CONTEXT MATRIX\n")
            result.append("=============================\n\n")
            result.append("Descriptive context matrix for all 12 bhavas. No new scoring or prediction is applied.\n\n")

            for (house in 1..12) {
                val signIndexForHouse = (lagnaSign + house - 1) % 12
                val signNameForHouse = rasiNames[signIndexForHouse]
                val lordNameForHouse = signLords[signIndexForHouse]
                val occupantsForHouse = planetPositions.filter { houseNumber(it.value, lagnaSidereal) == house }.keys
                val factorsForHouse = step24FactorsByHouse[house] ?: emptySet()
                val lordLongitudeForHouse = planetPositions[lordNameForHouse]
                val lordSignForHouse = lordLongitudeForHouse?.let { rasiNames[signIndex(it)] } ?: "Unavailable"
                val lordHouseForHouse = lordLongitudeForHouse?.let { houseNumber(it, lagnaSidereal) }

                result.append("House $house\n")
                result.append("Rasi: $signNameForHouse\n")
                result.append("Occupants: ${if (occupantsForHouse.isEmpty()) "None" else occupantsForHouse.joinToString()}\n")
                result.append("Bhava Lord: $lordNameForHouse\n")
                result.append("Lord position: ${lordSignForHouse} | House ${lordHouseForHouse ?: "Unavailable"}\n")
                result.append("Associated Factors: ${if (factorsForHouse.isEmpty()) "None" else factorsForHouse.joinToString()}\n")
                result.append("Distinct Factor Count: ${factorsForHouse.size}\n\n")
            }
            result.append("Step 25 note: this matrix is descriptive context only; it does not imply severity or predictive certainty.\n")

            // COMPLETION

            result.append("\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Step 6: Graha Drishti included\n")
            result.append("Step 7: Bhava Drishti Received Analysis included\n")
            result.append("Step 12: Planet Strength Ranking included\n")
            result.append("Step 13: Integrated Horoscope Analysis included\nStep 14: Integrated Bhava Prediction Score included\nStep 15: Prediction Interpretation Layer included\nStep 16: Planet-specific Bhava Interpretation included\nStep 17: Planetary Influence Integration included\nStep 18: Badhaka Lord Analysis included\nStep 19: Tithi Sunya Analysis included\nStep 20: Experimental Mudakku Rasi Analysis included\nStep 21: Karana Calculation included\nStep 22: Combined Astrological Factor Summary included\nStep 23: 12 Bhava Factor Association included\nStep 24: Bhava Factor Consolidation included\n")

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
