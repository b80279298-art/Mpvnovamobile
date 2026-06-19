package app.mpvnova.player

import androidx.core.view.isVisible
import app.mpvnova.player.databinding.DialogSubStyleSavePresetBinding

internal class PresetOverrideSelection(
    var overrideAss: Boolean,
    var selectiveAss: Boolean,
    var forceAll: Boolean,
)

// Wires the preset panel's three override rows. They are mutually exclusive — enabling one clears
// the others — matching the live subtitle-style dialog.
internal fun bindPresetOverrideRows(
    view: DialogSubStyleSavePresetBinding,
    sel: PresetOverrideSelection,
) {
    fun sync() {
        view.presetOverrideAssCheck.isVisible = sel.overrideAss
        view.presetSelectiveCheck.isVisible = sel.selectiveAss
        view.presetForceAllCheck.isVisible = sel.forceAll
    }
    sync()
    view.presetOverrideAssRow.setOnClickListener {
        sel.overrideAss = !sel.overrideAss
        if (sel.overrideAss) { sel.selectiveAss = false; sel.forceAll = false }
        sync()
    }
    view.presetSelectiveRow.setOnClickListener {
        sel.selectiveAss = !sel.selectiveAss
        if (sel.selectiveAss) { sel.overrideAss = false; sel.forceAll = false }
        sync()
    }
    view.presetForceAllRow.setOnClickListener {
        sel.forceAll = !sel.forceAll
        if (sel.forceAll) { sel.overrideAss = false; sel.selectiveAss = false }
        sync()
    }
}
