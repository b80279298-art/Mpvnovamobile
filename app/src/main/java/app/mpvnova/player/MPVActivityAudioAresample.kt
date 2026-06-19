package app.mpvnova.player

import android.util.Log
import java.util.Locale

internal fun MPVActivity.currentAudioFilterOutputSampleFormat(): String {
    return mapMpvAudioFormatToFfmpeg(
        mpvGetPropertyString("audio-out-params/format")
            ?: mpvGetPropertyString("audio-params/format")
    ) ?: "flt"
}

internal fun MPVActivity.centerBoostMixLevelLabel(): String {
    return String.format(Locale.US, "%.1f", centerBoostMixLevels[centerBoostLevel])
}

private fun MPVActivity.buildAudioAresampleFilter(owner: String): String {
    val outRate = mpvGetPropertyInt("audio-out-params/samplerate")
        ?.takeIf { it > 0 }
        ?: mpvGetPropertyInt("audio-params/samplerate")
            ?.takeIf { it > 0 }
        ?: DEFAULT_AUDIO_SAMPLE_RATE
    val sourceChannels = currentAudioChannelCount()
    val controlledDownmixActive = isDownmixOn() && sourceChannels >= MIN_SURROUND_CHANNELS
    val inputLayout = if (controlledDownmixActive) {
        "stereo"
    } else {
        mpvGetPropertyString("audio-params/channels")
            ?.takeIf { it.isNotBlank() }
    }
    val outputLayout = when {
        controlledDownmixActive -> "stereo"
        else -> mpvGetPropertyString("audio-out-params/channels")
            ?.takeIf { it.isNotBlank() }
            ?: inputLayout
            ?: "stereo"
    }

    val options = mutableListOf("$outRate")
    inputLayout?.let { options += "in_chlayout=$it" }
    options += "out_chlayout=$outputLayout"
    options += "out_sample_fmt=${currentAudioFilterOutputSampleFormat()}"
    if (isCenterBoostOn())
        options += "center_mix_level=${centerBoostMixLevelLabel()}"
    Log.i(
        MPV_ACTIVITY_TAG,
        if (controlledDownmixActive)
            "$owner using controlled Channel Downmix output: ${sourceChannels}ch -> stereo"
        else
            "$owner active without forced center downmix: ${sourceChannels}ch source"
    )
    return "aresample=${options.joinToString(":")}"
}

internal fun MPVActivity.buildDrcAresampleFilter(): String = buildAudioAresampleFilter("DRC")

internal fun MPVActivity.buildCenterBoostAudioStageFilter(): String {
    return "$centerBoostFilterLabel:lavfi=[${buildAudioAresampleFilter("Center Boost")}]"
}
