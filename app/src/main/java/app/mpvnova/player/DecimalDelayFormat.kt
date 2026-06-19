package app.mpvnova.player

import kotlin.math.roundToLong

private const val DELAY_DISPLAY_SCALE = 100.0

/**
 * Formats a delay value for an editable decimal field: rounds to 2 decimals (killing the
 * float-precision noise mpv returns for values like 0.1 -> 0.10000000149) and drops trailing
 * zeros, so "0.1" stays "0.1" instead of "0.10000000149011612". Shared by the sub- and
 * audio-delay pickers.
 */
internal fun formatDelayForField(value: Double): String {
    val rounded = (value * DELAY_DISPLAY_SCALE).roundToLong() / DELAY_DISPLAY_SCALE
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
