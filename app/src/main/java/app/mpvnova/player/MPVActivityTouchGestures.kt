package app.mpvnova.player

import android.media.AudioManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs
import kotlin.math.ln

private const val GESTURE_SEEK_MS_PER_PX = 80L
private const val GESTURE_BRIGHTNESS_PX_PER_STEP = 12f
private const val GESTURE_VOLUME_PX_PER_STEP = 15f
private const val GESTURE_DOUBLE_TAP_SEEK_MS = 10_000L
private const val GESTURE_ZOOM_MIN = -2.0   // 0.25x zoom (log2 scale)
private const val GESTURE_ZOOM_MAX = 2.0    // 4x zoom (log2 scale)
private val LN2 = ln(2.0)

internal fun MPVActivity.initTouchGestureDetector(): GestureDetector {
    return GestureDetector(this, PlayerGestureListener(this))
}

internal fun MPVActivity.initPinchGestureDetector(): ScaleGestureDetector {
    return ScaleGestureDetector(this, PlayerScaleListener(this))
}

internal fun MPVActivity.resetPinchZoom() {
    mpvSetPropertyDouble("video-zoom", 0.0)
    mpvSetPropertyDouble("video-pan-x", 0.0)
    mpvSetPropertyDouble("video-pan-y", 0.0)
    showToast("Zoom: 1×", cancel = true, durationMs = 700L)
}

internal fun MPVActivity.adjustGestureBrightness(steps: Int) {
    if (!playerScreenBrightnessActive) {
        playerScreenBrightnessActive = true
        playerScreenBrightnessPercent = defaultPlayerScreenBrightnessPercent()
    }
    playerScreenBrightnessPercent = (playerScreenBrightnessPercent + steps)
        .coerceIn(MIN_PLAYER_SCREEN_BRIGHTNESS_PERCENT, MAX_PLAYER_SCREEN_BRIGHTNESS_PERCENT)
    applyPlayerScreenBrightnessPreference()
    showToast(
        getString(R.string.btn_brightness) + ": ${playerScreenBrightnessPercent}%",
        cancel = true,
        durationMs = 900L
    )
}

private class PlayerGestureListener(
    private val activity: MPVActivity
) : GestureDetector.SimpleOnGestureListener() {

    private var scrollDirectionDecided = false
    private var isHorizontalScroll = false
    private var isRightSideScroll = false
    private var accumulatedBrightnessPx = 0f
    private var accumulatedVolumePx = 0f

    override fun onDown(e: MotionEvent): Boolean {
        scrollDirectionDecided = false
        accumulatedBrightnessPx = 0f
        accumulatedVolumePx = 0f
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        activity.toggleControls()
        return true
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        val containerWidth = activity.binding.outside.width
        val isRightSide = e.x > containerWidth / 2f
        val seekMs = if (isRightSide) GESTURE_DOUBLE_TAP_SEEK_MS else -GESTURE_DOUBLE_TAP_SEEK_MS
        activity.seekPlaybackFromDpad(seekMs)
        val label = if (isRightSide)
            "+${GESTURE_DOUBLE_TAP_SEEK_MS / 1000}s"
        else
            "-${GESTURE_DOUBLE_TAP_SEEK_MS / 1000}s"
        activity.showToast(label, cancel = true, durationMs = 700L)
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!scrollDirectionDecided) {
            scrollDirectionDecided = true
            isHorizontalScroll = abs(distanceX) >= abs(distanceY)
            val touchX = e1?.x ?: e2.x
            val containerWidth = activity.binding.outside.width
            isRightSideScroll = touchX > containerWidth / 2f
        }

        return if (isHorizontalScroll) {
            onHorizontalScroll(distanceX)
        } else if (isRightSideScroll) {
            onVolumeScroll(distanceY)
        } else {
            onBrightnessScroll(distanceY)
        }
    }

    private fun onHorizontalScroll(distanceX: Float): Boolean {
        val seekMs = (-distanceX * GESTURE_SEEK_MS_PER_PX).toLong()
        activity.seekPlaybackFromDpad(seekMs)
        return true
    }

    private fun onVolumeScroll(distanceY: Float): Boolean {
        accumulatedVolumePx += distanceY
        val steps = (accumulatedVolumePx / GESTURE_VOLUME_PX_PER_STEP).toInt()
        if (steps != 0) {
            accumulatedVolumePx -= steps * GESTURE_VOLUME_PX_PER_STEP
            val direction = if (steps > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            repeat(abs(steps)) {
                activity.audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    direction,
                    AudioManager.FLAG_SHOW_UI
                )
            }
        }
        return true
    }

    private fun onBrightnessScroll(distanceY: Float): Boolean {
        accumulatedBrightnessPx += distanceY
        val steps = (accumulatedBrightnessPx / GESTURE_BRIGHTNESS_PX_PER_STEP).toInt()
        if (steps != 0) {
            accumulatedBrightnessPx -= steps * GESTURE_BRIGHTNESS_PX_PER_STEP
            activity.adjustGestureBrightness(steps)
        }
        return true
    }
}

private class PlayerScaleListener(
    private val activity: MPVActivity
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        val currentZoom = mpvGetPropertyDouble("video-zoom") ?: 0.0
        val zoomDelta = ln(detector.scaleFactor.toDouble()) / LN2
        val newZoom = (currentZoom + zoomDelta).coerceIn(GESTURE_ZOOM_MIN, GESTURE_ZOOM_MAX)
        mpvSetPropertyDouble("video-zoom", newZoom)
        val zoomPercent = ((Math.pow(2.0, newZoom) * 100).toInt())
        activity.showToast("Zoom: ${zoomPercent}%", cancel = true, durationMs = 600L)
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        val currentZoom = mpvGetPropertyDouble("video-zoom") ?: 0.0
        if (currentZoom in -0.05..0.05) {
            activity.resetPinchZoom()
        }
    }
}
