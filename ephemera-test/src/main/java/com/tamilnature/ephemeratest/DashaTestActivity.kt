
package com.tamilnature.ephemeratest

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalDate

class DashaTestActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        scrollView.addView(layout)
        setContentView(scrollView)

        addText(layout, "VIMSHOTTARI DASHA TEST", 24)
        addText(layout, "TamilNature Astrology", 18)

        val birthDate = LocalDate.of(1984, 11, 19)

        // Moon longitude in degrees
        // Virgo 17°54'49"
        val moonLongitude =
            150.0 + 17.0 + 54.0 / 60.0 + 49.0 / 3600.0

        addText(layout, "Birth Date: $birthDate", 18)
        addText(layout, "Moon Longitude: $moonLongitude", 18)

        val mahadashas =
            VimshottariDasha.calculateMahadasha(
                birthDate,
                moonLongitude
            )

        addText(layout, "MAHADASHA", 22)

        mahadashas.forEach { dasha ->

            addText(
                layout,
                "${dasha.planet}\n" +
                "${dasha.startDate} → ${dasha.endDate}",
                18
            )

            val antardashas =
                VimshottariDasha.calculateAntardasha(dasha)

            antardashas.forEach { ad ->

                addText(
                    layout,
                    "   ${ad.antardashaLord}: " +
                    "${ad.startDate} → ${ad.endDate}",
                    16
                )
            }

            addText(layout, "--------------------", 14)
        }
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
