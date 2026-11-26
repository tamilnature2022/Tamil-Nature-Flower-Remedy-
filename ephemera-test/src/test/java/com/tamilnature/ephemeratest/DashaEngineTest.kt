package com.tamilnature.ephemeratest

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DashaEngineTest {

    @Test
    fun mahadashaSequenceIsCorrect() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )
        val expected = listOf(
            "கேது", "சுக்கிரன்", "சூரியன்", "சந்திரன்",
            "செவ்வாய்", "ராகு", "குரு", "சனி", "புதன்"
        )
        assertEquals(expected, result.map { it.planet })
    }

    @Test
    fun fixedMahadashaYearsAreCorrectAfterBalance() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )
        assertEquals(20.0, result[1].years, 0.001)
        assertEquals(6.0, result[2].years, 0.001)
        assertEquals(10.0, result[3].years, 0.001)
    }

    @Test
    fun mahadashaContainsNinePeriods() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )
        assertEquals(9, result.size)
    }

    @Test
    fun boundaryNakshatraLordIsCorrect() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            13.333334
        )
        assertEquals("சுக்கிரன்", result.first().planet)
    }

    @Test
    fun vimshottariYearsTotalIs120() {
        val years = listOf(
            7.0, 20.0, 6.0, 10.0, 7.0,
            18.0, 16.0, 19.0, 17.0
        )
        assertEquals(120.0, years.sum(), 0.001)
    }

    @Test
    fun halfNakshatraBalanceIsCorrect() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )
        assertEquals("கேது", result.first().planet)
        assertEquals(3.5, result.first().years, 0.01)
    }

    @Test
    fun balanceEndDateIsCorrect() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )
        assertEquals(
            LocalDate.of(2000, 1, 1).plusDays(1278),
            result.first().endDate
        )
    }

    @Test
    fun antardashaSequenceContainsNineLords() {
        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(9, result.size)
        assertEquals(
            listOf(
                "கேது", "சுக்கிரன்", "சூரியன்", "சந்திரன்",
                "செவ்வாய்", "ராகு", "குரு", "சனி", "புதன்"
            ),
            result.map { it.antardashaLord }
        )
    }

    @Test
    fun firstAntardashaEndDateIsCorrect() {
        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(md.startDate, result.first().startDate)
        assertTrue(result.first().endDate.isAfter(md.startDate))
    }

    @Test
    fun lastAntardashaEndsWithMahadasha() {
        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(md.endDate, result.last().endDate)
    }

    @Test
    fun firstAntardashaDurationMatchesVimshottariProportion() {
        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        val mdDays = ChronoUnit.DAYS.between(
            md.startDate, md.endDate
        )

        val expectedDays = (mdDays * 7.0 / 120.0).toLong()

        val actualDays = ChronoUnit.DAYS.between(
            result.first().startDate,
            result.first().endDate
        )

        assertEquals(expectedDays, actualDays)
    }

    @Test
    fun antardashaDatesHaveNoGapOrOverlap() {
        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(9, result.size)

        for (i in 0 until result.size - 1) {
            assertEquals(
                result[i].endDate,
                result[i + 1].startDate
            )
        }
    }

    @Test
    fun mahadashaDatesHaveNoGapOrOverlap() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )

        assertEquals(9, result.size)

        for (i in 0 until result.size - 1) {
            assertEquals(
                result[i].endDate,
                result[i + 1].startDate
            )
        }
    }

    @Test
    fun zeroLongitudeGivesFullKetuBalance() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            0.0
        )

        assertEquals("கேது", result.first().planet)
        assertEquals(7.0, result.first().years, 0.001)
    }

    @Test
    fun nearNakshatraEndBalanceIsVerySmall() {
        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            13.333333
        )

        assertEquals("கேது", result.first().planet)
        assertTrue(result.first().years >= 0.0)
        assertTrue(result.first().years < 0.001)
    }

    @Test
    fun fullKetuMahadashaReferenceDatesAreCorrect() {
        val birthDate = LocalDate.of(2000, 1, 1)

        val result = VimshottariDasha.calculateMahadasha(
            birthDate,
            0.0
        )

        assertEquals("கேது", result[0].planet)
        assertEquals(7.0, result[0].years, 0.001)

        assertEquals(
            LocalDate.of(2006, 12, 31),
            result[0].endDate
        )

        assertEquals("சுக்கிரன்", result[1].planet)
        assertEquals(
            result[0].endDate,
            result[1].startDate
        )

        assertEquals(20.0, result[1].years, 0.001)

        assertEquals(
            LocalDate.of(2026, 12, 31),
            result[1].endDate
        )
    }

    @Test
    fun all27NakshatrasMapToCorrectMahadashaLords() {

        val expectedLords = listOf(
            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்",

            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்",

            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்"
        )

        val nakshatraSize = 360.0 / 27.0

        for (i in 0 until 27) {

            val midpointLongitude =
                (i + 0.5) * nakshatraSize

            val result = VimshottariDasha.calculateMahadasha(
                LocalDate.of(2000, 1, 1),
                midpointLongitude
            )

            assertEquals(
                "Nakshatra index $i mapping failed",
                expectedLords[i],
                result.first().planet
            )
        }
    }

    @Test
    fun all26NakshatraBoundariesSwitchToCorrectLord() {

        val expectedLords = listOf(
            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்",

            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்",

            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்"
        )

        val nakshatraSize = 360.0 / 27.0
        val epsilon = 0.00000001

        for (boundary in 1 until 27) {

            val longitude =
                boundary * nakshatraSize

            val beforeLongitude =
                longitude - epsilon

            val afterLongitude =
                longitude + epsilon

            val beforeResult =
                VimshottariDasha.calculateMahadasha(
                    LocalDate.of(2000, 1, 1),
                    beforeLongitude
                )

            val afterResult =
                VimshottariDasha.calculateMahadasha(
                    LocalDate.of(2000, 1, 1),
                    afterLongitude
                )

            val expectedBefore =
                expectedLords[boundary - 1]

            val expectedAfter =
                expectedLords[boundary]

            assertEquals(
                "Boundary $boundary: lord before boundary incorrect",
                expectedBefore,
                beforeResult.first().planet
            )

            assertEquals(
                "Boundary $boundary: lord after boundary incorrect",
                expectedAfter,
                afterResult.first().planet
            )
        }
    }

    @Test
    fun nakshatraBalanceProgressionValidation() {

        val nakshatraSize = 360.0 / 27.0
        val birthDate = LocalDate.of(2000, 1, 1)

        val travelFractions = listOf(
            0.0,
            0.25,
            0.50,
            0.75,
            0.99
        )

        val balances = travelFractions.map { fraction ->

            val longitude =
                fraction * nakshatraSize

            val result =
                VimshottariDasha.calculateMahadasha(
                    birthDate,
                    longitude
                )

            assertEquals(
                "Starting lord should remain Ketu",
                "கேது",
                result.first().planet
            )

            result.first().years
        }

        assertEquals(7.0, balances[0], 0.001)
        assertEquals(5.25, balances[1], 0.01)
        assertEquals(3.5, balances[2], 0.01)
        assertEquals(1.75, balances[3], 0.01)

        assertTrue(balances[4] < 0.1)

        for (i in 0 until balances.size - 1) {

            assertTrue(
                "Balance should decrease as Moon longitude advances. " +
                    "Index $i: ${balances[i]} -> ${balances[i + 1]}",
                balances[i] > balances[i + 1]
            )
        }
    }

    @Test
    fun all9PlanetMahadashaBalanceProgressionValidation() {

        val nakshatraSize = 360.0 / 27.0
        val birthDate = LocalDate.of(2000, 1, 1)

        val planetYears = listOf(
            7.0, 20.0, 6.0, 10.0, 7.0,
            18.0, 16.0, 19.0, 17.0
        )

        val expectedLords = listOf(
            "கேது", "சுக்கிரன்", "சூரியன்",
            "சந்திரன்", "செவ்வாய்", "ராகு",
            "குரு", "சனி", "புதன்"
        )

        val travelFractions = listOf(
            0.0,
            0.25,
            0.50,
            0.75,
            0.99
        )

        for (planetIndex in 0 until 9) {

            val nakshatraIndex = planetIndex

            val nakshatraStart =
                nakshatraIndex * nakshatraSize

            val balances = travelFractions.map { fraction ->

                val longitude =
                    nakshatraStart +
                    (fraction * nakshatraSize)

                val result =
                    VimshottariDasha.calculateMahadasha(
                        birthDate,
                        longitude
                    )

                assertEquals(
                    "Planet mapping failed at index $planetIndex",
                    expectedLords[planetIndex],
                    result.first().planet
                )

                result.first().years
            }

            val fullYears = planetYears[planetIndex]

            assertEquals(
                fullYears,
                balances[0],
                0.01
            )

            assertEquals(
                fullYears * 0.75,
                balances[1],
                0.02
            )

            assertEquals(
                fullYears * 0.50,
                balances[2],
                0.02
            )

            assertEquals(
                fullYears * 0.25,
                balances[3],
                0.02
            )

            assertTrue(
                "99% travel balance should be small for " +
                    expectedLords[planetIndex],
                balances[4] < fullYears * 0.02
            )

            for (i in 0 until balances.size - 1) {

                assertTrue(
                    "Balance must decrease for " +
                        expectedLords[planetIndex] + ": " +
                        "${balances[i]} -> ${balances[i + 1]}",
                    balances[i] > balances[i + 1]
                )
            }
        }
    }
}
