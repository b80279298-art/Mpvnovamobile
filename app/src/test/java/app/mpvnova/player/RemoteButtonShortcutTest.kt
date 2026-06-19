package app.mpvnova.player

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteButtonShortcutTest {
    @Test
    fun remoteButtonKeyCodeRejectsDisabledAndInvalidValues() {
        assertNull(remoteButtonKeyCode(REMOTE_BUTTON_DISABLED))
        assertNull(remoteButtonKeyCode("not-a-keycode"))
        assertNull(remoteButtonKeyCode(KeyEvent.KEYCODE_UNKNOWN.toString()))
    }

    @Test
    fun remoteButtonKeyCodeRejectsReservedNavigationKeys() {
        assertNull(remoteButtonKeyCode(KeyEvent.KEYCODE_BACK.toString()))
        assertNull(remoteButtonKeyCode(KeyEvent.KEYCODE_HOME.toString()))
        assertNull(remoteButtonKeyCode(KeyEvent.KEYCODE_DPAD_CENTER.toString()))
    }

    @Test
    fun remoteButtonKeyCodeAllowsRemoteSpecificKeys() {
        assertEquals(KeyEvent.KEYCODE_CHANNEL_UP, remoteButtonKeyCode(KeyEvent.KEYCODE_CHANNEL_UP.toString()))
        assertEquals(KeyEvent.KEYCODE_PROG_BLUE, remoteButtonKeyCode(KeyEvent.KEYCODE_PROG_BLUE.toString()))
    }

    @Test
    fun remoteButtonAssignmentGuardAllowsOnlySafeKeys() {
        assertFalse(remoteButtonCanBeAssigned(KeyEvent.KEYCODE_BACK))
        assertFalse(remoteButtonCanBeAssigned(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(remoteButtonCanBeAssigned(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        assertTrue(remoteButtonCanBeAssigned(KeyEvent.KEYCODE_MEDIA_NEXT))
    }

    @Test
    fun captureDialogLetsDpadReachDialogControls() {
        assertTrue(remoteButtonLetsCaptureDialogHandle(KeyEvent.KEYCODE_DPAD_LEFT))
        assertTrue(remoteButtonLetsCaptureDialogHandle(KeyEvent.KEYCODE_DPAD_CENTER))
        assertTrue(remoteButtonLetsCaptureDialogHandle(KeyEvent.KEYCODE_ENTER))
        assertTrue(remoteButtonLetsCaptureDialogHandle(KeyEvent.KEYCODE_BACK))
        assertFalse(remoteButtonLetsCaptureDialogHandle(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
    }
}
