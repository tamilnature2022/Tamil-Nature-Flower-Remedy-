package com.tamilnature.ephemeratest

import org.junit.Test
import org.junit.Assert.assertEquals
import java.time.LocalDate

class VimshottariCycleTest {

    @Test
    fun fullMahadashaCycleTotals113AfterBalance() {

        val result = VimshottariDasha.calculateMahadasha(
            LocalDate.of(2000, 1, 1),
            0.0
        )

        val fullPeriods = result.drop(1)

        val totalYears = fullPeriods.sumOf { it.years }

        assertEquals(113.0, totalYears, 0.000001)
    }
}
