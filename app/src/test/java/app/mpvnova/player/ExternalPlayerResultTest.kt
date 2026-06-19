package app.mpvnova.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalPlayerResultTest {
    @Test
    fun playbackIsCompleteAtDuration() {
        assertTrue(isPlaybackCompleteForResult(positionMs = 600_000L, durationMs = 600_000L))
    }

    @Test
    fun playbackIsCompleteInsideNearEndWindow() {
        assertTrue(isPlaybackCompleteForResult(positionMs = 575_000L, durationMs = 600_000L))
    }

    @Test
    fun playbackIsNotCompleteWhenStreamEndsEarly() {
        assertFalse(isPlaybackCompleteForResult(positionMs = 600_000L, durationMs = 7_200_000L))
    }

    @Test
    fun playbackIsNotCompleteWithoutKnownDuration() {
        assertFalse(isPlaybackCompleteForResult(positionMs = 600_000L, durationMs = 0L))
    }

    // Stremio completion must report position 0 and duration 0 so its mpv-action contract
    // (RESULT_OK + pos==0 + dur==0 -> watched) marks the item watched and auto-nexts.
    @Test
    fun stremioCompletionReportsZeroPositionAndDuration() {
        val result = externalResultPositionDuration(
            callerPackage = STREMIO_PACKAGE,
            endBy = EXTERNAL_END_BY_PLAYBACK_COMPLETION,
            rawPositionMs = 1_415_000L,
            rawDurationMs = 1_440_000L,
        )
        assertEquals(0L, result.positionMs)
        assertEquals(0L, result.durationMs)
    }

    // Other launchers (Nuvio) read position/duration as progress; completion must report the
    // real near-end values, else they report "no progress data" and don't auto-next.
    @Test
    fun otherCallerCompletionReportsRealPositionAndDuration() {
        val result = externalResultPositionDuration(
            callerPackage = "com.nuvio.tv",
            endBy = EXTERNAL_END_BY_PLAYBACK_COMPLETION,
            rawPositionMs = 1_415_000L,
            rawDurationMs = 1_440_000L,
        )
        assertEquals(1_415_000L, result.positionMs)
        assertEquals(1_440_000L, result.durationMs)
    }

    // A mid-playback stop past the resume threshold reports the real position so launchers
    // can save resume progress.
    @Test
    fun userStopReportsRealPositionAndDuration() {
        val result = externalResultPositionDuration(
            callerPackage = STREMIO_PACKAGE,
            endBy = EXTERNAL_END_BY_USER,
            rawPositionMs = 600_000L,
            rawDurationMs = 1_440_000L,
        )
        assertEquals(600_000L, result.positionMs)
        assertEquals(1_440_000L, result.durationMs)
    }

    // A stop under the resume threshold collapses position to 0 (no trivially-early resume),
    // but still reports duration.
    @Test
    fun userStopBelowResumeThresholdReportsZeroPosition() {
        val result = externalResultPositionDuration(
            callerPackage = "com.nuvio.tv",
            endBy = EXTERNAL_END_BY_USER,
            rawPositionMs = 30_000L,
            rawDurationMs = 1_440_000L,
        )
        assertEquals(0L, result.positionMs)
        assertEquals(1_440_000L, result.durationMs)
    }
}
