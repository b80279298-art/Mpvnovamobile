package app.mpvnova.player

import android.view.View
import android.view.ViewGroup

internal fun View.detachFromParent() {
    (parent as? ViewGroup)?.removeView(this)
}
