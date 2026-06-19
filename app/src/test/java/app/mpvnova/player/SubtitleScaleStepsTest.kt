package app.mpvnova.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleScaleStepsTest {
    @Test
    fun subScaleStepsUseRealFiveHundredthValues() {
        assertEquals(31, SUB_SCALE_STEPS.size)
        assertEquals(0.50, SUB_SCALE_STEPS.first(), 0.0001)
        assertEquals(0.55, SUB_SCALE_STEPS[1], 0.0001)
        assertEquals(1.00, SUB_SCALE_STEPS[DEFAULT_SUB_SCALE_INDEX], 0.0001)
        assertEquals(2.00, SUB_SCALE_STEPS.last(), 0.0001)

        for (index in 1 until SUB_SCALE_STEPS.size) {
            assertEquals(0.05, SUB_SCALE_STEPS[index] - SUB_SCALE_STEPS[index - 1], 0.0001)
        }
    }

    @Test
    fun legacySubScaleLevelsMigrateByScaleValue() {
        assertEquals(nearestSubScaleIndex(0.50), migrateSubScaleLevel(0, 1))
        assertEquals(nearestSubScaleIndex(0.65), migrateSubScaleLevel(1, 1))
        assertEquals(nearestSubScaleIndex(1.00), migrateSubScaleLevel(3, 1))
        assertEquals(nearestSubScaleIndex(1.75), migrateSubScaleLevel(7, 1))
    }
}
