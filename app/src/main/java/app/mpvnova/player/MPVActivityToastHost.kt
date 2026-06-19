package app.mpvnova.player

import android.os.SystemClock
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.isVisible

internal fun currentToastUptimeMs(): Long = SystemClock.uptimeMillis()

internal fun MPVActivity.rehostActivePlayerToast() {
    val state = playerToastState ?: return
    if (state.remainingMs() <= 0L) {
        clearPlayerToastState(state)
        return
    }
    renderPlayerToast(state, cancel = true, animate = false)
    schedulePlayerToastHide(state)
}

internal fun MPVActivity.renderPlayerToast(state: PlayerToastState, cancel: Boolean, animate: Boolean) {
    // The in-player toast lives in the activity window, so open panels (drawer,
    // subtitle style, etc.) render on top of it. While one is up, host the same
    // chip inside that dialog's own window so it draws above the panel content.
    val dialogHost = currentPlayerDialogHost()
    if (dialogHost != null) {
        showToastInDialog(dialogHost, state, animate)
        return
    }
    showToastInActivity(state, cancel, animate)
}

internal fun MPVActivity.schedulePlayerToastHide(state: PlayerToastState) {
    fadeHandler.removeCallbacks(playerToastHideRunnable)
    overlayToastHideRunnable?.let(fadeHandler::removeCallbacks)
    overlayToastHideRunnable = null
    val delayMs = state.remainingMs()
    if (delayMs <= 0L) {
        clearPlayerToastState(state)
        return
    }
    if (overlayToastView != null) {
        val hideRunnable = Runnable { hideOverlayToast(state) }
        overlayToastHideRunnable = hideRunnable
        fadeHandler.postDelayed(hideRunnable, delayMs)
    } else {
        fadeHandler.postDelayed(playerToastHideRunnable, delayMs)
    }
}

private fun MPVActivity.showToastInActivity(state: PlayerToastState, cancel: Boolean, animate: Boolean) {
    removeOverlayToast()
    fadeHandler.removeCallbacks(playerToastHideRunnable)
    binding.playerToast.animate().cancel()
    if (cancel) {
        binding.playerToast.alpha = 1f
    }

    binding.playerToastTitle.isVisible = !state.title.isNullOrBlank()
    binding.playerToastTitle.text = state.title
    binding.playerToastMessage.text = state.detail
    updatePlayerToastPlacement()
    binding.playerToast.visibility = View.VISIBLE

    if (animate && binding.playerToast.alpha < 1f) {
        binding.playerToast.alpha = 0f
        binding.playerToast.animate().alpha(1f).setDuration(PLAYER_TOAST_FADE_IN_MS).withLayer()
    } else {
        binding.playerToast.alpha = 1f
    }
}

private fun MPVActivity.showToastInDialog(
    host: ViewGroup,
    state: PlayerToastState,
    animate: Boolean,
) {
    fadeHandler.removeCallbacks(playerToastHideRunnable)
    binding.playerToast.animate().cancel()
    binding.playerToast.visibility = View.GONE
    removeOverlayToast()
    val overlayHost = host.findViewById<ViewGroup>(android.R.id.content) ?: host
    val view = LayoutInflater.from(this).inflate(R.layout.view_overlay_toast, overlayHost, false)
    val titleView = view.findViewById<TextView>(R.id.overlayToastTitle)
    titleView.isVisible = !state.title.isNullOrBlank()
    titleView.text = state.title
    view.findViewById<TextView>(R.id.overlayToastMessage).text = state.detail
    val params = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.TOP or Gravity.CENTER_HORIZONTAL,
    ).apply { topMargin = Utils.convertDp(activityContext, TOAST_OVERLAY_TOP_DP) }
    overlayHost.addView(view, params)
    overlayToastView = view
    view.bringToFront()
    if (animate) {
        view.alpha = 0f
        view.animate().alpha(1f).setDuration(PLAYER_TOAST_FADE_IN_MS).withLayer()
    } else {
        view.alpha = 1f
    }
}

private fun MPVActivity.hideOverlayToast(state: PlayerToastState) {
    if (playerToastState?.token != state.token) return
    val view = overlayToastView ?: return
    overlayToastHideRunnable = null
    view.animate()
        .alpha(0f)
        .setDuration(PLAYER_TOAST_FADE_OUT_MS)
        .withLayer()
        .withEndAction {
            (view.parent as? ViewGroup)?.removeView(view)
            if (overlayToastView === view) {
                overlayToastView = null
            }
            if (playerToastState?.token == state.token) {
                playerToastState = null
            }
        }
}

private fun MPVActivity.removeOverlayToast() {
    overlayToastHideRunnable?.let(fadeHandler::removeCallbacks)
    overlayToastHideRunnable = null
    overlayToastView?.let { view ->
        view.animate().cancel()
        (view.parent as? ViewGroup)?.removeView(view)
    }
    overlayToastView = null
}

private fun MPVActivity.clearPlayerToastState(state: PlayerToastState) {
    if (playerToastState?.token != state.token) return
    fadeHandler.removeCallbacks(playerToastHideRunnable)
    removeOverlayToast()
    binding.playerToast.animate().cancel()
    binding.playerToast.visibility = View.GONE
    playerToastState = null
}

private fun MPVActivity.currentPlayerDialogHost(): ViewGroup? {
    playerDialogStack.removeAll { !it.isShowing }
    val dialog = playerDialogStack.lastOrNull { it.isShowing }
        ?: topPlayerDialog?.takeIf { it.isShowing }
    topPlayerDialog = dialog
    return dialog?.window?.decorView as? ViewGroup
}

private fun PlayerToastState.remainingMs(): Long =
    (hideAtMs - currentToastUptimeMs()).coerceAtLeast(0L)
