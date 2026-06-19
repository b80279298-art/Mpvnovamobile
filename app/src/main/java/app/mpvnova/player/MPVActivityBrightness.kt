package app.mpvnova.player

import android.content.res.Configuration
import android.view.View
import android.view.WindowManager
import kotlin.math.roundToInt

internal fun MPVActivity.applyPlayerScreenBrightnessPreference() {
    val brightness = if (playerScreenBrightnessActive) {
        playerScreenBrightnessPercent
            .coerceIn(MIN_PLAYER_SCREEN_BRIGHTNESS_PERCENT, MAX_PLAYER_SCREEN_BRIGHTNESS_PERCENT)
            .toFloat() / PERCENT_SCALE_FLOAT
    } else {
        null
    }
    val useOverlay = shouldUsePlayerBrightnessOverlay()

    val attributes = window.attributes
    attributes.screenBrightness = brightness
        ?.takeUnless { useOverlay }
        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    window.attributes = attributes

    applyPlayerBrightnessOverlay(brightness, useOverlay)
}

internal fun MPVActivity.defaultPlayerScreenBrightnessPercent(): Int {
    return if (shouldUsePlayerBrightnessOverlay()) {
        DEFAULT_PLAYER_SCREEN_BRIGHTNESS_PERCENT
    } else {
        Utils.getScreenBrightness(this)
            ?.let { (it * PERCENT_SCALE_FLOAT).roundToInt() }
            ?.coerceIn(MIN_PLAYER_SCREEN_BRIGHTNESS_PERCENT, MAX_PLAYER_SCREEN_BRIGHTNESS_PERCENT)
            ?: DEFAULT_PLAYER_SCREEN_BRIGHTNESS_PERCENT
    }
}

private fun MPVActivity.shouldUsePlayerBrightnessOverlay(): Boolean {
    val uiModeType = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
    return uiModeType == Configuration.UI_MODE_TYPE_TELEVISION
}

private fun MPVActivity.applyPlayerBrightnessOverlay(
    brightness: Float?,
    useOverlay: Boolean
) {
    val dimAmount = if (brightness != null && useOverlay) {
        (1f - brightness).coerceIn(0f, MAX_PLAYER_BRIGHTNESS_DIM_ALPHA)
    } else {
        0f
    }
    binding.playerBrightnessOverlay.alpha = dimAmount
    binding.playerBrightnessOverlay.visibility = if (dimAmount > 0f) View.VISIBLE else View.GONE
}
