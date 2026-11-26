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

        // Full Ketu Mahadasha: 7 years
        assertEquals("கேது", result[0].planet)
        assertEquals(7.0, result[0].years, 0.001)

        // Ketu ends on December 31, 2006
        assertEquals(
            LocalDate.of(2006, 12, 31),
            result[0].endDate
        )

        // Venus starts immediately after Ketu
        assertEquals("சுக்கிரன்", result[1].planet)
        assertEquals(
            result[0].endDate,
            result[1].startDate
        )

        // Venus Mahadasha: 20 years
        assertEquals(20.0, result[1].years, 0.001)

        assertEquals(
            LocalDate.of(2026, 12, 31),
            result[1].endDate
        )
    }
}
