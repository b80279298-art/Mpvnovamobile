package app.mpvnova.player

import androidx.preference.PreferenceManager.getDefaultSharedPreferences

internal fun MPVActivity.handleDrawerPreferenceChange(
    preference: PlayerDrawerPreference,
    newValue: Boolean,
) {
    when (preference.group) {
        PlayerDrawerPreferenceGroup.AUTOPAUSE -> handleDrawerAutopausePreference(preference, newValue)
        PlayerDrawerPreferenceGroup.INTERFACE -> handleDrawerInterfacePreference(preference, newValue)
        PlayerDrawerPreferenceGroup.VIDEO -> handleDrawerVideoPreference(preference, newValue)
        PlayerDrawerPreferenceGroup.PLAYBACK -> handleDrawerPlaybackPreference(preference, newValue)
    }
}

private fun MPVActivity.handleDrawerAutopausePreference(
    preference: PlayerDrawerPreference,
    newValue: Boolean,
) {
    when (preference) {
        PlayerDrawerPreference.AUTOPAUSE_CONTROLS -> {
            autoPauseControlsOverlayEnabled = newValue
            if (!newValue) controlsOverlayAutoPaused = false
        }
        PlayerDrawerPreference.AUTOPAUSE_SHIELD -> {
            autoPauseShieldHi10pEnabled = newValue
            if (!newValue) controlsOverlayAutoPaused = false
        }
        else -> Unit
    }
}

private fun MPVActivity.handleDrawerInterfacePreference(
    preference: PlayerDrawerPreference,
    newValue: Boolean,
) {
    when (preference) {
        PlayerDrawerPreference.KEEP_CONTROLS_VISIBLE -> keepControlsVisibleWhilePaused = newValue
        PlayerDrawerPreference.SHOW_MEDIA_TITLE -> {
            showMediaTitle = newValue
            refreshUi()
        }
        PlayerDrawerPreference.SHOW_CLOCK -> {
            showClockOverlay = newValue
            refreshUi()
        }
        PlayerDrawerPreference.BOTTOM_CONTROLS -> {
            controlsAtBottom = newValue
            onConfigurationChanged(resources.configuration)
        }
        PlayerDrawerPreference.EXIT_DOUBLE_BACK -> {
            exitWithDoubleBack = newValue
            lastBackPressMs = 0L
        }
        PlayerDrawerPreference.DPAD_UP_JUMPS_TOP -> dpadUpJumpsToTopControls = newValue
        PlayerDrawerPreference.HIDE_CONTROLS_WHILE_SEEKING -> hideControlsWhileSeeking = newValue
        PlayerDrawerPreference.MINIMAL_SEEKBAR_WHILE_SEEKING -> {
            minimalSeekbarWhileSeeking = newValue
            if (!newValue) hideMinimalSeekOverlay()
        }
        else -> Unit
    }
}

private fun MPVActivity.handleDrawerVideoPreference(
    preference: PlayerDrawerPreference,
    newValue: Boolean,
) {
    when (preference) {
        PlayerDrawerPreference.DECODER_AUTO_FALLBACK -> handleDrawerAutoFallbackChange(newValue)
        PlayerDrawerPreference.SHIELD_DECODER_MODE -> handleDrawerShieldDecoderModeChange(newValue)
        else -> Unit
    }
    if (preference == PlayerDrawerPreference.DECODER_AUTO_FALLBACK ||
        preference == PlayerDrawerPreference.SHIELD_DECODER_MODE) {
        refreshDrawerRowsIfVisible(DrawerTab.VIDEO)
    }
}

private fun MPVActivity.handleDrawerAutoFallbackChange(enabled: Boolean) {
    autoDecoderFallback = enabled
    if (enabled)
        return
    preferredDecoderMode = normalizedPreferredDecoderMode(preferredDecoderMode, shieldDecoderModeEnabled)
    getDefaultSharedPreferences(applicationContext)
        .edit()
        .putString("preferred_decoder_mode", preferredDecoderMode)
        .apply()
    sessionDecoderMode = preferredDecoderMode
    player.applyDecoderMode(preferredDecoderMode)
    updateDecoderButton()
}

private fun MPVActivity.handleDrawerShieldDecoderModeChange(enabled: Boolean) {
    shieldDecoderModeEnabled = enabled
    if (enabled || preferredDecoderMode != MPVView.DECODER_MODE_SHIELD_H10P)
        return

    preferredDecoderMode = defaultPreferredDecoderMode()
    getDefaultSharedPreferences(applicationContext)
        .edit()
        .putString("preferred_decoder_mode", preferredDecoderMode)
        .apply()
    if (!autoDecoderFallback) {
        sessionDecoderMode = preferredDecoderMode
        player.applyDecoderMode(preferredDecoderMode)
        updateDecoderButton()
    }
}

private fun MPVActivity.handleDrawerPlaybackPreference(
    preference: PlayerDrawerPreference,
    newValue: Boolean,
) {
    when (preference) {
        PlayerDrawerPreference.SAVE_POSITION -> shouldSavePosition = newValue
        PlayerDrawerPreference.PLAYLIST_EXIT_WARNING -> playlistExitWarning = newValue
        else -> Unit
    }
}
