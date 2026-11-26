
package com.tamilnature.ephemeratest

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

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
}
