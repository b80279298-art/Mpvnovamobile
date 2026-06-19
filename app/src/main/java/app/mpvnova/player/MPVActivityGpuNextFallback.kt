package app.mpvnova.player

import android.util.Log
import java.util.Locale

internal fun MPVActivity.retryGpuNextWithCopyHwdec(prefix: String, text: String) {
    gpuNextRenderFallbackStage = 1
    gpuNextCopyRetryConfirmed = false
    gpuNextCopyRetryDisplayedFrame = false
    Log.w(
        MPV_ACTIVITY_TAG,
        "gpu-next render failure detected, retrying with mediacodec-copy ($prefix: $text)"
    )
    player.fallbackGpuNextToCopyHwdec()
    eventUiHandler.post {
        updateDecoderButton()
        if (activityIsForeground) {
            showToast(
                getString(R.string.pref_gpu_next_title),
                getString(R.string.toast_gpu_next_copy_fallback),
                durationMs = GPU_NEXT_FALLBACK_TOAST_MS
            )
        }
    }
}

internal fun MPVActivity.keepGpuNextAfterRetry(prefix: String, text: String) {
    gpuNextRenderFallbackStage = 2
    Log.w(
        MPV_ACTIVITY_TAG,
        "gpu-next still reports render errors after the HW retry, but keeping " +
            "gpu-next to match stock mpv behavior ($prefix: $text)"
    )
}

internal fun MPVActivity.fallbackGpuNextToGpu(prefix: String, text: String) {
    gpuNextRenderFallbackStage = 2
    Log.w(MPV_ACTIVITY_TAG, "gpu-next render failure detected before HW retry, falling back to gpu ($prefix: $text)")
    player.fallbackGpuNextToGpu()
    eventUiHandler.post {
        updateDecoderButton()
        if (activityIsForeground) {
            showToast(
                getString(R.string.pref_gpu_next_title),
                getString(R.string.toast_gpu_next_fallback),
                durationMs = GPU_NEXT_FALLBACK_TOAST_MS
            )
        }
    }
}

internal fun MPVActivity.isGpuNextRenderFailure(prefix: String, text: String): Boolean {
    val normalizedPrefix = prefix.trim().lowercase(Locale.US)
    val normalizedText = text.trim().lowercase(Locale.US)
    return normalizedPrefix.contains("gpu-next") &&
        GPU_NEXT_RENDER_FAILURE_TEXT.any { normalizedText.contains(it) } ||
        GPU_NEXT_GENERAL_FAILURE_TEXT.any { normalizedText.contains(it) }
}

internal fun MPVActivity.updateGpuNextRetryConfirmation() {
    if (gpuNextRenderFallbackStage != 1 || gpuNextCopyRetryConfirmed)
        return

    val activeVo = player.activeVideoOutput.trim().lowercase(Locale.US)
    val requestedVo = player.requestedVideoOutput.trim().lowercase(Locale.US)
    val activeHwdec = player.hwdecActive.trim().lowercase(Locale.US)

    if (requestedVo.startsWith("gpu-next") &&
        activeVo.startsWith("gpu-next") &&
        activeHwdec == "mediacodec-copy"
    ) {
        gpuNextCopyRetryConfirmed = true
        Log.w(MPV_ACTIVITY_TAG, "Confirmed gpu-next retry is running with mediacodec-copy")
    }
}

internal fun MPVActivity.updateGpuNextRetryFrameConfirmation(prefix: String, text: String) {
    if (gpuNextRenderFallbackStage != 1 || gpuNextCopyRetryDisplayedFrame)
        return

    val normalizedPrefix = prefix.trim().lowercase(Locale.US)
    val normalizedText = text.trim().lowercase(Locale.US)
    val frameShown =
        normalizedPrefix == "cplayer" &&
            (normalizedText.contains("first video frame after restart shown") ||
                normalizedText.contains("playback restart complete"))

    if (frameShown) {
        gpuNextCopyRetryDisplayedFrame = true
        Log.w(MPV_ACTIVITY_TAG, "Confirmed gpu-next retry produced video output")
    }
}

internal fun MPVActivity.parseControlsTimeout(value: String?): Long {
    return when (value) {
        "never" -> -1L
        else -> value?.toLongOrNull()?.takeIf { it > 0L }
            ?: DEFAULT_CONTROLS_DISPLAY_TIMEOUT
    }
}
