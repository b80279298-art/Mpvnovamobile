package app.mpvnova.player

internal fun MPVActivity.drawerOptionValue(option: PlayerDrawerOption): String = when (option) {
    PlayerDrawerOption.PREFERRED_DECODER -> {
        val mode = normalizedPreferredDecoderMode(preferredDecoderMode, shieldDecoderModeEnabled)
        decoderModeCompactLabel(mode)
    }
    PlayerDrawerOption.SHIELD_FALLBACK -> shieldFallbackOption(shieldDecoderFallback).compactLabel
}
