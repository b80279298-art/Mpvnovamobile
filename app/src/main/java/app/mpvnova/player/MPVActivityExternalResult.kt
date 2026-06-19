package app.mpvnova.player

import android.content.Intent
import android.util.Log

internal const val EXTERNAL_END_BY_USER = "user"
internal const val EXTERNAL_END_BY_PLAYBACK_COMPLETION = "playback_completion"

internal fun MPVActivity.capturePlaybackResultSnapshot(updateCompletion: Boolean = false) {
    resultPositionMs = currentPlaybackPositionForResult().coerceAtLeast(0L)
    resultDurationMs = currentPlaybackDurationForResult().coerceAtLeast(0L)
    if (updateCompletion)
        playbackCompletionReached = isPlaybackCompleteForResult(resultPositionMs, resultDurationMs)
}

internal fun isPlaybackCompleteForResult(positionMs: Long, durationMs: Long): Boolean {
    return durationMs > 0L && positionMs >= durationMs - RESUME_NEAR_END_MS
}

internal fun MPVActivity.shouldFinishExternalPlaybackOnEndFile(): Boolean {
    val externalLaunch = intent.getBooleanExtra(EXTRA_EXTERNAL_PLAYER_RESULT, false)
    val loopFile = mpvGetPropertyString("loop-file")
    val loopPlaylist = mpvGetPropertyString("loop-playlist")
    val loopEnabled = loopFile.isEnabledLoopMode() || loopPlaylist.isEnabledLoopMode()
    val playlistCount = mpvGetPropertyInt("playlist-count") ?: psc.playlistCount
    val playlistPos = mpvGetPropertyInt("playlist-pos") ?: psc.playlistPos
    val finalPlaylistItem = playlistCount <= 1 || playlistPos >= playlistCount - 1
    return externalLaunch && !loopEnabled && finalPlaylistItem
}

internal data class ExternalResultPositionDuration(val positionMs: Long, val durationMs: Long)

// Build the position/duration we report back, mirroring is.xyz.mpv-android's native contract
// so every launcher reads us the way it expects:
//  - completion (EOF): position 0 and duration 0. Stremio only marks an item watched when
//    the result is RESULT_OK with position==0 && duration==0 (see ExternalPlayerContract in
//    its apk); Nuvio/MX key completion off end_by="playback_completion". Zeroing satisfies all.
//  - mid-playback stop: the real position/duration so launchers can save resume progress, but
//    positions under the resume threshold collapse to 0 so we don't persist a trivial resume.
internal fun externalResultPositionDuration(
    callerPackage: String?,
    endBy: String,
    rawPositionMs: Long,
    rawDurationMs: Long,
): ExternalResultPositionDuration {
    // Only Stremio's mpv contract wants completion as RESULT_OK + position 0 + duration 0.
    // Others (Nuvio) read position/duration as progress and need the real near-end values to
    // detect completion / auto-next — zeroing them makes Nuvio report "no progress data".
    if (callerPackage == STREMIO_PACKAGE && endBy == EXTERNAL_END_BY_PLAYBACK_COMPLETION)
        return ExternalResultPositionDuration(0L, 0L)
    val position = rawPositionMs.coerceAtLeast(0L)
    return ExternalResultPositionDuration(
        positionMs = if (position >= RESUME_MIN_POSITION_MS) position else 0L,
        durationMs = rawDurationMs.coerceAtLeast(0L),
    )
}

internal fun MPVActivity.buildExternalPlaybackResultIntent(endBy: String): Intent {
    val (safePosition, safeDuration) = externalResultPositionDuration(
        callerPackage = intent.getStringExtra(EXTRA_EXTERNAL_CALLER_PACKAGE),
        endBy = endBy,
        rawPositionMs = externalResultPosition(),
        rawDurationMs = externalResultDuration(),
    )
    Log.v(
        MPV_ACTIVITY_TAG,
        "external-result: end_by=$endBy position=$safePosition duration=$safeDuration"
    )
    return Intent(RESULT_INTENT).apply {
        data = if (intent.data?.scheme == "file") null else intent.data
        putExtra("position", safePosition.toInt())
        putExtra("duration", safeDuration.toInt())
        putExtra("extra_position", safePosition)
        putExtra("extra_duration", safeDuration)
        intent.data?.takeUnless { it.scheme == "file" }?.let {
            putExtra("extra_uri", it.toString())
        }
        putExtra("end_by", endBy)
        putExtra("return_result", true)
    }
}

private fun String?.isEnabledLoopMode(): Boolean {
    return this != null && this != "no" && this != "0" && this != "false"
}

private fun MPVActivity.externalResultPosition(): Long {
    return resultPositionMs.takeIf { it >= 0L } ?: psc.position
}

private fun MPVActivity.externalResultDuration(): Long {
    return resultDurationMs.takeIf { it > 0L } ?: psc.duration
}

private fun MPVActivity.currentPlaybackPositionForResult(): Long {
    return mpvGetPropertyDouble("time-pos/full")
        ?.times(MPV_MILLIS_PER_SECOND_DOUBLE)
        ?.toLong()
        ?: psc.position
}

private fun MPVActivity.currentPlaybackDurationForResult(): Long {
    return mpvGetPropertyDouble("duration/full")
        ?.times(MPV_MILLIS_PER_SECOND_DOUBLE)
        ?.toLong()
        ?: psc.duration
}
