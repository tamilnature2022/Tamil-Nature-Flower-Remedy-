
package com.tamilnature.ephemeratest

import java.time.LocalDate

object VimshottariDasha {

    data class Mahadasha(
        val planet: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val years: Double
    )

    data class Antardasha(
        val antardashaLord: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    private val lords = listOf(
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
        7.0,
        20.0,
        6.0,
        10.0,
        7.0,
        18.0,
        16.0,
        19.0,
        17.0
    )

    private const val TOTAL_YEARS = 120.0
    private const val DAYS_PER_YEAR = 365.25

    private fun indexOfLord(planet: String): Int {
        return lords.indexOf(planet)
    }

    private fun dateFromDays(
        start: LocalDate,
        days: Double
    ): LocalDate {
        return start.plusDays(days.toLong())
    }

    fun calculateMahadasha(
        birthDate: LocalDate,
        moonLongitude: Double
    ): List<Mahadasha> {

        val normalized =
            ((moonLongitude % 360.0) + 360.0) % 360.0

        val nakshatraSize = 360.0 / 27.0

        val nakshatraIndex =
            (normalized / nakshatraSize).toInt()
                .coerceIn(0, 26)

        val nakshatraStart =
            nakshatraIndex * nakshatraSize

        val travelled =
            normalized - nakshatraStart

        val remainingFraction =
            (nakshatraSize - travelled) / nakshatraSize

        val birthLordIndex =
            nakshatraIndex % 9

        val balanceYears =
            years[birthLordIndex] * remainingFraction

        val balanceDays =
            balanceYears * DAYS_PER_YEAR

        val firstEndDate =
            dateFromDays(birthDate, balanceDays)

        val result = mutableListOf<Mahadasha>()

        result.add(
            Mahadasha(
                planet = lords[birthLordIndex],
                startDate = birthDate,
                endDate = firstEndDate,
                years = balanceYears
            )
        )

        var currentDate = firstEndDate

        for (step in 1 until 9) {

            val index =
                (birthLordIndex + step) % 9

            val planetYears = years[index]

            val durationDays =
                planetYears * DAYS_PER_YEAR

            val endDate =
                dateFromDays(currentDate, durationDays)

            result.add(
                Mahadasha(
                    planet = lords[index],
                    startDate = currentDate,
                    endDate = endDate,
                    years = planetYears
                )
            )

            currentDate = endDate
        }

        return result
    }

    fun calculateAntardasha(
        mahadasha: Mahadasha
    ): List<Antardasha> {

        val mdIndex =
            indexOfLord(mahadasha.planet)

        if (mdIndex < 0) return emptyList()

        val totalDays =
            java.time.temporal.ChronoUnit.DAYS.between(
                mahadasha.startDate,
                mahadasha.endDate
            ).toDouble()

        val result = mutableListOf<Antardasha>()

        var currentDate = mahadasha.startDate

        for (step in 0 until 9) {

            val adIndex =
                (mdIndex + step) % 9

            val adLord =
                lords[adIndex]

            val proportion =
                years[adIndex] / TOTAL_YEARS

            val durationDays =
                totalDays * proportion

            val endDate =
                if (step == 8) {
                    mahadasha.endDate
                } else {
                    dateFromDays(currentDate, durationDays)
                }

            result.add(
                Antardasha(
                    antardashaLord = adLord,
                    startDate = currentDate,
                    endDate = endDate
                )
            )

            currentDate = endDate
        }

        return result
    }
}
