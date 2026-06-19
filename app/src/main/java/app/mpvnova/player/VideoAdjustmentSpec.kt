package app.mpvnova.player

import androidx.annotation.StringRes
import androidx.preference.PreferenceManager.getDefaultSharedPreferences

internal data class VideoAdjustmentSpec(
    @param:StringRes val titleRes: Int,
    val property: String,
    val rememberKey: String,
    val valueKey: String
)

internal val VIDEO_CONTRAST_ADJUSTMENT = VideoAdjustmentSpec(
    titleRes = R.string.contrast,
    property = "contrast",
    rememberKey = "remember_video_contrast",
    valueKey = "video_contrast"
)

internal val VIDEO_GAMMA_ADJUSTMENT = VideoAdjustmentSpec(
    titleRes = R.string.gamma,
    property = "gamma",
    rememberKey = "remember_video_gamma",
    valueKey = "video_gamma"
)

internal val VIDEO_SATURATION_ADJUSTMENT = VideoAdjustmentSpec(
    titleRes = R.string.saturation,
    property = "saturation",
    rememberKey = "remember_video_saturation",
    valueKey = "video_saturation"
)

private val rememberedVideoAdjustments = arrayOf(
    VIDEO_CONTRAST_ADJUSTMENT,
    VIDEO_GAMMA_ADJUSTMENT,
    VIDEO_SATURATION_ADJUSTMENT
)

internal fun MPVActivity.readVideoAdjustmentSettings(
    getRemember: (String) -> Boolean,
    getValue: (String) -> Int
) {
    rememberVideoContrast = getRemember(VIDEO_CONTRAST_ADJUSTMENT.rememberKey)
    videoContrastValue = readVideoAdjustmentValue(VIDEO_CONTRAST_ADJUSTMENT, getValue)
    rememberVideoGamma = getRemember(VIDEO_GAMMA_ADJUSTMENT.rememberKey)
    videoGammaValue = readVideoAdjustmentValue(VIDEO_GAMMA_ADJUSTMENT, getValue)
    rememberVideoSaturation = getRemember(VIDEO_SATURATION_ADJUSTMENT.rememberKey)
    videoSaturationValue = readVideoAdjustmentValue(VIDEO_SATURATION_ADJUSTMENT, getValue)
}

private fun readVideoAdjustmentValue(
    spec: VideoAdjustmentSpec,
    getValue: (String) -> Int
): Int {
    return getValue(spec.valueKey).coerceVideoAdjustment()
}

internal fun MPVActivity.applyRememberedVideoAdjustments() {
    for (spec in rememberedVideoAdjustments) {
        val value = if (rememberVideoAdjustment(spec)) {
            rememberedVideoAdjustmentValue(spec)
        } else {
            VIDEO_ADJUSTMENT_DEFAULT_INT
        }
        mpvSetPropertyInt(spec.property, value)
    }
}

internal fun MPVActivity.rememberVideoAdjustment(spec: VideoAdjustmentSpec): Boolean {
    return when (spec) {
        VIDEO_CONTRAST_ADJUSTMENT -> rememberVideoContrast
        VIDEO_GAMMA_ADJUSTMENT -> rememberVideoGamma
        VIDEO_SATURATION_ADJUSTMENT -> rememberVideoSaturation
        else -> false
    }
}

internal fun MPVActivity.rememberedVideoAdjustmentValue(spec: VideoAdjustmentSpec): Int {
    return when (spec) {
        VIDEO_CONTRAST_ADJUSTMENT -> videoContrastValue
        VIDEO_GAMMA_ADJUSTMENT -> videoGammaValue
        VIDEO_SATURATION_ADJUSTMENT -> videoSaturationValue
        else -> VIDEO_ADJUSTMENT_DEFAULT_INT
    }
}

internal fun MPVActivity.saveVideoAdjustmentChoice(
    spec: VideoAdjustmentSpec,
    value: Int,
    remember: Boolean
) {
    val normalizedValue = value.coerceVideoAdjustment()
    when (spec) {
        VIDEO_CONTRAST_ADJUSTMENT -> {
            rememberVideoContrast = remember
            videoContrastValue = normalizedValue
        }
        VIDEO_GAMMA_ADJUSTMENT -> {
            rememberVideoGamma = remember
            videoGammaValue = normalizedValue
        }
        VIDEO_SATURATION_ADJUSTMENT -> {
            rememberVideoSaturation = remember
            videoSaturationValue = normalizedValue
        }
    }

    getDefaultSharedPreferences(applicationContext).edit().apply {
        putBoolean(spec.rememberKey, remember)
        if (remember) {
            putInt(spec.valueKey, normalizedValue)
        } else {
            remove(spec.valueKey)
        }
    }.apply()
}

private fun Int.coerceVideoAdjustment(): Int {
    return coerceIn(VIDEO_ADJUSTMENT_MIN_INT, VIDEO_ADJUSTMENT_MAX_INT)
}
