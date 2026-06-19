package app.mpvnova.player

import android.content.Context
import android.view.KeyEvent
import java.util.Locale

internal fun remoteButtonKeyCode(value: String?): Int? {
    if (value == null || value == REMOTE_BUTTON_DISABLED)
        return null
    return value.toIntOrNull()?.takeIf { remoteButtonCanBeAssigned(it) }
}

internal fun remoteButtonCanBeAssigned(keyCode: Int): Boolean {
    return keyCode > KeyEvent.KEYCODE_UNKNOWN && keyCode !in RESERVED_REMOTE_BUTTON_KEYS
}

internal fun remoteButtonLetsCaptureDialogHandle(keyCode: Int): Boolean {
    return keyCode in CAPTURE_DIALOG_CONTROL_KEYS
}

internal fun remoteButtonDisplayName(context: Context, value: String?): String {
    val keyCode = remoteButtonKeyCode(value) ?: return context.getString(
        R.string.pref_remote_button_unassigned
    )
    return remoteButtonDisplayName(keyCode)
}

internal fun remoteButtonDisplayName(keyCode: Int): String {
    val rawName = KeyEvent.keyCodeToString(keyCode)
        .removePrefix("KEYCODE_")
        .takeIf { it.isNotBlank() && it != keyCode.toString() }
        ?: return "Keycode $keyCode"

    return rawName
        .lowercase(Locale.US)
        .split("_")
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
}

internal fun MPVActivity.interceptRemoteNextChapterButton(event: KeyEvent): Boolean {
    val keyCode = remoteNextChapterKeyCode
    val actionMatches = event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP
    val handled = keyCode != null && event.keyCode == keyCode && actionMatches

    if (handled && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
        seekChapterRelative(1, showFeedback = true)
    }

    return handled
}

private val RESERVED_REMOTE_BUTTON_KEYS = setOf(
    KeyEvent.KEYCODE_APP_SWITCH,
    KeyEvent.KEYCODE_ASSIST,
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_GUIDE,
    KeyEvent.KEYCODE_HOME,
    KeyEvent.KEYCODE_MENU,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
    KeyEvent.KEYCODE_POWER,
    KeyEvent.KEYCODE_SEARCH,
    KeyEvent.KEYCODE_SETTINGS,
    KeyEvent.KEYCODE_SLEEP,
    KeyEvent.KEYCODE_VOICE_ASSIST,
    KeyEvent.KEYCODE_VOLUME_DOWN,
    KeyEvent.KEYCODE_VOLUME_MUTE,
    KeyEvent.KEYCODE_VOLUME_UP,
    KeyEvent.KEYCODE_WAKEUP,
)

private val CAPTURE_DIALOG_CONTROL_KEYS = setOf(
    KeyEvent.KEYCODE_BACK,
    KeyEvent.KEYCODE_DPAD_CENTER,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_ENTER,
    KeyEvent.KEYCODE_ESCAPE,
    KeyEvent.KEYCODE_NUMPAD_ENTER,
)
