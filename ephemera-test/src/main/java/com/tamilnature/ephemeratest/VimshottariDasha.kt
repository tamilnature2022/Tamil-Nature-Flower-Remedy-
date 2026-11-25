
package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate
import java.util.Locale

class DashaTestActivity : Activity() {

    private val nakshatraNames = listOf(
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

    private val dashaLords = listOf(
        "கேது", "சுக்கிரன்", "சூரியன்",
        "சந்திரன்", "செவ்வாய்", "ராகு",
        "குரு", "சனி", "புதன்"
    )

    private val dashaYears = listOf(
        7.0, 20.0, 6.0, 10.0, 7.0,
        18.0, 16.0, 19.0, 17.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)

        addText(layout, "VIMSHOTTARI DASHA DIAGNOSTIC", 24)
        addText(layout, "TamilNature Astrology", 18)

        val birthDate = LocalDate.of(1984, 11, 19)

        val moonLongitude = 167.91361111111112

        val nakshatraSize = 360.0 / 27.0

        val nakshatraIndex =
            (moonLongitude / nakshatraSize).toInt()

        val nakshatraStart =
            nakshatraIndex * nakshatraSize

        val travelled =
            moonLongitude - nakshatraStart

        val travelledFraction =
            travelled / nakshatraSize

        val remainingFraction =
            (nakshatraSize - travelled) / nakshatraSize

        val birthLordIndex =
            nakshatraIndex % 9

        val birthLord =
            dashaLords[birthLordIndex]

        val fullDashaYears =
            dashaYears[birthLordIndex]

        val balanceYears =
            fullDashaYears * remainingFraction

        val balanceDays =
            balanceYears * 365.25

        val calculatedBalanceDate =
            birthDate.plusDays(balanceDays.toLong())

        addText(layout, "BIRTH DETAILS", 22)
        addText(layout, "Birth Date: $birthDate", 18)
        addText(layout, "Moon Longitude: ${String.format(Locale.US, "%.10f", moonLongitude)}°", 18)

        addText(layout, "DIAGNOSTIC CALCULATION", 22)

        addText(
            layout,
            "Nakshatra Index: $nakshatraIndex",
            18
        )

        addText(
            layout,
            "Nakshatra: ${nakshatraNames[nakshatraIndex]}",
            18
        )

        addText(
            layout,
            "Nakshatra Start: ${String.format(Locale.US, "%.10f", nakshatraStart)}°",
            18
        )

        addText(
            layout,
            "Travelled Degrees: ${String.format(Locale.US, "%.10f", travelled)}°",
            18
        )

        addText(
            layout,
            "Travelled Fraction: ${String.format(Locale.US, "%.10f", travelledFraction)}",
            18
        )

        addText(
            layout,
            "Remaining Fraction: ${String.format(Locale.US, "%.10f", remainingFraction)}",
            18
        )

        addText(layout, "DASHA BALANCE", 22)

        addText(
            layout,
            "Dasha Lord: $birthLord",
            18
        )

        addText(
            layout,
            "Full Dasha Years: $fullDashaYears",
            18
        )

        addText(
            layout,
            "Balance Years: ${String.format(Locale.US, "%.10f", balanceYears)}",
            18
        )

        addText(
            layout,
            "Balance Days: ${String.format(Locale.US, "%.10f", balanceDays)}",
            18
        )

        addText(
            layout,
            "Balance Days (Integer): ${balanceDays.toLong()}",
            18
        )

        addText(
            layout,
            "Calculated Balance End Date: $calculatedBalanceDate",
            20
        )

        addText(layout, "CURRENT ENGINE OUTPUT", 22)

        val mahadashas =
            VimshottariDasha.calculateMahadasha(
                birthDate,
                moonLongitude
            )

        mahadashas.forEach { dasha ->

            addText(
                layout,
                "${dasha.planet}\n${dasha.startDate} → ${dasha.endDate}",
                18
            )
        }

        addText(layout, "DIAGNOSTIC COMPLETE", 22)
    }

    private fun addText(
        layout: LinearLayout,
        text: String,
        size: Int
    ) {
        val view = TextView(this).apply {
            this.text = text
            textSize = size.toFloat()
            setTextColor(Color.BLACK)
            setPadding(0, 12, 0, 12)
            gravity = Gravity.START
        }

        layout.addView(view)
    }
}
