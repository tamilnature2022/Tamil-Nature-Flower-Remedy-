
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
            0.0
        )

        val expected = listOf(
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

        assertEquals(expected, result.map { it.planet })
    }

    @Test
    fun fullMahadashaYearsAreCorrect() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            0.0
        )

        val expectedYears = listOf(
            20.0,
            6.0,
            10.0,
            7.0,
            18.0,
            16.0,
            19.0,
            17.0
        )

        for (i in 1 until result.size) {
            assertEquals(
                expectedYears[i - 1],
                result[i].years,
                0.000001
            )
        }
    }

    @Test
    fun dashaSequenceContainsNinePeriods() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            0.0
        )

        assertEquals(9, result.size)
    }

    @Test
    fun nakshatraBoundaryChangesMahadashaLord() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            13.333334
        )

        assertEquals("சுக்கிரன்", result.first().planet)
    }

    @Test
    fun totalFullMahadashaYearsEqual120() {

        val expectedTotal = 120.0

        val years = listOf(
            7.0, 20.0, 6.0, 10.0, 7.0,
            18.0, 16.0, 19.0, 17.0
        )

        val actualTotal = years.sum()

        assertEquals(
            expectedTotal,
            actualTotal,
            0.000001
        )
    }

    @Test
    fun mahadashaBalanceAtHalfNakshatraIsCorrect() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )

        assertEquals("கேது", result.first().planet)

        assertEquals(
            3.5,
            result.first().years,
            0.000001
        )
    }

    @Test
    fun mahadashaBalanceEndDateIsCorrect() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        )

        assertEquals(
            LocalDate.of(2003, 7, 2),
            result.first().endDate
        )
    }

    @Test
    fun antardashaSequenceContainsNinePeriods() {

        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(9, result.size)

        val expected = listOf(
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

        assertEquals(
            expected,
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

        assertEquals(
            LocalDate.of(2000, 3, 15),
            result.first().endDate
        )
    }

    @Test
    fun lastAntardashaEndsWithMahadasha() {

        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        assertEquals(
            md.endDate,
            result.last().endDate
        )
    }

    @Test
    fun firstAntardashaDurationMatchesVimshottariProportion() {

        val md = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            6.6666666667
        ).first()

        val result = VimshottariDasha.calculateAntardasha(md)

        val mdDays = ChronoUnit.DAYS.between(
            md.startDate,
            md.endDate
        )

        val expectedDays = (mdDays * 7.0 / 120.0).toLong()

        val actualDays = ChronoUnit.DAYS.between(
            result.first().startDate,
            result.first().endDate
        )

        assertEquals(
            expectedDays,
            actualDays
        )
    }
}
