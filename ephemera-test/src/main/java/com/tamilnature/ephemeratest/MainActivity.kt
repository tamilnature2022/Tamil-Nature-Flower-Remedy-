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
import kotlin.math.roundToInt

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


    private fun appendStep70Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        // STEP 70: GLOBAL SHELL INTERACTION NETWORK ANALYSIS
        result.append("\n=============================\n")
        result.append("STEP 70 - GLOBAL SHELL INTERACTION NETWORK ANALYSIS\n")
        result.append("=============================\n\n")
        val step70ShellSet = step50CoreNumbers.values.toSet().sorted()
        val step70PairWeights = mutableMapOf<Pair<Int, Int>, Int>()
        step67CrossShellEdges.forEach { (step70A, step70B) ->
            val step70ShellA = step50CoreNumbers[step70A] ?: 0
            val step70ShellB = step50CoreNumbers[step70B] ?: 0
            if (step70ShellA != step70ShellB) {
                val step70Pair = if (step70ShellA < step70ShellB) step70ShellA to step70ShellB else step70ShellB to step70ShellA
                step70PairWeights[step70Pair] = (step70PairWeights[step70Pair] ?: 0) + 1
            }
        }
        val step70ShellDegree = step70ShellSet.associateWith { step70Shell ->
            step70PairWeights.keys.count { step70Shell == it.first || step70Shell == it.second }
        }
        val step70ShellWeightedDegree = step70ShellSet.associateWith { step70Shell ->
            step70PairWeights.filterKeys { step70Shell == it.first || step70Shell == it.second }.values.sum()
        }
        result.append("Shell-network node profile (shell | nodes | interaction partners | weighted interaction degree):\n")
        step70ShellSet.forEach { step70Shell ->
            val step70NodeCount = step50CoreNumbers.count { it.value == step70Shell }
            result.append("$step70Shell | $step70NodeCount | ${step70ShellDegree[step70Shell] ?: 0} | ${step70ShellWeightedDegree[step70Shell] ?: 0}\n")
        }
        result.append("\nWeighted shell-interaction network edges (shell pair | cross-shell links):\n")
        step70PairWeights.entries.sortedWith(compareBy({ it.key.first }, { it.key.second })).forEach { (step70Pair, step70Weight) ->
            result.append("${step70Pair.first} - ${step70Pair.second} | $step70Weight\n")
        }
        val step70PossiblePairs = if (step70ShellSet.size > 1) step70ShellSet.size * (step70ShellSet.size - 1) / 2 else 0
        val step70ObservedPairs = step70PairWeights.size
        val step70NetworkDensity = if (step70PossiblePairs > 0) step70ObservedPairs.toDouble() / step70PossiblePairs else 0.0
        val step70TotalCrossLinks = step70PairWeights.values.sum()
        val step70MostConnectedShell = step70ShellSet.maxByOrNull { step70ShellDegree[it] ?: 0 }
        result.append("\nGlobal shell network summary:\n")
        result.append("Shell levels | ${step70ShellSet.size}\n")
        result.append("Connected shell-pairs | $step70ObservedPairs\n")
        result.append("Possible shell-pairs | $step70PossiblePairs\n")
        result.append("Shell-network density | ${String.format(Locale.US, "%.4f", step70NetworkDensity)}\n")
        result.append("Total cross-shell links | $step70TotalCrossLinks\n")
        result.append("Most interaction-connected shell | ${step70MostConnectedShell ?: "N/A"}\n")
        result.append("\nThis is a descriptive shell-level network summary; cross-shell link counts do not imply causal influence.\n")
    }


    private fun appendStep71Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 71 - SHELL INTERACTION INTENSITY & PARTICIPATION ANALYSIS\n")
        result.append("=============================\n\n")
        val shells = step50CoreNumbers.values.toSet().sorted()
        val pairCounts = mutableMapOf<Pair<Int, Int>, Int>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                val pair = if (sa < sb) sa to sb else sb to sa
                pairCounts[pair] = (pairCounts[pair] ?: 0) + 1
            }
        }
        result.append("Shell-pair interaction intensity (pair | links | possible node pairs | normalized intensity):\n")
        val pairIntensity = pairCounts.entries.map { (pair, count) ->
            val nA = step50CoreNumbers.count { it.value == pair.first }
            val nB = step50CoreNumbers.count { it.value == pair.second }
            val possible = nA * nB
            val intensity = if (possible > 0) count.toDouble() / possible else 0.0
            Triple(pair, count, intensity)
        }.sortedWith(compareByDescending<Triple<Pair<Int, Int>, Int, Double>> { it.third }.thenByDescending { it.second })
        pairIntensity.forEach { (pair, count, intensity) ->
            val possible = (step50CoreNumbers.count { it.value == pair.first }) * (step50CoreNumbers.count { it.value == pair.second })
            result.append("${pair.first}-${pair.second} | $count | $possible | ${String.format(Locale.US, "%.4f", intensity)}\n")
        }
        val totalLinks = pairCounts.values.sum()
        result.append("\nShell participation profile (shell | cross-shell links | share of cross-shell links):\n")
        shells.forEach { shell ->
            val incident = pairCounts.filterKeys { shell == it.first || shell == it.second }.values.sum()
            val share = if (totalLinks > 0) incident.toDouble() / (2.0 * totalLinks) else 0.0
            result.append("$shell | $incident | ${String.format(Locale.US, "%.4f", share)}\n")
        }
        val strongest = pairIntensity.firstOrNull()
        result.append("\nHighest normalized shell-pair intensity | ${strongest?.first ?: "N/A"} | ${strongest?.third?.let { String.format(Locale.US, "%.4f", it) } ?: "N/A"}\n")
        result.append("This analysis describes normalized cross-shell connectivity, not causal influence.\n")
    }


    private fun appendStep72Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 72 - SHELL INTERACTION DISTRIBUTION & BALANCE ANALYSIS\n")
        result.append("=============================\n\n")
        val shellSizes = step50CoreNumbers.values.groupingBy { it }.eachCount().toSortedMap()
        val pairCounts = mutableMapOf<Pair<Int, Int>, Int>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                val pair = if (sa < sb) sa to sb else sb to sa
                pairCounts[pair] = (pairCounts[pair] ?: 0) + 1
            }
        }
        val total = pairCounts.values.sum()
        result.append("Cross-shell interaction distribution (shell pair | links | percentage):\n")
        pairCounts.entries.sortedByDescending { it.value }.forEach { (pair, count) ->
            val pct = if (total > 0) count.toDouble() / total * 100.0 else 0.0
            result.append("${pair.first}-${pair.second} | $count | ${String.format(Locale.US, "%.2f", pct)}%\n")
        }
        val shares = pairCounts.values.map { if (total > 0) it.toDouble() / total else 0.0 }
        val hhi = shares.sumOf { it * it }
        val entropy = if (shares.size > 1) -shares.filter { it > 0 }.sumOf { it * kotlin.math.ln(it) } else 0.0
        val maxEntropy = if (shares.size > 1) kotlin.math.ln(shares.size.toDouble()) else 0.0
        val normalizedEntropy = if (maxEntropy > 0) entropy / maxEntropy else 0.0
        result.append("\nDistribution metrics:\n")
        result.append("Total cross-shell links | $total\n")
        result.append("Pair concentration (HHI) | ${String.format(Locale.US, "%.4f", hhi)}\n")
        result.append("Normalized distribution entropy | ${String.format(Locale.US, "%.4f", normalizedEntropy)}\n")
        result.append("\nShell-pair opportunity-normalized balance (pair | observed | possible | rate):\n")
        pairCounts.entries.sortedWith(compareBy<Map.Entry<Pair<Int, Int>, Int>> { it.key.first }.thenBy { it.key.second }).forEach { (pair, count) ->
            val possible = (shellSizes[pair.first] ?: 0) * (shellSizes[pair.second] ?: 0)
            val rate = if (possible > 0) count.toDouble() / possible else 0.0
            result.append("${pair.first}-${pair.second} | $count | $possible | ${String.format(Locale.US, "%.4f", rate)}\n")
        }
        result.append("\nInterpretation: higher entropy indicates links distributed across more shell-pairs; HHI reflects concentration. These are descriptive network statistics, not causal measures.\n")
    }

    private fun appendStep73Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 73 - NODE SHELL INTERACTION CENTRALITY & PARTICIPATION ANALYSIS\n")
        result.append("=============================\n\n")
        val nodeShellLinks = mutableMapOf<String, MutableMap<Int, Int>>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                nodeShellLinks.getOrPut(a) { mutableMapOf() }[sb] = (nodeShellLinks[a]?.get(sb) ?: 0) + 1
                nodeShellLinks.getOrPut(b) { mutableMapOf() }[sa] = (nodeShellLinks[b]?.get(sa) ?: 0) + 1
            }
        }
        val totals = nodeShellLinks.mapValues { (_, links) -> links.values.sum() }
        result.append("Node shell-interaction centrality (node | own shell | cross-shell links | shells reached):\n")
        totals.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).forEach { (node, count) ->
            val ownShell = step50CoreNumbers[node] ?: 0
            val reached = nodeShellLinks[node]?.keys?.sorted()?.joinToString(", ") ?: ""
            result.append("$node | $ownShell | $count | $reached\n")
        }
        val maxLinks = totals.values.maxOrNull() ?: 0
        val mean = if (totals.isNotEmpty()) totals.values.average() else 0.0
        val topShare = if (totals.values.sum() > 0) maxLinks.toDouble() / totals.values.sum() else 0.0
        val shellReach = nodeShellLinks.values.map { it.keys.size }
        result.append("\nNode interaction summary metrics:\n")
        result.append("Active boundary nodes | ${totals.size}\n")
        result.append("Mean cross-shell links per active node | ${String.format(Locale.US, "%.3f", mean)}\n")
        result.append("Maximum node interaction count | $maxLinks\n")
        result.append("Top node share of counted node-link incidences | ${String.format(Locale.US, "%.4f", topShare)}\n")
        result.append("Mean distinct shell levels reached | ${String.format(Locale.US, "%.3f", if (shellReach.isNotEmpty()) shellReach.average() else 0.0)}\n")
        result.append("\nInterpretation: these rankings describe cross-shell connectivity participation only and should not be interpreted as causal or predictive evidence.\n")
    }

    private fun appendStep74Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 74 - NODE CROSS-SHELL INTERACTION DIVERSITY ANALYSIS\n")
        result.append("=============================\n\n")
        val linksByNode = mutableMapOf<String, MutableMap<Int, Int>>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                linksByNode.getOrPut(a) { mutableMapOf() }[sb] = (linksByNode[a]?.get(sb) ?: 0) + 1
                linksByNode.getOrPut(b) { mutableMapOf() }[sa] = (linksByNode[b]?.get(sa) ?: 0) + 1
            }
        }
        fun entropy(counts: Collection<Int>): Double {
            val total = counts.sum().toDouble()
            if (total <= 0.0 || counts.size <= 1) return 0.0
            val raw = counts.map { it / total }.sumOf { -it * kotlin.math.ln(it) }
            return raw / kotlin.math.ln(counts.size.toDouble())
        }
        result.append("Node | own shell | total cross-shell links | distinct shells | normalized interaction entropy\n")
        val nodeProfiles = linksByNode.entries.map { (node, shellCounts) ->
            val total = shellCounts.values.sum()
            Triple(node, shellCounts, entropy(shellCounts.values)) to total
        }.sortedWith(compareByDescending<Pair<Triple<String, MutableMap<Int, Int>, Double>, Int>> { it.second }.thenBy { it.first.first })
        nodeProfiles.forEach { (profile, total) ->
            val (node, shellCounts, ent) = profile
            result.append("$node | ${step50CoreNumbers[node] ?: 0} | $total | ${shellCounts.keys.size} | ${String.format(Locale.US, "%.4f", ent)}\n")
        }
        val shellGroups = step50CoreNumbers.keys.groupBy { step50CoreNumbers[it] ?: 0 }.toSortedMap()
        result.append("\nOwn-shell aggregate (shell | active nodes | mean normalized diversity):\n")
        shellGroups.forEach { (shell, nodes) ->
            val active = nodes.mapNotNull { linksByNode[it] }.filter { it.isNotEmpty() }
            val avg = if (active.isNotEmpty()) active.map { entropy(it.values) }.average() else 0.0
            result.append("$shell | ${active.size} | ${String.format(Locale.US, "%.4f", avg)}\n")
        }
        result.append("\nNormalized entropy reflects how evenly a node's cross-shell links are distributed among the shell levels it reaches; it is descriptive, not causal or predictive.\n")
    }


    private fun appendStep75Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 75 - CROSS-SHELL EDGE DENSITY & OCCUPANCY PROFILE\n")
        result.append("=============================\n\n")
        val shellNodes = step50CoreNumbers.keys.groupBy { step50CoreNumbers[it] ?: 0 }.toSortedMap()
        val pairCounts = mutableMapOf<Pair<Int, Int>, Int>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                val pair = if (sa < sb) Pair(sa, sb) else Pair(sb, sa)
                pairCounts[pair] = (pairCounts[pair] ?: 0) + 1
            }
        }
        result.append("Shell pair | nodes in shell A | nodes in shell B | observed cross-shell links | possible pairs | density\n")
        pairCounts.entries.sortedWith(Comparator { a, b ->
            val firstCompare = a.key.first.compareTo(b.key.first)
            if (firstCompare != 0) firstCompare else a.key.second.compareTo(b.key.second)
        }).forEach { (pair, count) ->
            val nA = shellNodes[pair.first]?.size ?: 0
            val nB = shellNodes[pair.second]?.size ?: 0
            val possible = nA.toLong() * nB.toLong()
            val density = if (possible > 0L) count.toDouble() / possible.toDouble() else 0.0
            result.append("${pair.first}-${pair.second} | $nA | $nB | $count | $possible | ${String.format(Locale.US, "%.5f", density)}\n")
        }
        val densities = pairCounts.map { (pair, count) ->
            val possible = (shellNodes[pair.first]?.size ?: 0).toLong() * (shellNodes[pair.second]?.size ?: 0).toLong()
            if (possible > 0L) count.toDouble() / possible.toDouble() else 0.0
        }
        result.append("\nObserved shell-pair combinations | ${pairCounts.size}\n")
        result.append("Mean density among observed shell pairs | ${String.format(Locale.US, "%.5f", if (densities.isNotEmpty()) densities.average() else 0.0)}\n")
        result.append("\nDensity is a structural occupancy measure for the derived graph and is descriptive, not causal or predictive.\n")
    }

    private fun appendStep76Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 76 - SHELL GAP CONNECTIVITY PROFILE\n")
        result.append("=============================\n\n")
        val shellNodes = step50CoreNumbers.keys.groupBy { step50CoreNumbers[it] ?: 0 }.toSortedMap()
        val edgeCounts = mutableMapOf<Int, Int>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                val gap = kotlin.math.abs(sa - sb)
                edgeCounts[gap] = (edgeCounts[gap] ?: 0) + 1
            }
        }
        val possibleByGap = mutableMapOf<Int, Long>()
        val shellIds = shellNodes.keys.toList()
        for (i in shellIds.indices) {
            for (j in i + 1 until shellIds.size) {
                val gap = kotlin.math.abs(shellIds[i] - shellIds[j])
                val possible = (shellNodes[shellIds[i]]?.size ?: 0).toLong() * (shellNodes[shellIds[j]]?.size ?: 0).toLong()
                possibleByGap[gap] = (possibleByGap[gap] ?: 0L) + possible
            }
        }
        result.append("Shell gap | observed links | possible node pairs | occupancy density | link share\n")
        val totalEdges = edgeCounts.values.sum()
        edgeCounts.keys.union(possibleByGap.keys).sorted().forEach { gap ->
            val count = edgeCounts[gap] ?: 0
            val possible = possibleByGap[gap] ?: 0L
            val density = if (possible > 0L) count.toDouble() / possible.toDouble() else 0.0
            val share = if (totalEdges > 0) count.toDouble() / totalEdges.toDouble() else 0.0
            result.append("$gap | $count | $possible | ${String.format(Locale.US, "%.5f", density)} | ${String.format(Locale.US, "%.5f", share)}\n")
        }
        result.append("\nTotal cross-shell links | $totalEdges\n")
        result.append("\nShell-gap occupancy is a descriptive graph statistic and does not establish causal or predictive relationships.\n")
    }


    private fun appendStep77Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 77 - SHELL GAP LINK SHARE & ENTROPY PROFILE\n")
        result.append("=============================\n\n")
        val gapCounts = mutableMapOf<Int, Int>()
        step67CrossShellEdges.forEach { (a, b) ->
            val sa = step50CoreNumbers[a] ?: 0
            val sb = step50CoreNumbers[b] ?: 0
            if (sa != sb) {
                val gap = kotlin.math.abs(sa - sb)
                gapCounts[gap] = (gapCounts[gap] ?: 0) + 1
            }
        }
        val total = gapCounts.values.sum()
        result.append("Shell gap | link count | link share | contribution to entropy\n")
        var entropy = 0.0
        gapCounts.toSortedMap().forEach { (gap, count) ->
            val share = if (total > 0) count.toDouble() / total.toDouble() else 0.0
            val contribution = if (share > 0.0) -share * kotlin.math.ln(share) else 0.0
            entropy += contribution
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${String.format(Locale.US, "%.5f", contribution)}\n")
        }
        val normalized = if (gapCounts.size > 1) entropy / kotlin.math.ln(gapCounts.size.toDouble()) else 0.0
        result.append("\nTotal cross-shell links | $total\n")
        result.append("Shell-gap Shannon entropy | ${String.format(Locale.US, "%.5f", entropy)}\n")
        result.append("Normalized shell-gap entropy | ${String.format(Locale.US, "%.5f", normalized)}\n")
        result.append("\nEntropy describes the distribution of cross-shell links across gap sizes; it is not causal or predictive.\n")
    }


    private fun appendStep78Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 78 - SHELL GAP WEIGHTED DISTANCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        val total = gaps.size
        if (total == 0) {
            result.append("No cross-shell links available for shell-gap distance statistics.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val sorted = gaps.sorted()
        val mean = sorted.average()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble() else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        val variance = sorted.map { (it - mean) * (it - mean) }.average()
        val counts = sorted.groupingBy { it }.eachCount().toSortedMap()
        result.append("Shell gap | link count | share | weighted distance contribution\n")
        counts.forEach { (gap, count) ->
            val share = count.toDouble() / total.toDouble()
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${String.format(Locale.US, "%.5f", gap * share)}\n")
        }
        result.append("\nCross-shell link count | $total\n")
        result.append("Mean shell gap (link-weighted) | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Median shell gap | ${String.format(Locale.US, "%.5f", median)}\n")
        result.append("Shell-gap variance | ${String.format(Locale.US, "%.5f", variance)}\n")
        result.append("Maximum observed shell gap | ${sorted.maxOrNull() ?: 0}\n")
        result.append("\nThese statistics summarize observed shell-level distances in the derived graph and do not imply causal or predictive relationships.\n")
    }



    private fun appendStep79Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 79 - SHELL GAP CUMULATIVE DISTRIBUTION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }.sorted()
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for cumulative gap distribution.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val total = gaps.size
        val counts = gaps.groupingBy { it }.eachCount().toSortedMap()
        result.append("Shell gap | count | share | cumulative count | cumulative share\n")
        var cumulative = 0
        counts.forEach { (gap, count) ->
            cumulative += count
            val share = count.toDouble() / total.toDouble()
            val cumulativeShare = cumulative.toDouble() / total.toDouble()
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | $cumulative | ${String.format(Locale.US, "%.5f", cumulativeShare)}\n")
        }
        fun percentile(p: Double): Int = gaps[(kotlin.math.ceil(p * total).toInt().coerceAtLeast(1) - 1).coerceAtMost(total - 1)]
        result.append("\nCross-shell link count | $total\n")
        result.append("Gap median | ${percentile(0.50)}\n")
        result.append("Gap 75th percentile | ${percentile(0.75)}\n")
        result.append("Gap 90th percentile | ${percentile(0.90)}\n")
        result.append("Gap 95th percentile | ${percentile(0.95)}\n")
        result.append("\nCumulative distribution summarizes observed shell-gap distances and is descriptive, not causal or predictive.\n")
    }

    private fun appendStep80Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 80 - SHELL GAP DISPERSION AND SHAPE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for dispersion and shape analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val mean = gaps.average()
        val variance = gaps.map { (it - mean) * (it - mean) }.average()
        val sd = kotlin.math.sqrt(variance)
        val thirdMoment = gaps.map { (it - mean) * (it - mean) * (it - mean) }.average()
        val fourthMoment = gaps.map { val d = it - mean; d * d * d * d }.average()
        val skewness = if (sd > 0.0) thirdMoment / (sd * sd * sd) else 0.0
        val excessKurtosis = if (variance > 0.0) fourthMoment / (variance * variance) - 3.0 else 0.0
        val sorted = gaps.sorted()
        val range = sorted.last() - sorted.first()
        result.append("Observed cross-shell link count | ${gaps.size}\n")
        result.append("Mean shell gap | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Standard deviation | ${String.format(Locale.US, "%.5f", sd)}\n")
        result.append("Variance | ${String.format(Locale.US, "%.5f", variance)}\n")
        result.append("Gap range (max-min) | ${String.format(Locale.US, "%.5f", range)}\n")
        result.append("Skewness | ${String.format(Locale.US, "%.5f", skewness)}\n")
        result.append("Excess kurtosis | ${String.format(Locale.US, "%.5f", excessKurtosis)}\n")
        result.append("\nThese descriptive distribution-shape measures summarize observed shell gaps only; they do not imply causal or predictive relationships.\n")
    }



    private fun appendStep81Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 81 - SHELL GAP QUARTILE AND ROBUST SPREAD PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }.sorted()
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for quartile analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        fun percentile(p: Double): Double {
            val index = (p * (gaps.size - 1)).coerceIn(0.0, (gaps.size - 1).toDouble())
            val lower = index.toInt()
            val upper = kotlin.math.ceil(index).toInt()
            val fraction = index - lower
            return gaps[lower] * (1.0 - fraction) + gaps[upper] * fraction
        }
        val q1 = percentile(0.25)
        val median = percentile(0.50)
        val q3 = percentile(0.75)
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr
        val outliers = gaps.filter { it < lowerFence || it > upperFence }
        result.append("Observed cross-shell links | ${gaps.size}\n")
        result.append("First quartile (Q1) | ${String.format(Locale.US, "%.5f", q1)}\n")
        result.append("Median (Q2) | ${String.format(Locale.US, "%.5f", median)}\n")
        result.append("Third quartile (Q3) | ${String.format(Locale.US, "%.5f", q3)}\n")
        result.append("Interquartile range (IQR) | ${String.format(Locale.US, "%.5f", iqr)}\n")
        result.append("Lower Tukey fence | ${String.format(Locale.US, "%.5f", lowerFence)}\n")
        result.append("Upper Tukey fence | ${String.format(Locale.US, "%.5f", upperFence)}\n")
        result.append("Values outside fences (potential outliers) | ${outliers.size}\n")
        result.append("\nQuartiles and fences summarize the observed gap distribution; potential outliers are not errors or causal indicators.\n")
    }


    private fun appendStep82Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 82 - SHELL GAP OUTLIER DETAIL PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }.sorted()
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for outlier detail analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        fun percentile(p: Double): Double {
            val index = (p * (gaps.size - 1)).coerceIn(0.0, (gaps.size - 1).toDouble())
            val lower = index.toInt()
            val upper = kotlin.math.ceil(index).toInt()
            val fraction = index - lower
            return gaps[lower] * (1.0 - fraction) + gaps[upper] * fraction
        }
        val q1 = percentile(0.25)
        val q3 = percentile(0.75)
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr
        val lowerOutliers = gaps.filter { it < lowerFence }
        val upperOutliers = gaps.filter { it > upperFence }
        val inRange = gaps.count { it >= lowerFence && it <= upperFence }
        result.append("Observed cross-shell links | ${gaps.size}\n")
        result.append("Within Tukey fences | $inRange (${String.format(Locale.US, "%.2f", 100.0 * inRange / gaps.size)}%)\n")
        result.append("Below lower fence | ${lowerOutliers.size}\n")
        result.append("Above upper fence | ${upperOutliers.size}\n")
        result.append("Lower fence | ${String.format(Locale.US, "%.5f", lowerFence)}\n")
        result.append("Upper fence | ${String.format(Locale.US, "%.5f", upperFence)}\n")
        result.append("\nOutlier counts describe unusual gap sizes within this dataset; they do not imply significance, error, or causality.\n")
    }

    private fun appendStep83Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 83 - SHELL GAP FREQUENCY CONCENTRATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for gap-frequency concentration analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val frequencies = gaps.groupingBy { it }.eachCount().values.map { it.toDouble() }.sortedDescending()
        val total = frequencies.sum()
        val shares = frequencies.map { it / total }
        val hhi = shares.sumOf { it * it }
        val effectiveCategories = if (hhi > 0.0) 1.0 / hhi else 0.0
        val sortedAsc = frequencies.sorted()
        val n = sortedAsc.size
        val gini = if (total > 0.0 && n > 0) {
            val weighted = sortedAsc.mapIndexed { i, v -> (i + 1).toDouble() * v }.sum()
            (2.0 * weighted / (n * total)) - ((n + 1.0) / n)
        } else 0.0
        result.append("Observed cross-shell links | ${gaps.size}\n")
        result.append("Distinct shell-gap values | ${frequencies.size}\n")
        result.append("Largest gap-frequency share | ${String.format(Locale.US, "%.2f", shares.maxOrNull()?.times(100.0) ?: 0.0)}%\n")
        result.append("Gap-frequency HHI | ${String.format(Locale.US, "%.5f", hhi)}\n")
        result.append("Effective distinct-gap count | ${String.format(Locale.US, "%.5f", effectiveCategories)}\n")
        result.append("Gap-frequency Gini coefficient | ${String.format(Locale.US, "%.5f", gini.coerceIn(0.0, 1.0))}\n")
        result.append("\nThese concentration indicators describe the distribution of observed shell-gap categories; they are not inferential or predictive.\n")
    }

    private fun appendStep84Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 84 - SHELL GAP FREQUENCY BALANCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for gap-frequency balance analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val frequencyMap = gaps.groupingBy { it }.eachCount().toSortedMap()
        val counts = frequencyMap.values.map { it.toDouble() }
        val mean = counts.average()
        val variance = if (counts.size > 1) counts.sumOf { (it - mean) * (it - mean) } / counts.size else 0.0
        val sd = kotlin.math.sqrt(variance)
        val cv = if (mean > 0.0) sd / mean else 0.0
        val maxGap = frequencyMap.maxByOrNull { it.value }
        val minGap = frequencyMap.minByOrNull { it.value }
        val total = counts.sum()
        val normalizedBalance = if (counts.size > 1 && total > 0.0) {
            val entropy = counts.map { it / total }.sumOf { share -> if (share > 0.0) -share * kotlin.math.ln(share) else 0.0 }
            entropy / kotlin.math.ln(counts.size.toDouble())
        } else 1.0
        result.append("Observed cross-shell links | ${gaps.size}\n")
        result.append("Distinct gap categories | ${frequencyMap.size}\n")
        result.append("Mean links per gap category | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Frequency standard deviation | ${String.format(Locale.US, "%.5f", sd)}\n")
        result.append("Frequency coefficient of variation | ${String.format(Locale.US, "%.5f", cv)}\n")
        result.append("Normalized gap-frequency balance (0-1) | ${String.format(Locale.US, "%.5f", normalizedBalance)}\n")
        if (maxGap != null) result.append("Most frequent gap | ${maxGap.key} (links: ${maxGap.value})\n")
        if (minGap != null) result.append("Least frequent gap | ${minGap.key} (links: ${minGap.value})\n")
        result.append("\nBalance metrics describe observed category distribution only; they are not causal or predictive.\n")
    }

    private fun appendStep85Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 85 - SHELL GAP COMPARATIVE FREQUENCY PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for comparative frequency analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val freq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val total = gaps.size.toDouble()
        val meanFrequency = freq.values.map { it.toDouble() }.average()
        result.append("Total cross-shell links | ${gaps.size}\n")
        result.append("Gap category | Link count | Share (%) | Ratio to mean category frequency\n")
        freq.forEach { (gap, count) ->
            val share = count / total * 100.0
            val ratio = if (meanFrequency > 0.0) count / meanFrequency else 0.0
            result.append("Gap $gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${String.format(Locale.US, "%.5f", ratio)}\n")
        }
        result.append("Mean category frequency | ${String.format(Locale.US, "%.5f", meanFrequency)}\n")
        result.append("\nComparisons describe observed gap frequencies only; they are not causal or predictive.\n")
    }

    private fun appendStep86Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 86 - SHELL GAP FREQUENCY RANK & RELATIVE DEVIATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for gap ranking.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val freq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val mean = freq.values.map { it.toDouble() }.average()
        result.append("Gap | Count | Rank | Deviation from mean frequency | Relative deviation (%)\n")
        val ranked = freq.entries.sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
        ranked.forEachIndexed { index, entry ->
            val deviation = entry.value.toDouble() - mean
            val relative = if (mean != 0.0) deviation / mean * 100.0 else 0.0
            result.append("${entry.key} | ${entry.value} | ${index + 1} | ${String.format(Locale.US, "%.5f", deviation)} | ${String.format(Locale.US, "%.5f", relative)}\n")
        }
        result.append("Mean gap-category frequency | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("\nThis ranking compares observed gap-category frequencies; it is descriptive, not causal or predictive.\n")
    }

    private fun appendStep87Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 87 - SHELL GAP COMPARATIVE FREQUENCY RATIO PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for comparative frequency analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val freq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val total = freq.values.sum()
        val mean = freq.values.map { it.toDouble() }.average()
        val highest = freq.maxByOrNull { it.value }
        val lowest = freq.minByOrNull { it.value }
        result.append("Gap | Count | Share (%) | Ratio to mean frequency\n")
        freq.forEach { (gap, count) ->
            val share = count.toDouble() / total * 100.0
            val ratio = if (mean > 0.0) count.toDouble() / mean else 0.0
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${String.format(Locale.US, "%.5f", ratio)}\n")
        }
        result.append("Mean frequency | ${String.format(Locale.US, "%.5f", mean)}\n")
        if (highest != null) result.append("Highest-frequency gap | ${highest.key} (count ${highest.value})\n")
        if (lowest != null) result.append("Lowest-frequency gap | ${lowest.key} (count ${lowest.value})\n")
        result.append("\nRatios compare observed gap-category frequencies with their mean; descriptive, not causal or predictive.\n")
    }

    private fun appendStep88Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 88 - SHELL GAP FREQUENCY COMPARISON SUMMARY\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 88 summary.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val freq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val total = gaps.size
        val maxCount = freq.values.maxOrNull() ?: 0
        val minCount = freq.values.minOrNull() ?: 0
        val distinct = freq.size
        val meanCount = freq.values.map { it.toDouble() }.average()
        val dominant = freq.filterValues { it == maxCount }.keys
        val rare = freq.filterValues { it == minCount }.keys
        result.append("Gap | Count | Share (%) | Difference from mean count\n")
        freq.forEach { (gap, count) ->
            val share = count.toDouble() / total * 100.0
            val diff = count.toDouble() - meanCount
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${String.format(Locale.US, "%.5f", diff)}\n")
        }
        result.append("Observed gap categories | $distinct\n")
        result.append("Mean category frequency | ${String.format(Locale.US, "%.5f", meanCount)}\n")
        result.append("Most frequent gap value(s) | ${dominant.joinToString(", ")} (count $maxCount)\n")
        result.append("Least frequent gap value(s) | ${rare.joinToString(", ")} (count $minCount)\n")
        result.append("\nThis is a descriptive comparison of observed shell-gap frequencies, not causal or predictive.\n")
    }


    private fun appendStep89Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 89 - SHELL GAP FREQUENCY TREND PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 89 trend analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val total = gaps.size
        result.append("Gap | Count | Share (%) | Change from previous observed gap\n")
        var previous: Int? = null
        var previousCount: Int? = null
        freq.forEach { (gap, count) ->
            val share = count.toDouble() / total * 100.0
            val change = if (previous != null && previousCount != null && gap == previous!! + 1) count - previousCount!! else null
            result.append("$gap | $count | ${String.format(Locale.US, "%.5f", share)} | ${change?.toString() ?: "N/A"}\n")
            previous = gap
            previousCount = count
        }
        val adjacentChanges = keys.zipWithNext().filter { (a, b) -> b == a + 1 }.map { (a, b) -> freq[b]!! - freq[a]!! }
        if (adjacentChanges.isNotEmpty()) {
            val rising = adjacentChanges.count { it > 0 }
            val falling = adjacentChanges.count { it < 0 }
            val unchanged = adjacentChanges.count { it == 0 }
            result.append("Adjacent-gap count increases | $rising\n")
            result.append("Adjacent-gap count decreases | $falling\n")
            result.append("Adjacent-gap count unchanged | $unchanged\n")
            result.append("Mean adjacent frequency change | ${String.format(Locale.US, "%.5f", adjacentChanges.average())}\n")
        } else {
            result.append("Adjacent-gap trend | Insufficient consecutive gap categories\n")
        }
        result.append("\nObserved frequency changes are descriptive; they do not imply causation or prediction.\n")
    }



    private fun appendStep90Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 90 - SHELL GAP TREND STABILITY PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 90 stability analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val adjacentChanges = freq.keys.toList().zipWithNext()
            .filter { (a, b) -> b == a + 1 }
            .map { (a, b) -> freq[b]!! - freq[a]!! }
        if (adjacentChanges.isEmpty()) {
            result.append("Trend stability | Insufficient consecutive gap categories\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val mean = adjacentChanges.average()
        val variance = adjacentChanges.map { (it - mean) * (it - mean) }.average()
        val sd = kotlin.math.sqrt(variance)
        val signChanges = adjacentChanges.zipWithNext().count { (a, b) ->
            (a > 0 && b < 0) || (a < 0 && b > 0)
        }
        val positive = adjacentChanges.count { it > 0 }
        val negative = adjacentChanges.count { it < 0 }
        val zero = adjacentChanges.count { it == 0 }
        result.append("Adjacent frequency changes | ${adjacentChanges.joinToString(", ")}\n")
        result.append("Mean change | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Change variance | ${String.format(Locale.US, "%.5f", variance)}\n")
        result.append("Change standard deviation | ${String.format(Locale.US, "%.5f", sd)}\n")
        result.append("Positive changes | $positive\n")
        result.append("Negative changes | $negative\n")
        result.append("Zero changes | $zero\n")
        result.append("Direction reversals between changes | $signChanges\n")
        val stability = if (sd == 0.0) "No variation in observed adjacent changes"
            else "Observed adjacent changes vary (SD=${String.format(Locale.US, "%.5f", sd)})"
        result.append("Descriptive stability summary | $stability\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep91Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 91 - SHELL GAP TREND ACCELERATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 91 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val changes = freq.keys.toList().zipWithNext()
            .filter { (a, b) -> b == a + 1 }
            .map { (a, b) -> freq[b]!! - freq[a]!! }
        val acceleration = changes.zipWithNext().map { (a, b) -> b - a }
        if (acceleration.isEmpty()) {
            result.append("Insufficient adjacent frequency changes to calculate acceleration.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val mean = acceleration.average()
        val variance = acceleration.map { (it - mean) * (it - mean) }.average()
        result.append("Frequency changes | ${changes.joinToString(", ")}\n")
        result.append("Second differences (acceleration) | ${acceleration.joinToString(", ")}\n")
        result.append("Mean acceleration | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Acceleration variance | ${String.format(Locale.US, "%.5f", variance)}\n")
        result.append("Positive acceleration | ${acceleration.count { it > 0 }}\n")
        result.append("Negative acceleration | ${acceleration.count { it < 0 }}\n")
        result.append("Zero acceleration | ${acceleration.count { it == 0 }}\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep92Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 92 - SHELL GAP TREND JERK PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 92 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val changes = freq.keys.toList().zipWithNext()
            .filter { (a, b) -> b == a + 1 }
            .map { (a, b) -> freq[b]!! - freq[a]!! }
        val acceleration = changes.zipWithNext().map { (a, b) -> b - a }
        val jerk = acceleration.zipWithNext().map { (a, b) -> b - a }
        if (jerk.isEmpty()) {
            result.append("Insufficient consecutive acceleration values to calculate third differences.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val mean = jerk.average()
        val variance = jerk.map { (it - mean) * (it - mean) }.average()
        result.append("Third differences (jerk-like profile) | ${jerk.joinToString(", ")}\n")
        result.append("Mean third difference | ${String.format(Locale.US, "%.5f", mean)}\n")
        result.append("Third-difference variance | ${String.format(Locale.US, "%.5f", variance)}\n")
        result.append("Positive values | ${jerk.count { it > 0 }}\n")
        result.append("Negative values | ${jerk.count { it < 0 }}\n")
        result.append("Zero values | ${jerk.count { it == 0 }}\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep93Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 93 - SHELL GAP FREQUENCY TURNING-POINT PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 93 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val consecutive = keys.zipWithNext().filter { (a, b) -> b == a + 1 }
        val changes = consecutive.map { (a, b) -> freq[b]!! - freq[a]!! }
        val signs = changes.map { if (it > 0) 1 else if (it < 0) -1 else 0 }
        val nonZeroSigns = signs.filter { it != 0 }
        val turningPoints = nonZeroSigns.zipWithNext().count { (a, b) -> a != b }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Consecutive-gap frequency changes | ${changes.joinToString(", ")}\n")
        result.append("Trend sign changes (turning points) | $turningPoints\n")
        result.append("Increasing transitions | ${changes.count { it > 0 }}\n")
        result.append("Decreasing transitions | ${changes.count { it < 0 }}\n")
        result.append("Flat transitions | ${changes.count { it == 0 }}\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep94Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 94 - SHELL GAP FREQUENCY LOCAL PEAK AND VALLEY PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 94 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val peaks = mutableListOf<String>()
        val valleys = mutableListOf<String>()
        for (i in 1 until keys.lastIndex) {
            val prev = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val next = freq[keys[i + 1]] ?: 0
            if (current > prev && current > next) peaks.add("gap ${keys[i]} (frequency $current)")
            if (current < prev && current < next) valleys.add("gap ${keys[i]} (frequency $current)")
        }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Local peaks | ${if (peaks.isEmpty()) "None detected" else peaks.joinToString()}\n")
        result.append("Local valleys | ${if (valleys.isEmpty()) "None detected" else valleys.joinToString()}\n")
        result.append("Interior gap positions evaluated | ${maxOf(0, keys.size - 2)}\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep95Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 95 - SHELL GAP LOCAL PEAK PROMINENCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 95 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val peaks = mutableListOf<String>()
        for (i in 1 until keys.lastIndex) {
            val left = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val right = freq[keys[i + 1]] ?: 0
            if (current > left && current > right) {
                val prominence = current - maxOf(left, right)
                peaks.add("gap ${keys[i]}: frequency $current, prominence $prominence")
            }
        }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Local peak prominence | ${if (peaks.isEmpty()) "No interior peaks detected" else peaks.joinToString("; ")}\n")
        result.append("Peak prominence definition | peak frequency minus the larger adjacent frequency\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }


    private fun appendStep96Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 96 - SHELL GAP LOCAL VALLEY PROMINENCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 96 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val valleys = mutableListOf<String>()
        for (i in 1 until keys.lastIndex) {
            val left = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val right = freq[keys[i + 1]] ?: 0
            if (current < left && current < right) {
                val prominence = minOf(left, right) - current
                valleys.add("gap ${keys[i]}: frequency $current, prominence $prominence")
            }
        }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Local valley prominence | ${if (valleys.isEmpty()) "No interior valleys detected" else valleys.joinToString("; ")}\n")
        result.append("Valley prominence definition | the smaller adjacent frequency minus valley frequency\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }


    private fun appendStep97Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 97 - SHELL GAP PEAK-VALLEY CONTRAST PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 97 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val peaks = mutableListOf<String>()
        val valleys = mutableListOf<String>()
        for (i in 1 until keys.lastIndex) {
            val left = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val right = freq[keys[i + 1]] ?: 0
            if (current > left && current > right) {
                peaks.add("gap ${keys[i]}: frequency $current, contrast ${current - maxOf(left, right)}")
            }
            if (current < left && current < right) {
                valleys.add("gap ${keys[i]}: frequency $current, contrast ${minOf(left, right) - current}")
            }
        }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Peak-valley contrast | peaks: ${if (peaks.isEmpty()) "none detected" else peaks.joinToString("; ")}\n")
        result.append("Peak-valley contrast | valleys: ${if (valleys.isEmpty()) "none detected" else valleys.joinToString("; ")}\n")
        result.append("Contrast compares an interior point with its two adjacent observed gap values.\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }

    private fun appendStep98Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 98 - SHELL GAP PEAK-VALLEY SEPARATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 98 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        val peaks = mutableListOf<Int>()
        val valleys = mutableListOf<Int>()
        for (i in 1 until keys.lastIndex) {
            val left = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val right = freq[keys[i + 1]] ?: 0
            if (current > left && current > right) peaks.add(keys[i])
            if (current < left && current < right) valleys.add(keys[i])
        }
        val peakPairs = peaks.flatMapIndexed { i, p -> peaks.drop(i + 1).map { q -> kotlin.math.abs(q - p) } }
        val valleyPairs = valleys.flatMapIndexed { i, p -> valleys.drop(i + 1).map { q -> kotlin.math.abs(q - p) } }
        val peakValleyPairs = peaks.flatMap { p -> valleys.map { v -> kotlin.math.abs(p - v) } }
        fun describe(name: String, values: List<Int>) {
            if (values.isEmpty()) {
                result.append("$name | no pairs available\n")
            } else {
                val sorted = values.sorted()
                val mean = sorted.average()
                result.append("$name | count ${sorted.size}, mean separation %.2f, min %d, max %d\n".format(mean, sorted.first(), sorted.last()))
            }
        }
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Detected peak gaps | ${if (peaks.isEmpty()) "none" else peaks.joinToString()}\n")
        result.append("Detected valley gaps | ${if (valleys.isEmpty()) "none" else valleys.joinToString()}\n")
        describe("Peak-to-peak separation", peakPairs)
        describe("Valley-to-valley separation", valleyPairs)
        describe("Peak-valley separation", peakValleyPairs)
        result.append("Separations use absolute differences between detected gap values.\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }


    private fun appendStep99Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 99 - SHELL GAP PEAK-VALLEY STRUCTURE COMPARISON\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 99 analysis.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }
        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val keys = freq.keys.toList()
        var peaks = 0
        var valleys = 0
        for (i in 1 until keys.lastIndex) {
            val left = freq[keys[i - 1]] ?: 0
            val current = freq[keys[i]] ?: 0
            val right = freq[keys[i + 1]] ?: 0
            if (current > left && current > right) peaks++
            if (current < left && current < right) valleys++
        }
        val total = peaks + valleys
        val ratio = if (valleys > 0) peaks.toDouble() / valleys.toDouble() else null
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Detected local peaks | $peaks\n")
        result.append("Detected local valleys | $valleys\n")
        result.append("Peak-valley count ratio | ${ratio?.let { "%.3f".format(it) } ?: "undefined (no valleys detected"}\n")
        result.append("Combined detected extrema | $total\n")
        result.append("Interior observed gap values examined | ${maxOf(0, keys.size - 2)}\n")
        result.append("Peak/valley counts use strict comparisons against adjacent observed gap values.\n")
        result.append("\nThis profile is descriptive and does not imply causation or prediction.\n")
    }


    private fun appendStep101Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 101 - SHELL GAP FREQUENCY LEAVE-ONE-LINK-OUT SENSITIVITY\n")
        result.append("=============================\n\n")

        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 2) {
            result.append("At least two cross-shell links are required for leave-one-link-out sensitivity.\n")
            result.append("Sensitivity metrics unavailable for this dataset.\n")
            result.append("Statistics are descriptive only, not causal or predictive.\n")
            return
        }

        fun normalizedEntropy(values: List<Int>): Double {
            if (values.isEmpty()) return 0.0
            val f = values.groupingBy { it }.eachCount()
            if (f.size <= 1) return 0.0
            val n = values.size.toDouble()
            val h = -f.values.map { it / n }.sumOf { p -> p * kotlin.math.ln(p) }
            return h / kotlin.math.ln(f.size.toDouble())
        }

        val baselineEntropy = normalizedEntropy(gaps)
        val baselineTopShare = gaps.groupingBy { it }.eachCount().values.maxOrNull()?.toDouble()?.div(gaps.size) ?: 0.0
        val entropyChanges = gaps.indices.map { index ->
            normalizedEntropy(gaps.filterIndexed { i, _ -> i != index }) - baselineEntropy
        }
        val topShareChanges = gaps.indices.map { index ->
            val remaining = gaps.filterIndexed { i, _ -> i != index }
            val top = remaining.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
            top.toDouble() / remaining.size - baselineTopShare
        }
        result.append("Cross-shell gap link count | ${gaps.size}\n")
        result.append("Baseline normalized gap entropy | %.4f\n".format(baselineEntropy))
        result.append("Baseline dominant-gap share | %.4f\n".format(baselineTopShare))
        result.append("Leave-one-link-out entropy change range | %.4f to %.4f\n".format(entropyChanges.minOrNull() ?: 0.0, entropyChanges.maxOrNull() ?: 0.0))
        result.append("Leave-one-link-out dominant-share change range | %.4f to %.4f\n".format(topShareChanges.minOrNull() ?: 0.0, topShareChanges.maxOrNull() ?: 0.0))
        result.append("Largest absolute entropy change | %.4f\n".format(entropyChanges.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0))
        result.append("Largest absolute dominant-share change | %.4f\n".format(topShareChanges.maxOfOrNull { kotlin.math.abs(it) } ?: 0.0))
        result.append("Interpretation | Indicates how sensitive gap-frequency summaries are to removal of one link.\n")
        result.append("Statistics are descriptive only, not causal or predictive.\n")
    }




    private fun appendStep104RobustDispersionJackknife(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 104 - ROBUST DISPERSION LEAVE-ONE-LINK-OUT SENSITIVITY\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Robust jackknife sensitivity metrics unavailable.\n")
            result.append("Statistics are descriptive only, not causal or predictive.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            val n = sorted.size
            return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        fun robustMetrics(g: List<Int>): List<Double> {
            val v = g.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val iqr = q3 - q1
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, iqr, mad, 1.4826 * mad)
        }
        val baseline = robustMetrics(gaps)
        val jack = gaps.indices.map { i -> robustMetrics(gaps.filterIndexed { j, _ -> j != i }) }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        result.append("Valid cross-shell gap link count | ${gaps.size}\n")
        labels.forEachIndexed { index, label ->
            val vals = jack.map { it[index] }
            val baselineValue = baseline[index]
            val lo = vals.minOrNull() ?: baselineValue
            val hi = vals.maxOrNull() ?: baselineValue
            val mean = vals.average()
            val se = kotlin.math.sqrt((gaps.size - 1).toDouble() / gaps.size *
                vals.sumOf { (it - mean) * (it - mean) })
            result.append("$label baseline | %.4f\n".format(baselineValue))
            result.append("$label leave-one-link-out range | %.4f to %.4f\n".format(lo, hi))
            result.append("$label jackknife SE | %.4f\n".format(se))
        }
        result.append("Interpretation | Quantifies sensitivity of robust dispersion summaries to removal of one cross-shell link.\n")
        result.append("Statistics are descriptive only, not causal or predictive.\n")
    }









    

    private fun appendStep117ConsensusThresholdSensitivity(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 117 - CONSENSUS THRESHOLD SENSITIVITY PROFILE\n")
        result.append("=============================\n\n")
        val gaps117 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps117.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Threshold sensitivity profile unavailable.\n")
            return
        }
        val median117 = gaps117.sorted().let { sorted ->
            if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble()
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val mean117 = gaps117.average()
        val mad117 = gaps117.map { kotlin.math.abs(it - median117) }.sorted().let { sorted ->
            if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble()
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val dispersion117 = if (mean117 != 0.0) mad117 / mean117 else 0.0
        val thresholds117 = listOf(60.0, 65.0, 70.0, 75.0, 80.0, 85.0)
        val baseConsensus117 = (100.0 * (1.0 - dispersion117)).coerceIn(0.0, 100.0)
        result.append("Robust summary inputs | link count: ${gaps117.size} | mean gap: %.4f | median gap: %.4f | MAD: %.4f\n".format(mean117, median117, mad117))
        result.append("Base heuristic consensus index | %.2f%%\n\n".format(baseConsensus117))
        result.append("Threshold | Classification | Distance from threshold\n")
        thresholds117.forEach { threshold ->
            val classification = when {
                baseConsensus117 >= threshold -> "Meets threshold"
                else -> "Below threshold"
            }
            val distance = baseConsensus117 - threshold
            result.append("%.1f%% | %s | %.2f points\n".format(threshold, classification, distance))
        }
        val nearest117 = thresholds117.minByOrNull { kotlin.math.abs(baseConsensus117 - it) } ?: 0.0
        result.append("\nNearest tested decision threshold | %.1f%%\n".format(nearest117))
        result.append("Interpretation | Shows how a heuristic index changes status under different cut-off values; thresholds are illustrative, not validated.\n")
        result.append("Limitations | This profile is descriptive sensitivity analysis and does not establish statistical confidence, prediction validity, or causal influence.\n")
    }

    private fun appendStep116RankStabilityConsensusProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 116 - RANK STABILITY CONSENSUS & CONFIDENCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Consensus profile unavailable.\n")
            return
        }
        fun median116(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metrics116(sample: List<Int>): List<Double> {
            val values = sample.map { it.toDouble() }
            val med = median116(values)
            val sorted = values.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median116(lower) else med
            val q3 = if (upper.isNotEmpty()) median116(upper) else med
            val mad = median116(values.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val names = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val scenarios = listOf(
            listOf(0.25, 0.25, 0.25, 0.25),
            listOf(0.40, 0.20, 0.20, 0.20),
            listOf(0.20, 0.40, 0.20, 0.20),
            listOf(0.20, 0.20, 0.40, 0.20),
            listOf(0.20, 0.20, 0.20, 0.40)
        )
        val rng = java.util.Random(116L)
        val scores = MutableList(4) { 0.0 }
        for (k in 0..3) {
            val batches = List(10) {
                List(200) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    metrics116(sample)[k]
                }.average()
            }
            val mean = batches.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val maxDeviation = batches.maxOf { kotlin.math.abs(it - mean) }
            val trend = batches.drop(5).average() - batches.take(5).average()
            val deviationScore = (100.0 * (1.0 - maxDeviation / tolerance)).coerceIn(0.0, 100.0)
            val trendScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tolerance)).coerceIn(0.0, 100.0)
            scores[k] = (deviationScore + trendScore) / 2.0
        }
        val scenarioRanks = scenarios.map { weights ->
            val weighted = scores.indices.map { scores[it] * weights[it] }
            val ordered = weighted.indices.sortedByDescending { weighted[it] }
            val ranks = MutableList(4) { 0 }
            ordered.forEachIndexed { rank, index -> ranks[index] = rank + 1 }
            ranks
        }
        result.append("Consensus profile across five weighting scenarios\n")
        result.append("Component | Mean rank | Rank range | Top-rank frequency | Top-3 frequency | Consensus score\n")
        for (i in names.indices) {
            val ranks = scenarioRanks.map { it[i] }
            val meanRank = ranks.average()
            val rankRange = (ranks.maxOrNull() ?: 0) - (ranks.minOrNull() ?: 0)
            val top1 = ranks.count { it == 1 } / scenarios.size.toDouble() * 100.0
            val top3 = ranks.count { it <= 3 } / scenarios.size.toDouble() * 100.0
            val consensus = (top1 * 0.60 + top3 * 0.25 + (100.0 - rankRange * 25.0).coerceIn(0.0, 100.0) * 0.15)
            val classification = when {
                consensus >= 85.0 -> "High scenario consensus"
                consensus >= 65.0 -> "Moderate scenario consensus"
                else -> "Low scenario consensus"
            }
            result.append("${names[i]} | %.2f | %d | %.1f%% | %.1f%% | %.1f%% (%s)\n".format(meanRank, rankRange, top1, top3, consensus, classification))
        }
        result.append("\nConsensus score weights: top-rank frequency 60%, top-3 frequency 25%, rank-range stability 15%.\n")
        result.append("Interpretation | Consensus reflects stability under these five selected weighting scenarios only.\n")
        result.append("Limitations | This heuristic score is not a statistical confidence measure, validated prediction, or causal inference.\n")
    }

    private fun appendStep115MultiScenarioRankStability(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 115 - MULTI-SCENARIO RANK STABILITY SUMMARY\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Multi-scenario rank stability summary unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metric(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val componentNames = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val scenarios = listOf(
            "Equal weight" to listOf(0.25, 0.25, 0.25, 0.25),
            "Median emphasis" to listOf(0.40, 0.20, 0.20, 0.20),
            "IQR emphasis" to listOf(0.20, 0.40, 0.20, 0.20),
            "MAD emphasis" to listOf(0.20, 0.20, 0.40, 0.20),
            "Robust scale emphasis" to listOf(0.20, 0.20, 0.20, 0.40)
        )
        val rng = java.util.Random(115L)
        val componentScores = MutableList(4) { 0.0 }
        for (k in 0..3) {
            val batches = List(10) {
                List(200) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    metric(sample)[k]
                }.average()
            }
            val mean = batches.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val maxDeviation = batches.maxOf { kotlin.math.abs(it - mean) }
            val trend = batches.drop(5).average() - batches.take(5).average()
            val deviationScore = (100.0 * (1.0 - maxDeviation / tolerance)).coerceIn(0.0, 100.0)
            val trendScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tolerance)).coerceIn(0.0, 100.0)
            componentScores[k] = (deviationScore + trendScore) / 2.0
        }
        val scenarioRanks = scenarios.map { (_, weights) ->
            val weighted = componentScores.indices.map { componentScores[it] * weights[it] }
            val sortedIndices = weighted.indices.sortedByDescending { weighted[it] }
            val ranks = MutableList(4) { 0 }
            sortedIndices.forEachIndexed { rank, idx -> ranks[idx] = rank + 1 }
            ranks
        }
        val rankLists = componentNames.indices.map { idx -> scenarioRanks.map { it[idx] } }
        result.append("Scenarios analyzed | ${scenarios.size}\n")
        result.append("Components assessed | ${componentNames.size}\n\n")
        result.append("Scenario agreement matrix | diagonal=100%, off-diagonal reports exact-position component agreement\n")
        for (a in scenarios.indices) {
            val row = scenarios.indices.map { b ->
                if (a == b) 100.0 else {
                    (0..3).count { scenarioRanks[a][it] == scenarioRanks[b][it] } / 4.0 * 100.0
                }
            }
            result.append(scenarios[a].first + " | " + row.joinToString(" | ") { "%.1f%%".format(it) } + "\n")
        }
        result.append("\nComponent rank summary across scenarios\n")
        for (i in componentNames.indices) {
            val ranks = rankLists[i]
            val avgRank = ranks.average()
            val range = (ranks.maxOrNull() ?: 0) - (ranks.minOrNull() ?: 0)
            val topFrequency = ranks.count { it == 1 } / scenarios.size.toDouble() * 100.0
            val consistentFrequency = ranks.count { it == avgRank.roundToInt() } / scenarios.size.toDouble() * 100.0
            result.append("${componentNames[i]} | average rank %.2f | rank range %d | top-rank frequency %.1f%% | rounded-average-rank frequency %.1f%%\n".format(avgRank, range, topFrequency, consistentFrequency))
        }
        val allPairAgreement = (scenarios.indices.flatMap { a ->
            (a + 1 until scenarios.size).map { b ->
                (0..3).count { scenarioRanks[a][it] == scenarioRanks[b][it] } / 4.0 * 100.0
            }
        }).average()
        result.append("\nMean pairwise exact-position agreement | %.2f%%\n".format(allPairAgreement))
        result.append("Interpretation | Summarizes ranking consistency across the defined weighting scenarios.\n")
        result.append("Limitations | Heuristic, descriptive sensitivity summary; not validated inferential or predictive statistics.\n")
    }

private fun appendStep114RankAgreementRobustness(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 114 - RANK AGREEMENT ROBUSTNESS PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Leave-one-scenario-out rank robustness unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val s = values.sorted()
            return if (s.size % 2 == 1) s[s.size / 2]
            else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val names = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val rng = java.util.Random(114L)
        val componentScores = MutableList(4) { 0.0 }
        repeat(4) { k ->
            val batchMeans = List(10) {
                List(200) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    metrics(sample)[k]
                }.average()
            }
            val mean = batchMeans.average()
            val tol = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val deviation = batchMeans.maxOf { kotlin.math.abs(it - mean) }
            val trend = batchMeans.drop(5).average() - batchMeans.take(5).average()
            val dScore = (100.0 * (1.0 - deviation / tol)).coerceIn(0.0, 100.0)
            val tScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tol)).coerceIn(0.0, 100.0)
            componentScores[k] = (dScore + tScore) / 2.0
        }
        val scenarios = listOf(
            "Equal weight" to listOf(0.25, 0.25, 0.25, 0.25),
            "Median emphasis" to listOf(0.40, 0.20, 0.20, 0.20),
            "IQR emphasis" to listOf(0.20, 0.40, 0.20, 0.20),
            "MAD emphasis" to listOf(0.20, 0.20, 0.40, 0.20),
            "Robust scale emphasis" to listOf(0.20, 0.20, 0.20, 0.40)
        )
        val vectors = scenarios.map { (_, weights) ->
            val contributions = names.indices.map { componentScores[it] * weights[it] }
            val order = contributions.indices.sortedByDescending { contributions[it] }
            val ranks = MutableList(4) { 0 }
            order.forEachIndexed { rank, idx -> ranks[idx] = rank + 1 }
            ranks
        }
        fun pairAgreement(indices: List<Int>): Pair<Double, Double> {
            if (indices.size < 2) return 0.0 to 0.0
            var exactSum = 0.0
            var pairOrderAgree = 0
            var pairOrderTotal = 0
            var comparisons = 0
            for (x in 0 until indices.size) for (y in x + 1 until indices.size) {
                comparisons++
                val ra = vectors[indices[x]]
                val rb = vectors[indices[y]]
                exactSum += ra.indices.count { ra[it] == rb[it] } / 4.0 * 100.0
                for (i in 0 until 4) for (j in i + 1 until 4) {
                    if ((ra[i] - ra[j]) == (rb[i] - rb[j])) pairOrderAgree++
                    pairOrderTotal++
                }
            }
            return (exactSum / comparisons) to (if (pairOrderTotal > 0) pairOrderAgree / pairOrderTotal.toDouble() * 100.0 else 0.0)
        }
        result.append("Leave-one-scenario-out results:\n")
        val exactScores = mutableListOf<Double>()
        val orderScores = mutableListOf<Double>()
        for (omit in scenarios.indices) {
            val retained = scenarios.indices.filter { it != omit }
            val (exact, order) = pairAgreement(retained)
            exactScores.add(exact)
            orderScores.add(order)
            result.append("Omit ${scenarios[omit].first} | exact-position agreement %.2f%% | pair-order agreement %.2f%%\n".format(exact, order))
        }
        val (fullExact, fullOrder) = pairAgreement(scenarios.indices.toList())
        result.append("\nFull scenario-set baseline | exact-position agreement %.2f%% | pair-order agreement %.2f%%\n".format(fullExact, fullOrder))
        result.append("LOSO exact-position agreement range | %.2f%% to %.2f%%\n".format(exactScores.minOrNull() ?: 0.0, exactScores.maxOrNull() ?: 0.0))
        result.append("LOSO pair-order agreement range | %.2f%% to %.2f%%\n".format(orderScores.minOrNull() ?: 0.0, orderScores.maxOrNull() ?: 0.0))
        result.append("LOSO sensitivity | maximum absolute exact-agreement shift from baseline %.2f percentage points\n".format(exactScores.maxOfOrNull { kotlin.math.abs(it - fullExact) } ?: 0.0))
        result.append("LOSO sensitivity | maximum absolute pair-order shift from baseline %.2f percentage points\n".format(orderScores.maxOfOrNull { kotlin.math.abs(it - fullOrder) } ?: 0.0))
        result.append("Interpretation | Ranges summarize how aggregate rank agreement changes when one weighting scenario is omitted.\n")
        result.append("Limitations | Descriptive, heuristic sensitivity analysis; not validated inferential or predictive statistics.\n")
    }

    private fun appendStep113RankAgreementCorrelation(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 113 - RANK AGREEMENT & CORRELATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Rank agreement analysis unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val names = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val rng = java.util.Random(113L)
        val componentScores = MutableList(4) { 0.0 }
        repeat(4) { metricIndex ->
            val batchScores = ArrayList<Double>()
            repeat(10) {
                val batchValues = ArrayList<Double>()
                repeat(200) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    batchValues.add(metrics(sample)[metricIndex])
                }
                batchScores.add(batchValues.average())
            }
            val mean = batchScores.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val deviation = batchScores.maxOf { kotlin.math.abs(it - mean) }
            val trend = batchScores.drop(5).average() - batchScores.take(5).average()
            val dScore = (100.0 * (1.0 - deviation / tolerance)).coerceIn(0.0, 100.0)
            val tScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tolerance)).coerceIn(0.0, 100.0)
            componentScores[metricIndex] = (dScore + tScore) / 2.0
        }
        val scenarios = listOf(
            "Equal weight" to listOf(0.25, 0.25, 0.25, 0.25),
            "Median emphasis" to listOf(0.40, 0.20, 0.20, 0.20),
            "IQR emphasis" to listOf(0.20, 0.40, 0.20, 0.20),
            "MAD emphasis" to listOf(0.20, 0.20, 0.40, 0.20),
            "Robust scale emphasis" to listOf(0.20, 0.20, 0.20, 0.40)
        )
        val rankVectors = scenarios.map { (_, weights) ->
            val contributions = names.indices.map { componentScores[it] * weights[it] }
            val order = contributions.indices.sortedByDescending { contributions[it] }
            val ranks = MutableList(4) { 0 }
            order.forEachIndexed { rank, index -> ranks[index] = rank + 1 }
            ranks
        }
        result.append("Scenario pair comparison (4 components):\n")
        var pairCount = 0
        var exactPositionSum = 0.0
        var concordantPairs = 0
        var totalPairs = 0
        for (a in rankVectors.indices) {
            for (b in a + 1 until rankVectors.size) {
                pairCount++
                val ra = rankVectors[a]
                val rb = rankVectors[b]
                val exact = ra.indices.count { ra[it] == rb[it] }
                val exactPct = exact / 4.0 * 100.0
                exactPositionSum += exactPct
                var concordance = 0
                for (i in ra.indices) for (j in i + 1 until ra.size) {
                    val signA = ra[i] - ra[j]
                    val signB = rb[i] - rb[j]
                    if (signA == signB) concordance++
                }
                concordantPairs += concordance
                totalPairs += 6
                result.append("${scenarios[a].first} vs ${scenarios[b].first} | exact position agreement %.2f%% | pair order agreement %.2f%%\n".format(exactPct, concordance / 6.0 * 100.0))
            }
        }
        val averageExactAgreement = if (pairCount > 0) exactPositionSum / pairCount else 0.0
        val overallPairAgreement = if (totalPairs > 0) concordantPairs / totalPairs.toDouble() * 100.0 else 0.0
        result.append("\nAverage exact-position agreement across scenario pairs | %.2f%%\n".format(averageExactAgreement))
        result.append("Overall pair-order agreement | %.2f%%\n".format(overallPairAgreement))
        result.append("\nSpearman-style rank correlation (scenario pairs):\n")
        for (a in rankVectors.indices) for (b in a + 1 until rankVectors.size) {
            val ra = rankVectors[a].map { it.toDouble() }
            val rb = rankVectors[b].map { it.toDouble() }
            val meanA = ra.average()
            val meanB = rb.average()
            val covariance = ra.indices.sumOf { (ra[it] - meanA) * (rb[it] - meanB) }
            val denomA = kotlin.math.sqrt(ra.sumOf { (it - meanA) * (it - meanA) })
            val denomB = kotlin.math.sqrt(rb.sumOf { (it - meanB) * (it - meanB) })
            val rho = if (denomA > 0.0 && denomB > 0.0) covariance / (denomA * denomB) else 0.0
            result.append("${scenarios[a].first} vs ${scenarios[b].first} | rho %.4f\n".format(rho))
        }
        result.append("\nSummary | Average exact-position agreement %.2f%%; overall pair-order agreement %.2f%%.\n".format(averageExactAgreement, overallPairAgreement))
        result.append("Limitations | Rank comparisons are descriptive and heuristic; they are not validated predictive or inferential statistics.\n")
    }

    private fun appendStep112RankConsistency(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 112 - COMPOSITE STABILITY RANK CONSISTENCY\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Rank consistency analysis unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val names = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val rng = java.util.Random(112L)
        val componentScores = MutableList(4) { 0.0 }
        repeat(4) { metricIndex ->
            val batchScores = ArrayList<Double>()
            repeat(10) {
                val batchValues = ArrayList<Double>()
                repeat(200) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    batchValues.add(metrics(sample)[metricIndex])
                }
                batchScores.add(batchValues.average())
            }
            val mean = batchScores.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val deviation = batchScores.maxOf { kotlin.math.abs(it - mean) }
            val trend = batchScores.drop(5).average() - batchScores.take(5).average()
            val dScore = (100.0 * (1.0 - deviation / tolerance)).coerceIn(0.0, 100.0)
            val tScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tolerance)).coerceIn(0.0, 100.0)
            componentScores[metricIndex] = (dScore + tScore) / 2.0
        }
        val scenarios = listOf(
            "Equal weight" to listOf(0.25, 0.25, 0.25, 0.25),
            "Median emphasis" to listOf(0.40, 0.20, 0.20, 0.20),
            "IQR emphasis" to listOf(0.20, 0.40, 0.20, 0.20),
            "MAD emphasis" to listOf(0.20, 0.20, 0.40, 0.20),
            "Robust scale emphasis" to listOf(0.20, 0.20, 0.20, 0.40)
        )
        val rankTotals = MutableList(4) { 0.0 }
        val rankLists = MutableList(4) { mutableListOf<Int>() }
        result.append("Component scores used for ranking (heuristic):\n")
        names.indices.forEach { i -> result.append("${names[i]} | %.2f / 100\n".format(componentScores[i])) }
        result.append("\nScenario contribution ranks (1 = largest weighted contribution):\n")
        scenarios.forEach { (scenario, weights) ->
            val contributions = names.indices.map { i -> componentScores[i] * weights[i] }
            val order = contributions.indices.sortedByDescending { contributions[it] }
            val ranks = MutableList(4) { 0 }
            order.forEachIndexed { rank, index -> ranks[index] = rank + 1 }
            names.indices.forEach { i ->
                rankTotals[i] += ranks[i]
                rankLists[i].add(ranks[i])
            }
            result.append("$scenario | ")
            result.append(names.indices.joinToString("; ") { "${names[it]}=${ranks[it]}" })
            result.append("\n")
        }
        result.append("\nAverage rank and rank range across scenarios:\n")
        names.indices.forEach { i ->
            val averageRank = rankTotals[i] / scenarios.size
            val minRank = rankLists[i].minOrNull() ?: 0
            val maxRank = rankLists[i].maxOrNull() ?: 0
            result.append("${names[i]} | average rank %.2f | range %d-%d\n".format(averageRank, minRank, maxRank))
        }
        val ranges = names.indices.map { (rankLists[it].maxOrNull() ?: 0) - (rankLists[it].minOrNull() ?: 0) }
        result.append("\nRank consistency summary | ")
        result.append(if (ranges.all { it == 0 }) "Ranks unchanged across tested scenarios."
            else "Rank shifts occur under the tested weighting scenarios.")
        result.append("\nInterpretation | Rank results describe relative weighted contributions within illustrative scenarios only.\n")
        result.append("Limitations | Heuristic sensitivity analysis; ranks are not validated or predictive.\n")
    }

    private fun appendStep111WeightSensitivity(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 111 - COMPOSITE STABILITY WEIGHT SENSITIVITY\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Weight sensitivity analysis unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lo = sorted.take(sorted.size / 2)
            val hi = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lo.isNotEmpty()) median(lo) else med
            val q3 = if (hi.isNotEmpty()) median(hi) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3-q1, mad, 1.4826*mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val batches = 10
        val perBatch = 200
        val rng = java.util.Random(111L)
        val componentScores = MutableList(4) { 0.0 }
        repeat(4) { metricIndex ->
            val batchMeans = ArrayList<Double>()
            repeat(batches) {
                val sampleMetrics = ArrayList<Double>()
                repeat(perBatch) {
                    val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                    sampleMetrics.add(metrics(sample)[metricIndex])
                }
                batchMeans.add(sampleMetrics.average())
            }
            val mean = batchMeans.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean)*0.05, 0.0001)
            val maxDeviation = batchMeans.maxOf { kotlin.math.abs(it-mean) }
            val trend = batchMeans.drop(5).average()-batchMeans.take(5).average()
            val dScore = (100.0*(1.0-maxDeviation/tolerance)).coerceIn(0.0,100.0)
            val tScore = (100.0*(1.0-kotlin.math.abs(trend)/tolerance)).coerceIn(0.0,100.0)
            componentScores[metricIndex]=(dScore+tScore)/2.0
            result.append("${labels[metricIndex]} baseline component | %.2f / 100\n".format(componentScores[metricIndex]))
        }
        val scenarios = listOf(
            "Equal weight" to listOf(0.25,0.25,0.25,0.25),
            "Median emphasis" to listOf(0.40,0.20,0.20,0.20),
            "IQR emphasis" to listOf(0.20,0.40,0.20,0.20),
            "MAD emphasis" to listOf(0.20,0.20,0.40,0.20),
            "Robust scale emphasis" to listOf(0.20,0.20,0.20,0.40)
        )
        result.append("\nScenario scores:\n")
        val scores = ArrayList<Double>()
        scenarios.forEach { (name, weights) ->
            val score = componentScores.indices.sumOf { componentScores[it]*weights[it] }
            scores.add(score)
            result.append("$name | %.2f / 100 | weights ".format(score))
            result.append(weights.joinToString(", ") { "%.2f".format(it) })
            result.append("\n")
        }
        val range = scores.maxOrNull()!!-scores.minOrNull()!!
        result.append("\nWeight-sensitivity score range | %.2f points\n".format(range))
        result.append("Interpretation | Smaller score range indicates less sensitivity to the tested weight scenarios.\n")
        result.append("Scenarios are illustrative, not empirically validated weighting schemes.\n")
        result.append("Limitations | Heuristic sensitivity analysis; not a formal statistical test or predictive measure.\n")
        result.append("Statistics are descriptive only.\n")
    }

    private fun appendStep110CompositeBootstrapStabilityIndex(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 110 - COMPOSITE BOOTSTRAP STABILITY INDEX\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Composite Stability Index unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            return if (sorted.size % 2 == 1) sorted[sorted.size / 2]
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        fun metricValues(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val batches = 10
        val perBatch = 200
        val rng = java.util.Random(110L)
        val batchValues = labels.indices.map { ArrayList<Double>() }
        repeat(batches) {
            val collected = labels.indices.map { ArrayList<Double>() }
            repeat(perBatch) {
                val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                metricValues(sample).forEachIndexed { i, value -> collected[i].add(value) }
            }
            labels.indices.forEach { i -> batchValues[i].add(collected[i].average()) }
        }
        result.append("Bootstrap batches | $batches\n")
        result.append("Resamples per batch | $perBatch\n")
        result.append("Composite score range | 0 to 100\n")
        result.append("Component tolerance | max(5% of absolute batch mean, 0.0001)\n\n")
        var totalScore = 0.0
        labels.forEachIndexed { i, label ->
            val values = batchValues[i]
            val mean = values.average()
            val tolerance = kotlin.math.max(kotlin.math.abs(mean) * 0.05, 0.0001)
            val maxDeviation = values.maxOf { kotlin.math.abs(it - mean) }
            val trend = values.drop(batches / 2).average() - values.take(batches / 2).average()
            val deviationScore = (100.0 * (1.0 - maxDeviation / tolerance)).coerceIn(0.0, 100.0)
            val trendScore = (100.0 * (1.0 - kotlin.math.abs(trend) / tolerance)).coerceIn(0.0, 100.0)
            val componentScore = (deviationScore + trendScore) / 2.0
            totalScore += componentScore
            result.append("$label deviation score | %.2f / 100\n".format(deviationScore))
            result.append("$label trend score | %.2f / 100\n".format(trendScore))
            result.append("$label component score | %.2f / 100\n\n".format(componentScore))
        }
        val composite = totalScore / labels.size
        val category = when {
            composite >= 80.0 -> "HIGH heuristic stability"
            composite >= 60.0 -> "MODERATE heuristic stability"
            else -> "LOW heuristic stability"
        }
        result.append("Composite Bootstrap Stability Index | %.2f / 100\n".format(composite))
        result.append("Composite interpretation | $category\n")
        result.append("Scoring note | Average of four metric components, each combining deviation and half-to-half trend scores.\n")
        result.append("Limitations | This heuristic index is not a formal statistical test, proof of convergence, causal result, or predictive score.\n")
        result.append("Statistics are descriptive only.\n")
    }

    private fun appendStep109BootstrapConvergenceDiagnostics(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 109 - BOOTSTRAP CONVERGENCE DIAGNOSTICS\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Convergence diagnostics unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val s = values.sorted()
            return if (s.size % 2 == 1) s[s.size / 2]
            else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val batches = 10
        val perBatch = 200
        val rng = java.util.Random(109L)
        val batchMeans = labels.indices.map { ArrayList<Double>() }
        repeat(batches) { batch ->
            val collected = labels.indices.map { ArrayList<Double>() }
            repeat(perBatch) {
                val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                metrics(sample).forEachIndexed { i, value -> collected[i].add(value) }
            }
            labels.indices.forEach { i -> batchMeans[i].add(collected[i].average()) }
        }
        result.append("Bootstrap batches | $batches\n")
        result.append("Resamples per batch | $perBatch\n")
        result.append("Diagnostic tolerance | 5% of absolute cumulative mean (minimum tolerance 0.0001)\n\n")
        labels.forEachIndexed { i, label ->
            val values = batchMeans[i]
            val cumulative = values.runningFold(0.0) { acc, value -> acc + value }
                .drop(1).mapIndexed { index, sum -> sum / (index + 1) }
            val cumulativeMean = values.average()
            val deviations = values.map { it - cumulativeMean }
            val maxDeviation = deviations.maxOf { kotlin.math.abs(it) }
            val firstHalf = values.take(batches / 2).average()
            val secondHalf = values.drop(batches / 2).average()
            val trendDifference = secondHalf - firstHalf
            val tolerance = kotlin.math.max(kotlin.math.abs(cumulativeMean) * 0.05, 0.0001)
            val stable = maxDeviation <= tolerance && kotlin.math.abs(trendDifference) <= tolerance
            result.append("$label batch mean | %.4f\n".format(cumulativeMean))
            result.append("$label maximum batch deviation | %.4f\n".format(maxDeviation))
            result.append("$label first-half vs second-half difference | %.4f\n".format(trendDifference))
            result.append("$label tolerance | %.4f\n".format(tolerance))
            result.append("$label diagnostic | ${if (stable) "STABLE (within heuristic tolerance)" else "CHECK VARIABILITY"}\n\n")
        }
        result.append("Interpretation | Stability flags use a descriptive heuristic, not a formal convergence proof.\n")
        result.append("Diagnostics are descriptive only and do not establish causality or prediction.\n")
    }

    private fun appendStep108BootstrapConvergence(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 108 - BOOTSTRAP CONVERGENCE ANALYSIS\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Bootstrap convergence analysis unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val s = values.sorted()
            return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        fun metric(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val s = v.sorted()
            val lower = s.take(s.size / 2)
            val upper = s.drop((s.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val batchCount = 10
        val perBatch = 200
        val rng = java.util.Random(108L)
        val cumulative = labels.indices.map { ArrayList<Double>() }
        result.append("Valid cross-shell gap count | ${gaps.size}\n")
        result.append("Bootstrap batches | $batchCount\n")
        result.append("Resamples per batch | $perBatch\n")
        result.append("Total resamples | ${batchCount * perBatch}\n\n")
        repeat(batchCount) { batchIndex ->
            val batchValues = labels.indices.map { ArrayList<Double>(perBatch) }
            repeat(perBatch) {
                val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                val m = metric(sample)
                m.forEachIndexed { i, value ->
                    batchValues[i].add(value)
                    cumulative[i].add(value)
                }
            }
            result.append("Batch ${batchIndex + 1}:\n")
            labels.forEachIndexed { i, label ->
                result.append("$label batch mean | %.4f\n".format(batchValues[i].average()))
            }
            result.append("\n")
        }
        result.append("Cumulative convergence summary:\n")
        labels.forEachIndexed { i, label ->
            val values = cumulative[i]
            val overallMean = values.average()
            val overallSD = kotlin.math.sqrt(values.map { (it - overallMean) * (it - overallMean) }.average())
            val lastBatch = values.takeLast(perBatch).average()
            result.append("$label cumulative mean | %.4f\n".format(overallMean))
            result.append("$label cumulative SD | %.4f\n".format(overallSD))
            result.append("$label last-batch vs cumulative mean difference | %.4f\n".format(lastBatch - overallMean))
        }
        result.append("\nInterpretation | Batch summaries and last-batch differences provide a descriptive convergence check.\n")
        result.append("Convergence diagnostics are descriptive and do not establish causality or prediction.\n")
    }
    private fun appendStep107BootstrapDistributionShape(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 107 - BOOTSTRAP DISTRIBUTION SHAPE ANALYSIS\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Bootstrap distribution shape analysis unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val s = values.sorted()
            return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val s = v.sorted()
            val lo = s.take(s.size / 2)
            val hi = s.drop((s.size + 1) / 2)
            val q1 = if (lo.isNotEmpty()) median(lo) else med
            val q3 = if (hi.isNotEmpty()) median(hi) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val rng = java.util.Random(107L)
        val resampleCount = 1000
        val distributions = labels.indices.map { ArrayList<Double>(resampleCount) }
        repeat(resampleCount) {
            val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
            val values = metrics(sample)
            values.forEachIndexed { i, value -> distributions[i].add(value) }
        }
        result.append("Valid cross-shell gap count | ${gaps.size}\n")
        result.append("Bootstrap resamples | $resampleCount\n\n")
        distributions.forEachIndexed { index, values ->
            val mean = values.average()
            val med = median(values)
            val variance = values.map { (it - mean) * (it - mean) }.average()
            val sd = kotlin.math.sqrt(variance)
            val skew = if (sd > 0.0) values.map { ((it - mean) / sd).let { z -> z * z * z } }.average() else 0.0
            result.append("${labels[index]} bootstrap mean | %.4f\n".format(mean))
            result.append("${labels[index]} bootstrap median | %.4f\n".format(med))
            result.append("${labels[index]} mean-median difference | %.4f\n".format(mean - med))
            result.append("${labels[index]} bootstrap SD | %.4f\n".format(sd))
            result.append("${labels[index]} bootstrap skewness | %.4f\n\n".format(skew))
        }
        result.append("Interpretation | Mean-median difference and skewness summarize bootstrap distribution shape; values are descriptive.\n")
        result.append("This profile does not establish causality or prediction.\n")
    }

    private fun appendStep106BootstrapIntervalStability(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 106 - BOOTSTRAP INTERVAL STABILITY PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Bootstrap interval stability profile unavailable.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            val n = sorted.size
            return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val batchCount = 10
        val resamplesPerBatch = 200
        val rng = java.util.Random(106L)
        val batchIntervals = ArrayList<List<Pair<Double, Double>>>()
        repeat(batchCount) {
            val samples = ArrayList<List<Double>>(resamplesPerBatch)
            repeat(resamplesPerBatch) {
                val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
                samples.add(metrics(sample))
            }
            val intervals = labels.indices.map { index ->
                val sorted = samples.map { it[index] }.sorted()
                val low = sorted[(0.025 * (resamplesPerBatch - 1)).toInt()]
                val high = sorted[(0.975 * (resamplesPerBatch - 1)).toInt()]
                Pair(low, high)
            }
            batchIntervals.add(intervals)
        }
        result.append("Valid cross-shell gap count | ${gaps.size}\n")
        result.append("Bootstrap batches | $batchCount\n")
        result.append("Resamples per batch | $resamplesPerBatch\n")
        result.append("Total resamples | ${batchCount * resamplesPerBatch}\n\n")
        labels.forEachIndexed { index, label ->
            val widths = batchIntervals.map { it[index].second - it[index].first }
            val endpointsLow = batchIntervals.map { it[index].first }
            val endpointsHigh = batchIntervals.map { it[index].second }
            result.append("$label mean interval width across batches | %.4f\n".format(widths.average()))
            result.append("$label interval-width range | %.4f to %.4f\n".format(widths.minOrNull() ?: 0.0, widths.maxOrNull() ?: 0.0))
            result.append("$label lower-bound batch range | %.4f to %.4f\n".format(endpointsLow.minOrNull() ?: 0.0, endpointsLow.maxOrNull() ?: 0.0))
            result.append("$label upper-bound batch range | %.4f to %.4f\n\n".format(endpointsHigh.minOrNull() ?: 0.0, endpointsHigh.maxOrNull() ?: 0.0))
        }
        result.append("Interpretation | Batch-to-batch interval variation is a descriptive check on bootstrap interval stability.\n")
        result.append("This profile does not establish causality or prediction.\n")
    }

    private fun appendStep105RobustBootstrapProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 105 - ROBUST DISPERSION BOOTSTRAP CONFIDENCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Bootstrap confidence profile unavailable.\n")
            result.append("Statistics are descriptive only, not causal or predictive.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            val n = sorted.size
            return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        fun metrics(sample: List<Int>): List<Double> {
            val v = sample.map { it.toDouble() }
            val med = median(v)
            val sorted = v.sorted()
            val lower = sorted.take(sorted.size / 2)
            val upper = sorted.drop((sorted.size + 1) / 2)
            val q1 = if (lower.isNotEmpty()) median(lower) else med
            val q3 = if (upper.isNotEmpty()) median(upper) else med
            val mad = median(v.map { kotlin.math.abs(it - med) })
            return listOf(med, q3 - q1, mad, 1.4826 * mad)
        }
        val bootstrapCount = 1000
        val rng = java.util.Random(105L)
        val bootstrapMetrics = ArrayList<List<Double>>(bootstrapCount)
        repeat(bootstrapCount) {
            val sample = List(gaps.size) { gaps[rng.nextInt(gaps.size)] }
            bootstrapMetrics.add(metrics(sample))
        }
        val labels = listOf("Median", "IQR", "MAD", "MAD-based robust scale")
        val baseline = metrics(gaps)
        result.append("Valid cross-shell gap count | ${gaps.size}\n")
        result.append("Bootstrap resamples | $bootstrapCount\n")
        result.append("Confidence interval | Percentile 95% (2.5th to 97.5th)\n")
        labels.forEachIndexed { index, label ->
            val sorted = bootstrapMetrics.map { it[index] }.sorted()
            val lowIndex = (0.025 * (bootstrapCount - 1)).toInt()
            val highIndex = (0.975 * (bootstrapCount - 1)).toInt()
            result.append("$label observed value | %.4f\n".format(baseline[index]))
            result.append("$label bootstrap 95% interval | %.4f to %.4f\n".format(sorted[lowIndex], sorted[highIndex]))
        }
        result.append("Interpretation | Bootstrap intervals estimate resampling variability in robust gap summaries.\n")
        result.append("Bootstrap resampling is descriptive and does not establish causality or prediction.\n")
    }

    private fun appendStep103RobustGapDispersion(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 103 - CROSS-SHELL GAP ROBUST DISPERSION PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.isEmpty()) {
            result.append("No valid cross-shell gaps available.\n")
            result.append("Robust dispersion metrics unavailable.\n")
            result.append("Statistics are descriptive only, not causal or predictive.\n")
            return
        }
        fun median(values: List<Double>): Double {
            val sorted = values.sorted()
            val n = sorted.size
            return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
        }
        val values = gaps.map { it.toDouble() }
        val med = median(values)
        val q1 = median(values.filterIndexed { i, _ -> i < values.size }.sorted().let { sorted ->
            sorted.take(sorted.size / 2)
        })
        val sortedValues = values.sorted()
        val lower = sortedValues.take(sortedValues.size / 2)
        val upper = sortedValues.drop((sortedValues.size + 1) / 2)
        val q1v = if (lower.isNotEmpty()) median(lower) else med
        val q3v = if (upper.isNotEmpty()) median(upper) else med
        val iqr = q3v - q1v
        val mad = median(values.map { kotlin.math.abs(it - med) })
        val robustScale = 1.4826 * mad
        result.append("Valid cross-shell gap count | ${gaps.size}\n")
        result.append("Median gap | %.4f\n".format(med))
        result.append("Q1 | %.4f\n".format(q1v))
        result.append("Q3 | %.4f\n".format(q3v))
        result.append("Interquartile range (IQR) | %.4f\n".format(iqr))
        result.append("Median absolute deviation (MAD) | %.4f\n".format(mad))
        result.append("MAD-based robust scale (1.4826 x MAD) | %.4f\n".format(robustScale))
        result.append("Interpretation | Robust summaries describe gap dispersion with reduced sensitivity to extreme values.\n")
        result.append("Statistics are descriptive only, not causal or predictive.\n")
    }

    private fun appendStep102Analysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 102 - CROSS-SHELL GAP MEAN AND VARIANCE JACKKNIFE PROFILE\n")
        result.append("=============================\n\n")
        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps.size < 2) {
            result.append("At least two cross-shell links are required for jackknife analysis.\n")
            result.append("Metrics unavailable for this dataset.\n")
            result.append("Statistics are descriptive only, not causal or predictive.\n")
            return
        }
        val mean = gaps.average()
        val variance = gaps.map { (it - mean) * (it - mean) }.average()
        val jackMeans = gaps.indices.map { i -> gaps.filterIndexed { j, _ -> j != i }.average() }
        val jackVars = gaps.indices.map { i ->
            val remaining = gaps.filterIndexed { j, _ -> j != i }
            val m = remaining.average()
            remaining.map { (it - m) * (it - m) }.average()
        }
        val meanRange = (jackMeans.minOrNull() ?: mean)..(jackMeans.maxOrNull() ?: mean)
        val varianceRange = (jackVars.minOrNull() ?: variance)..(jackVars.maxOrNull() ?: variance)
        val meanJackknifeSE = kotlin.math.sqrt((gaps.size - 1).toDouble() / gaps.size * jackMeans.sumOf { (it - jackMeans.average()) * (it - jackMeans.average()) })
        val varJackknifeSE = kotlin.math.sqrt((gaps.size - 1).toDouble() / gaps.size * jackVars.sumOf { (it - jackVars.average()) * (it - jackVars.average()) })
        result.append("Cross-shell gap link count | ${gaps.size}\n")
        result.append("Baseline mean gap | %.4f\n".format(mean))
        result.append("Baseline population variance | %.4f\n".format(variance))
        result.append("Leave-one-link-out mean range | %.4f to %.4f\n".format(meanRange.start, meanRange.endInclusive))
        result.append("Leave-one-link-out variance range | %.4f to %.4f\n".format(varianceRange.start, varianceRange.endInclusive))
        result.append("Jackknife SE (mean gap) | %.4f\n".format(meanJackknifeSE))
        result.append("Jackknife SE (variance) | %.4f\n".format(varJackknifeSE))
        result.append("Interpretation | Describes the sensitivity of mean and variance summaries to one-link removal.\n")
        result.append("Statistics are descriptive only, not causal or predictive.\n")
    }

    private fun appendStep100MilestoneAnalysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 100 - SHELL GAP ANALYSIS MILESTONE SUMMARY\n")
        result.append("=============================\n\n")

        val gaps = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }

        if (gaps.isEmpty()) {
            result.append("No cross-shell links available for Step 100 summary.\n")
            result.append("Summary metrics cannot be calculated for an empty gap dataset.\n")
            result.append("\nStatistics are descriptive only, not causal or predictive.\n")
            return
        }

        val observedFreq = gaps.groupingBy { it }.eachCount().toSortedMap()
        val freq = if (observedFreq.isEmpty()) sortedMapOf<Int, Int>() else
            (observedFreq.keys.first()..observedFreq.keys.last()).associateWith { observedFreq[it] ?: 0 }.toSortedMap()
        val sorted = gaps.sorted()
        val total = gaps.size
        val mean = gaps.average()
        val median = if (sorted.size % 2 == 1) sorted[sorted.size / 2].toDouble()
            else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        val variance = gaps.map { (it - mean) * (it - mean) }.average()
        val sd = kotlin.math.sqrt(variance)
        val cumulative = freq.entries.fold(0) { acc, entry -> acc + entry.value }
        val shares = freq.mapValues { it.value.toDouble() / total.toDouble() }
        val entropy = -shares.values.filter { it > 0.0 }.sumOf { it * kotlin.math.ln(it) }
        val normalizedEntropy = if (freq.size > 1) entropy / kotlin.math.ln(freq.size.toDouble()) else 0.0
        val peakGaps = freq.keys.filterIndexed { index, gap ->
            index > 0 && index < freq.size - 1 &&
                freq[gap]!! > freq[freq.keys.elementAt(index - 1)]!! &&
                freq[gap]!! > freq[freq.keys.elementAt(index + 1)]!!
        }
        val valleyGaps = freq.keys.filterIndexed { index, gap ->
            index > 0 && index < freq.size - 1 &&
                freq[gap]!! < freq[freq.keys.elementAt(index - 1)]!! &&
                freq[gap]!! < freq[freq.keys.elementAt(index + 1)]!!
        }

        result.append("Total cross-shell links represented | $total\n")
        result.append("Observed shell-gap categories | ${freq.size}\n")
        result.append("Minimum gap | ${sorted.first()}\n")
        result.append("Maximum gap | ${sorted.last()}\n")
        result.append("Mean gap | %.3f\n".format(mean))
        result.append("Median gap | %.3f\n".format(median))
        result.append("Variance | %.3f\n".format(variance))
        result.append("Standard deviation | %.3f\n".format(sd))
        result.append("Gap frequency | ${freq.entries.joinToString { "${it.key}:${it.value}" }}\n")
        result.append("Gap share (%) | ${freq.entries.joinToString { "${it.key}:%.2f".format(shares[it.key]!! * 100.0) }}\n")
        result.append("Cumulative count (ordered gap categories) | $cumulative\n")
        result.append("Shannon entropy | %.4f\n".format(entropy))
        result.append("Normalized entropy | %.4f\n".format(normalizedEntropy))
        result.append("Detected local peaks | ${if (peakGaps.isEmpty()) "none" else peakGaps.joinToString()}\n")
        result.append("Detected local valleys | ${if (valleyGaps.isEmpty()) "none" else valleyGaps.joinToString()}\n")
        result.append("Peak and valley detection uses strict comparisons of adjacent observed gap categories.\n")
        result.append("\nThis milestone summary consolidates descriptive metrics and does not imply causation or prediction.\n")
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

            // STEP 26: BHAVA LORD CROSS-LINK MATRIX
            result.append("\n=============================\n")
            result.append("STEP 26 - BHAVA LORD CROSS-LINK MATRIX\n")
            result.append("=============================\n\n")
            result.append("Descriptive mapping of each bhava to the bhava occupied by its lord. No scoring or prediction is applied.\n\n")

            val step26LinksByDestination = mutableMapOf<Int, MutableList<Int>>()
            for (sourceHouse in 1..12) {
                val sourceSignIndex = (lagnaSign + sourceHouse - 1) % 12
                val sourceLord = signLords[sourceSignIndex]
                val sourceLordLongitude = planetPositions[sourceLord]
                val destinationHouse = sourceLordLongitude?.let { houseNumber(it, lagnaSidereal) }
                result.append("House $sourceHouse lord ($sourceLord): ")
                if (destinationHouse != null && destinationHouse in 1..12) {
                    result.append("placed in House $destinationHouse\n")
                    step26LinksByDestination.getOrPut(destinationHouse) { mutableListOf() }.add(sourceHouse)
                } else {
                    result.append("position unavailable\n")
                }
            }
            result.append("\nDESTINATION HOUSE SUMMARY\n")
            for (destinationHouse in 1..12) {
                val sourceHouses = step26LinksByDestination[destinationHouse] ?: emptyList()
                result.append("House $destinationHouse receives lord-links from: ")
                result.append(if (sourceHouses.isEmpty()) "None" else sourceHouses.joinToString { "House $it" })
                result.append("\n")
            }
            result.append("Step 26 note: these are lord-placement relationships only; they are not strength, severity, or predictive scores.\n")

            // STEP 27: BHAVA NETWORK SUMMARY (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 27 - BHAVA NETWORK SUMMARY\n")
            result.append("=============================\n\n")
            result.append("Overview of house-lord links and factor associations as a descriptive network. No scoring or prediction is applied.\n\n")

            for (house in 1..12) {
                val signForNetwork = (lagnaSign + house - 1) % 12
                val lordForNetwork = signLords[signForNetwork]
                val lordLongForNetwork = planetPositions[lordForNetwork]
                val destinationForNetwork = lordLongForNetwork?.let { houseNumber(it, lagnaSidereal) }
                val factorsForNetwork = step24FactorsByHouse[house] ?: emptySet()
                val occupantsForNetwork = planetPositions.filter { houseNumber(it.value, lagnaSidereal) == house }.keys
                val incomingForNetwork = step26LinksByDestination[house] ?: emptyList()

                result.append("HOUSE $house NETWORK NODE\n")
                result.append("House sign: ${rasiNames[signForNetwork]}\n")
                result.append("Occupants: ${if (occupantsForNetwork.isEmpty()) "None" else occupantsForNetwork.joinToString()}\n")
                result.append("House lord: $lordForNetwork\n")
                result.append("Lord destination: ${destinationForNetwork?.let { "House $it" } ?: "Unavailable"}\n")
                result.append("Incoming lord-links from: ${if (incomingForNetwork.isEmpty()) "None" else incomingForNetwork.joinToString { "House $it" }}\n")
                result.append("Associated factors: ${if (factorsForNetwork.isEmpty()) "None" else factorsForNetwork.joinToString()}\n")
                result.append("Network description: House $house lord links to ${destinationForNetwork?.let { "House $it" } ?: "an unavailable destination"}; ${incomingForNetwork.size} incoming link(s).\n\n")
            }
            result.append("Step 27 note: this is a structural relationship summary only. It does not imply beneficial/adverse effects, severity, or predictive certainty.\n")

            // STEP 28: BHAVA NETWORK PATTERN ANALYSIS (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 28 - BHAVA NETWORK PATTERN ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Patterns are grouped from the existing house-lord links and factor associations. This is descriptive only; no scoring is applied.\n\n")

            val step28HighIncomingNodes = (1..12).filter { (step26LinksByDestination[it] ?: emptyList()).size >= 2 }
            val step28FactorClusters = (1..12).filter { (step24FactorsByHouse[it] ?: emptySet()).isNotEmpty() }
            val step28OccupiedHouses = (1..12).filter { house -> planetPositions.any { houseNumber(it.value, lagnaSidereal) == house } }

            result.append("NETWORK HUB PATTERN (2 or more incoming lord-links)\n")
            result.append(if (step28HighIncomingNodes.isEmpty()) "No house has 2 or more incoming lord-links.\n\n" else step28HighIncomingNodes.joinToString { "House $it (${(step26LinksByDestination[it] ?: emptyList()).size} incoming)" } + "\n\n")

            result.append("FACTOR ASSOCIATION CLUSTER\n")
            result.append(if (step28FactorClusters.isEmpty()) "No houses have listed factor associations.\n\n" else step28FactorClusters.joinToString { house -> "House $house: ${(step24FactorsByHouse[house] ?: emptySet()).joinToString()}" } + "\n\n")

            result.append("OCCUPIED HOUSE GROUP\n")
            result.append(if (step28OccupiedHouses.isEmpty()) "No occupied houses identified.\n\n" else step28OccupiedHouses.joinToString { "House $it" } + "\n\n")

            result.append("HOUSE-LORD DESTINATION PATTERN\n")
            for (sourceHouse in 1..12) {
                val sourceSign = (lagnaSign + sourceHouse - 1) % 12
                val sourceLord = signLords[sourceSign]
                val sourceLordLongitude = planetPositions[sourceLord]
                val targetHouse = sourceLordLongitude?.let { houseNumber(it, lagnaSidereal) }
                result.append("House $sourceHouse lord ($sourceLord) -> ${targetHouse?.let { "House $it" } ?: "Unavailable"}\n")
            }
            result.append("\nStep 28 note: these are organizational patterns derived from Steps 24, 26, and 27. They do not establish strength, outcomes, or predictive certainty.\n")

            // STEP 29: BHAVA NETWORK CONNECTIVITY ANALYSIS (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 29 - BHAVA NETWORK CONNECTIVITY ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Direct house-lord connectivity, incoming links, and isolated nodes are summarized descriptively. No scoring is applied.\n\n")

            val step29OutgoingTargets = mutableMapOf<Int, Int>()
            val step29IncomingCounts = (1..12).associateWith { (step26LinksByDestination[it] ?: emptyList()).size }
            for (sourceHouse in 1..12) {
                val sourceSign = (lagnaSign + sourceHouse - 1) % 12
                val sourceLord = signLords[sourceSign]
                val sourceLongitude = planetPositions[sourceLord]
                val targetHouse = sourceLongitude?.let { houseNumber(it, lagnaSidereal) }
                if (targetHouse != null && targetHouse in 1..12) step29OutgoingTargets[sourceHouse] = targetHouse
            }

            result.append("DIRECT CONNECTIVITY (SOURCE -> DESTINATION)\n")
            for (sourceHouse in 1..12) {
                val target = step29OutgoingTargets[sourceHouse]
                result.append("House $sourceHouse -> ${target?.let { "House $it" } ?: "Unavailable"}\n")
            }
            result.append("\nMULTI-LINK DESTINATION HOUSES (2 or more incoming)\n")
            val step29MultiLinkHouses = (1..12).filter { (step29IncomingCounts[it] ?: 0) >= 2 }
            result.append(if (step29MultiLinkHouses.isEmpty()) "None identified\n\n" else step29MultiLinkHouses.joinToString { "House $it (${step29IncomingCounts[it]} incoming)" } + "\n\n")

            result.append("ISOLATED HOUSE NODES (no incoming links and no available outgoing link)\n")
            val step29IsolatedHouses = (1..12).filter { house -> (step29IncomingCounts[house] ?: 0) == 0 && !step29OutgoingTargets.containsKey(house) }
            result.append(if (step29IsolatedHouses.isEmpty()) "None identified\n\n" else step29IsolatedHouses.joinToString { "House $it" } + "\n\n")

            result.append("CONNECTIVITY SUMMARY\n")
            result.append("Available direct lord-links: ${step29OutgoingTargets.size} of 12\n")
            result.append("Houses receiving 2 or more links: ${step29MultiLinkHouses.size}\n")
            result.append("Isolated nodes under the defined criteria: ${step29IsolatedHouses.size}\n")
            result.append("Step 29 note: connectivity is a structural description only; it does not indicate beneficence, maleficence, strength, or predictive certainty.\n")

            // STEP 30: BHAVA NETWORK STRUCTURAL CLASSIFICATION (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 30 - BHAVA NETWORK STRUCTURAL CLASSIFICATION\n")
            result.append("=============================\n\n")
            result.append("Classifies houses by incoming lord-link count. These labels are organizational only.\n\n")

            val step30IncomingCounts = (1..12).associateWith { (step26LinksByDestination[it] ?: emptyList()).size }
            val step30HighConnectivity = (1..12).filter { (step30IncomingCounts[it] ?: 0) >= 3 }
            val step30ModerateConnectivity = (1..12).filter { (step30IncomingCounts[it] ?: 0) in 2..2 }
            val step30LowConnectivity = (1..12).filter { (step30IncomingCounts[it] ?: 0) == 1 }
            val step30NoIncoming = (1..12).filter { (step30IncomingCounts[it] ?: 0) == 0 }

            result.append("HIGH CONNECTIVITY (3 or more incoming links)\n")
            result.append(if (step30HighConnectivity.isEmpty()) "None identified\n\n" else step30HighConnectivity.joinToString { "House $it (${step30IncomingCounts[it]} links)" } + "\n\n")
            result.append("MODERATE CONNECTIVITY (2 incoming links)\n")
            result.append(if (step30ModerateConnectivity.isEmpty()) "None identified\n\n" else step30ModerateConnectivity.joinToString { "House $it (2 links)" } + "\n\n")
            result.append("LOW CONNECTIVITY (1 incoming link)\n")
            result.append(if (step30LowConnectivity.isEmpty()) "None identified\n\n" else step30LowConnectivity.joinToString { "House $it (1 link)" } + "\n\n")
            result.append("NO INCOMING LINK (0 incoming links)\n")
            result.append(if (step30NoIncoming.isEmpty()) "None identified\n\n" else step30NoIncoming.joinToString { "House $it" } + "\n\n")
            result.append("Step 30 note: this classification is based only on incoming-link counts and is not an astrological strength score or prediction.\n")

            // STEP 31: BHAVA NETWORK TOPOLOGY ANALYSIS (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 31 - BHAVA NETWORK TOPOLOGY ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Source nodes, destination nodes, reciprocal links, and directed loops are summarized structurally. No scoring or prediction is applied.\n\n")

            val step31Edges = step29OutgoingTargets.filter { (source, target) -> source in 1..12 && target in 1..12 }
            val step31Sources = step31Edges.keys.sorted()
            val step31Destinations = step31Edges.values.toSet().sorted()

            result.append("SOURCE NODES (houses with available outgoing lord-links)\n")
            result.append(if (step31Sources.isEmpty()) "None identified\n\n" else step31Sources.joinToString { "House $it -> House ${step31Edges[it]}" } + "\n\n")

            result.append("DESTINATION NODES (houses receiving outgoing lord-links)\n")
            result.append(if (step31Destinations.isEmpty()) "None identified\n\n" else step31Destinations.joinToString { "House $it" } + "\n\n")

            val step31Bidirectional = mutableListOf<String>()
            for (a in 1..12) {
                val b = step31Edges[a] ?: continue
                if (b != a && step31Edges[b] == a && a < b) step31Bidirectional.add("House $a <-> House $b")
            }
            result.append("BIDIRECTIONAL LINKS (reciprocal direct connections)\n")
            result.append(if (step31Bidirectional.isEmpty()) "None identified\n\n" else step31Bidirectional.joinToString("\n") + "\n\n")

            // Find directed cycles in the single-outgoing-edge network, avoiding duplicate cycle reports.
            val step31Cycles = mutableListOf<List<Int>>()
            val step31CycleKeys = mutableSetOf<String>()
            for (startNode in 1..12) {
                val path = mutableListOf<Int>()
                val indexInPath = mutableMapOf<Int, Int>()
                var current: Int? = startNode
                while (current != null && current in 1..12 && !indexInPath.containsKey(current)) {
                    indexInPath[current] = path.size
                    path.add(current)
                    current = step31Edges[current]
                }
                if (current != null && indexInPath.containsKey(current)) {
                    val cycle = path.subList(indexInPath[current]!!, path.size).toList()
                    if (cycle.isNotEmpty()) {
                        val canonical = cycle.indices.map { offset -> cycle.drop(offset) + cycle.take(offset) }.minBy { it.joinToString(",") }
                        val key = canonical.joinToString(",")
                        if (step31CycleKeys.add(key)) step31Cycles.add(canonical)
                    }
                }
            }
            result.append("DIRECTED NETWORK LOOPS (cycles in house-lord destinations)\n")
            result.append(if (step31Cycles.isEmpty()) "None identified\n\n" else step31Cycles.joinToString("\n") { cycle -> cycle.joinToString(" -> ") { "House $it" } + " -> House ${cycle.first()}" } + "\n\n")

            result.append("TOPOLOGY SUMMARY\n")
            result.append("Source nodes: ${step31Sources.size}\n")
            result.append("Distinct destination nodes: ${step31Destinations.size}\n")
            result.append("Reciprocal link pairs: ${step31Bidirectional.size}\n")
            result.append("Directed loop patterns: ${step31Cycles.size}\n")
            result.append("Step 31 note: topology labels describe connection structure only and do not imply astrological strength, outcomes, or predictive certainty.\n")

            // STEP 32: BHAVA NETWORK PATH ANALYSIS (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 32 - BHAVA NETWORK PATH ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Direct paths, two-step paths, and reachability are summarized from the house-lord network. No scoring or prediction is applied.\n\n")

            val step32Edges = step29OutgoingTargets.filter { (source, target) -> source in 1..12 && target in 1..12 }
            result.append("DIRECT PATHS (one-step connections)\n")
            result.append(if (step32Edges.isEmpty()) "None identified\n\n" else step32Edges.entries.sortedBy { it.key }.joinToString("\n") { "House ${it.key} -> House ${it.value}" } + "\n\n")

            val step32TwoStepPaths = mutableListOf<String>()
            for (source in 1..12) {
                val intermediate = step32Edges[source] ?: continue
                val destination = step32Edges[intermediate] ?: continue
                step32TwoStepPaths.add("House $source -> House $intermediate -> House $destination")
            }
            result.append("TWO-STEP PATHS (one intermediate house)\n")
            result.append(if (step32TwoStepPaths.isEmpty()) "None identified\n\n" else step32TwoStepPaths.distinct().joinToString("\n") + "\n\n")

            val step32Reachability = mutableMapOf<Int, List<Int>>()
            for (source in 1..12) {
                val reached = mutableListOf<Int>()
                val visited = mutableSetOf<Int>()
                var current = step32Edges[source]
                while (current != null && current in 1..12 && visited.add(current)) {
                    reached.add(current)
                    current = step32Edges[current]
                }
                step32Reachability[source] = reached
            }
            result.append("REACHABILITY (houses reachable by following lord-links)\n")
            for (source in 1..12) {
                val reached = step32Reachability[source] ?: emptyList()
                result.append("House $source reaches: ${if (reached.isEmpty()) "None" else reached.joinToString { "House $it" }}\n")
            }
            result.append("\nPATH SUMMARY\n")
            result.append("Direct paths: ${step32Edges.size}\n")
            result.append("Two-step paths: ${step32TwoStepPaths.distinct().size}\n")
            result.append("Reachability rows: ${step32Reachability.size}\n")
            result.append("Step 32 note: path and reachability descriptions reflect the directed house-lord network only; they do not imply astrological strength, outcomes, or predictive certainty.\n")

            // STEP 33: BHAVA NETWORK INFLUENCE PROPAGATION (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 33 - BHAVA NETWORK INFLUENCE PROPAGATION\n")
            result.append("=============================\n\n")
            result.append("Direct and indirect propagation chains are described by traversing house-lord links. No influence strength, positive/negative interpretation, score, or prediction is applied.\n\n")

            val step33PropagationChains = mutableMapOf<Int, List<Int>>()
            val step33Termination = mutableMapOf<Int, String>()
            for (source in 1..12) {
                val chain = mutableListOf<Int>()
                val visited = mutableSetOf<Int>()
                visited.add(source)
                var current = step32Edges[source]
                var termination = "No outgoing link"
                while (current != null && current in 1..12) {
                    if (!visited.add(current)) {
                        chain.add(current)
                        termination = "Loop detected at House $current"
                        break
                    }
                    chain.add(current)
                    if (chain.size >= 12) {
                        termination = "Traversal limit reached"
                        break
                    }
                    val next = step32Edges[current]
                    if (next == null) {
                        termination = "Chain ends at House $current"
                        break
                    }
                    current = next
                }
                if (chain.isEmpty() && source in step32Edges) termination = "Invalid or unavailable destination"
                step33PropagationChains[source] = chain
                step33Termination[source] = termination
            }

            result.append("LEVEL 1 - DIRECT LINKS\n")
            result.append(if (step32Edges.isEmpty()) "None identified\n\n" else step32Edges.entries.sortedBy { it.key }.joinToString("\n") { "House ${it.key} -> House ${it.value}" } + "\n\n")

            result.append("PROPAGATION CHAINS (direct and indirect structural links)\n")
            for (source in 1..12) {
                val chain = step33PropagationChains[source] ?: emptyList()
                result.append("House $source: ${if (chain.isEmpty()) "No propagation chain" else (listOf(source) + chain).joinToString(" -> ") { "House $it" }}\n")
            }
            result.append("\nCHAIN TERMINATION\n")
            for (source in 1..12) {
                result.append("House $source: ${step33Termination[source] ?: "Unavailable"}\n")
            }
            result.append("\nPROPAGATION SUMMARY\n")
            result.append("Chains analyzed: ${step33PropagationChains.size}\n")
            result.append("Chains with at least one indirect step: ${step33PropagationChains.values.count { it.size >= 2 }}\n")
            result.append("Chains with loop detection: ${step33Termination.values.count { it.contains("Loop detected") }}\n")
            result.append("Step 33 note: propagation is a graph traversal of house-lord links only and does not establish astrological strength, causation, outcomes, or predictive certainty.\n")

            // STEP 34: BHAVA NETWORK DEPENDENCY ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 34 - BHAVA NETWORK DEPENDENCY ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Dependency is summarized from the directed house-lord link network. This is structural only; no strength, outcome, or prediction is inferred.\n\n")

            val step34PrimaryDependency = mutableMapOf<Int, Int?>()
            val step34DependencyChains = mutableMapOf<Int, List<Int>>()
            val step34TerminalNodes = mutableMapOf<Int, Int?>()
            for (source in 1..12) {
                val direct = step32Edges[source]
                step34PrimaryDependency[source] = direct
                val chain = mutableListOf<Int>()
                val visited = mutableSetOf(source)
                var current = direct
                while (current != null && current in 1..12 && visited.add(current) && chain.size < 12) {
                    chain.add(current)
                    current = step32Edges[current]
                }
                step34DependencyChains[source] = chain
                step34TerminalNodes[source] = chain.lastOrNull()
            }

            result.append("PRIMARY DEPENDENCY (direct link)\n")
            for (source in 1..12) {
                val direct = step34PrimaryDependency[source]
                result.append("House $source: ${direct?.let { "depends structurally on House $it" } ?: "No direct link"}\n")
            }

            result.append("\nSECONDARY DEPENDENCY (indirect chain)\n")
            for (source in 1..12) {
                val chain = step34DependencyChains[source] ?: emptyList()
                val indirect = chain.drop(1)
                result.append("House $source: ${if (indirect.isEmpty()) "No secondary dependency identified" else indirect.joinToString(" -> ") { "House $it" }}\n")
            }

            result.append("\nDEPENDENCY CHAINS\n")
            for (source in 1..12) {
                val chain = step34DependencyChains[source] ?: emptyList()
                result.append("House $source: ${if (chain.isEmpty()) "No chain" else (listOf(source) + chain).joinToString(" -> ") { "House $it" }}\n")
            }

            val step34SharedDependencies = step34TerminalNodes.entries
                .filter { it.value != null }
                .groupBy { it.value!! }
                .filterValues { it.size > 1 }
            result.append("\nSHARED DEPENDENCY (common terminal nodes)\n")
            if (step34SharedDependencies.isEmpty()) {
                result.append("No shared terminal dependency groups identified\n")
            } else {
                for ((destination, sources) in step34SharedDependencies.toSortedMap()) {
                    result.append("House $destination is a common terminal node for: ${sources.map { it.key }.sorted().joinToString { "House $it" }}\n")
                }
            }

            result.append("\nDEPENDENCY SUMMARY\n")
            result.append("House nodes analyzed: 12\n")
            result.append("Direct dependencies identified: ${step34PrimaryDependency.values.count { it != null }}\n")
            result.append("Chains with indirect dependency: ${step34DependencyChains.values.count { it.size >= 2 }}\n")
            result.append("Shared terminal groups: ${step34SharedDependencies.size}\n")
            result.append("Step 34 note: dependency labels describe directed graph relationships only and do not imply causal, beneficial/adverse, or predictive effects.\n")

            // STEP 35: BHAVA NETWORK CENTRALITY ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 35 - BHAVA NETWORK CENTRALITY ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Centrality measures describe connectivity in the directed house-link network only.\n\n")

            val step35InDegree = (1..12).associateWith { house ->
                step32Edges.values.count { destination -> destination == house }
            }
            val step35OutDegree = (1..12).associateWith { house ->
                if (step32Edges[house] != null && step32Edges[house] in 1..12) 1 else 0
            }
            val step35MaximumInDegree = step35InDegree.values.maxOrNull() ?: 0
            val step35CentralHouses = step35InDegree.entries
                .filter { it.value == step35MaximumInDegree && it.value > 0 }
                .map { it.key }
                .sorted()
            val step35NoIncomingLinks = step35InDegree.entries
                .filter { it.value == 0 }
                .map { it.key }
                .sorted()

            result.append("IN-DEGREE (incoming direct links)\n")
            for (house in 1..12) {
                result.append("House $house: ${step35InDegree[house] ?: 0} incoming link(s)\n")
            }

            result.append("\nOUT-DEGREE (outgoing direct links)\n")
            for (house in 1..12) {
                result.append("House $house: ${step35OutDegree[house] ?: 0} outgoing link(s)\n")
            }

            result.append("\nNETWORK HUB(S) - MAXIMUM IN-DEGREE\n")
            result.append(if (step35CentralHouses.isEmpty()) {
                "No incoming-link hub identified\n"
            } else {
                step35CentralHouses.joinToString(", ") { "House $it" } + " with $step35MaximumInDegree incoming link(s)\n"
            })

            result.append("\nHOUSES WITH ZERO INCOMING LINKS\n")
            result.append(if (step35NoIncomingLinks.isEmpty()) {
                "None\n"
            } else {
                step35NoIncomingLinks.joinToString(", ") { "House $it" } + "\n"
            })

            result.append("\nCENTRALITY SUMMARY\n")
            result.append("House nodes analyzed: 12\n")
            result.append("Valid direct links: ${step32Edges.count { it.value != null && it.value in 1..12 }}\n")
            result.append("Maximum incoming links for any house: $step35MaximumInDegree\n")
            result.append("Hub house(s): ${if (step35CentralHouses.isEmpty()) "None" else step35CentralHouses.joinToString(", ") { "House $it" }}\n")
            result.append("Zero-incoming house count: ${step35NoIncomingLinks.size}\n")
            result.append("Step 35 note: centrality is a descriptive network-connectivity measure only; it does not imply astrological strength, causation, outcomes, or predictive certainty.\n")

            // STEP 36: BHAVA NETWORK CLUSTER ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 36 - BHAVA NETWORK CLUSTER ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Clusters are weakly connected groups in the house-link network, treating links as undirected for grouping only. No score or prediction is applied.\n\n")

            val step36Adjacency = (1..12).associateWith { mutableSetOf<Int>() }.toMutableMap()
            for ((source, destination) in step32Edges) {
                if (source in 1..12 && destination in 1..12) {
                    step36Adjacency[source]?.add(destination)
                    step36Adjacency[destination]?.add(source)
                }
            }
            val step36Visited = mutableSetOf<Int>()
            val step36Clusters = mutableListOf<List<Int>>()
            for (start in 1..12) {
                if (start in step36Visited) continue
                val queue = mutableListOf(start)
                val cluster = mutableListOf<Int>()
                step36Visited.add(start)
                while (queue.isNotEmpty()) {
                    val node = queue.removeAt(0)
                    cluster.add(node)
                    for (neighbor in (step36Adjacency[node] ?: emptySet()).sorted()) {
                        if (step36Visited.add(neighbor)) queue.add(neighbor)
                    }
                }
                step36Clusters.add(cluster.sorted())
            }

            result.append("WEAKLY CONNECTED CLUSTERS\n")
            for ((index, cluster) in step36Clusters.withIndex()) {
                result.append("Cluster ${index + 1}: ${cluster.joinToString { "House $it" }}\n")
            }

            val step36Isolated = step36Clusters.filter { cluster ->
                cluster.size == 1 && (step36Adjacency[cluster.first()]?.isEmpty() ?: true)
            }.map { it.first() }
            result.append("\nISOLATED HOUSE NODES\n")
            result.append(if (step36Isolated.isEmpty()) "None\n" else step36Isolated.joinToString { "House $it" } + "\n")

            result.append("CLUSTER SUMMARY\n")
            result.append("Cluster count: ${step36Clusters.size}\n")
            result.append("Largest cluster size: ${step36Clusters.maxOfOrNull { it.size } ?: 0}\n")
            result.append("Isolated nodes: ${step36Isolated.size}\n")
            result.append("Step 36 note: cluster grouping reflects undirected connectivity only; it does not imply astrological strength, causation, outcomes, or predictive certainty.\n")

            // STEP 37: BHAVA NETWORK CLUSTER COHESION ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 37 - BHAVA NETWORK CLUSTER COHESION ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Descriptive assessment of within-cluster and between-cluster links; no scoring or prediction is applied.\n\n")

            val step37ClusterOfHouse = mutableMapOf<Int, Int>()
            for ((clusterIndex, cluster) in step36Clusters.withIndex()) {
                for (house in cluster) step37ClusterOfHouse[house] = clusterIndex
            }
            val step37UniqueEdges = step32Edges.mapNotNull { (source, destination) ->
                if (source in 1..12 && destination in 1..12 && source != destination) {
                    Pair(minOf(source, destination), maxOf(source, destination))
                } else null
            }.toSet()
            var step37InterClusterEdges = 0
            for ((a, b) in step37UniqueEdges) {
                if (step37ClusterOfHouse[a] != step37ClusterOfHouse[b]) step37InterClusterEdges++
            }
            result.append("CLUSTER COHESION DETAILS\n")
            for ((index, cluster) in step36Clusters.withIndex()) {
                val members = cluster.toSet()
                val internalEdges = step37UniqueEdges.count { (a, b) -> a in members && b in members }
                val possibleEdges = cluster.size * (cluster.size - 1) / 2
                val density = if (possibleEdges > 0) internalEdges.toDouble() / possibleEdges else 0.0
                result.append("Cluster ${index + 1}: ${cluster.joinToString { "House $it" }}\n")
                result.append("  Internal unique links: $internalEdges\n")
                result.append("  Possible undirected links: $possibleEdges\n")
                result.append("  Link density: ${String.format(Locale.US, "%.3f", density)}\n")
            }
            result.append("\nINTER-CLUSTER LINKS\n")
            result.append("Unique links crossing cluster boundaries: $step37InterClusterEdges\n")
            result.append("Step 37 note: cluster cohesion and density are descriptive graph properties only; they do not indicate astrological strength, causation, outcomes, or predictive certainty.\n")

            // STEP 38: BHAVA NETWORK BRIDGE / ARTICULATION NODE ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 38 - BHAVA NETWORK BRIDGE / ARTICULATION NODE ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Identifies house nodes whose removal would increase the number of connected components in the undirected network.\n\n")

            val step38Discovery = mutableMapOf<Int, Int>()
            val step38Low = mutableMapOf<Int, Int>()
            val step38Parent = mutableMapOf<Int, Int>()
            val step38Articulation = mutableSetOf<Int>()
            var step38Time = 0
            fun step38Dfs(node: Int) {
                step38Time++
                step38Discovery[node] = step38Time
                step38Low[node] = step38Time
                var children = 0
                for (neighbor in (step36Adjacency[node] ?: emptySet()).sorted()) {
                    if (neighbor !in step38Discovery) {
                        children++
                        step38Parent[neighbor] = node
                        step38Dfs(neighbor)
                        step38Low[node] = minOf(step38Low[node] ?: step38Time, step38Low[neighbor] ?: step38Time)
                        if (node !in step38Parent && children > 1) step38Articulation.add(node)
                        if (node in step38Parent && (step38Low[neighbor] ?: step38Time) >= (step38Discovery[node] ?: step38Time)) step38Articulation.add(node)
                    } else if (neighbor != step38Parent[node]) {
                        step38Low[node] = minOf(step38Low[node] ?: step38Time, step38Discovery[neighbor] ?: step38Time)
                    }
                }
            }
            for (node in 1..12) if (node !in step38Discovery) step38Dfs(node)

            result.append("ARTICULATION / BRIDGE-NODE HOUSES\n")
            result.append(if (step38Articulation.isEmpty()) "None identified\n" else step38Articulation.sorted().joinToString { "House $it" } + "\n")
            result.append("\nSTRUCTURAL SUMMARY\n")
            result.append("Articulation-node count: ${step38Articulation.size}\n")
            result.append("These nodes are structural connectors in the undirected graph under this model.\n")
            result.append("Step 38 note: graph separation properties are descriptive only and do not indicate astrological strength, causation, outcomes, or predictive certainty.\n")


            // STEP 39: BHAVA NETWORK BRIDGE-LINK ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 39 - BHAVA NETWORK BRIDGE-LINK ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Identifies undirected connections whose removal would increase the number of connected components.\n\n")

            val step39Discovery = mutableMapOf<Int, Int>()
            val step39Low = mutableMapOf<Int, Int>()
            val step39Parent = mutableMapOf<Int, Int>()
            val step39Bridges = mutableListOf<Pair<Int, Int>>()
            var step39Time = 0

            fun step39Dfs(node: Int) {
                step39Time++
                step39Discovery[node] = step39Time
                step39Low[node] = step39Time
                for (neighbor in (step36Adjacency[node] ?: emptySet()).sorted()) {
                    if (neighbor !in step39Discovery) {
                        step39Parent[neighbor] = node
                        step39Dfs(neighbor)
                        step39Low[node] = minOf(step39Low[node] ?: step39Time, step39Low[neighbor] ?: step39Time)
                        if ((step39Low[neighbor] ?: step39Time) > (step39Discovery[node] ?: step39Time)) {
                            step39Bridges.add(minOf(node, neighbor) to maxOf(node, neighbor))
                        }
                    } else if (neighbor != step39Parent[node]) {
                        step39Low[node] = minOf(step39Low[node] ?: step39Time, step39Discovery[neighbor] ?: step39Time)
                    }
                }
            }

            for (node in 1..12) if (node !in step39Discovery) step39Dfs(node)
            val step39UniqueBridges = step39Bridges.distinct().sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })

            result.append("BRIDGE LINKS\n")
            result.append(if (step39UniqueBridges.isEmpty()) "None identified\n" else step39UniqueBridges.joinToString("\n") { "House ${it.first} <-> House ${it.second}" } + "\n")
            result.append("\nSTRUCTURAL SUMMARY\n")
            result.append("Bridge-link count: ${step39UniqueBridges.size}\n")
            result.append("Bridge links are connections whose removal separates part of this undirected graph.\n")
            result.append("Step 39 note: this is descriptive graph structure only; it does not indicate astrological strength, causation, outcomes, or predictive certainty.\n")

            // STEP 40: BHAVA NETWORK LINK REDUNDANCY ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 40 - BHAVA NETWORK LINK REDUNDANCY ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Classifies each unique undirected network link by whether it is a bridge-link or has an alternate route in the graph.\n\n")

            val step40AllLinks = mutableListOf<Pair<Int, Int>>()
            for (node in 1..12) {
                for (neighbor in (step36Adjacency[node] ?: emptySet()).sorted()) {
                    if (node < neighbor) step40AllLinks.add(node to neighbor)
                }
            }
            val step40UniqueLinks = step40AllLinks.distinct().sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
            val step40RedundantLinks = step40UniqueLinks.filter { it !in step39UniqueBridges }
            val step40BridgeLinks = step40UniqueLinks.filter { it in step39UniqueBridges }

            result.append("LINKS WITH ALTERNATE ROUTES (NON-BRIDGE)\n")
            result.append(if (step40RedundantLinks.isEmpty()) "None identified\n" else step40RedundantLinks.joinToString("\n") { "House ${it.first} <-> House ${it.second}" } + "\n")
            result.append("\nBRIDGE LINKS (NO ALTERNATE ROUTE)\n")
            result.append(if (step40BridgeLinks.isEmpty()) "None identified\n" else step40BridgeLinks.joinToString("\n") { "House ${it.first} <-> House ${it.second}" } + "\n")
            result.append("\nSTRUCTURAL SUMMARY\n")
            result.append("Unique undirected link count: ${step40UniqueLinks.size}\n")
            result.append("Links with alternate routes: ${step40RedundantLinks.size}\n")
            result.append("Bridge links: ${step40BridgeLinks.size}\n")
            result.append("Alternate-route links are not bridges in this undirected graph; this is a graph-topology description only, not astrological strength, causation, outcome, or prediction.\n")

            // STEP 41: BHAVA NETWORK BICONNECTED COMPONENT ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 41 - BHAVA NETWORK BICONNECTED COMPONENT ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Identifies vertex-biconnected blocks in the undirected graph using DFS low-link values and an edge stack.\n\n")

            val step41Discovery = mutableMapOf<Int, Int>()
            val step41Low = mutableMapOf<Int, Int>()
            val step41EdgeStack = mutableListOf<Pair<Int, Int>>()
            val step41Blocks = mutableListOf<Set<Int>>()
            var step41Time = 0

            fun step41Dfs(node: Int, parent: Int) {
                step41Time++
                step41Discovery[node] = step41Time
                step41Low[node] = step41Time
                var step41Children = 0
                for (neighbor in (step36Adjacency[node] ?: emptySet()).sorted()) {
                    if (neighbor !in step41Discovery) {
                        step41Children++
                        step41EdgeStack.add(node to neighbor)
                        step41Dfs(neighbor, node)
                        step41Low[node] = minOf(step41Low[node] ?: step41Time, step41Low[neighbor] ?: step41Time)
                        if ((step41Low[neighbor] ?: step41Time) >= (step41Discovery[node] ?: step41Time)) {
                            val step41BlockEdges = mutableListOf<Pair<Int, Int>>()
                            while (step41EdgeStack.isNotEmpty()) {
                                val edge = step41EdgeStack.removeAt(step41EdgeStack.lastIndex)
                                step41BlockEdges.add(edge)
                                if (edge.first == node && edge.second == neighbor) break
                                if (edge.first == neighbor && edge.second == node) break
                            }
                            val step41BlockNodes = step41BlockEdges.flatMap { listOf(it.first, it.second) }.toSet()
                            if (step41BlockNodes.isNotEmpty()) step41Blocks.add(step41BlockNodes)
                        }
                    } else if (neighbor != parent && (step41Discovery[neighbor] ?: 0) < (step41Discovery[node] ?: 0)) {
                        step41EdgeStack.add(node to neighbor)
                        step41Low[node] = minOf(step41Low[node] ?: step41Time, step41Discovery[neighbor] ?: step41Time)
                    }
                }
            }

            for (node in 1..12) {
                if (node !in step41Discovery) {
                    if ((step36Adjacency[node] ?: emptySet()).isEmpty()) {
                        step41Blocks.add(setOf(node))
                    } else {
                        step41Dfs(node, -1)
                        if (step41EdgeStack.isNotEmpty()) {
                            val step41RemainingEdges = step41EdgeStack.toList()
                            step41EdgeStack.clear()
                            val step41RemainingNodes = step41RemainingEdges.flatMap { listOf(it.first, it.second) }.toSet()
                            if (step41RemainingNodes.isNotEmpty()) step41Blocks.add(step41RemainingNodes)
                        }
                    }
                }
            }

            val step41UniqueBlocks = step41Blocks.distinct().sortedBy { it.minOrNull() ?: 0 }
            result.append("BICONNECTED BLOCKS\n")
            result.append(if (step41UniqueBlocks.isEmpty()) "None identified\n" else step41UniqueBlocks.mapIndexed { index, block ->
                "Block ${index + 1}: " + block.sorted().joinToString(", ") { "House $it" }
            }.joinToString("\n") + "\n")
            result.append("\nSTRUCTURAL SUMMARY\n")
            result.append("Biconnected block count: ${step41UniqueBlocks.size}\n")
            result.append("Largest block size: ${step41UniqueBlocks.maxOfOrNull { it.size } ?: 0} houses\n")
            result.append("This block decomposition describes graph topology only; it does not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 42: BHAVA BLOCK-CUT TREE ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 42 - BHAVA BLOCK-CUT TREE ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Constructs the block-cut representation linking biconnected blocks to articulation houses. This is a structural graph representation only.\n\n")

            val step42ArticulationHouses = step38Articulation.sorted()
            val step42BlockCutLinks = mutableListOf<Pair<Int, Int>>()
            for (articulationHouse in step42ArticulationHouses) {
                for (blockIndex in step41UniqueBlocks.indices) {
                    if (articulationHouse in step41UniqueBlocks[blockIndex]) {
                        step42BlockCutLinks.add((blockIndex + 1) to articulationHouse)
                    }
                }
            }

            result.append("ARTICULATION HOUSE NODES\n")
            result.append(if (step42ArticulationHouses.isEmpty()) "None identified\n" else step42ArticulationHouses.joinToString { "House $it" } + "\n")

            result.append("BLOCK-CUT CONNECTIONS (BLOCK -> ARTICULATION HOUSE)\n")
            result.append(if (step42BlockCutLinks.isEmpty()) "None identified\n" else step42BlockCutLinks.distinct().joinToString("\n") { (blockId, house) -> "Block $blockId <-> House $house" } + "\n")

            val step42BlocksConnectedToArticulation = step42BlockCutLinks.map { it.first }.toSet().size
            val step42BlockCutNodeCount = step41UniqueBlocks.size + step42ArticulationHouses.size
            result.append("BLOCK-CUT SUMMARY\n")
            result.append("Biconnected blocks: ${step41UniqueBlocks.size}\n")
            result.append("Articulation-house nodes: ${step42ArticulationHouses.size}\n")
            result.append("Block-articulation incidences: ${step42BlockCutLinks.distinct().size}\n")
            result.append("Blocks linked to articulation houses: $step42BlocksConnectedToArticulation\n")
            result.append("Total block-cut representation nodes: $step42BlockCutNodeCount\n")
            result.append("Step 42 note: block-cut representation describes connectivity structure only and does not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 43: BLOCK-CUT REPRESENTATION TOPOLOGY METRICS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 43 - BLOCK-CUT REPRESENTATION TOPOLOGY METRICS\n")
            result.append("=============================\n\n")
            result.append("Measures degree, leaf nodes, branching nodes, and connected components in the block-cut representation. Structural graph metrics only.\n\n")

            val step43BlockCutAdjacency = mutableMapOf<String, MutableSet<String>>()
            for (blockIndex in step41UniqueBlocks.indices) {
                step43BlockCutAdjacency["B${blockIndex + 1}"] = mutableSetOf()
            }
            for (house in step42ArticulationHouses) {
                step43BlockCutAdjacency["H$house"] = mutableSetOf()
            }
            for ((blockId, house) in step42BlockCutLinks.distinct()) {
                val blockNode = "B$blockId"
                val houseNode = "H$house"
                step43BlockCutAdjacency[blockNode]?.add(houseNode)
                step43BlockCutAdjacency[houseNode]?.add(blockNode)
            }

            val step43Degrees = step43BlockCutAdjacency.mapValues { it.value.size }
            val step43Leaves = step43Degrees.filterValues { it == 1 }.keys.sorted()
            val step43BranchingNodes = step43Degrees.filterValues { it >= 3 }.keys.sorted()
            val step43Visited = mutableSetOf<String>()
            val step43Components = mutableListOf<Set<String>>()
            for (node in step43BlockCutAdjacency.keys.sorted()) {
                if (node !in step43Visited) {
                    val queue = ArrayDeque<String>()
                    val component = mutableSetOf<String>()
                    queue.add(node)
                    step43Visited.add(node)
                    while (queue.isNotEmpty()) {
                        val current = queue.removeFirst()
                        component.add(current)
                        for (neighbor in (step43BlockCutAdjacency[current] ?: emptySet()).sorted()) {
                            if (neighbor !in step43Visited) {
                                step43Visited.add(neighbor)
                                queue.add(neighbor)
                            }
                        }
                    }
                    step43Components.add(component)
                }
            }
            val step43MaximumDegree = step43Degrees.values.maxOrNull() ?: 0
            val step43DegreeOneCount = step43Leaves.size
            val step43DegreeZeroCount = step43Degrees.count { it.value == 0 }

            result.append("BLOCK-CUT NODE DEGREES\n")
            result.append(if (step43Degrees.isEmpty()) "No nodes\n" else step43Degrees.toSortedMap().entries.joinToString("\n") { "${it.key}: degree ${it.value}" } + "\n")
            result.append("\nLEAF NODES (DEGREE 1)\n")
            result.append(if (step43Leaves.isEmpty()) "None identified\n" else step43Leaves.joinToString(", ") + "\n")
            result.append("\nBRANCHING NODES (DEGREE >= 3)\n")
            result.append(if (step43BranchingNodes.isEmpty()) "None identified\n" else step43BranchingNodes.joinToString(", ") + "\n")
            result.append("\nBLOCK-CUT COMPONENTS\n")
            result.append(if (step43Components.isEmpty()) "None\n" else step43Components.mapIndexed { index, component -> "Component ${index + 1}: " + component.sorted().joinToString(", ") }.joinToString("\n") + "\n")
            result.append("\nSTEP 43 SUMMARY\n")
            result.append("Representation node count: ${step43BlockCutAdjacency.size}\n")
            result.append("Connected component count: ${step43Components.size}\n")
            result.append("Degree-1 leaf count: $step43DegreeOneCount\n")
            result.append("Degree-0 isolated node count: $step43DegreeZeroCount\n")
            result.append("Branching node count (degree >= 3): ${step43BranchingNodes.size}\n")
            result.append("Maximum representation-node degree: $step43MaximumDegree\n")
            result.append("Step 43 note: these are descriptive graph-topology metrics only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 44: BLOCK-CUT GRAPH DISTANCE AND CENTRALITY METRICS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 44 - BLOCK-CUT GRAPH DISTANCE AND CENTRALITY METRICS\n")
            result.append("=============================\n\n")
            result.append("Calculates shortest-path distances, graph diameter within connected components, and descriptive closeness values for block-cut representation nodes. Structural graph metrics only.\n\n")

            val step44DistanceMaps = mutableMapOf<String, Map<String, Int>>()
            for (sourceNode in step43BlockCutAdjacency.keys.sorted()) {
                val distances = mutableMapOf(sourceNode to 0)
                val queue = ArrayDeque<String>()
                queue.add(sourceNode)
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    for (neighbor in (step43BlockCutAdjacency[current] ?: emptySet()).sorted()) {
                        if (neighbor !in distances) {
                            distances[neighbor] = distances[current]!! + 1
                            queue.add(neighbor)
                        }
                    }
                }
                step44DistanceMaps[sourceNode] = distances
            }

            var step44Diameter = 0
            var step44LongestPair = "None"
            var step44ReachablePairCount = 0
            for ((sourceNode, distances) in step44DistanceMaps) {
                for ((targetNode, distance) in distances) {
                    if (sourceNode < targetNode) {
                        step44ReachablePairCount++
                        if (distance > step44Diameter) {
                            step44Diameter = distance
                            step44LongestPair = "$sourceNode - $targetNode"
                        }
                    }
                }
            }

            val step44Closeness = step44DistanceMaps.mapValues { (_, distances) ->
                val reachable = distances.size - 1
                val distanceSum = distances.values.sum()
                if (reachable <= 0 || distanceSum <= 0) 0.0 else reachable.toDouble() / distanceSum.toDouble()
            }
            val step44RankedCloseness = step44Closeness.entries.sortedWith(
                compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key }
            )
            val step44AverageDistance = if (step44ReachablePairCount > 0) {
                step44DistanceMaps.values.sumOf { distances -> distances.values.sum() }.toDouble() / (step44ReachablePairCount * 2).toDouble()
            } else 0.0

            result.append("SHORTEST-PATH / DISTANCE SUMMARY\n")
            result.append("Reachable unordered node pairs: $step44ReachablePairCount\n")
            result.append("Maximum shortest-path distance (diameter across components): $step44Diameter\n")
            result.append("Longest-distance node pair: $step44LongestPair\n")
            result.append("Average shortest-path distance across reachable pairs: ${String.format(Locale.US, "%.4f", step44AverageDistance)}\n")
            result.append("\nNODE CLOSENESS (REACHABLE-NODE NORMALIZED)\n")
            result.append(if (step44RankedCloseness.isEmpty()) "No nodes\n" else step44RankedCloseness.joinToString("\n") { "${it.key}: ${String.format(Locale.US, "%.4f", it.value)}" } + "\n")
            result.append("\nSTEP 44 SUMMARY\n")
            result.append("Nodes evaluated: ${step44DistanceMaps.size}\n")
            result.append("Reachable unordered node pairs: $step44ReachablePairCount\n")
            result.append("Graph diameter metric: $step44Diameter\n")
            result.append("Step 44 note: shortest paths and closeness are descriptive graph-topology metrics only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 45: COMPONENT ECCENTRICITY, RADIUS, CENTER AND PERIPHERY (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 45 - COMPONENT ECCENTRICITY, RADIUS, CENTER AND PERIPHERY\n")
            result.append("=============================\n\n")
            result.append("Computes each representation node's eccentricity within its reachable component, and identifies component radius, center and periphery. Structural-only metrics.\n\n")

            val step45Eccentricity = step44DistanceMaps.mapValues { (_, distances) ->
                if (distances.isEmpty()) 0 else distances.values.maxOrNull() ?: 0
            }
            val step45RankedEccentricity = step45Eccentricity.entries.sortedWith(
                compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
            )
            val step45Unassigned = step44DistanceMaps.keys.toMutableSet()
            val step45ComponentSummaries = mutableListOf<String>()
            var step45ComponentIndex = 0
            while (step45Unassigned.isNotEmpty()) {
                val seed = step45Unassigned.sorted().first()
                val componentNodes = (step44DistanceMaps[seed]?.keys ?: setOf(seed)).toSet()
                step45Unassigned.removeAll(componentNodes)
                step45ComponentIndex++
                val componentEcc = componentNodes.mapNotNull { node -> step45Eccentricity[node]?.let { node to it } }.toMap()
                val radius = componentEcc.values.minOrNull() ?: 0
                val peripheralEcc = componentEcc.values.maxOrNull() ?: 0
                val centers = componentEcc.filterValues { it == radius }.keys.sorted()
                val periphery = componentEcc.filterValues { it == peripheralEcc }.keys.sorted()
                step45ComponentSummaries.add(
                    "Component $step45ComponentIndex: nodes=${componentNodes.size}, radius=$radius, center=${centers.joinToString(", ")}, periphery=${periphery.joinToString(", ")}, maximum eccentricity=$peripheralEcc"
                )
            }

            result.append("NODE ECCENTRICITY (WITHIN REACHABLE COMPONENT)\n")
            result.append(if (step45RankedEccentricity.isEmpty()) "No nodes\n" else step45RankedEccentricity.joinToString("\n") { "${it.key}: ${it.value}" } + "\n")
            result.append("\nCOMPONENT RADIUS / CENTER / PERIPHERY\n")
            result.append(if (step45ComponentSummaries.isEmpty()) "No components\n" else step45ComponentSummaries.joinToString("\n") + "\n")
            result.append("\nSTEP 45 SUMMARY\n")
            result.append("Nodes evaluated: ${step45Eccentricity.size}\n")
            result.append("Connected components summarized: $step45ComponentIndex\n")
            result.append("Step 45 note: eccentricity, radius, center and periphery are descriptive graph-topology measures only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 46: CONNECTED-COMPONENT DIAMETER AND DISTANCE PROFILE (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 46 - CONNECTED-COMPONENT DIAMETER AND DISTANCE PROFILE\n")
            result.append("=============================\n\n")
            result.append("Reports component-wise diameter, average shortest-path distance, and reachable pair totals using the STEP 44 distance maps. Structural measures only.\n\n")

            val step46Unassigned = step44DistanceMaps.keys.toMutableSet()
            val step46Profiles = mutableListOf<String>()
            var step46ComponentIndex = 0
            while (step46Unassigned.isNotEmpty()) {
                val seed = step46Unassigned.sorted().first()
                val componentNodes = (step44DistanceMaps[seed]?.keys ?: setOf(seed)).toSet()
                step46Unassigned.removeAll(componentNodes)
                step46ComponentIndex++
                var componentDiameter = 0
                var componentPairCount = 0
                var componentDistanceSum = 0L
                var componentLongestPair = "None"
                for (source in componentNodes.sorted()) {
                    val distances = step44DistanceMaps[source] ?: emptyMap()
                    for ((target, distance) in distances) {
                        if (source < target && target in componentNodes) {
                            componentPairCount++
                            componentDistanceSum += distance.toLong()
                            if (distance > componentDiameter) {
                                componentDiameter = distance
                                componentLongestPair = "$source - $target"
                            }
                        }
                    }
                }
                val componentAverage = if (componentPairCount > 0) componentDistanceSum.toDouble() / componentPairCount.toDouble() else 0.0
                step46Profiles.add(
                    "Component $step46ComponentIndex: nodes=${componentNodes.size}, pairs=$componentPairCount, diameter=$componentDiameter, longest pair=$componentLongestPair, average distance=${String.format(Locale.US, "%.4f", componentAverage)}"
                )
            }

            result.append("COMPONENT DISTANCE PROFILES\n")
            result.append(if (step46Profiles.isEmpty()) "No components\n" else step46Profiles.joinToString("\n") + "\n")
            result.append("\nSTEP 46 SUMMARY\n")
            result.append("Components profiled: $step46ComponentIndex\n")
            result.append("Step 46 note: component-wise diameter and average distance are descriptive graph-topology measures only; they do not indicate astrological strength, causation, outcome, or prediction.\n")


            // STEP 47: COMPONENT-WISE DISTANCE DISTRIBUTION AND GLOBAL EFFICIENCY (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 47 - COMPONENT DISTANCE DISTRIBUTION AND GLOBAL EFFICIENCY\n")
            result.append("=============================\n\n")
            result.append("Summarizes shortest-path distance frequencies and inverse-distance efficiency within reachable node pairs. Structural measures only.\n\n")

            val step47DistanceFrequency = sortedMapOf<Int, Int>()
            var step47ReachablePairs = 0
            var step47EfficiencySum = 0.0
            var step47ComponentIndex = 0
            val step47Unassigned = step44DistanceMaps.keys.toMutableSet()
            val step47ComponentLines = mutableListOf<String>()
            while (step47Unassigned.isNotEmpty()) {
                val seed = step47Unassigned.sorted().first()
                val componentNodes = (step44DistanceMaps[seed]?.keys ?: setOf(seed)).toSet()
                step47Unassigned.removeAll(componentNodes)
                step47ComponentIndex++
                var localPairs = 0
                var localEfficiency = 0.0
                val localFrequency = sortedMapOf<Int, Int>()
                for (source in componentNodes.sorted()) {
                    val distances = step44DistanceMaps[source] ?: emptyMap()
                    for ((target, distance) in distances) {
                        if (source < target && target in componentNodes && distance > 0) {
                            localPairs++
                            localFrequency[distance] = (localFrequency[distance] ?: 0) + 1
                            localEfficiency += 1.0 / distance.toDouble()
                        }
                    }
                }
                step47ReachablePairs += localPairs
                step47EfficiencySum += localEfficiency
                for ((distance, count) in localFrequency) {
                    step47DistanceFrequency[distance] = (step47DistanceFrequency[distance] ?: 0) + count
                }
                val componentEfficiency = if (localPairs > 0) localEfficiency / localPairs.toDouble() else 0.0
                step47ComponentLines.add("Component $step47ComponentIndex: nodes=${componentNodes.size}, reachable pairs=$localPairs, average inverse-distance efficiency=${String.format(Locale.US, "%.4f", componentEfficiency)}")
            }
            val step47AverageEfficiency = if (step47ReachablePairs > 0) step47EfficiencySum / step47ReachablePairs.toDouble() else 0.0
            result.append("COMPONENT EFFICIENCY PROFILES\n")
            result.append(if (step47ComponentLines.isEmpty()) "No components\n" else step47ComponentLines.joinToString("\n") + "\n")
            result.append("\nSHORTEST-PATH DISTANCE FREQUENCY (UNORDERED PAIRS)\n")
            result.append(if (step47DistanceFrequency.isEmpty()) "No reachable pairs\n" else step47DistanceFrequency.entries.joinToString("\n") { "Distance ${it.key}: ${it.value} pair(s)" } + "\n")
            result.append("\nSTEP 47 SUMMARY\n")
            result.append("Components evaluated: $step47ComponentIndex\n")
            result.append("Reachable unordered pairs: $step47ReachablePairs\n")
            result.append("Average inverse-distance efficiency across reachable pairs: ${String.format(Locale.US, "%.4f", step47AverageEfficiency)}\n")
            result.append("Step 47 note: distance frequency and inverse-distance efficiency are descriptive graph-topology measures only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 48: DEGREE DISTRIBUTION AND DEGREE ASSORTATIVITY (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 48 - DEGREE DISTRIBUTION AND DEGREE ASSORTATIVITY\n")
            result.append("=============================\n\n")
            result.append("Summarizes node-degree frequencies and Pearson degree correlation across undirected edges. Structural measures only.\n\n")

            val step48DegreeFrequency = sortedMapOf<Int, Int>()
            val step48Degrees = step43BlockCutAdjacency.mapValues { (_, neighbors) -> neighbors.size }
            for (degree in step48Degrees.values) {
                step48DegreeFrequency[degree] = (step48DegreeFrequency[degree] ?: 0) + 1
            }
            val step48NodeCount = step48Degrees.size
            val step48DegreeSum = step48Degrees.values.sum().toLong()
            val step48MeanDegree = if (step48NodeCount > 0) step48DegreeSum.toDouble() / step48NodeCount.toDouble() else 0.0
            val step48Edges = mutableListOf<Pair<Int, Int>>()
            for ((node, neighbors) in step43BlockCutAdjacency) {
                for (neighbor in neighbors) {
                    if (node < neighbor && neighbor in step48Degrees) {
                        step48Edges.add((step48Degrees[node] ?: 0) to (step48Degrees[neighbor] ?: 0))
                    }
                }
            }
            val step48EdgeCount = step48Edges.size
            var step48Assortativity = Double.NaN
            if (step48EdgeCount > 0) {
                val xValues = step48Edges.map { it.first.toDouble() }
                val yValues = step48Edges.map { it.second.toDouble() }
                val xMean = xValues.average()
                val yMean = yValues.average()
                var covarianceSum = 0.0
                var xVarianceSum = 0.0
                var yVarianceSum = 0.0
                for (i in step48Edges.indices) {
                    val dx = xValues[i] - xMean
                    val dy = yValues[i] - yMean
                    covarianceSum += dx * dy
                    xVarianceSum += dx * dx
                    yVarianceSum += dy * dy
                }
                val denominator = kotlin.math.sqrt(xVarianceSum * yVarianceSum)
                if (denominator > 0.0) step48Assortativity = covarianceSum / denominator
            }
            result.append("NODE DEGREE FREQUENCY\n")
            result.append(if (step48DegreeFrequency.isEmpty()) "No nodes\n" else step48DegreeFrequency.entries.joinToString("\n") { "Degree ${it.key}: ${it.value} node(s)" } + "\n")
            result.append("\nSTEP 48 SUMMARY\n")
            result.append("Nodes evaluated: $step48NodeCount\n")
            result.append("Undirected edges evaluated: $step48EdgeCount\n")
            result.append("Mean degree: ${String.format(Locale.US, "%.4f", step48MeanDegree)}\n")
            result.append("Degree assortativity (Pearson correlation across edge endpoints): ${if (step48Assortativity.isNaN()) "Undefined" else String.format(Locale.US, "%.4f", step48Assortativity)}\n")
            result.append("Step 48 note: degree distribution and degree assortativity are descriptive graph-topology measures only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 49: LOCAL CLUSTERING COEFFICIENT AND GRAPH TRANSITIVITY (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 49 - LOCAL CLUSTERING COEFFICIENT AND GRAPH TRANSITIVITY\n")
            result.append("=============================\n\n")
            result.append("Measures neighbor connectivity using triangle relationships. Structural measures only.\n\n")

            var step49TriangleCount = 0L
            var step49ConnectedTriples = 0L
            val step49LocalCoefficients = mutableMapOf<String, Double>()
            for ((step49Node, step49NeighborsRaw) in step43BlockCutAdjacency) {
                val step49Neighbors = step49NeighborsRaw.filter { it in step43BlockCutAdjacency.keys }.toSet()
                val step49Degree = step49Neighbors.size
                if (step49Degree < 2) {
                    step49LocalCoefficients[step49Node] = 0.0
                    continue
                }
                val step49PossiblePairs = step49Degree.toLong() * (step49Degree - 1L) / 2L
                step49ConnectedTriples += step49PossiblePairs
                var step49LinksAmongNeighbors = 0L
                val step49SortedNeighbors = step49Neighbors.sorted()
                for (i in step49SortedNeighbors.indices) {
                    for (j in i + 1 until step49SortedNeighbors.size) {
                        if (step49SortedNeighbors[j] in (step43BlockCutAdjacency[step49SortedNeighbors[i]] ?: emptySet())) {
                            step49LinksAmongNeighbors++
                        }
                    }
                }
                step49TriangleCount += step49LinksAmongNeighbors
                step49LocalCoefficients[step49Node] = step49LinksAmongNeighbors.toDouble() / step49PossiblePairs.toDouble()
            }
            val step49MeanLocal = if (step49LocalCoefficients.isNotEmpty()) step49LocalCoefficients.values.average() else 0.0
            val step49Transitivity = if (step49ConnectedTriples > 0L) step49TriangleCount.toDouble() / step49ConnectedTriples.toDouble() else 0.0
            result.append("LOCAL CLUSTERING COEFFICIENTS\n")
            result.append(if (step49LocalCoefficients.isEmpty()) "No nodes\n" else step49LocalCoefficients.entries.sortedBy { it.key }.joinToString("\n") { "Node ${it.key}: ${String.format(Locale.US, "%.4f", it.value)}" } + "\n")
            result.append("\nSTEP 49 SUMMARY\n")
            result.append("Nodes evaluated: ${step49LocalCoefficients.size}\n")
            result.append("Triangle relationships counted: $step49TriangleCount\n")
            result.append("Connected neighbor-pairs (triples): $step49ConnectedTriples\n")
            result.append("Mean local clustering coefficient: ${String.format(Locale.US, "%.4f", step49MeanLocal)}\n")
            result.append("Graph transitivity: ${String.format(Locale.US, "%.4f", step49Transitivity)}\n")
            result.append("Step 49 note: clustering and transitivity are descriptive graph-topology measures only; they do not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 50: K-CORE DECOMPOSITION (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 50 - K-CORE DECOMPOSITION\n")
            result.append("=============================\n\n")
            result.append("Identifies nested subgraphs by iterative degree peeling. Structural measures only.\n\n")

            val step50Graph = mutableMapOf<String, MutableSet<String>>()
            for ((step50Node, step50NeighborsRaw) in step43BlockCutAdjacency) {
                step50Graph[step50Node] = step50NeighborsRaw.filter { it in step43BlockCutAdjacency.keys && it != step50Node }.toMutableSet()
            }
            val step50CoreNumbers = mutableMapOf<String, Int>()
            val step50Remaining = step50Graph.keys.toMutableSet()
            var step50Core = 0
            while (step50Remaining.isNotEmpty()) {
                val step50LowDegreeNodes = step50Remaining.filter { node ->
                    (step50Graph[node] ?: emptySet()).count { it in step50Remaining } <= step50Core
                }
                if (step50LowDegreeNodes.isNotEmpty()) {
                    for (node in step50LowDegreeNodes) {
                        step50CoreNumbers[node] = step50Core
                        step50Remaining.remove(node)
                    }
                } else {
                    step50Core++
                }
            }
            val step50CoreFrequency = step50CoreNumbers.values.groupingBy { it }.eachCount().toSortedMap()
            val step50MaxCore = step50CoreNumbers.values.maxOrNull() ?: 0
            val step50MaxCoreNodes = step50CoreNumbers.filterValues { it == step50MaxCore }.keys.sorted()
            result.append("NODE CORE NUMBERS\n")
            result.append(if (step50CoreNumbers.isEmpty()) "No nodes\n" else step50CoreNumbers.entries.sortedBy { it.key }.joinToString("\n") { "Node ${it.key}: k-core ${it.value}" } + "\n")
            result.append("\nK-CORE FREQUENCY\n")
            result.append(if (step50CoreFrequency.isEmpty()) "No cores\n" else step50CoreFrequency.entries.joinToString("\n") { "Core ${it.key}: ${it.value} node(s)" } + "\n")
            result.append("\nSTEP 50 SUMMARY\n")
            result.append("Nodes evaluated: ${step50CoreNumbers.size}\n")
            result.append("Maximum core index: $step50MaxCore\n")
            result.append("Maximum-core nodes: ${if (step50MaxCoreNodes.isEmpty()) "None" else step50MaxCoreNodes.joinToString(", ")}\n")
            result.append("Step 50 note: k-core decomposition is a descriptive graph-topology measure only; it does not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 51: K-CORE STRUCTURE PROFILE (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 51 - K-CORE STRUCTURE PROFILE\n")
            result.append("=============================\n\n")
            val step51CoreLevels = (1..step50MaxCore).toList()
            result.append("CORE LEVEL PROFILE\n")
            if (step51CoreLevels.isEmpty()) {
                result.append("No positive core levels found\n")
            } else {
                for (step51Level in step51CoreLevels) {
                    val step51Nodes = step50CoreNumbers.filterValues { it >= step51Level }.keys
                    val step51NodeCount = step51Nodes.size
                    var step51EdgeCount = 0L
                    for (step51Node in step51Nodes) {
                        step51EdgeCount += (step50Graph[step51Node] ?: mutableSetOf()).count { it in step51Nodes }
                    }
                    step51EdgeCount /= 2L
                    val step51Density = if (step51NodeCount > 1) {
                        2.0 * step51EdgeCount.toDouble() / (step51NodeCount.toDouble() * (step51NodeCount - 1).toDouble())
                    } else 0.0
                    result.append("k >= $step51Level: nodes=$step51NodeCount, edges=$step51EdgeCount, density=${String.format(Locale.US, "%.4f", step51Density)}\n")
                }
            }
            result.append("\nSTEP 51 SUMMARY\n")
            result.append("Core levels profiled: ${step51CoreLevels.size}\n")
            result.append("Step 51 note: core-level profiles describe graph structure only; they do not imply astrological strength, causation, outcome, or prediction.\n")

            // STEP 52: K-CORE SHELL AND INTER-SHELL EDGE PROFILE (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 52 - K-CORE SHELL AND INTER-SHELL EDGE PROFILE\n")
            result.append("=============================\n\n")
            val step52Shells = step50CoreNumbers.entries.groupBy { it.value }.toSortedMap()
            result.append("CORE SHELL NODE DISTRIBUTION\n")
            if (step52Shells.isEmpty()) {
                result.append("No core shell data\n")
            } else {
                for ((step52Shell, step52Entries) in step52Shells) {
                    result.append("Shell $step52Shell: ${step52Entries.size} node(s) - ${step52Entries.map { it.key }.sorted().joinToString(", ")}\n")
                }
            }
            val step52ShellEdgeCounts = sortedMapOf<String, Long>()
            val step52ShellKeys = step50CoreNumbers.keys
            for (step52Node in step52ShellKeys) {
                val step52Neighbors = step50Graph[step52Node] ?: mutableSetOf()
                for (step52Neighbor in step52Neighbors) {
                    if (step52Neighbor in step52ShellKeys && step52Node < step52Neighbor) {
                        val step52A = step50CoreNumbers[step52Node] ?: 0
                        val step52B = step50CoreNumbers[step52Neighbor] ?: 0
                        val step52Key = if (step52A == step52B) "Shell $step52A internal" else "Shell $step52A - Shell $step52B"
                        step52ShellEdgeCounts[step52Key] = (step52ShellEdgeCounts[step52Key] ?: 0L) + 1L
                    }
                }
            }
            result.append("\nSHELL EDGE PROFILE\n")
            if (step52ShellEdgeCounts.isEmpty()) result.append("No edges counted\n")
            else step52ShellEdgeCounts.forEach { (step52Key, step52Count) -> result.append("$step52Key: $step52Count edge(s)\n") }
            result.append("\nSTEP 52 SUMMARY\n")
            result.append("Shell levels present: ${step52Shells.keys.joinToString(", ")}\n")
            result.append("Step 52 note: shell and edge profiles are descriptive graph topology metrics only.\n")

            // STEP 53: CORE-SHELL EDGE ASSORTATIVITY (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 53 - CORE-SHELL EDGE ASSORTATIVITY\n")
            result.append("=============================\n\n")
            val step53ShellPairs = mutableListOf<Pair<Double, Double>>()
            for (step53Node in step50CoreNumbers.keys.sorted()) {
                val step53Neighbors = step50Graph[step53Node] ?: mutableSetOf()
                for (step53Neighbor in step53Neighbors) {
                    if (step53Neighbor in step50CoreNumbers && step53Node < step53Neighbor) {
                        step53ShellPairs.add(Pair(
                            (step50CoreNumbers[step53Node] ?: 0).toDouble(),
                            (step50CoreNumbers[step53Neighbor] ?: 0).toDouble()
                        ))
                    }
                }
            }
            val step53ShellAssortativity = if (step53ShellPairs.size >= 2) {
                val step53XMean = step53ShellPairs.map { it.first }.average()
                val step53YMean = step53ShellPairs.map { it.second }.average()
                val step53Numerator = step53ShellPairs.sumOf { (x, y) -> (x - step53XMean) * (y - step53YMean) }
                val step53XDenominator = step53ShellPairs.sumOf { (x, _) -> (x - step53XMean) * (x - step53XMean) }
                val step53YDenominator = step53ShellPairs.sumOf { (_, y) -> (y - step53YMean) * (y - step53YMean) }
                if (step53XDenominator > 0.0 && step53YDenominator > 0.0)
                    step53Numerator / kotlin.math.sqrt(step53XDenominator * step53YDenominator) else null
            } else null
            result.append("CORE-SHELL EDGE CORRELATION\n")
            result.append("Edges sampled: ${step53ShellPairs.size}\n")
            result.append("Shell assortativity (Pearson): ${step53ShellAssortativity?.let { String.format(Locale.US, "%.4f", it) } ?: "Not defined"}\n")
            result.append("\nSTEP 53 SUMMARY\n")
            result.append("This coefficient describes whether edges tend to connect nodes with similar core-shell indices.\n")
            result.append("Step 53 note: descriptive graph topology only; it does not indicate astrological strength, causation, outcome, or prediction.\n")

            // STEP 54: CORE-SHELL CONNECTED COMPONENT PROFILE (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 54 - CORE-SHELL CONNECTED COMPONENT PROFILE\n")
            result.append("=============================\n\n")
            val step54ShellGroups = step50CoreNumbers.entries.groupBy { it.value }
            for (step54Shell in step54ShellGroups.keys.sorted()) {
                val step54Nodes = step54ShellGroups[step54Shell]?.map { it.key }?.toSet() ?: emptySet()
                val step54Remaining = step54Nodes.toMutableSet()
                var step54ComponentCount = 0
                val step54ComponentSizes = mutableListOf<Int>()
                while (step54Remaining.isNotEmpty()) {
                    val step54Start = step54Remaining.first()
                    val step54Queue = ArrayDeque<String>()
                    val step54Visited = mutableSetOf<String>()
                    step54Queue.add(step54Start)
                    step54Visited.add(step54Start)
                    step54Remaining.remove(step54Start)
                    while (step54Queue.isNotEmpty()) {
                        val step54Current = step54Queue.removeFirst()
                        for (step54Neighbor in (step50Graph[step54Current] ?: emptySet())) {
                            if (step54Neighbor in step54Nodes && step54Neighbor !in step54Visited) {
                                step54Visited.add(step54Neighbor)
                                step54Remaining.remove(step54Neighbor)
                                step54Queue.add(step54Neighbor)
                            }
                        }
                    }
                    step54ComponentCount++
                    step54ComponentSizes.add(step54Visited.size)
                }
                result.append("Shell $step54Shell: nodes=${step54Nodes.size}, components=$step54ComponentCount, component sizes=${step54ComponentSizes.sortedDescending().joinToString(", ")}\n")
            }
            result.append("Step 54 note: shell-restricted connectivity profile is descriptive graph topology only.\n")

            // STEP 55: CORE-SHELL MIXING MATRIX (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 55 - CORE-SHELL MIXING MATRIX\n")
            result.append("=============================\n\n")
            val step55ShellPairCounts = mutableMapOf<Pair<Int, Int>, Int>()
            val step55Shells = step50CoreNumbers.values.toSet().sorted()
            for (step55Node in step50CoreNumbers.keys.sorted()) {
                for (step55Neighbor in (step50Graph[step55Node] ?: emptySet())) {
                    if (step55Neighbor in step50CoreNumbers && step55Node < step55Neighbor) {
                        val step55A = step50CoreNumbers[step55Node] ?: 0
                        val step55B = step50CoreNumbers[step55Neighbor] ?: 0
                        val step55Pair = if (step55A <= step55B) Pair(step55A, step55B) else Pair(step55B, step55A)
                        step55ShellPairCounts[step55Pair] = (step55ShellPairCounts[step55Pair] ?: 0) + 1
                    }
                }
            }
            result.append("Shell-pair edge counts (upper-triangle representation):\n")
            for (step55A in step55Shells) {
                for (step55B in step55Shells.filter { it >= step55A }) {
                    result.append("Shell $step55A - Shell $step55B: ${step55ShellPairCounts[Pair(step55A, step55B)] ?: 0} edges\n")
                }
            }
            result.append("Step 55 note: edge counts between core shells are descriptive graph topology only.\n")


            // STEP 56: CORE-SHELL STRUCTURAL COHESION (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 56 - CORE-SHELL STRUCTURAL COHESION\n")
            result.append("=============================\n\n")
            for (step56Shell in step50CoreNumbers.values.toSet().sorted()) {
                val step56Nodes = step50CoreNumbers.filterValues { it == step56Shell }.keys
                val step56NodeCount = step56Nodes.size
                var step56InternalEdges = 0
                var step56ExternalEdges = 0
                var step56BoundaryNodes = 0
                for (step56Node in step56Nodes) {
                    var step56HasExternal = false
                    for (step56Neighbor in (step50Graph[step56Node] ?: emptySet())) {
                        if (step56Neighbor in step50CoreNumbers) {
                            if (step56Neighbor in step56Nodes) {
                                if (step56Node < step56Neighbor) step56InternalEdges++
                            } else {
                                step56ExternalEdges++
                                step56HasExternal = true
                            }
                        }
                    }
                    if (step56HasExternal) step56BoundaryNodes++
                }
                val step56PossibleEdges = step56NodeCount * (step56NodeCount - 1) / 2
                val step56Density = if (step56PossibleEdges > 0) step56InternalEdges.toDouble() / step56PossibleEdges else 0.0
                val step56ExternalRatio = if (step56InternalEdges + step56ExternalEdges > 0) step56ExternalEdges.toDouble() / (step56InternalEdges + step56ExternalEdges) else 0.0
                result.append("Shell $step56Shell: nodes=$step56NodeCount, internal edges=$step56InternalEdges, density=${String.format(Locale.US, "%.4f", step56Density)}, external edges=$step56ExternalEdges, boundary nodes=$step56BoundaryNodes, external edge ratio=${String.format(Locale.US, "%.4f", step56ExternalRatio)}\n")
            }
            result.append("Step 56 note: cohesion indicators are descriptive graph-topology measures.\n")

            // STEP 57: CORE-SHELL NEIGHBOR COMPOSITION PROFILE (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 57 - CORE-SHELL NEIGHBOR COMPOSITION PROFILE\n")
            result.append("=============================\n\n")
            val step57Shells = step50CoreNumbers.values.toSet().sorted()
            for (step57Shell in step57Shells) {
                val step57Nodes = step50CoreNumbers.filterValues { it == step57Shell }.keys
                val step57Composition = mutableMapOf<Int, Int>()
                var step57TotalEdges = 0
                for (step57Node in step57Nodes) {
                    for (step57Neighbor in (step50Graph[step57Node] ?: emptySet())) {
                        val step57NeighborShell = step50CoreNumbers[step57Neighbor] ?: continue
                        step57Composition[step57NeighborShell] = (step57Composition[step57NeighborShell] ?: 0) + 1
                        step57TotalEdges++
                    }
                }
                result.append("Shell $step57Shell neighbor-shell composition (edge incidences):\n")
                for (step57NeighborShell in step57Shells) {
                    val step57Count = step57Composition[step57NeighborShell] ?: 0
                    val step57Share = if (step57TotalEdges > 0) step57Count.toDouble() / step57TotalEdges else 0.0
                    result.append("  Neighbor shell $step57NeighborShell: $step57Count (${String.format(Locale.US, "%.4f", step57Share)})\n")
                }
                result.append("  Total incident edges counted: $step57TotalEdges\n")
            }
            result.append("Step 57 note: neighbor-shell composition is descriptive graph topology only.\n")


            // STEP 58: CORE-SHELL HIERARCHY TRANSITION PROFILE (DESCRIPTIVE)
            result.append("\n=============================\n")
            result.append("STEP 58 - CORE-SHELL HIERARCHY TRANSITION PROFILE\n")
            result.append("=============================\n\n")
            val step58Shells = step50CoreNumbers.values.toSet().sorted()
            val step58TransitionMatrix = mutableMapOf<Pair<Int, Int>, Int>()
            var step58SameShellEdges = 0
            var step58UpwardEdges = 0
            var step58DownwardEdges = 0
            for (step58Node in step50Graph.keys.sorted()) {
                val step58NodeShell = step50CoreNumbers[step58Node] ?: continue
                for (step58Neighbor in (step50Graph[step58Node] ?: emptySet()).sorted()) {
                    if (step58Node >= step58Neighbor) continue
                    val step58NeighborShell = step50CoreNumbers[step58Neighbor] ?: continue
                    step58TransitionMatrix[step58NodeShell to step58NeighborShell] =
                        (step58TransitionMatrix[step58NodeShell to step58NeighborShell] ?: 0) + 1
                    when {
                        step58NodeShell == step58NeighborShell -> step58SameShellEdges++
                        step58NodeShell < step58NeighborShell -> step58UpwardEdges++
                        else -> step58DownwardEdges++
                    }
                }
            }
            result.append("Shell transition matrix (edge counts):\n")
            result.append("Rows = source shell; columns = target shell\n")
            for (step58RowShell in step58Shells) {
                result.append("Shell $step58RowShell: ")
                result.append(step58Shells.joinToString(", ") { step58ColShell ->
                    "$step58ColShell=${step58TransitionMatrix[step58RowShell to step58ColShell] ?: 0}"
                })
                result.append("\n")
            }
            result.append("Undirected same-shell edges: $step58SameShellEdges\n")
            result.append("Undirected cross-shell edges: ${step58UpwardEdges + step58DownwardEdges}\n")
            result.append("Cross-shell pairs (lower-to-higher shell orientation): $step58UpwardEdges\n")
            result.append("Step 58 note: each undirected edge is counted once; orientation is based on shell index only.\n")

            // STEP 59: CORE-SHELL RETENTION PROFILE (DESCRIPTIVE, NOT DYNAMIC STABILITY)
            result.append("\n=============================\n")
            result.append("STEP 59 - CORE-SHELL RETENTION PROFILE\n")
            result.append("=============================\n\n")
            val step59TotalNodes = step50CoreNumbers.size
            val step59ShellLevels = step50CoreNumbers.values.toSet().sorted()
            result.append("Total nodes in core-number profile: $step59TotalNodes\n")
            result.append("Shell level | nodes exactly at level | nodes retained at or above level | retained share\n")
            for (step59Level in step59ShellLevels) {
                val step59Exact = step50CoreNumbers.values.count { it == step59Level }
                val step59Retained = step50CoreNumbers.values.count { it >= step59Level }
                val step59Share = if (step59TotalNodes > 0) step59Retained.toDouble() / step59TotalNodes else 0.0
                result.append("$step59Level | $step59Exact | $step59Retained | ${String.format(Locale.US, "%.4f", step59Share)}\n")
            }
            val step59MaxShell = step50CoreNumbers.values.maxOrNull() ?: 0
            val step59MaxShellCount = step50CoreNumbers.values.count { it == step59MaxShell }
            val step59MaxShellShare = if (step59TotalNodes > 0) step59MaxShellCount.toDouble() / step59TotalNodes else 0.0
            result.append("Maximum core index: $step59MaxShell\n")
            result.append("Nodes at maximum core index: $step59MaxShellCount\n")
            result.append("Maximum-shell node share: ${String.format(Locale.US, "%.4f", step59MaxShellShare)}\n")
            result.append("Step 59 note: this is a static core-retention profile derived from k-core numbers; it does not model temporal stability or node-removal simulations.\n")


            // STEP 60: CORE-SHELL NODE IMPORTANCE RANKING (CORE NUMBER + DEGREE)
            result.append("\n=============================\n")
            result.append("STEP 60 - CORE-SHELL NODE IMPORTANCE RANKING\n")
            result.append("=============================\n\n")
            val step60NodeProfile = step50CoreNumbers.keys.map { step60Node ->
                val step60Core = step50CoreNumbers[step60Node] ?: 0
                val step60Degree = (step50Graph[step60Node] ?: emptySet()).size
                Triple(step60Node, step60Core, step60Degree)
            }.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }
                .thenByDescending { it.third }.thenBy { it.first })
            result.append("Rank | Node | Core Number | Degree\n")
            step60NodeProfile.forEachIndexed { step60Index, step60Item ->
                result.append("${step60Index + 1} | ${step60Item.first} | ${step60Item.second} | ${step60Item.third}\n")
            }
            val step60MaxCore = step50CoreNumbers.values.maxOrNull() ?: 0
            result.append("Maximum core index: $step60MaxCore\n")
            result.append("Nodes at maximum core index: ${step50CoreNumbers.filterValues { it == step60MaxCore }.keys.sorted().joinToString(", ")}\n")
            result.append("Ranking rule: core number descending, then degree descending, then node name ascending.\n")

            // STEP 61: CORE-PEELING ORDER ANALYSIS (STRUCTURAL ONLY)
            result.append("\n=============================\n")
            result.append("STEP 61 - CORE-PEELING ORDER ANALYSIS\n")
            result.append("=============================\n\n")
            val step61Remaining = step50Graph.keys.toMutableSet()
            val step61PeelingOrder = mutableListOf<Triple<String, Int, Int>>()
            var step61Order = 1
            while (step61Remaining.isNotEmpty()) {
                val step61Candidate = step61Remaining.minWithOrNull(
                    compareBy<String> { (step50Graph[it] ?: emptySet()).count { neighbor -> neighbor in step61Remaining } }
                        .thenBy { it }
                ) ?: break
                val step61Degree = (step50Graph[step61Candidate] ?: emptySet()).count { it in step61Remaining }
                val step61Shell = step50CoreNumbers[step61Candidate] ?: 0
                step61PeelingOrder.add(Triple(step61Candidate, step61Degree, step61Shell))
                step61Remaining.remove(step61Candidate)
                step61Order++
            }
            result.append("Order | Node | Degree-at-removal | Core shell\n")
            step61PeelingOrder.forEachIndexed { step61Index, step61Item ->
                result.append("${step61Index + 1} | ${step61Item.first} | ${step61Item.second} | ${step61Item.third}\n")
            }
            result.append("Total nodes peeled: ${step61PeelingOrder.size}\n")
            result.append("Peeling rule: minimum current degree, ties by node name ascending.\n")
            result.append("Core shell is the previously calculated k-core number.\n")
            result.append("Step 61 note: peeling order is a deterministic structural ordering, not an astrological prediction.\n")


            // STEP 62: CORE-PEELING LAYER STATISTICS
            result.append("\n=============================\n")
            result.append("STEP 62 - CORE-PEELING LAYER STATISTICS\n")
            result.append("=============================\n\n")
            val step62ShellStats = step61PeelingOrder.groupBy { it.third }.toSortedMap()
            result.append("Shell | Peeled Nodes | Average Degree-at-removal | Maximum Degree-at-removal\n")
            step62ShellStats.forEach { (step62Shell, step62Items) ->
                val step62Avg = step62Items.map { it.second }.average()
                val step62Max = step62Items.maxOfOrNull { it.second } ?: 0
                result.append("$step62Shell | ${step62Items.size} | ${String.format(Locale.US, "%.4f", step62Avg)} | $step62Max\n")
            }
            result.append("\nShell-wise peeling count:\n")
            step62ShellStats.forEach { (step62Shell, step62Items) ->
                result.append("Shell $step62Shell: ${step62Items.size} nodes\n")
            }
            result.append("Statistics are based on the Step 61 deterministic peeling order and previously calculated core shell numbers.\n")

            // STEP 63: CORE-PEELING PERSISTENCE & LAYER TRANSITION
            result.append("\n=============================\n")
            result.append("STEP 63 - CORE-PEELING PERSISTENCE & LAYER TRANSITION\n")
            result.append("=============================\n\n")
            val step63Shells = step62ShellStats.keys.sorted()
            result.append("Shell | Nodes | Persistence Share | Transition to Next Shell\n")
            step63Shells.forEachIndexed { step63Index, step63Shell ->
                val step63Count = step62ShellStats[step63Shell]?.size ?: 0
                val step63Share = if (step62ShellStats.values.sumOf { it.size } > 0) step63Count.toDouble() / step62ShellStats.values.sumOf { it.size } else 0.0
                val step63NextShell = step63Shells.getOrNull(step63Index + 1)
                val step63Transition = if (step63NextShell != null) {
                    val step63NextCount = step62ShellStats[step63NextShell]?.size ?: 0
                    if (step63Count > 0) step63NextCount.toDouble() / step63Count else 0.0
                } else null
                result.append("$step63Shell | $step63Count | ${String.format(Locale.US, "%.4f", step63Share)} | ${step63Transition?.let { String.format(Locale.US, "%.4f", it) } ?: "N/A"}\n")
            }
            val step63ShellNodeSets = step62ShellStats.mapValues { (_, items) -> items.map { it.first }.toSet() }
            result.append("\nLayer-to-layer node comparison (adjacent shell labels):\n")
            step63Shells.forEachIndexed { step63Index, step63Shell ->
                val step63NextShell = step63Shells.getOrNull(step63Index + 1)
                if (step63NextShell != null) {
                    val step63CurrentSet = step63ShellNodeSets[step63Shell] ?: emptySet()
                    val step63NextSet = step63ShellNodeSets[step63NextShell] ?: emptySet()
                    val step63Overlap = step63CurrentSet.intersect(step63NextSet).size
                    result.append("Shell $step63Shell -> Shell $step63NextShell: shared nodes=$step63Overlap, current layer size=${step63CurrentSet.size}, next layer size=${step63NextSet.size}\n")
                }
            }
            result.append("Persistence is descriptive shell-wise distribution; adjacent shell groups are not temporal states and may contain different node sets.\n")

            // STEP 64: CORE-PEELING STRUCTURAL STABILITY PROFILE
            result.append("\n=============================\n")
            result.append("STEP 64 - CORE-PEELING STRUCTURAL STABILITY PROFILE\n")
            result.append("=============================\n\n")
            val step64Total = step61PeelingOrder.size
            val step64OriginalEdges = step50Graph.values.sumOf { it.size } / 2
            result.append("Peeling progress | Remaining nodes | Remaining edges | Node retention | Edge retention\n")
            val step64Remaining = step50Graph.keys.toMutableSet()
            val step64Snapshots = mutableListOf<Pair<Int, Set<String>>>()
            step64Snapshots.add(0 to step64Remaining.toSet())
            step61PeelingOrder.forEachIndexed { step64Index, step64Item ->
                step64Remaining.remove(step64Item.first)
                step64Snapshots.add((step64Index + 1) to step64Remaining.toSet())
            }
            val step64SnapshotIndices = if (step64Total > 0) listOf(0, step64Total / 4, step64Total / 2, (step64Total * 3) / 4, step64Total).distinct().sorted() else listOf(0)
            step64SnapshotIndices.forEach { step64Index ->
                val step64Set = step64Snapshots.getOrNull(step64Index)?.second ?: emptySet()
                val step64Edges = step50Graph.entries.sumOf { (node, neighbors) -> neighbors.count { it in step64Set && node in step64Set } } / 2
                val step64NodeRetention = if (step64Total > 0) step64Set.size.toDouble() / step64Total else 0.0
                val step64EdgeRetention = if (step64OriginalEdges > 0) step64Edges.toDouble() / step64OriginalEdges else 0.0
                val step64Progress = if (step64Total > 0) step64Index.toDouble() / step64Total else 0.0
                result.append("${String.format(Locale.US, "%.2f", step64Progress)} | ${step64Set.size} | $step64Edges | ${String.format(Locale.US, "%.4f", step64NodeRetention)} | ${String.format(Locale.US, "%.4f", step64EdgeRetention)}\n")
            }
            result.append("\nNode survival by core shell:\n")
            step62ShellStats.toSortedMap().forEach { (step64Shell, step64Items) ->
                val step64Count = step64Items.size
                val step64ShellShare = if (step64Total > 0) step64Count.toDouble() / step64Total else 0.0
                result.append("Shell $step64Shell: $step64Count nodes; fraction=${String.format(Locale.US, "%.4f", step64ShellShare)}\n")
            }
            val step64DegreeDrops = step61PeelingOrder.map { step64Item ->
                val step64Node = step64Item.first
                val step64OriginalDegree = (step50Graph[step64Node] ?: emptySet()).size
                step64OriginalDegree - step64Item.second
            }
            result.append("\nDegree loss summary (original degree minus degree at removal):\n")
            result.append("Average degree loss: ${String.format(Locale.US, "%.4f", step64DegreeDrops.average())}\n")
            result.append("Maximum degree loss: ${step64DegreeDrops.maxOrNull() ?: 0}\n")
            result.append("Step 64 measures structural retention across deterministic peeling snapshots; it is not temporal data or a predictive stability claim.\n")

            // STEP 65: CORE-PEELING STRUCTURAL BOUNDARY ANALYSIS
            result.append("\n=============================\n")
            result.append("STEP 65 - CORE-PEELING STRUCTURAL BOUNDARY ANALYSIS\n")
            result.append("=============================\n\n")
            val step65ShellLevels = step50CoreNumbers.values.toSet().sorted()
            result.append("Shell | Nodes | Internal edges | Boundary edges | Boundary nodes | Boundary-node share\n")
            step65ShellLevels.forEach { step65Shell ->
                val step65Nodes = step50CoreNumbers.filterValues { it == step65Shell }.keys
                val step65NodeSet = step65Nodes.toSet()
                var step65InternalEdges = 0
                var step65BoundaryEdges = 0
                val step65BoundaryNodes = mutableSetOf<String>()
                step65NodeSet.forEach nodeLoop@{ step65Node ->
                    (step50Graph[step65Node] ?: emptySet()).forEach neighborLoop@{ step65Neighbor ->
                        val step65NeighborShell = step50CoreNumbers[step65Neighbor] ?: return@neighborLoop
                        if (step65NeighborShell == step65Shell) {
                            step65InternalEdges++
                        } else {
                            step65BoundaryEdges++
                            step65BoundaryNodes.add(step65Node)
                        }
                    }
                }
                step65InternalEdges /= 2
                val step65BoundaryShare = if (step65Nodes.isNotEmpty()) step65BoundaryNodes.size.toDouble() / step65Nodes.size else 0.0
                result.append("$step65Shell | ${step65Nodes.size} | $step65InternalEdges | $step65BoundaryEdges | ${step65BoundaryNodes.size} | ${String.format(Locale.US, "%.4f", step65BoundaryShare)}\n")
            }
            result.append("\nBoundary node list by shell:\n")
            step65ShellLevels.forEach { step65Shell ->
                val step65Nodes = step50CoreNumbers.filterValues { it == step65Shell }.keys
                val step65BoundaryNodes = step65Nodes.filter { step65Node ->
                    (step50Graph[step65Node] ?: emptySet()).any { step50CoreNumbers[it] != step65Shell }
                }.sorted()
                result.append("Shell $step65Shell: ${step65BoundaryNodes.joinToString(", ")}\n")
            }
            result.append("Boundary edges count shell-to-other-shell links; boundary nodes have at least one neighbor in another shell.\n")
            result.append("Step 65 is a descriptive graph boundary profile, not a temporal or predictive measure.\n")

            // STEP 66: BOUNDARY CONNECTIVITY & BOUNDARY INFLUENCE ANALYSIS
            result.append("\n=============================\n")
            result.append("STEP 66 - BOUNDARY CONNECTIVITY & BOUNDARY INFLUENCE ANALYSIS\n")
            result.append("=============================\n\n")
            result.append("Shell | Nodes | Boundary nodes | Cross-shell links | Avg cross-shell degree | Boundary influence ratio\n")
            step65ShellLevels.forEach { step66Shell ->
                val step66Nodes = step50CoreNumbers.filterValues { it == step66Shell }.keys
                val step66BoundaryNodes = step66Nodes.filter { step66Node ->
                    (step50Graph[step66Node] ?: emptySet()).any { step50CoreNumbers[it] != step66Shell }
                }
                val step66CrossLinks = step66Nodes.sumOf { step66Node ->
                    (step50Graph[step66Node] ?: emptySet()).count { step50CoreNumbers[it] != step66Shell }
                }
                val step66AvgCrossDegree = if (step66Nodes.isNotEmpty()) step66CrossLinks.toDouble() / step66Nodes.size else 0.0
                val step66InfluenceRatio = if (step66Nodes.isNotEmpty()) step66BoundaryNodes.size.toDouble() / step66Nodes.size else 0.0
                result.append("$step66Shell | ${step66Nodes.size} | ${step66BoundaryNodes.size} | $step66CrossLinks | ${String.format(Locale.US, "%.4f", step66AvgCrossDegree)} | ${String.format(Locale.US, "%.4f", step66InfluenceRatio)}\n")
            }
            result.append("\nBoundary node cross-shell connectivity ranking:\n")
            val step66BoundaryRank = step50CoreNumbers.keys.map { step66Node ->
                val step66Shell = step50CoreNumbers[step66Node] ?: 0
                val step66CrossNeighbors = (step50Graph[step66Node] ?: emptySet()).filter { step50CoreNumbers[it] != step66Shell }
                Triple(step66Node, step66Shell, step66CrossNeighbors.size)
            }.filter { it.third > 0 }.sortedByDescending { it.third }
            step66BoundaryRank.forEach { (step66Node, step66Shell, step66CrossCount) ->
                result.append("$step66Node (shell $step66Shell): $step66CrossCount cross-shell neighbors\n")
            }
            result.append("Step 66 summarizes cross-shell connectivity and the proportion of shell nodes linked outside their shell; it is descriptive, not causal or predictive.\n")

            // STEP 67: BOUNDARY BRIDGE & STRUCTURAL BOTTLENECK ANALYSIS
            result.append("\n=============================\n")
            result.append("STEP 67 - BOUNDARY BRIDGE & STRUCTURAL BOTTLENECK ANALYSIS\n")
            result.append("=============================\n\n")
            val step67CrossShellEdges = mutableListOf<Pair<String, String>>()
            step50Graph.forEach { (step67Node, step67Neighbors) ->
                step67Neighbors.forEach { step67Neighbor ->
                    val step67ShellA = step50CoreNumbers[step67Node] ?: 0
                    val step67ShellB = step50CoreNumbers[step67Neighbor] ?: 0
                    if (step67ShellA != step67ShellB && step67Node < step67Neighbor) {
                        step67CrossShellEdges.add(step67Node to step67Neighbor)
                    }
                }
            }
            result.append("Cross-shell edge list (unique undirected links):\n")
            step67CrossShellEdges.forEach { (step67A, step67B) ->
                result.append("$step67A (shell ${step50CoreNumbers[step67A]}) -- $step67B (shell ${step50CoreNumbers[step67B]})\n")
            }
            val step67BottleneckRanking = step50CoreNumbers.keys.map { step67Node ->
                val step67Shell = step50CoreNumbers[step67Node] ?: 0
                val step67CrossNeighbors = (step50Graph[step67Node] ?: emptySet()).filter { (step50CoreNumbers[it] ?: 0) != step67Shell }
                val step67UniqueShells = step67CrossNeighbors.map { step50CoreNumbers[it] ?: 0 }.toSet().size
                Triple(step67Node, step67CrossNeighbors.size, step67UniqueShells)
            }.filter { it.second > 0 }.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
            result.append("\nBoundary bottleneck ranking (cross-shell neighbor count; distinct adjacent shell count):\n")
            step67BottleneckRanking.forEach { (step67Node, step67Links, step67ShellCount) ->
                result.append("$step67Node: $step67Links cross-shell links; $step67ShellCount adjacent shell levels\n")
            }
            val step67TotalCrossEdges = step67CrossShellEdges.size
            val step67BoundaryNodeCount = step67BottleneckRanking.size
            result.append("\nTotal unique cross-shell edges: $step67TotalCrossEdges\n")
            result.append("Boundary/bottleneck nodes identified: $step67BoundaryNodeCount\n")
            result.append("This descriptive ranking identifies nodes with cross-shell connectivity; it does not establish causal influence or formal graph bridge status.\n")

            // STEP 68: BOUNDARY VULNERABILITY & SHELL SEPARATION ANALYSIS
            result.append("\n=============================\n")
            result.append("STEP 68 - BOUNDARY VULNERABILITY & SHELL SEPARATION ANALYSIS\n")
            result.append("=============================\n\n")
            val step68Shells = step50CoreNumbers.values.toSet().sorted()
            val step68ShellBoundaryEdges = mutableMapOf<Int, Int>()
            val step68ShellNodes = mutableMapOf<Int, Int>()
            step68Shells.forEach { step68Shell ->
                step68ShellNodes[step68Shell] = step50CoreNumbers.count { it.value == step68Shell }
                step68ShellBoundaryEdges[step68Shell] = step67CrossShellEdges.count { (step68A, step68B) ->
                    (step50CoreNumbers[step68A] ?: 0) == step68Shell || (step50CoreNumbers[step68B] ?: 0) == step68Shell
                }
            }
            result.append("Shell boundary exposure profile (shell | nodes | incident cross-shell edges | exposure per node):\n")
            step68Shells.forEach { step68Shell ->
                val step68Count = step68ShellNodes[step68Shell] ?: 0
                val step68Exposure = step68ShellBoundaryEdges[step68Shell] ?: 0
                val step68PerNode = if (step68Count > 0) step68Exposure.toDouble() / step68Count else 0.0
                result.append("$step68Shell | $step68Count | $step68Exposure | ${String.format(Locale.US, "%.4f", step68PerNode)}\n")
            }
            val step68ShellPairCounts = step67CrossShellEdges.groupingBy { (step68A, step68B) ->
                val step68Low = minOf(step50CoreNumbers[step68A] ?: 0, step50CoreNumbers[step68B] ?: 0)
                val step68High = maxOf(step50CoreNumbers[step68A] ?: 0, step50CoreNumbers[step68B] ?: 0)
                "$step68Low-$step68High"
            }.eachCount().toSortedMap()
            result.append("\nShell-pair separation profile (shell pair | cross-shell links):\n")
            step68ShellPairCounts.forEach { (step68Pair, step68Count) -> result.append("$step68Pair | $step68Count\n") }
            val step68NodeExposureRanking = step50CoreNumbers.keys.map { step68Node ->
                val step68Shell = step50CoreNumbers[step68Node] ?: 0
                val step68Outside = (step50Graph[step68Node] ?: emptySet()).filter { (step50CoreNumbers[it] ?: 0) != step68Shell }
                val step68DistinctShells = step68Outside.map { step50CoreNumbers[it] ?: 0 }.toSet().size
                Triple(step68Node, step68Outside.size, step68DistinctShells)
            }.filter { it.second > 0 }.sortedWith(compareByDescending<Triple<String, Int, Int>> { it.second }.thenByDescending { it.third })
            result.append("\nBoundary exposure ranking (node | cross-shell degree | distinct shell levels):\n")
            step68NodeExposureRanking.forEach { (step68Node, step68Degree, step68Levels) ->
                result.append("$step68Node | $step68Degree | $step68Levels\n")
            }
            result.append("\nThis analysis describes shell exposure and cross-shell link distribution; it is not a simulated removal test and does not establish causal vulnerability.\n")

            // STEP 69: SHELL BOUNDARY FLOW & STRUCTURAL INTERACTION ANALYSIS
            result.append("\n=============================\n")
            result.append("STEP 69 - SHELL BOUNDARY FLOW & STRUCTURAL INTERACTION ANALYSIS\n")
            result.append("=============================\n\n")
            val step69ShellInteraction = mutableMapOf<Pair<Int, Int>, Int>()
            step67CrossShellEdges.forEach { (step69A, step69B) ->
                val step69ShellA = step50CoreNumbers[step69A] ?: 0
                val step69ShellB = step50CoreNumbers[step69B] ?: 0
                val step69Pair = if (step69ShellA <= step69ShellB) step69ShellA to step69ShellB else step69ShellB to step69ShellA
                step69ShellInteraction[step69Pair] = (step69ShellInteraction[step69Pair] ?: 0) + 1
            }
            result.append("Shell interaction flow (lower shell -> higher shell | links | normalized share):\n")
            val step69TotalInteractions = step69ShellInteraction.values.sum()
            step69ShellInteraction.toSortedMap(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second }).forEach { (step69Pair, step69Count) ->
                val step69Share = if (step69TotalInteractions > 0) step69Count.toDouble() / step69TotalInteractions else 0.0
                result.append("${step69Pair.first} -> ${step69Pair.second} | $step69Count | ${String.format(Locale.US, "%.4f", step69Share)}\n")
            }
            val step69NodeFlow = step50CoreNumbers.keys.map { step69Node ->
                val step69Shell = step50CoreNumbers[step69Node] ?: 0
                val step69Neighbors = (step50Graph[step69Node] ?: emptySet()).filter { (step50CoreNumbers[it] ?: 0) != step69Shell }
                val step69ShellsReached = step69Neighbors.map { step50CoreNumbers[it] ?: 0 }.toSet().sorted()
                Triple(step69Node, step69Neighbors.size, step69ShellsReached)
            }.filter { it.second > 0 }.sortedWith(compareByDescending<Triple<String, Int, List<Int>>> { it.second }.thenBy { it.first })
            result.append("\nNode boundary-flow profile (node | cross-shell links | shell levels reached):\n")
            step69NodeFlow.forEach { (step69Node, step69Links, step69Reached) ->
                result.append("$step69Node | $step69Links | ${step69Reached.joinToString(", ")}\n")
            }
            val step69ShellTotals = step68Shells.map { step69Shell ->
                val step69Nodes = step50CoreNumbers.filterValues { it == step69Shell }.keys
                val step69Out = step69Nodes.sumOf { step69Node -> (step50Graph[step69Node] ?: emptySet()).count { (step50CoreNumbers[it] ?: 0) != step69Shell } }
                step69Shell to step69Out
            }
            result.append("\nShell boundary interaction totals (shell | incident cross-shell links counted from nodes):\n")
            step69ShellTotals.forEach { (step69Shell, step69Count) -> result.append("$step69Shell | $step69Count\n") }
            result.append("\nThis profile summarizes descriptive cross-shell interactions and normalized link shares; it does not represent causal flow.\n")

            appendStep70Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep71Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep72Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep73Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep74Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep75Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep76Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep77Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep78Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep79Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep80Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep81Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep82Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep83Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep84Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep85Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep86Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep87Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep88Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep89Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep90Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep91Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep92Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep93Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep94Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep95Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep96Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep97Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep98Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep99Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep100MilestoneAnalysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep101Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep102Analysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep103RobustGapDispersion(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep104RobustDispersionJackknife(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep105RobustBootstrapProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep106BootstrapIntervalStability(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep107BootstrapDistributionShape(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep108BootstrapConvergence(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep109BootstrapConvergenceDiagnostics(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep110CompositeBootstrapStabilityIndex(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep111WeightSensitivity(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep112RankConsistency(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep113RankAgreementCorrelation(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep114RankAgreementRobustness(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep115MultiScenarioRankStability(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep116RankStabilityConsensusProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep117ConsensusThresholdSensitivity(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep118ConsensusScoreDistribution(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep119ConsensusDistributionConcentration(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep120ConcentrationStabilitySensitivity(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep121ConcentrationPatternPersistence(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep122ConcentrationTransitionAnalysis(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep123TransitionStabilityPersistence(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep124TransitionPatternDistribution(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep125TransitionPatternConcentration(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep126TransitionPatternAssociation(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep127AssociationEntropyAndStrength(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep128AssociationBalanceAndReciprocity(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep129DirectionalAssociationAsymmetry(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep130DirectionalMagnitudeProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep131ScenarioBandPersistence(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep132ScenarioBandSpread(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep133RobustnessTierSummary(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep134RobustnessIndex(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep135RobustnessAudit(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep136GapQuantileProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep137GapIqrOutlierSummary(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep138GapOutlierDetail(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep139GapBandFrequency(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep140GapBandConcentration(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep141GapBandEntropy(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep142GapBandCumulativeProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep143ExtendedGapThresholdProfile(result, step50CoreNumbers, step67CrossShellEdges)
            appendStep144GapIntervalDistribution(result, step50CoreNumbers, step67CrossShellEdges)

            // COMPLETION

            result.append("\n")
            result.append("CALCULATION COMPLETE\n")
            result.append("Ayanamsa: Lahiri\n")
            result.append("Node: Mean Node\n")
            result.append("Step 6: Graha Drishti included\n")
            result.append("Step 7: Bhava Drishti Received Analysis included\n")
            result.append("Step 12: Planet Strength Ranking included\n")
            result.append("Step 13: Integrated Horoscope Analysis included\nStep 14: Integrated Bhava Prediction Score included\nStep 15: Prediction Interpretation Layer included\nStep 16: Planet-specific Bhava Interpretation included\nStep 17: Planetary Influence Integration included\nStep 18: Badhaka Lord Analysis included\nStep 19: Tithi Sunya Analysis included\nStep 20: Experimental Mudakku Rasi Analysis included\nStep 21: Karana Calculation included\nStep 22: Combined Astrological Factor Summary included\nStep 23: 12 Bhava Factor Association included\nStep 24: Bhava Factor Consolidation included\nStep 25: Bhava Factor Context Matrix included\nStep 26: Bhava Lord Cross-Link Matrix included\nStep 27: Bhava Network Summary included\nStep 28: Bhava Network Pattern Analysis included\nStep 29: Bhava Network Connectivity Analysis included\nStep 30: Bhava Network Structural Classification included\nStep 31: Bhava Network Topology Analysis included\nStep 32: Bhava Network Path Analysis included\nStep 33: Bhava Network Influence Propagation included\nStep 34: Bhava Network Dependency Analysis included\nStep 35: Bhava Network Centrality Analysis included\nStep 36: Bhava Network Cluster Analysis included\nStep 37: Bhava Network Cluster Cohesion Analysis included\nStep 38: Bhava Network Articulation Node Analysis included\nStep 39: Bhava Network Bridge-Link Analysis included\nStep 40: Bhava Network Link Redundancy Analysis included\nStep 41: Bhava Network Biconnected Component Analysis included\nStep 42: Bhava Network Block-Cut Tree Analysis included\nStep 43: Bhava Block-Cut Representation Topology Metrics included\nStep 44: Block-Cut Graph Distance and Centrality Metrics included\nStep 45: Component Eccentricity, Radius, Center and Periphery included\nStep 46: Connected-Component Diameter and Distance Profile included\nStep 47: Component Distance Distribution and Global Efficiency included\nStep 48: Degree Distribution and Degree Assortativity included\nStep 49: Local Clustering Coefficient and Graph Transitivity included\nStep 50: K-Core Decomposition included\nStep 51: K-Core Structure Profile included\nStep 52: K-Core Shell and Inter-Shell Edge Profile included\nStep 53: Core-Shell Edge Assortativity included\nStep 54: Core-Shell Connected Component Profile included\nStep 55: Core-Shell Mixing Matrix included\nStep 56: Core-Shell Structural Cohesion included\nStep 57: Core-Shell Neighbor Composition Profile included\nStep 58: Core-Shell Hierarchy Transition Profile included\nStep 59: Core-Shell Retention Profile included\nStep 60: Core-Shell Node Importance Ranking included\nStep 61: Core-Peeling Order Analysis included\nStep 62: Core-Peeling Layer Statistics included\nStep 63: Core-Peeling Persistence & Layer Transition included\nStep 64: Core-Peeling Structural Stability Profile included\nStep 65: Core-Peeling Structural Boundary Analysis included\nStep 66: Boundary Connectivity & Boundary Influence Analysis included\nStep 67: Boundary Bridge & Structural Bottleneck Analysis included\nStep 68: Boundary Vulnerability & Shell Separation Analysis included\nStep 69: Shell Boundary Flow & Structural Interaction Analysis included\nStep 70: Global Shell Interaction Network Analysis included\nStep 71: Shell Interaction Intensity & Participation Analysis included\nStep 72: Shell Interaction Distribution & Balance Analysis included\nStep 73: Node Shell Interaction Centrality & Participation Analysis included\nStep 74: Node Cross-Shell Interaction Diversity Analysis included\nStep 75: Cross-Shell Edge Density & Occupancy Profile included\nStep 76: Shell Gap Connectivity Profile included\nStep 77: Shell Gap Link Share & Entropy Profile included\nStep 78: Shell Gap Weighted Distance Profile included\nStep 79: Shell Gap Cumulative Distribution Profile included\nStep 80: Shell Gap Dispersion and Shape Profile included\nStep 81: Shell Gap Quartile and Robust Spread Profile included\nStep 82: Shell Gap Outlier Detail Profile included\nStep 83: Shell Gap Frequency Concentration Profile included\nStep 84: Shell Gap Frequency Balance Profile included\nStep 85: Shell Gap Comparative Frequency Profile included\nStep 86: Shell Gap Frequency Rank & Relative Deviation Profile included\nStep 87: Shell Gap Comparative Frequency Ratio Profile included\nStep 88: Shell Gap Frequency Comparison Summary included\nStep 89: Shell Gap Frequency Trend Profile included\nStep 90: Shell Gap Trend Stability Profile included\nStep 91: Shell Gap Trend Acceleration Profile included\nStep 92: Shell Gap Trend Third-Difference Profile included\nStep 93: Shell Gap Frequency Turning-Point Profile included\nStep 94: Shell Gap Local Peak & Valley Profile included\nStep 95: Shell Gap Local Peak Prominence Profile included\nStep 96: Shell Gap Local Valley Prominence Profile included\nStep 97: Shell Gap Peak-Valley Contrast Profile included\nStep 98: Shell Gap Peak-Valley Separation Profile included\nStep 99: Shell Gap Peak-Valley Structure Comparison included\nStep 101: Shell Gap Frequency Leave-One-Link-Out Sensitivity included\nStep 102: Cross-Shell Gap Mean and Variance Jackknife Profile included\nStep 103: Cross-Shell Gap Robust Dispersion Profile included\nStep 104: Robust Dispersion Leave-One-Link-Out Sensitivity included\nStep 105: Robust Dispersion Bootstrap Confidence Profile included\nStep 106: Bootstrap Interval Stability Profile included\nStep 107: Bootstrap Distribution Shape Analysis included\nStep 109: Bootstrap Convergence Diagnostics included\nStep 110: Composite Bootstrap Stability Index included\nStep 111: Composite Stability Weight Sensitivity included\nStep 117: Consensus Threshold Sensitivity Profile included\nStep 122: Concentration Profile Transition & Structural Change Analysis included\nStep 118: Consensus Score Distribution & Separation Profile included\nStep 121: Concentration Pattern Persistence & Rank Stability included\n")

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


    private fun appendStep118ConsensusScoreDistribution(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 118 - CONSENSUS SCORE DISTRIBUTION & SEPARATION PROFILE\n")
        result.append("=============================\n\n")
        val gaps118 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps118.size < 3) {
            result.append("At least three valid cross-shell links are required.\n")
            result.append("Consensus distribution profile unavailable.\n")
            return
        }
        val mean118 = gaps118.average()
        val median118 = gaps118.sorted().let { s ->
            if (s.size % 2 == 1) s[s.size / 2].toDouble()
            else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        val mad118 = gaps118.map { kotlin.math.abs(it - median118) }.sorted().let { s ->
            if (s.size % 2 == 1) s[s.size / 2].toDouble()
            else (s[s.size / 2 - 1] + s[s.size / 2]) / 2.0
        }
        val score118 = (100.0 * (1.0 - (if (mean118 != 0.0) mad118 / mean118 else 0.0)).coerceIn(0.0, 1.0))
        result.append("Consensus heuristic score: %.2f%%\n".format(score118))
        result.append("Score basis: cross-shell gap median and median absolute deviation.\n")
        result.append("Score distribution bands (band | count | share):\n")
        val bands118 = listOf(0.0 to 19.999, 20.0 to 39.999, 40.0 to 59.999, 60.0 to 79.999, 80.0 to 100.0)
        var bandPopulation118 = 0
        bands118.forEach { (lo, hi) ->
            val count = if (score118 >= lo && score118 <= hi) 1 else 0
            bandPopulation118 += count
            result.append("%.0f-%.0f%% | %d | %.4f\n".format(lo, hi, count, count.toDouble()))
        }
        result.append("\nBand population: $bandPopulation118 score value represented.\n")
        result.append("Band separation (adjacent band midpoint difference): 20.00 score points.\n")
        result.append("Distribution summary: the current calculation yields one aggregate consensus score, not a population of independent scores; band counts therefore represent placement of this aggregate score only.\n")
        result.append("Limitations: this is a descriptive heuristic profile and is not validated statistical confidence or predictive evidence.\n")
    }



    private fun appendStep119ConsensusDistributionConcentration(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 119 - CONSENSUS DISTRIBUTION CONCENTRATION & DOMINANCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps119 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb) else null
        }
        if (gaps119.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Concentration profile unavailable.\n")
            return
        }
        val maxGap119 = gaps119.maxOrNull()?.toDouble() ?: 0.0
        val normalized119 = gaps119.map { if (maxGap119 > 0.0) it.toDouble() / maxGap119 * 100.0 else 0.0 }
        val bands119 = listOf(0.0 to 19.999, 20.0 to 39.999, 40.0 to 59.999, 60.0 to 79.999, 80.0 to 100.0)
        val counts119 = bands119.map { (lo, hi) -> normalized119.count { it >= lo && it <= hi } }
        val total119 = counts119.sum()
        result.append("Normalized cross-shell gap distribution (band | count | share):\n")
        bands119.forEachIndexed { i, (lo, hi) ->
            val count = counts119[i]
            val share = if (total119 > 0) count.toDouble() / total119 else 0.0
            result.append("%.0f-%.0f%% | %d | %.4f\n".format(lo, hi, count, share))
        }
        val dominantIndex119 = counts119.indices.maxByOrNull { counts119[it] } ?: 0
        val dominantCount119 = counts119.getOrElse(dominantIndex119) { 0 }
        val dominantBand119 = bands119[dominantIndex119]
        val dominance119 = if (total119 > 0) dominantCount119.toDouble() / total119 else 0.0
        val shares119 = counts119.filter { it > 0 }.map { it.toDouble() / total119 }
        val concentration119 = shares119.sumOf { it * it }
        val entropy119 = if (shares119.size > 1) -shares119.sumOf { it * kotlin.math.ln(it) } / kotlin.math.ln(5.0) else 0.0
        result.append("\nDominant band: %.0f-%.0f%% (%d observations)\n".format(dominantBand119.first, dominantBand119.second, dominantCount119))
        result.append("Band dominance ratio: %.4f\n".format(dominance119))
        result.append("Band concentration index (sum of squared band shares): %.4f\n".format(concentration119))
        result.append("Normalized band entropy (0-1; higher indicates more spread across bands): %.4f\n".format(entropy119))
        result.append("Concentration/dispersion interpretation: ")
        result.append(if (dominance119 >= 0.60) "observations are concentrated in one normalized band.\n" else "observations are distributed across multiple normalized bands.\n")
        result.append("Distribution imbalance indicator (dominant share minus average band share): %.4f\n".format(dominance119 - 0.2))
        result.append("Normalization uses the maximum observed cross-shell gap in this calculation; bands describe this dataset only.\n")
        result.append("Limitations: descriptive distribution metrics are not validated statistical confidence or predictive evidence.\n")
    }


    private fun appendStep120ConcentrationStabilitySensitivity(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 120 - CONSENSUS CONCENTRATION STABILITY & SENSITIVITY PROFILE\n")
        result.append("=============================\n\n")
        val gaps120 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps120.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Concentration sensitivity profile unavailable.\n")
            return
        }
        val max120 = gaps120.maxOrNull() ?: 0.0
        val values120 = gaps120.map { if (max120 > 0.0) it / max120 * 100.0 else 0.0 }
        val shifts120 = listOf(0.0, 5.0, 10.0, 15.0)
        result.append("Band-boundary sensitivity scenarios (shift | dominant band | dominance ratio | concentration index | normalized entropy):\n")
        val scenarioDominants120 = mutableListOf<Int>()
        shifts120.forEach { shift ->
            val bounds = listOf(
                (0.0 + shift).coerceAtMost(20.0),
                (20.0 + shift).coerceAtMost(40.0),
                (40.0 + shift).coerceAtMost(60.0),
                (60.0 + shift).coerceAtMost(80.0),
                100.0
            )
            val counts = MutableList(5) { 0 }
            values120.forEach { value ->
                val index = bounds.indexOfFirst { value <= it }.coerceAtLeast(0).coerceAtMost(4)
                counts[index]++
            }
            val total = counts.sum()
            val dominantIndex = counts.indices.maxByOrNull { counts[it] } ?: 0
            scenarioDominants120.add(dominantIndex)
            val dominantRatio = if (total > 0) counts[dominantIndex].toDouble() / total else 0.0
            val shares = counts.filter { it > 0 }.map { it.toDouble() / total }
            val concentration = shares.sumOf { it * it }
            val entropy = if (shares.size > 1) -shares.sumOf { it * kotlin.math.ln(it) } / kotlin.math.ln(5.0) else 0.0
            result.append("%.0f | band %d | %.4f | %.4f | %.4f\n".format(shift, dominantIndex + 1, dominantRatio, concentration, entropy))
        }
        val uniqueDominants = scenarioDominants120.toSet().size
        val dominantStability = if (scenarioDominants120.isNotEmpty()) scenarioDominants120.count { it == scenarioDominants120[0] }.toDouble() / scenarioDominants120.size else 0.0
        result.append("\nDominant-band scenario count: ${scenarioDominants120.size}\n")
        result.append("Distinct dominant bands across scenarios: $uniqueDominants\n")
        result.append("Dominant-band stability ratio (relative to baseline): %.4f\n".format(dominantStability))
        result.append("Sensitivity interpretation: ")
        result.append(if (uniqueDominants == 1) "dominant band remained unchanged across tested boundary shifts.\n" else "dominant band changed under at least one tested boundary shift.\n")
        result.append("Scenario shifts are illustrative boundary perturbations and do not constitute statistical validation.\n")
        result.append("Limitations: descriptive sensitivity analysis only; no causal or predictive claim is implied.\n")
    }


    private fun appendStep121ConcentrationPatternPersistence(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 121 - CONCENTRATION PATTERN PERSISTENCE & RANK STABILITY\n")
        result.append("=============================\n\n")
        val gaps121 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps121.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Persistence profile unavailable.\n")
            return
        }
        val maximum121 = gaps121.maxOrNull() ?: 0.0
        val normalized121 = gaps121.map { if (maximum121 > 0.0) it / maximum121 * 100.0 else 0.0 }
        val shifts121 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        val scenarioDominantBands121 = mutableListOf<Int>()
        val scenarioProfiles121 = mutableListOf<List<Int>>()
        result.append("Boundary-shift scenario band populations (shift | counts across 5 bands | dominant band):\n")
        shifts121.forEach { shift ->
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val counts = MutableList(5) { 0 }
            normalized121.forEach { v ->
                val index = boundaries.indexOfFirst { v <= it }.let { if (it < 0) 4 else it }
                counts[index]++
            }
            val dominant = counts.indices.maxByOrNull { counts[it] } ?: 0
            scenarioDominantBands121.add(dominant)
            scenarioProfiles121.add(counts)
            result.append("%.0f | %s | band %d\n".format(shift, counts.joinToString(", "), dominant + 1))
        }
        val baseline = scenarioProfiles121.firstOrNull() ?: emptyList()
        val populationAgreement121 = if (baseline.isNotEmpty() && scenarioProfiles121.size > 1) {
            scenarioProfiles121.drop(1).map { profile ->
                val denom = baseline.sum().coerceAtLeast(1)
                1.0 - baseline.indices.sumOf { i -> kotlin.math.abs(baseline[i] - profile.getOrElse(i) { 0 }) }.toDouble() / (2.0 * denom)
            }.average()
        } else 1.0
        val dominantPersistence121 = if (scenarioDominantBands121.isNotEmpty()) {
            scenarioDominantBands121.count { it == scenarioDominantBands121.first() }.toDouble() / scenarioDominantBands121.size
        } else 0.0
        val switching121 = scenarioDominantBands121.zipWithNext().count { it.first != it.second }
        val unique121 = scenarioDominantBands121.toSet().size
        result.append("\nDominant-band persistence relative to baseline: %.4f\n".format(dominantPersistence121))
        result.append("Adjacent scenario dominant-band switches: $switching121\n")
        result.append("Distinct dominant bands across scenarios: $unique121\n")
        result.append("Mean population-profile agreement with baseline (0-1): %.4f\n".format(populationAgreement121))
        result.append("Persistence summary: ")
        result.append(if (dominantPersistence121 >= 0.80 && populationAgreement121 >= 0.80) "dominant band and population profile show high agreement across these illustrative shifts.\n" else "at least one dominant-band or population-profile change is present across the illustrative shifts.\n")
        result.append("Interpretation is limited to sensitivity across chosen boundary shifts; it is not temporal persistence, statistical validation, or predictive evidence.\n")
    }


    private fun appendStep122ConcentrationTransitionAnalysis(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 122 - CONCENTRATION PROFILE TRANSITION & STRUCTURAL CHANGE ANALYSIS\n")
        result.append("=============================\n\n")
        val gaps122 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps122.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Transition profile unavailable.\n")
            return
        }
        val maxGap122 = gaps122.maxOrNull() ?: 0.0
        val normalized122 = gaps122.map { if (maxGap122 > 0.0) it / maxGap122 * 100.0 else 0.0 }
        val shifts122 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun profile122(shift: Double): List<Int> {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val counts = MutableList(5) { 0 }
            normalized122.forEach { value ->
                val idx = bounds.indexOfFirst { value <= it }.let { if (it < 0) 4 else it }
                counts[idx]++
            }
            return counts
        }
        val profiles122 = shifts122.map { profile122(it) }
        result.append("Scenario population profiles (shift | band 1..5 counts):\n")
        shifts122.forEachIndexed { i, shift ->
            result.append("%.0f | %s\n".format(shift, profiles122[i].joinToString(", ")))
        }
        val transition122 = Array(5) { IntArray(5) }
        normalized122.forEach { value ->
            fun band122(shift: Double): Int {
                val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
                return bounds.indexOfFirst { value <= it }.let { if (it < 0) 4 else it }
            }
            val start = band122(shifts122.first())
            val end = band122(shifts122.last())
            transition122[start][end]++
        }
        result.append("\nBaseline-to-final scenario transition matrix (rows=baseline bands; columns=final bands):\n")
        result.append("Band | " + (1..5).joinToString(" | ") { "B$it" } + "\n")
        for (row in 0..4) {
            result.append("B${row + 1} | " + transition122[row].joinToString(" | ") + "\n")
        }
        val total122 = normalized122.size
        val changed122 = normalized122.count { value ->
            val base = listOf(20.0, 40.0, 60.0, 80.0).indexOfFirst { value <= it }.let { if (it < 0) 4 else it }
            val finalBounds = listOf(40.0, 60.0, 80.0, 100.0)
            val fin = finalBounds.indexOfFirst { value <= it }.let { if (it < 0) 4 else it }
            base != fin
        }
        val changeRate122 = if (total122 > 0) changed122.toDouble() / total122 else 0.0
        result.append("\nObserved band transition count: $changed122 / $total122\n")
        result.append("Transition proportion (baseline vs final boundary scenario): %.4f\n".format(changeRate122))
        val absolutePopulationChange122 = profiles122.first().indices.sumOf { idx ->
            kotlin.math.abs(profiles122.first()[idx] - profiles122.last()[idx])
        }
        result.append("Total absolute band-population change: $absolutePopulationChange122\n")
        result.append("Structural-change summary: ")
        result.append(if (changeRate122 == 0.0 && absolutePopulationChange122 == 0) "band assignments and population counts were unchanged between these boundary scenarios.\n" else "band assignments or population counts changed between these boundary scenarios.\n")
        result.append("This is a descriptive boundary-scenario comparison only; it does not imply temporal change, causation, or predictive validity.\n")
    }


    private fun appendStep123TransitionStabilityPersistence(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 123 - TRANSITION STABILITY & CHANGE PERSISTENCE PROFILE\n")
        result.append("=============================\n\n")
        val gaps123 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps123.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Transition stability profile unavailable.\n")
            return
        }
        val maxGap123 = gaps123.maxOrNull() ?: 0.0
        val values123 = gaps123.map { if (maxGap123 > 0.0) it / maxGap123 * 100.0 else 0.0 }
        val shifts123 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band123(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val idx = bounds.indexOfFirst { value <= it }
            return if (idx < 0) 4 else idx
        }
        val scenarioBands123 = shifts123.map { shift -> values123.map { band123(it, shift) } }
        result.append("Adjacent-scenario persistence (shift pair | unchanged | changed | persistence ratio):\n")
        var totalTransitions123 = 0
        var totalUnchanged123 = 0
        var totalChanged123 = 0
        (0 until shifts123.size - 1).forEach { i ->
            val left = scenarioBands123[i]
            val right = scenarioBands123[i + 1]
            val unchanged = left.indices.count { left[it] == right[it] }
            val changed = left.size - unchanged
            val ratio = if (left.isNotEmpty()) unchanged.toDouble() / left.size else 0.0
            totalTransitions123 += left.size
            totalUnchanged123 += unchanged
            totalChanged123 += changed
            result.append("%.0f -> %.0f | %d | %d | %.4f\n".format(shifts123[i], shifts123[i + 1], unchanged, changed, ratio))
        }
        val overallPersistence123 = if (totalTransitions123 > 0) totalUnchanged123.toDouble() / totalTransitions123 else 0.0
        val changePersistence123 = if (totalTransitions123 > 0) totalChanged123.toDouble() / totalTransitions123 else 0.0
        result.append("\nAggregate adjacent-scenario assignment persistence: %.4f\n".format(overallPersistence123))
        result.append("Aggregate adjacent-scenario change proportion: %.4f\n".format(changePersistence123))
        val transitionFrequency123 = Array(5) { IntArray(5) }
        (0 until shifts123.size - 1).forEach { i ->
            scenarioBands123[i].indices.forEach { j -> transitionFrequency123[scenarioBands123[i][j]][scenarioBands123[i + 1][j]]++ }
        }
        result.append("\nAdjacent-scenario transition frequency matrix (rows=from; columns=to):\n")
        result.append("Band | " + (1..5).joinToString(" | ") { "B$it" } + "\n")
        for (row in 0..4) result.append("B${row + 1} | " + transitionFrequency123[row].joinToString(" | ") + "\n")
        val scenarioProfileAgreement123 = shifts123.indices.map { i ->
            val profile = IntArray(5)
            scenarioBands123[i].forEach { profile[it]++ }
            profile
        }
        val profileDeviation123 = scenarioProfileAgreement123.first().indices.sumOf { band ->
            scenarioProfileAgreement123.drop(1).sumOf { kotlin.math.abs(it[band] - scenarioProfileAgreement123.first()[band]) }
        }
        result.append("Total band-population deviation from baseline across shifts: $profileDeviation123\n")
        result.append("Persistence summary: ")
        result.append(if (overallPersistence123 >= 0.80) "band assignments show high agreement across adjacent boundary-shift scenarios.\n" else "assignment changes are present across adjacent boundary-shift scenarios.\n")
        result.append("This descriptive sensitivity measure does not represent time-based persistence, causal change, or predictive/statistical validation.\n")
    }


    private fun appendStep124TransitionPatternDistribution(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 124 - TRANSITION PATTERN DISTRIBUTION & FREQUENCY PROFILE\n")
        result.append("=============================\n\n")
        val gaps124 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps124.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Transition pattern distribution unavailable.\n")
            return
        }
        val maxGap124 = gaps124.maxOrNull() ?: 0.0
        val normalized124 = gaps124.map { if (maxGap124 > 0.0) it / maxGap124 * 100.0 else 0.0 }
        val shifts124 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band124(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val ix = bounds.indexOfFirst { value <= it }
            return if (ix < 0) 4 else ix
        }
        val scenarios124 = shifts124.map { shift -> normalized124.map { band124(it, shift) } }
        val freq124 = Array(5) { IntArray(5) }
        (0 until shifts124.size - 1).forEach { scenario ->
            scenarios124[scenario].indices.forEach { item ->
                freq124[scenarios124[scenario][item]][scenarios124[scenario + 1][item]]++
            }
        }
        val transitions124 = mutableListOf<Pair<Pair<Int, Int>, Int>>()
        for (from in 0..4) for (dest in 0..4) {
            if (freq124[from][dest] > 0) transitions124.add(Pair(from, dest) to freq124[from][dest])
        }
        val total124 = freq124.sumOf { row -> row.sum() }
        val distinct124 = transitions124.size
        val ranked124 = transitions124.sortedByDescending { it.second }
        result.append("Transition frequency matrix (rows=from band; columns=to band):\n")
        result.append("Band | " + (1..5).joinToString(" | ") { "B$it" } + "\n")
        for (row in 0..4) result.append("B${row + 1} | " + freq124[row].joinToString(" | ") + "\n")
        result.append("\nObserved transition events: $total124\n")
        result.append("Distinct transition patterns: $distinct124 / 25\n")
        if (ranked124.isNotEmpty()) {
            val most = ranked124.first()
            result.append("Most frequent transition: B${most.first.first + 1} -> B${most.first.second + 1} (${most.second} events)\n")
            val topShare124 = if (total124 > 0) most.second.toDouble() / total124 else 0.0
            result.append("Most frequent transition share: %.4f\n".format(topShare124))
            val rare124 = ranked124.filter { it.second == 1 }
            result.append("Single-occurrence transition patterns: ${rare124.size}\n")
            result.append("Rarity profile: ")
            result.append(if (rare124.isEmpty()) "no observed pattern occurred only once in this scenario set.\n" else "${rare124.size} pattern(s) occurred once in this scenario set.\n")
            val countBands124 = listOf(1, 2, 3, 4, 5).map { threshold -> ranked124.count { it.second >= threshold } }
            result.append("Transition frequency bands (patterns occurring at least 1/2/3/4/5 times): ${countBands124.joinToString(" / ")}\n")
            val concentration124 = if (total124 > 0) ranked124.take(3).sumOf { it.second }.toDouble() / total124 else 0.0
            result.append("Top-3 pattern concentration: %.4f\n".format(concentration124))
        }
        result.append("Distribution summary: frequencies describe observed transitions across the boundary-shift scenarios only; they are not time-series events or evidence of predictive validity.\n")
    }


    private fun appendStep125TransitionPatternConcentration(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 125 - TRANSITION PATTERN CONCENTRATION & DOMINANCE ANALYSIS\n")
        result.append("=============================\n\n")
        val gaps125 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps125.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Transition concentration profile unavailable.\n")
            return
        }
        val maxGap125 = gaps125.maxOrNull() ?: 0.0
        val normalized125 = gaps125.map { if (maxGap125 > 0.0) it / maxGap125 * 100.0 else 0.0 }
        val shifts125 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band125(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val idx = bounds.indexOfFirst { value <= it }
            return if (idx < 0) 4 else idx
        }
        val scenarios125 = shifts125.map { shift -> normalized125.map { band125(it, shift) } }
        val freq125 = Array(5) { IntArray(5) }
        (0 until shifts125.size - 1).forEach { scenario ->
            scenarios125[scenario].indices.forEach { item ->
                freq125[scenarios125[scenario][item]][scenarios125[scenario + 1][item]]++
            }
        }
        val counts125 = freq125.flatMap { row -> row.toList() }.filter { it > 0 }
        val total125 = counts125.sum()
        if (total125 == 0) {
            result.append("No transition events counted.\n")
            return
        }
        val sorted125 = counts125.sortedDescending()
        val shares125 = counts125.map { it.toDouble() / total125 }
        val hhi125 = shares125.sumOf { it * it }
        val entropy125 = -shares125.sumOf { if (it > 0.0) it * kotlin.math.ln(it) else 0.0 }
        val maxEntropy125 = if (shares125.size > 1) kotlin.math.ln(shares125.size.toDouble()) else 0.0
        val normalizedEntropy125 = if (maxEntropy125 > 0.0) entropy125 / maxEntropy125 else 0.0
        val dominant125 = freq125.indices.flatMap { from -> freq125[from].indices.map { to -> Pair(from to to, freq125[from][to]) } }.filter { it.second > 0 }.maxByOrNull { it.second }
        result.append("Observed transition events: $total125\n")
        result.append("Distinct observed patterns: ${counts125.size} / 25\n")
        if (dominant125 != null) {
            result.append("Dominant pattern: B${dominant125.first.first + 1} -> B${dominant125.first.second + 1} (${dominant125.second} events)\n")
            result.append("Dominant-pattern share: %.4f\n".format(dominant125.second.toDouble() / total125))
        }
        result.append("Top-3 pattern share: %.4f\n".format(sorted125.take(3).sum().toDouble() / total125))
        result.append("Pattern concentration (HHI): %.4f\n".format(hhi125))
        result.append("Pattern entropy: %.4f\n".format(entropy125))
        result.append("Normalized pattern entropy: %.4f\n".format(normalizedEntropy125))
        result.append("Diversity summary: ")
        result.append(if (normalizedEntropy125 >= 0.75) "observed patterns are relatively distributed across categories in this scenario set.\n" else "frequency is comparatively concentrated among the observed transition patterns.\n")
        result.append("These descriptive measures reflect the constructed boundary-shift scenarios only; they do not establish time-series behavior, causal effects, or predictive validity.\n")
    }

    private fun appendStep126TransitionPatternAssociation(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 126 - TRANSITION PATTERN ASSOCIATION & CONDITIONAL PROFILE\n")
        result.append("=============================\n\n")
        val gaps126 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps126.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Conditional transition association profile unavailable.\n")
            return
        }
        val maxGap126 = gaps126.maxOrNull() ?: 0.0
        val normalized126 = gaps126.map { if (maxGap126 > 0.0) it / maxGap126 * 100.0 else 0.0 }
        val shifts126 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band126(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val index = bounds.indexOfFirst { value <= it }
            return if (index < 0) 4 else index
        }
        val scenarios126 = shifts126.map { shift -> normalized126.map { band126(it, shift) } }
        val matrix126 = Array(5) { IntArray(5) }
        (0 until shifts126.size - 1).forEach { scenario ->
            scenarios126[scenario].indices.forEach { item ->
                matrix126[scenarios126[scenario][item]][scenarios126[scenario + 1][item]]++
            }
        }
        val total126 = matrix126.sumOf { row -> row.sum() }
        if (total126 <= 0) {
            result.append("No conditional transition events counted.\n")
            return
        }
        result.append("Association matrix counts (source band rows -> destination band columns):\n")
        matrix126.indices.forEach { row ->
            result.append("B${row + 1}: ")
            result.append(matrix126[row].joinToString("  "))
            result.append("\n")
        }
        var diagonal126 = 0
        matrix126.indices.forEach { diagonal126 += matrix126[it][it] }
        result.append("Total adjacent-scenario transition events: $total126\n")
        result.append("Same-band retention share: %.4f\n".format(diagonal126.toDouble() / total126))
        matrix126.indices.forEach { row ->
            val rowTotal = matrix126[row].sum()
            if (rowTotal > 0) {
                val dominantCol = matrix126[row].indices.maxByOrNull { matrix126[row][it] } ?: 0
                result.append("B${row + 1} conditional dominant destination: B${dominantCol + 1} ")
                result.append("(row share %.4f; %d/%d)\n".format(matrix126[row][dominantCol].toDouble() / rowTotal, matrix126[row][dominantCol], rowTotal))
            } else {
                result.append("B${row + 1} conditional destination: no events\n")
            }
        }
        result.append("This conditional association profile is descriptive of constructed boundary-shift scenarios; it does not establish causal relationships or predictive validity.\n")
    }


    private fun appendStep127AssociationEntropyAndStrength(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 127 - ASSOCIATION ENTROPY & STRENGTH PROFILE\n")
        result.append("=============================\n\n")
        val gaps127 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps127.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Association entropy and strength profile unavailable.\n")
            return
        }
        val maxGap127 = gaps127.maxOrNull() ?: 0.0
        val normalized127 = gaps127.map { if (maxGap127 > 0.0) it / maxGap127 * 100.0 else 0.0 }
        val shifts127 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band127(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val index = bounds.indexOfFirst { value <= it }
            return if (index < 0) 4 else index
        }
        val scenarios127 = shifts127.map { shift -> normalized127.map { band127(it, shift) } }
        val matrix127 = Array(5) { IntArray(5) }
        (0 until shifts127.size - 1).forEach { scenario ->
            scenarios127[scenario].indices.forEach { item ->
                matrix127[scenarios127[scenario][item]][scenarios127[scenario + 1][item]]++
            }
        }
        val total127 = matrix127.sumOf { row -> row.sum() }
        if (total127 <= 0) {
            result.append("No adjacent-scenario association events counted.\n")
            return
        }
        val rowTotals127 = matrix127.map { it.sum() }
        val colTotals127 = IntArray(5) { col -> matrix127.sumOf { row -> row[col] } }
        fun entropy127(probabilities: List<Double>): Double = probabilities.filter { it > 0.0 }.sumOf { -it * kotlin.math.ln(it) }
        val maxEntropy127 = kotlin.math.ln(5.0)
        var weightedRowEntropy127 = 0.0
        result.append("Row-wise conditional entropy (natural-log units):\n")
        matrix127.indices.forEach { row ->
            val rowTotal = rowTotals127[row]
            if (rowTotal > 0) {
                val probs = matrix127[row].map { it.toDouble() / rowTotal }
                val h = entropy127(probs)
                weightedRowEntropy127 += rowTotal.toDouble() / total127 * h
                result.append("B${row + 1}: %.4f (normalized %.4f)\n".format(h, if (maxEntropy127 > 0) h / maxEntropy127 else 0.0))
            } else result.append("B${row + 1}: no events\n")
        }
        val sourceProb127 = rowTotals127.map { it.toDouble() / total127 }
        val destinationProb127 = colTotals127.map { it.toDouble() / total127 }
        val sourceEntropy127 = entropy127(sourceProb127)
        val destinationEntropy127 = entropy127(destinationProb127)
        val jointEntropy127 = entropy127(matrix127.flatMap { row -> row.map { it.toDouble() / total127 } })
        val association127 = (sourceEntropy127 + destinationEntropy127 - jointEntropy127).coerceAtLeast(0.0)
        val normalizedAssociation127 = if (minOf(sourceEntropy127, destinationEntropy127) > 0.0) association127 / minOf(sourceEntropy127, destinationEntropy127) else 0.0
        result.append("Weighted conditional entropy: %.4f\n".format(weightedRowEntropy127))
        result.append("Source-band entropy: %.4f\n".format(sourceEntropy127))
        result.append("Destination-band entropy: %.4f\n".format(destinationEntropy127))
        result.append("Descriptive association information: %.4f\n".format(association127))
        result.append("Normalized association indicator: %.4f\n".format(normalizedAssociation127.coerceIn(0.0, 1.0)))
        result.append("These entropy and association indicators summarize constructed boundary-shift transitions only; they do not establish causal relationships or predictive validity.\n")
    }




    private fun appendStep128AssociationBalanceAndReciprocity(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n=============================\n")
        result.append("STEP 128 - ASSOCIATION BALANCE & RECIPROCITY PROFILE\n")
        result.append("=============================\n\n")
        val gaps128 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps128.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Association balance and reciprocity profile unavailable.\n")
            return
        }
        val maxGap128 = gaps128.maxOrNull() ?: 0.0
        val normalized128 = gaps128.map { if (maxGap128 > 0.0) it / maxGap128 * 100.0 else 0.0 }
        val shifts128 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band128(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val index = bounds.indexOfFirst { value <= it }
            return if (index < 0) 4 else index
        }
        val scenarios128 = shifts128.map { shift -> normalized128.map { band128(it, shift) } }
        val matrix128 = Array(5) { IntArray(5) }
        (0 until shifts128.size - 1).forEach { scenario ->
            scenarios128[scenario].indices.forEach { item ->
                matrix128[scenarios128[scenario][item]][scenarios128[scenario + 1][item]]++
            }
        }
        val total128 = matrix128.sumOf { row -> row.sum() }
        if (total128 <= 0) {
            result.append("No adjacent-scenario association events counted.\n")
            return
        }
        val rowTotals128 = matrix128.map { row -> row.sum() }
        val colTotals128 = IntArray(5) { col -> matrix128.sumOf { row -> row[col] } }
        val diagonal128 = matrix128.indices.sumOf { matrix128[it][it] }
        val diagonalShare128 = diagonal128.toDouble() / total128
        var reciprocalPairs128 = 0
        var reciprocalMass128 = 0
        matrix128.indices.forEach { i ->
            (i + 1 until matrix128.size).forEach { j ->
                val pairMass = minOf(matrix128[i][j], matrix128[j][i])
                if (pairMass > 0) reciprocalPairs128++
                reciprocalMass128 += pairMass
            }
        }
        val imbalance128 = rowTotals128.indices.map { i -> kotlin.math.abs(rowTotals128[i] - colTotals128[i]) }.sum().toDouble() / (2.0 * total128)
        result.append("Adjacent-scenario transition matrix (rows B1-B5; columns B1-B5):\n")
        matrix128.forEachIndexed { row, values ->
            result.append("B${row + 1}: ")
            result.append(values.joinToString(" "))
            result.append("\n")
        }
        result.append("Diagonal retention count: %d/%d (share %.4f)\n".format(diagonal128, total128, diagonalShare128))
        result.append("Reciprocal nonzero band-pair count: %d\n".format(reciprocalPairs128))
        result.append("Reciprocal transition mass (paired minimum counts): %d\n".format(reciprocalMass128))
        result.append("Source-destination marginal imbalance indicator: %.4f\n".format(imbalance128.coerceIn(0.0, 1.0)))
        result.append("Row totals: %s\n".format(rowTotals128.joinToString(" ")))
        result.append("Column totals: %s\n".format(colTotals128.joinToString(" ")))
        result.append("These balance and reciprocity metrics describe constructed boundary-shift scenarios only; they do not establish causal relationships or predictive validity.\n")
    }


    private fun appendStep129DirectionalAssociationAsymmetry(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 129: Directional Association Asymmetry Profile ---\n")
        val gaps129 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps129.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Directional asymmetry profile unavailable.\n")
            return
        }
        val maxGap129 = gaps129.maxOrNull() ?: 0.0
        val norm129 = gaps129.map { if (maxGap129 > 0.0) it / maxGap129 * 100.0 else 0.0 }
        val shifts129 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band129(value: Double, shift: Double): Int {
            val bounds = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val ix = bounds.indexOfFirst { value <= it }
            return if (ix < 0) 4 else ix
        }
        val scenarios129 = shifts129.map { sh -> norm129.map { band129(it, sh) } }
        val matrix129 = Array(5) { IntArray(5) }
        (0 until shifts129.size - 1).forEach { sc ->
            scenarios129[sc].indices.forEach { ix ->
                matrix129[scenarios129[sc][ix]][scenarios129[sc + 1][ix]]++
            }
        }
        val total129 = matrix129.sumOf { row -> row.sum() }
        if (total129 == 0) {
            result.append("No adjacent-scenario transition observations counted.\n")
            return
        }
        var forwardMass129 = 0
        var reverseMass129 = 0
        var upward129 = 0
        var downward129 = 0
        matrix129.indices.forEach { i ->
            (i + 1 until matrix129.size).forEach { j ->
                forwardMass129 += matrix129[i][j]
                reverseMass129 += matrix129[j][i]
                upward129 += matrix129[i][j]
                downward129 += matrix129[j][i]
            }
        }
        val signed129 = (upward129 - downward129).toDouble() / total129
        val absolute129 = (upward129 + downward129).toDouble() / total129
        val net129 = signed129.coerceIn(-1.0, 1.0)
        val dominance129 = if (upward129 + downward129 > 0)
            maxOf(upward129, downward129).toDouble() / (upward129 + downward129) else 0.0
        result.append("Upward band transitions (higher destination band): %d\n".format(upward129))
        result.append("Downward band transitions (lower destination band): %d\n".format(downward129))
        result.append("Signed directional balance indicator: %.4f\n".format(net129))
        result.append("Directional movement share: %.4f\n".format(absolute129.coerceIn(0.0, 1.0)))
        result.append("Dominant direction share among off-diagonal transitions: %.4f\n".format(dominance129))
        result.append("These indicators describe constructed adjacent boundary-shift scenarios only; they do not establish causality or predictive validity.\n")
    }

    private fun appendStep130DirectionalMagnitudeProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 130: Directional Transition Magnitude Profile ---\n")
        val gaps130 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps130.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Directional magnitude profile unavailable.\n")
            return
        }
        val maxGap130 = gaps130.maxOrNull() ?: 0.0
        val normalized130 = gaps130.map { if (maxGap130 > 0.0) it / maxGap130 * 100.0 else 0.0 }
        val shifts130 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band130(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarios130 = shifts130.map { shift -> normalized130.map { band130(it, shift) } }
        val moves130 = mutableListOf<Int>()
        (0 until shifts130.size - 1).forEach { scenarioIndex ->
            scenarios130[scenarioIndex].indices.forEach { observationIndex ->
                moves130.add(scenarios130[scenarioIndex + 1][observationIndex] - scenarios130[scenarioIndex][observationIndex])
            }
        }
        if (moves130.isEmpty()) {
            result.append("No adjacent-scenario movement observations counted.\n")
            return
        }
        val total130 = moves130.size
        val upward130 = moves130.count { it > 0 }
        val downward130 = moves130.count { it < 0 }
        val stationary130 = moves130.count { it == 0 }
        val net130 = moves130.sum().toDouble() / total130
        val meanAbs130 = moves130.sumOf { kotlin.math.abs(it) }.toDouble() / total130
        val maxAbs130 = moves130.maxOf { kotlin.math.abs(it) }
        result.append("Adjacent-scenario movement observations: %d\n".format(total130))
        result.append("Upward movement count: %d\n".format(upward130))
        result.append("Downward movement count: %d\n".format(downward130))
        result.append("Stationary movement count: %d\n".format(stationary130))
        result.append("Mean signed band-step movement: %.4f\n".format(net130))
        result.append("Mean absolute band-step movement: %.4f\n".format(meanAbs130))
        result.append("Maximum absolute band-step movement: %d\n".format(maxAbs130))
        result.append("These descriptive measures summarize adjacent boundary-shift scenarios only; they do not establish predictive validity or causality.\n")
    }



    private fun appendStep131ScenarioBandPersistence(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 131: Scenario Band Persistence Profile ---\n")
        val gaps131 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null && sa != sb) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps131.isEmpty()) {
            result.append("No valid cross-shell gap observations available.\n")
            result.append("Scenario persistence profile unavailable.\n")
            return
        }
        val maxGap131 = gaps131.maxOrNull() ?: 0.0
        val normalized131 = gaps131.map { if (maxGap131 > 0.0) it / maxGap131 * 100.0 else 0.0 }
        val shifts131 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band131(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarioBands131 = shifts131.map { shift -> normalized131.map { band131(it, shift) } }
        val observations131 = normalized131.indices
        val persistent131 = observations131.count { index -> scenarioBands131.map { it[index] }.distinct().size == 1 }
        val total131 = observations131.count()
        val stability131 = if (total131 > 0) persistent131.toDouble() / total131 * 100.0 else 0.0
        val adjacentSame131 = mutableListOf<Boolean>()
        for (i in 0 until scenarioBands131.size - 1) {
            for (j in observations131) adjacentSame131.add(scenarioBands131[i][j] == scenarioBands131[i + 1][j])
        }
        val adjacentRate131 = if (adjacentSame131.isNotEmpty()) adjacentSame131.count { it }.toDouble() / adjacentSame131.size * 100.0 else 0.0
        result.append("Cross-shell observations assessed: %d\n".format(total131))
        result.append("Bands unchanged across all five scenarios: %d\n".format(persistent131))
        result.append("Five-scenario persistence rate: %.2f%%\n".format(stability131))
        result.append("Adjacent-scenario band retention rate: %.2f%%\n".format(adjacentRate131))
        result.append("This is a descriptive robustness profile for the selected boundary-shift scenarios; it is not predictive or causal validation.\n")
    }



    private fun appendStep132ScenarioBandSpread(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 132: Scenario Band Spread Profile ---\n")
        val gaps132 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps132.isEmpty()) {
            result.append("Scenario band spread profile unavailable.\n")
            return
        }
        val maxGap132 = gaps132.maxOrNull() ?: 0.0
        val normalized132 = gaps132.map { if (maxGap132 > 0.0) it / maxGap132 * 100.0 else 0.0 }
        val shifts132 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band132(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarioBands132 = shifts132.map { shift -> normalized132.map { band132(it, shift) } }
        val spread132 = normalized132.indices.map { idx ->
            val bands = scenarioBands132.map { it[idx] }
            (bands.maxOrNull() ?: 0) - (bands.minOrNull() ?: 0)
        }
        val zeroSpread132 = spread132.count { it == 0 }
        val meanSpread132 = spread132.average()
        val maxSpread132 = spread132.maxOrNull() ?: 0
        val spreadGroups132 = spread132.groupingBy { it }.eachCount().toSortedMap()
        result.append("Observations assessed: %d\n".format(spread132.size))
        result.append("Zero band-spread observations: %d (%.2f%%)\n".format(zeroSpread132, zeroSpread132.toDouble() / spread132.size * 100.0))
        result.append("Mean band spread across five scenarios: %.4f\n".format(meanSpread132))
        result.append("Maximum band spread: %d\n".format(maxSpread132))
        result.append("Spread distribution (spread : count): %s\n".format(spreadGroups132.entries.joinToString(", ") { "${it.key}:${it.value}" }))
        result.append("This is a descriptive sensitivity summary of the selected boundary shifts, not predictive or causal validation.\n")
    }



    private fun appendStep133RobustnessTierSummary(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 133: Robustness Tier Summary ---\n")
        val gaps133 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps133.isEmpty()) {
            result.append("Robustness tier summary unavailable: no valid observations.\n")
            return
        }
        val maxGap133 = gaps133.maxOrNull() ?: 0.0
        val normalized133 = gaps133.map { if (maxGap133 > 0.0) it / maxGap133 * 100.0 else 0.0 }
        val shifts133 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band133(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarioBands133 = shifts133.map { shift -> normalized133.map { band133(it, shift) } }
        val spreads133 = normalized133.indices.map { idx ->
            val bands = scenarioBands133.map { it[idx] }
            (bands.maxOrNull() ?: 0) - (bands.minOrNull() ?: 0)
        }
        val tierCounts133 = mapOf(
            "Robust (spread 0)" to spreads133.count { it == 0 },
            "Low variation (spread 1)" to spreads133.count { it == 1 },
            "Moderate variation (spread 2)" to spreads133.count { it == 2 },
            "High variation (spread 3+)" to spreads133.count { it >= 3 }
        )
        result.append("Observations classified: %d\n".format(spreads133.size))
        tierCounts133.forEach { (label, count) ->
            result.append("%s: %d (%.2f%%)\n".format(label, count, count.toDouble() / spreads133.size * 100.0))
        }
        result.append("Tiering is a descriptive grouping of scenario band sensitivity, not predictive, causal, or scientific validation.\n")
    }



    private fun appendStep134RobustnessIndex(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 134: Descriptive Robustness Index ---\n")
        val gaps134 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps134.isEmpty()) {
            result.append("Robustness index unavailable: no valid observations.\n")
            return
        }
        val maxGap134 = gaps134.maxOrNull() ?: 0.0
        val normalized134 = gaps134.map { if (maxGap134 > 0.0) it / maxGap134 * 100.0 else 0.0 }
        val shifts134 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band134(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift)
                .map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarioBands134 = shifts134.map { shift -> normalized134.map { band134(it, shift) } }
        val indexValues134 = normalized134.indices.map { idx ->
            val bands = scenarioBands134.map { it[idx] }
            val spread = (bands.maxOrNull() ?: 0) - (bands.minOrNull() ?: 0)
            (100.0 - spread * 25.0).coerceIn(0.0, 100.0)
        }
        val distribution134 = indexValues134.groupingBy { it.toInt() }.eachCount().toSortedMap()
        result.append("Observations scored: %d\n".format(indexValues134.size))
        result.append("Mean descriptive robustness index: %.2f / 100\n".format(indexValues134.average()))
        result.append("Minimum index: %.2f\n".format(indexValues134.minOrNull() ?: 0.0))
        result.append("Maximum index: %.2f\n".format(indexValues134.maxOrNull() ?: 0.0))
        result.append("Index distribution (score : count): %s\n".format(distribution134.entries.joinToString(", ") { "${it.key}:${it.value}" }))
        result.append("Index is a descriptive encoding of band spread under the selected scenarios, not a predictive or scientifically validated score.\n")
    }



    private fun appendStep135RobustnessAudit(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 135: Robustness Audit Distribution ---\n")
        val gaps135 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }
        if (gaps135.isEmpty()) {
            result.append("Audit unavailable: no valid observations.\n")
            return
        }
        val maxGap135 = gaps135.maxOrNull() ?: 0.0
        val normalized135 = gaps135.map { if (maxGap135 > 0.0) it / maxGap135 * 100.0 else 0.0 }
        val shifts135 = listOf(0.0, 5.0, 10.0, 15.0, 20.0)
        fun band135(value: Double, shift: Double): Int {
            val boundaries = listOf(20.0 + shift, 40.0 + shift, 60.0 + shift, 80.0 + shift).map { it.coerceAtMost(100.0) }
            val found = boundaries.indexOfFirst { value <= it }
            return if (found < 0) 4 else found
        }
        val scenarioBands135 = shifts135.map { shift -> normalized135.map { band135(it, shift) } }
        val auditScores135 = normalized135.indices.map { idx ->
            val bands = scenarioBands135.map { it[idx] }
            val spread = (bands.maxOrNull() ?: 0) - (bands.minOrNull() ?: 0)
            (100.0 - spread * 25.0).coerceIn(0.0, 100.0)
        }
        val tiers135 = listOf("High consistency (100)" to { v: Double -> v == 100.0 },
            "Moderate consistency (75)" to { v: Double -> v == 75.0 },
            "Lower consistency (50 or less)" to { v: Double -> v <= 50.0 })
        result.append("Audit observations: %d\n".format(auditScores135.size))
        tiers135.forEach { (label, predicate) ->
            val count = auditScores135.count(predicate)
            result.append("%s: %d (%.2f%%)\n".format(label, count, count.toDouble() / auditScores135.size * 100.0))
        }
        result.append("Audit summarizes score consistency under the selected boundary-shift scenarios; it is descriptive and not predictive or validation evidence.\n")
    }


    private fun appendStep136GapQuantileProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 136: Gap Quantile Profile ---\n")
        val values136 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }.sorted()
        if (values136.isEmpty()) {
            result.append("Quantile profile unavailable: no valid observations.\n")
            return
        }
        fun quantile136(p: Double): Double {
            val pos = (values136.size - 1) * p
            val lower = pos.toInt()
            val upper = kotlin.math.ceil(pos).toInt()
            if (lower == upper) return values136[lower]
            val fraction = pos - lower
            return values136[lower] * (1.0 - fraction) + values136[upper] * fraction
        }
        result.append("Valid observations: %d\n".format(values136.size))
        result.append("Minimum gap: %.2f\n".format(values136.first()))
        result.append("Q1 (25th percentile): %.2f\n".format(quantile136(0.25)))
        result.append("Median (50th percentile): %.2f\n".format(quantile136(0.50)))
        result.append("Q3 (75th percentile): %.2f\n".format(quantile136(0.75)))
        result.append("Maximum gap: %.2f\n".format(values136.last()))
        result.append("Interquartile range (Q3-Q1): %.2f\n".format(quantile136(0.75) - quantile136(0.25)))
        result.append("Quantiles describe the distribution of absolute score gaps; they do not imply significance, prediction, or causal interpretation.\n")
    }

    private fun appendStep137GapIqrOutlierSummary(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 137: Gap IQR Outlier Summary ---\n")
        val gaps137 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb).toDouble() else null
        }.sorted()
        if (gaps137.isEmpty()) {
            result.append("IQR outlier summary unavailable: no valid observations.\n")
            return
        }
        fun quantile137(p: Double): Double {
            val pos = (gaps137.size - 1) * p
            val lower = pos.toInt()
            val upper = kotlin.math.ceil(pos).toInt()
            if (lower == upper) return gaps137[lower]
            val fraction = pos - lower
            return gaps137[lower] * (1.0 - fraction) + gaps137[upper] * fraction
        }
        val q1 = quantile137(0.25)
        val q3 = quantile137(0.75)
        val iqr = q3 - q1
        val lowerFence = q1 - 1.5 * iqr
        val upperFence = q3 + 1.5 * iqr
        val lowerCount = gaps137.count { it < lowerFence }
        val upperCount = gaps137.count { it > upperFence }
        result.append("Valid observations: %d\n".format(gaps137.size))
        result.append("Q1: %.2f | Q3: %.2f | IQR: %.2f\n".format(q1, q3, iqr))
        result.append("Lower Tukey fence: %.2f\n".format(lowerFence))
        result.append("Upper Tukey fence: %.2f\n".format(upperFence))
        result.append("Below lower fence: %d\n".format(lowerCount))
        result.append("Above upper fence: %d\n".format(upperCount))
        result.append("IQR-flagged observations: %d (%.2f%%)\n".format(lowerCount + upperCount, (lowerCount + upperCount) * 100.0 / gaps137.size))
        result.append("Tukey fences are descriptive flags for unusually distant gap values, not evidence of error, significance, or prediction.\n")
    }

    private fun appendStep138GapOutlierDetail(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 138: Gap Outlier Detail ---\n")
        val observations138 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) Triple(a, b, kotlin.math.abs(sa - sb).toDouble()) else null
        }.sortedBy { it.third }
        if (observations138.isEmpty()) {
            result.append("Outlier detail unavailable: no valid observations.\n")
            return
        }
        val gaps = observations138.map { it.third }
        fun quantile138(p: Double): Double {
            val pos = (gaps.size - 1) * p
            val lower = pos.toInt()
            val upper = kotlin.math.ceil(pos).toInt()
            if (lower == upper) return gaps[lower]
            val fraction = pos - lower
            return gaps[lower] * (1.0 - fraction) + gaps[upper] * fraction
        }
        val q1 = quantile138(0.25)
        val q3 = quantile138(0.75)
        val upperFence = q3 + 1.5 * (q3 - q1)
        val flagged = observations138.filter { it.third > upperFence }
        result.append("Valid edges: %d\n".format(observations138.size))
        result.append("Upper fence: %.2f\n".format(upperFence))
        result.append("High-gap flagged edges: %d\n".format(flagged.size))
        if (flagged.isEmpty()) {
            result.append("No edges exceeded the upper Tukey fence.\n")
        } else {
            flagged.forEach { (a, b, gap) ->
                result.append("%s ↔ %s | gap %.2f\n".format(a, b, gap))
            }
        }
        result.append("These are descriptive high-gap flags only; they are not predictions or evidence of astrological effect.\n")
    }


    private fun appendStep139GapBandFrequency(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 139: Gap Band Frequency Profile ---\n")
        val gaps139 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps139.isEmpty()) {
            result.append("Gap-band frequency unavailable: no valid observations.\n")
            return
        }
        val bands139 = listOf("0–5", "6–10", "11–15", "16–20", "21+")
        val counts139 = listOf(
            gaps139.count { it in 0..5 },
            gaps139.count { it in 6..10 },
            gaps139.count { it in 11..15 },
            gaps139.count { it in 16..20 },
            gaps139.count { it >= 21 }
        )
        result.append("Valid observations: %d\n".format(gaps139.size))
        bands139.forEachIndexed { index, band ->
            val count = counts139[index]
            result.append("Gap %s: %d (%.2f%%)\n".format(band, count, count * 100.0 / gaps139.size))
        }
        result.append("Bands summarize absolute score-gap frequency descriptively; they do not imply significance or prediction.\n")
    }

    private fun appendStep140GapBandConcentration(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 140: Gap Band Concentration Summary ---\n")
        val gaps140 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps140.isEmpty()) {
            result.append("Gap-band concentration unavailable: no valid observations.\n")
            return
        }
        val bands140 = listOf("0–5", "6–10", "11–15", "16–20", "21+")
        val counts140 = listOf(
            gaps140.count { it in 0..5 },
            gaps140.count { it in 6..10 },
            gaps140.count { it in 11..15 },
            gaps140.count { it in 16..20 },
            gaps140.count { it >= 21 }
        )
        val total140 = counts140.sum()
        val nonZero140 = counts140.count { it > 0 }
        val largest140 = counts140.maxOrNull() ?: 0
        val concentration140 = if (total140 > 0) largest140 * 100.0 / total140 else 0.0
        val hhi140 = if (total140 > 0) counts140.sumOf { count ->
            val share = count.toDouble() / total140
            share * share
        } else 0.0
        result.append("Valid observations: %d\n".format(total140))
        result.append("Occupied gap bands: %d of %d\n".format(nonZero140, bands140.size))
        result.append("Largest band share: %.2f%%\n".format(concentration140))
        result.append("Band concentration (HHI, 0–1): %.4f\n".format(hhi140))
        bands140.forEachIndexed { index, band ->
            result.append("%s: %d (%.2f%%)\n".format(band, counts140[index], counts140[index] * 100.0 / total140))
        }
        result.append("Concentration statistics describe the distribution across defined gap bands only; they are not inferential, predictive, or evidence of an astrological effect.\n")
    }


    private fun appendStep141GapBandEntropy(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 141: Gap Band Entropy Profile ---\n")
        val gaps141 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps141.isEmpty()) {
            result.append("Gap-band entropy unavailable: no valid observations.\n")
            return
        }
        val bands141 = listOf("0–5", "6–10", "11–15", "16–20", "21+")
        val counts141 = listOf(
            gaps141.count { it in 0..5 },
            gaps141.count { it in 6..10 },
            gaps141.count { it in 11..15 },
            gaps141.count { it in 16..20 },
            gaps141.count { it >= 21 }
        )
        val total141 = counts141.sum()
        val probabilities141 = counts141.filter { it > 0 }.map { it.toDouble() / total141 }
        val entropy141 = probabilities141.sumOf { probability -> -probability * kotlin.math.ln(probability) }
        val maxEntropy141 = kotlin.math.ln(bands141.size.toDouble())
        val normalized141 = if (maxEntropy141 > 0.0) entropy141 / maxEntropy141 else 0.0
        result.append("Valid observations: %d\n".format(total141))
        result.append("Occupied bands: %d of %d\n".format(probabilities141.size, bands141.size))
        result.append("Entropy (natural-log units): %.4f\n".format(entropy141))
        result.append("Normalized entropy (0–1): %.4f\n".format(normalized141))
        bands141.forEachIndexed { index, band ->
            result.append("%s: %d (%.2f%%)\n".format(band, counts141[index], counts141[index] * 100.0 / total141))
        }
        result.append("Entropy summarizes spread across defined gap bands only; it is descriptive and not predictive or evidence of astrological effect.\n")
    }


    private fun appendStep142GapBandCumulativeProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 142: Gap Band Cumulative Profile ---\n")
        val gaps142 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps142.isEmpty()) {
            result.append("Cumulative gap profile unavailable: no valid observations.\n")
            return
        }
        val total142 = gaps142.size
        val thresholds142 = listOf(5, 10, 15, 20)
        result.append("Valid observations: %d\n".format(total142))
        thresholds142.forEach { threshold ->
            val count142 = gaps142.count { it <= threshold }
            val pct142 = count142 * 100.0 / total142
            result.append("Gap <= %d: %d (%.2f%%)\n".format(threshold, count142, pct142))
        }
        val above20Count142 = gaps142.count { it > 20 }
        result.append("Gap > 20: %d (%.2f%%)\n".format(above20Count142, above20Count142 * 100.0 / total142))
        result.append("Cumulative proportions summarize observed absolute gaps only; they are descriptive and not predictive or evidence of an astrological effect.\n")
    }


    private fun appendStep143ExtendedGapThresholdProfile(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 143: Extended Gap Threshold Profile ---\n")
        val gaps143 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps143.isEmpty()) {
            result.append("Extended threshold profile unavailable: no valid observations.\n")
            return
        }
        val total143 = gaps143.size
        val thresholds143 = listOf(25, 30, 35, 40, 45, 50)
        result.append("Valid observations: %d\n".format(total143))
        thresholds143.forEach { threshold ->
            val count143 = gaps143.count { it <= threshold }
            val pct143 = count143 * 100.0 / total143
            result.append("Gap <= %d: %d (%.2f%%)\n".format(threshold, count143, pct143))
        }
        val above50Count143 = gaps143.count { it > 50 }
        result.append("Gap > 50: %d (%.2f%%)\n".format(above50Count143, above50Count143 * 100.0 / total143))
        result.append("Extended thresholds describe observed absolute gap coverage only; they do not establish prediction, causation, or astrological validity.\n")
    }



    private fun appendStep144GapIntervalDistribution(
        result: StringBuilder,
        step50CoreNumbers: Map<String, Int>,
        step67CrossShellEdges: List<Pair<String, String>>
    ) {
        result.append("\n--- Step 144: Gap Interval Distribution ---\n")
        val gaps144 = step67CrossShellEdges.mapNotNull { (a, b) ->
            val sa = step50CoreNumbers[a]
            val sb = step50CoreNumbers[b]
            if (sa != null && sb != null) kotlin.math.abs(sa - sb) else null
        }
        if (gaps144.isEmpty()) {
            result.append("Gap interval distribution unavailable: no valid observations.\n")
            return
        }
        val total144 = gaps144.size
        val intervals144 = listOf(
            Pair("0-10", 0..10),
            Pair("11-20", 11..20),
            Pair("21-30", 21..30),
            Pair("31-40", 31..40),
            Pair("41-50", 41..50),
            Pair("51+", 51..Int.MAX_VALUE)
        )
        result.append("Valid observations: %d\n".format(total144))
        intervals144.forEach { (label, range) ->
            val count144 = gaps144.count { it in range }
            result.append("Gap %s: %d (%.2f%%)\n".format(label, count144, count144 * 100.0 / total144))
        }
        result.append("Interval counts partition observed absolute gaps; they are descriptive and not predictive or evidence of an astrological effect.\n")
    }

}
