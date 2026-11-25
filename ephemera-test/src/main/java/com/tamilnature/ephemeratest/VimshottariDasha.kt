
package com.tamilnature.ephemeratest

import java.time.LocalDate

object VimshottariDasha {

    private val planets = listOf(
        "கேது",
        "சுக்கிரன்",
        "சூரியன்",
        "சந்திரன்",
        "செவ்வாய்",
        "ராகு",
        "குரு",
        "சனி",
        "புதன்"
    )

    private val years = listOf(
        7.0, 20.0, 6.0, 10.0, 7.0,
        18.0, 16.0, 19.0, 17.0
    )

    data class DashaPeriod(
        val planet: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class AntardashaPeriod(
        val mahadashaLord: String,
        val antardashaLord: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    private const val NAKSHATRA_SIZE = 360.0 / 27.0
    private const val TOTAL_DASHA_YEARS = 120.0
    private const val YEAR_DAYS = 365.25

    private fun normalize(longitude: Double): Double {
        return ((longitude % 360.0) + 360.0) % 360.0
    }

    fun calculateMahadasha(
        birthDate: LocalDate,
        moonLongitude: Double
    ): List<DashaPeriod> {

        val longitude = normalize(moonLongitude)

        val nakshatraIndex =
            (longitude / NAKSHATRA_SIZE).toInt()

        val nakshatraStart =
            nakshatraIndex * NAKSHATRA_SIZE

        val travelled =
            longitude - nakshatraStart

        val remainingFraction =
            (NAKSHATRA_SIZE - travelled) / NAKSHATRA_SIZE

        val birthLordIndex = nakshatraIndex % 9

        val balanceYears =
            years[birthLordIndex] * remainingFraction

        val balanceDays =
            balanceYears * YEAR_DAYS

        val firstEndDate =
            birthDate.plusDays(balanceDays.toLong())

        val result = mutableListOf<DashaPeriod>()

        result.add(
            DashaPeriod(
                planet = planets[birthLordIndex],
                startDate = birthDate,
                endDate = firstEndDate
            )
        )

        var currentDate = firstEndDate

        for (i in 1 until 9) {

            val lordIndex =
                (birthLordIndex + i) % 9

            val durationDays =
                years[lordIndex] * YEAR_DAYS

            val endDate =
                currentDate.plusDays(durationDays.toLong())

            result.add(
                DashaPeriod(
                    planet = planets[lordIndex],
                    startDate = currentDate,
                    endDate = endDate
                )
            )

            currentDate = endDate
        }

        return result
    }

    fun calculateAntardasha(
        mahadasha: DashaPeriod
    ): List<AntardashaPeriod> {

        val mdIndex =
            planets.indexOf(mahadasha.planet)

        if (mdIndex < 0) return emptyList()

        val result = mutableListOf<AntardashaPeriod>()

        var currentDate = mahadasha.startDate

        for (i in 0 until 9) {

            val adIndex =
                (mdIndex + i) % 9

            val durationYears =
                years[mdIndex] *
                years[adIndex] /
                TOTAL_DASHA_YEARS

            val durationDays =
                durationYears * YEAR_DAYS

            val endDate =
                currentDate.plusDays(durationDays.toLong())

            result.add(
                AntardashaPeriod(
                    mahadashaLord = mahadasha.planet,
                    antardashaLord = planets[adIndex],
                    startDate = currentDate,
                    endDate = endDate
                )
            )

            currentDate = endDate
        }

        return result
    }
}
