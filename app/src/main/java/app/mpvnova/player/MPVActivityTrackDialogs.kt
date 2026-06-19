package app.mpvnova.player

import androidx.appcompat.app.AlertDialog
import android.os.Build
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.WindowManager
import kotlin.math.roundToInt

internal fun MPVActivity.cycleAudio() = trackSwitchNotification {
    player.cycleAudio(); TrackData(player.aid, "audio")
}

internal fun MPVActivity.cycleSub() = trackSwitchNotification {
    player.cycleSub(); TrackData(player.sid, "sub")
}

internal fun MPVActivity.showWidePlayerDialog(dialog: AlertDialog, layout: PlayerDialogLayout = PlayerDialogLayout()) {
    showPlayerDialog(dialog)
    dialog.window?.apply {
        setBackgroundDrawableResource(android.R.color.transparent)
        decorView.setPadding(0, 0, 0, 0)
        setGravity(layout.gravity)

        val screenWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else @Suppress("DEPRECATION") {
            val dm = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels
        }
        val maxWidthPx = Utils.convertDp(activityContext, layout.maxWidthDp)
        val desiredWidth = (screenWidth * layout.widthFraction).roundToInt()
        val desiredHeight = layout.heightFraction?.let {
            val screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                windowManager.currentWindowMetrics.bounds.height()
            } else @Suppress("DEPRECATION") {
                val dm = DisplayMetrics()
                windowManager.defaultDisplay.getRealMetrics(dm)
                dm.heightPixels
            }
            val fractionalHeight = (screenHeight * it).roundToInt()
            val maxHeightPx = layout.maxHeightDp?.let { dp -> Utils.convertDp(activityContext, dp) }
                ?: Int.MAX_VALUE
            minOf(fractionalHeight, maxHeightPx)
        } ?: WindowManager.LayoutParams.WRAP_CONTENT
        setLayout(minOf(desiredWidth, maxWidthPx), desiredHeight)
        if (layout.verticalOffsetDp != 0f) {
            attributes = attributes.apply {
                y = Utils.convertDp(activityContext, layout.verticalOffsetDp)
            }
        }
    }
    rehostActivePlayerToast()
}

internal fun MPVActivity.showPlayerDialog(dialog: AlertDialog) {
    playerDialogStack.removeAll { !it.isShowing }
    topPlayerDialog = dialog
    dialog.show()
    if (dialog !in playerDialogStack) {
        playerDialogStack += dialog
    }
    rehostActivePlayerToast()
}

internal fun MPVActivity.pickAudio() {
    val restore = keepPlaybackForDialog()
    val tracks = player.tracks.getValue("audio")
    val selectedId = player.aid
    val items = tracks.map {
        MediaPickerDialog.Item(it.name, it.mpvId, it.mpvId == selectedId)
    }
    val impl = audioPickerDialog ?: MediaPickerDialog().also {
        audioPickerDialog = it
    }
    lateinit var dialog: AlertDialog
    configureAudioPickerCallbacks(impl, tracks) { dialog.dismiss() }

    @Suppress("DEPRECATION")
    dialog = with(AlertDialog.Builder(this)) {
        val inflater = LayoutInflater.from(context)
        setView(impl.buildView(inflater, audioPickerOptions(items)))
        setOnDismissListener { restore(); reopenDrawerIfPending() }
        create()
    }
    showWidePlayerDialog(
        dialog,
        PlayerDialogLayout(
            widthFraction = 0.78f,
            maxWidthDp = 1080f,
        )
    )
}

internal fun MPVActivity.buildSubItems(): List<MediaPickerDialog.Item> {
    val rawTracks = player.tracks.getValue("sub")
    val primaryId = player.sid
    val secondaryId = player.secondarySid

    val noneTrack = rawTracks.firstOrNull { it.mpvId == -1 }
    val primaryTrack = rawTracks.firstOrNull { it.mpvId != -1 && it.mpvId == primaryId }
    val secondaryTrack = rawTracks.firstOrNull { it.mpvId != -1 && it.mpvId == secondaryId }
    val pinnedIds = setOfNotNull(primaryTrack?.mpvId, secondaryTrack?.mpvId)

    val orderedTracks = buildList {
        noneTrack?.let { add(it) }
        primaryTrack?.let { add(it) }
        secondaryTrack?.let { add(it) }
        addAll(rawTracks.filter { it.mpvId != -1 && it.mpvId !in pinnedIds })
    }

    val hasSecondary = secondaryTrack != null
    return orderedTracks.map { t ->
        val label: CharSequence = when {
            t.mpvId == -1 -> t.name
            t.mpvId == primaryId && hasSecondary -> "▾  BOTTOM  ·  ${t.name}"
            t.mpvId == secondaryId -> "▴  TOP  ·  ${t.name}"
            else -> t.name
        }
        val checked = t.mpvId == primaryId ||
                (t.mpvId != -1 && t.mpvId == secondaryId)
        MediaPickerDialog.Item(label, t.mpvId, checked)
    }
}

internal fun MPVActivity.pickSub() {
    val restore = keepPlaybackForDialog()
    val impl = subtitlePickerDialog ?: MediaPickerDialog().also {
        subtitlePickerDialog = it
    }
    lateinit var dialog: AlertDialog
    configureSubPickerCallbacks(impl) { dialog.dismiss() }
    dialog = createSubPickerDialog(impl, restore)
    showWidePlayerDialog(
        dialog,
        PlayerDialogLayout(
            widthFraction = 0.78f,
            maxWidthDp = 1080f,
        )
    )
}

internal fun MPVActivity.openSubDelayDialog() {
    val restore = keepPlaybackForDialog()
    showSubDelayPicker(
        restore,
        PlayerDialogLayout(
            widthFraction = ADVANCED_SUB_DELAY_DIALOG_WIDTH_FRACTION,
            maxWidthDp = ADVANCED_SUB_DELAY_DIALOG_MAX_WIDTH_DP,
        )
    )
}

internal fun MPVActivity.openPlaylistMenu(restore: StateRestoreCallback) {
    val impl = playlistDialog ?: PlaylistDialog(player).also {
        playlistDialog = it
    }
    lateinit var dialog: AlertDialog

    impl.listeners = object : PlaylistDialog.Listeners {
        private fun openFilePicker(skip: Int) {
            openFilePickerFor("", skip) { result, data ->
                if (result == RESULT_OK) {
                    val path = data?.getStringExtra("path") ?: return@openFilePickerFor
                    mpvCommand(arrayOf("loadfile", path, "append"))
                    impl.refresh()
                }
            }
        }
        override fun pickFile() = openFilePicker(FilePickerActivity.FILE_PICKER)

        override fun openUrl() {
            val helper = Utils.OpenUrlDialog(activityContext)
            with (helper) {
                builder.setPositiveButton(R.string.dialog_ok) { _, _ ->
                    mpvCommand(arrayOf("loadfile", helper.text, "append"))
                    impl.refresh()
                }
                builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                showPlayerDialog(create())
            }
        }

        override fun onItemPicked(item: MPVView.PlaylistItem) {
            mpvSetPropertyInt("playlist-pos", item.index)
            dialog.dismiss()
        }
    }

    dialog = with (AlertDialog.Builder(this)) {
        val inflater = LayoutInflater.from(context)
        setView(impl.buildView(inflater))
        setOnDismissListener { restore(); reopenDrawerIfPending() }
        create()
    }
    showWidePlayerDialog(
        dialog,
        PlayerDialogLayout(
            widthFraction = 0.62f,
            maxWidthDp = 720f,
            heightFraction = 0.82f,
            maxHeightDp = 760f,
        )
    )
}
