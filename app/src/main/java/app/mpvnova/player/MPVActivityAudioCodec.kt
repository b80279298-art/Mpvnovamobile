package app.mpvnova.player

import java.util.Locale

internal fun MPVActivity.selectedAudioTrackCodec(): String? {
    val trackCount = mpvGetPropertyInt("track-list/count") ?: return null
    return (0 until trackCount)
        .firstOrNull { index ->
            mpvGetPropertyString("track-list/$index/type") == "audio" &&
                mpvGetPropertyBoolean("track-list/$index/selected") == true
        }
        ?.let(::audioTrackCodec)
}

internal fun MPVActivity.audioTrackCodec(index: Int): String? {
    return listOf(
        "codec",
        "demux-codec",
        "codec-name",
        "decoder-desc"
    ).firstNotNullOfOrNull { field ->
        mpvGetPropertyString("track-list/$index/$field")?.takeIf { it.isNotBlank() }
    }
}

internal fun MPVActivity.currentAudioCodecName(): String? {
    return selectedAudioTrackCodec()
        ?: mpvGetPropertyString("audio-codec-name")
        ?: mpvGetPropertyString("audio-codec")
}

internal fun String?.isEac3CodecName(): Boolean {
    val normalized = this
        ?.trim()
        ?.lowercase(Locale.US)
        ?: return false
    return normalized == "eac3" ||
        normalized == "e-ac-3" ||
        normalized == "e-ac3" ||
        normalized.contains("eac3") ||
        normalized.contains("e-ac-3")
}
