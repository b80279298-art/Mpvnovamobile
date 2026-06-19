package app.mpvnova.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleStyleSpecTest {
    @Test
    fun fullOpacityMapsToOpaqueAlphaFf() {
        // mpv uses standard alpha: 100% opacity -> alpha FF.
        assertEquals("#FFFFFFFF", mpvSubtitleColor(0xFFFFFF, 100))
    }

    @Test
    fun zeroOpacityMapsToTransparentAlphaZero() {
        assertEquals("#00000000", mpvSubtitleColor(0x000000, 0))
    }

    @Test
    fun halfOpacityRoundsToMidAlpha() {
        // 50% opacity -> alpha ~0x80 (128).
        assertEquals("#80FF453A", mpvSubtitleColor(0xFF453A, 50))
    }

    @Test
    fun opacityIsClampedIntoRange() {
        assertEquals("#FF0A84FF", mpvSubtitleColor(0x0A84FF, 150))
        assertEquals("#000A84FF", mpvSubtitleColor(0x0A84FF, -20))
    }

    @Test
    fun colorStringIgnoresHighBitsOfRgb() {
        // Callers may pass an ARGB int; only the low 24 bits are colour.
        assertEquals("#FF30D158", mpvSubtitleColor(0xFF30D158.toInt(), 100))
    }

    @Test
    fun defaultColorIdsResolveToKnownSwatches() {
        assertEquals("white", SUBTITLE_COLOR_OPTIONS[subtitleColorOptionIndex(SUBTITLE_TEXT_COLOR_DEFAULT_ID)].id)
        assertEquals("black", SUBTITLE_COLOR_OPTIONS[subtitleColorOptionIndex(SUBTITLE_BORDER_COLOR_DEFAULT_ID)].id)
    }

    @Test
    fun unknownColorIdFallsBackToFirstSwatch() {
        assertEquals(0, subtitleColorOptionIndex("not-a-real-color"))
    }

    @Test
    fun nearestOpacityIndexSnapsToTenPercentSteps() {
        assertEquals(0, nearestOpacityIndex(2))
        assertEquals(5, nearestOpacityIndex(48))
        assertEquals(10, nearestOpacityIndex(97))
    }
}
