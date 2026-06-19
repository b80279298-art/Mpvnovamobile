package app.mpvnova.player

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import app.mpvnova.player.databinding.DialogSubStyleSavePresetBinding
import java.util.Locale

private const val PRESET_LAYOUT_DISABLED_ALPHA = 0.56f
private const val PRESET_LAYOUT_DISABLED_BUTTON_ALPHA = 0.38f

internal class SubtitlePresetLayoutController(
    private val activity: MPVActivity,
    private val view: DialogSubStyleSavePresetBinding,
    editing: SubtitleStylePreset?,
) {
    var includeLayout = editing?.includeLayout ?: false
        private set

    var scaleLevel = editing
        ?.takeIf { it.includeLayout }
        ?.scaleLevel
        ?.coerceIn(0, activity.subScaleSteps.lastIndex)
        ?: activity.subScaleLevel
        private set

    private var posLevel = editing
        ?.takeIf { it.includeLayout }
        ?.let { nearestSubPositionIndex(activity.subPosSteps, it.posPct) }
        ?: activity.subPosLevel

    val posPct: Int
        get() = activity.subPosSteps[posLevel]

    private val originalScaleLevel = activity.subScaleLevel
    private val originalPosLevel = activity.subPosLevel

    fun bind() {
        view.presetIncludeLayoutRow.setOnClickListener { toggleIncludeLayout() }
        view.presetSubScaleMinusBtn.setOnClickListener { adjustLayout(scaleDelta = -1) }
        view.presetSubScalePlusBtn.setOnClickListener { adjustLayout(scaleDelta = 1) }
        view.presetSubPosMinusBtn.setOnClickListener { adjustLayout(positionDelta = -1) }
        view.presetSubPosPlusBtn.setOnClickListener { adjustLayout(positionDelta = 1) }
        sync()
    }

    fun accept() {
        if (includeLayout) {
            applyPreview()
            activity.writeSettings()
        } else {
            restore()
        }
    }

    fun restore() {
        activity.subScaleLevel = originalScaleLevel
        activity.subPosLevel = originalPosLevel
        activity.applySubScaleProperty()
        activity.applySubPosProperty()
    }

    private fun toggleIncludeLayout() {
        includeLayout = !includeLayout
        if (includeLayout) applyPreview() else restore()
        sync()
    }

    private fun adjustLayout(scaleDelta: Int = 0, positionDelta: Int = 0) {
        includeLayout = true
        scaleLevel = (scaleLevel + scaleDelta).coerceIn(0, activity.subScaleSteps.lastIndex)
        posLevel = (posLevel + positionDelta).coerceIn(0, activity.subPosSteps.lastIndex)
        applyPreview()
        sync()
    }

    private fun applyPreview() {
        activity.subScaleLevel = scaleLevel
        activity.subPosLevel = posLevel
        activity.applySubScaleProperty()
        activity.applySubPosProperty()
    }

    fun sync() {
        view.presetIncludeLayoutCheck.isVisible = includeLayout
        view.presetIncludeLayoutRow.isActivated = includeLayout
        syncValueRow(
            row = view.presetSubScaleRow,
            value = view.presetSubScaleValue,
            minus = view.presetSubScaleMinusBtn,
            plus = view.presetSubScalePlusBtn,
            enabled = includeLayout,
            canDecrease = scaleLevel > 0,
            canIncrease = scaleLevel < activity.subScaleSteps.lastIndex,
            label = scaleLabel(),
        )
        syncValueRow(
            row = view.presetSubPosRow,
            value = view.presetSubPosValue,
            minus = view.presetSubPosMinusBtn,
            plus = view.presetSubPosPlusBtn,
            enabled = includeLayout,
            canDecrease = posLevel > 0,
            canIncrease = posLevel < activity.subPosSteps.lastIndex,
            label = positionLabel(),
        )
    }

    private fun scaleLabel(): String {
        val scale = activity.subScaleSteps[scaleLevel.coerceIn(0, activity.subScaleSteps.lastIndex)]
        return if (scale == DEFAULT_SUB_SCALE) {
            activity.getString(R.string.sub_scale_default)
        } else {
            String.format(Locale.US, "%.2fx", scale)
        }
    }

    private fun positionLabel(): String {
        val position = activity.subPosSteps[posLevel.coerceIn(0, activity.subPosSteps.lastIndex)]
        return if (position == DEFAULT_SUB_POSITION_PERCENT) {
            activity.getString(R.string.sub_pos_default)
        } else {
            "$position%"
        }
    }

    private fun syncValueRow(
        row: View,
        value: TextView,
        minus: ImageButton,
        plus: ImageButton,
        enabled: Boolean,
        canDecrease: Boolean,
        canIncrease: Boolean,
        label: String,
    ) {
        row.alpha = if (enabled) 1f else PRESET_LAYOUT_DISABLED_ALPHA
        value.text = label
        syncButton(minus, enabled && canDecrease)
        syncButton(plus, enabled && canIncrease)
    }

    private fun syncButton(button: ImageButton, enabled: Boolean) {
        button.isEnabled = true
        button.isClickable = enabled
        button.isFocusable = true
        button.alpha = if (enabled) 1f else PRESET_LAYOUT_DISABLED_BUTTON_ALPHA
        button.imageTintList = ColorStateList.valueOf(
            ContextCompat.getColor(
                activity,
                if (enabled) R.color.tv_text else R.color.tint_disabled,
            ),
        )
    }
}
