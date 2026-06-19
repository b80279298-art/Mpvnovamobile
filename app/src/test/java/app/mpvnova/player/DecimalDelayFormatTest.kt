package app.mpvnova.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DecimalDelayFormatTest {

    @Test
    fun keepsCleanDecimalsClean() {
        assertEquals("0.1", formatDelayForField(0.1))
        assertEquals("0.3", formatDelayForField(0.3))
        assertEquals("0.05", formatDelayForField(0.05))
        assertEquals("-0.1", formatDelayForField(-0.1))
    }

    @Test
    fun stripsMpvFloatPrecisionNoise() {
        // What mpv returns for a sub-/audio-delay of 0.1 (stored as a 32-bit float).
        assertEquals("0.1", formatDelayForField(0.10000000149011612))
        assertEquals("-0.1", formatDelayForField(-0.10000000149011612))
    }

    @Test
    fun stripsStepAccumulationNoise() {
        // 0.1 + 0.1 + 0.1 in IEEE-754 doubles.
        assertEquals("0.3", formatDelayForField(0.1 + 0.1 + 0.1))
    }

    @Test
    fun dropsTrailingZerosAndDecimalPointForWholeValues() {
        assertEquals("0", formatDelayForField(0.0))
        assertEquals("2", formatDelayForField(2.0))
        assertEquals("600", formatDelayForField(600.0))
    }

    @Test
    fun roundsToTwoDecimals() {
        assertEquals("0.13", formatDelayForField(0.125))
        assertEquals("1.23", formatDelayForField(1.234))
    }

    @Test
    fun roundsTinyValuesToZeroWithoutNegativeSign() {
        assertEquals("0", formatDelayForField(-0.001))
        assertEquals("0", formatDelayForField(0.0001))
    }
}
