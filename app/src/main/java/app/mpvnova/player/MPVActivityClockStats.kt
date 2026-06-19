package app.mpvnova.player

import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clock + "Ends at" + stats overlay updates for the time-info panel and
 * the stats text view in the player UI. Both update once per second on
 * the clock heartbeat (driven by [MPVActivity.clockRunnable]); the
 * tick guard inside [updateClockInfo] makes the call effectively free
 * if it gets invoked more than once per second.
 */

internal fun MPVActivity.updateStats() {
    if (!statsFPS)
        return
    val fps = player.estimatedVfFps ?: return
    val statsText = getString(R.string.ui_fps, fps)
    if (binding.statsTextView.text.toString() != statsText)
        binding.statsTextView.text = statsText
}

internal fun MPVActivity.updateClockInfo(force: Boolean = false) {
    val now = System.currentTimeMillis()
    val tick = now / MILLIS_PER_SECOND_LONG
    if (!force && lastClockInfoTick == tick)
        return
    lastClockInfoTick = tick

    val is24Hour = android.text.format.DateFormat.is24HourFormat(this)
    if (clockFormatter == null || clockFormatterIs24 != is24Hour) {
        val pattern = if (is24Hour) "HH:mm" else "h:mm a"
        clockFormatter = SimpleDateFormat(pattern, Locale.getDefault())
        clockFormatterIs24 = is24Hour
    }
    val formatter = clockFormatter ?: return
    val clockText = formatter.format(Date(now))
    if (binding.clockTextView.text.toString() != clockText)
        binding.clockTextView.text = clockText

    val remainingSeconds = (psc.durationSec - psc.positionSec).coerceAtLeast(0)
    if (psc.durationSec > 0 && remainingSeconds > 0) {
        val playbackSpeed = psc.speed.takeIf { it > 0f } ?: 1f
        val wallClockRemainingMs = (remainingSeconds * MILLIS_PER_SECOND_LONG / playbackSpeed).toLong()
        val endTimeMillis = now + wallClockRemainingMs
        val endsAtText = getString(
            R.string.player_ends_at,
            formatter.format(Date(endTimeMillis))
        )
        binding.endsAtTextView.visibility = View.VISIBLE
        if (binding.endsAtTextView.text.toString() != endsAtText)
            binding.endsAtTextView.text = endsAtText
    } else {
        binding.endsAtTextView.visibility = View.GONE
    }
}
