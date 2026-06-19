package app.mpvnova.player

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

// Rough Canvas stand-in for how libass will render the subs. Close, not exact.
internal class SubtitleStylePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    data class Spec(
        val text: String,
        val textColor: Int,
        val outlineColor: Int,
        val outlineWidthPx: Float,
        val backgroundColor: Int,
        val shadowColor: Int,
        val shadowRadiusPx: Float,
        val shadowOffsetPx: Float,
        val blurRadiusPx: Float,
        val letterSpacingEm: Float,
        val typeface: Typeface?,
    )

    private var spec: Spec? = null
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        textAlign = Paint.Align.CENTER
        // Round joins/caps so thick outlines don't spike at sharp corners (W, M).
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBounds = Rect()

    init {
        // setShadowLayer needs a software layer to render.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setSpec(spec: Spec) {
        this.spec = spec
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = spec ?: return
        val text = s.text.ifBlank { return }

        val textSize = height * TEXT_HEIGHT_RATIO
        applyTextSize(textSize, s.typeface)
        applyBlur(s.blurRadiusPx)
        fillPaint.letterSpacing = s.letterSpacingEm
        strokePaint.letterSpacing = s.letterSpacingEm

        val centerX = width / 2f
        val baseline = height / 2f - (fillPaint.descent() + fillPaint.ascent()) / 2f
        val textWidth = fillPaint.measureText(text)

        drawBackgroundBox(canvas, s, text, centerX, baseline, textWidth, textSize)
        drawOutline(canvas, s, text, centerX, baseline)
        drawFill(canvas, s, text, centerX, baseline)
    }

    private fun applyTextSize(textSize: Float, typeface: Typeface?) {
        val face = typeface ?: Typeface.DEFAULT
        fillPaint.textSize = textSize
        fillPaint.typeface = face
        strokePaint.textSize = textSize
        strokePaint.typeface = face
    }

    private fun applyBlur(blurRadiusPx: Float) {
        val filter = if (blurRadiusPx > 0f) {
            BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        } else {
            null
        }
        fillPaint.maskFilter = filter
        strokePaint.maskFilter = filter
    }

    private fun drawBackgroundBox(
        canvas: Canvas,
        s: Spec,
        text: String,
        centerX: Float,
        baseline: Float,
        textWidth: Float,
        textSize: Float,
    ) {
        if (Color.alpha(s.backgroundColor) == 0) return
        boxPaint.color = s.backgroundColor
        fillPaint.getTextBounds(text, 0, text.length, textBounds)
        val padX = textSize * BOX_PAD_X_RATIO
        val padY = textSize * BOX_PAD_Y_RATIO
        val rect = RectF(
            centerX - textWidth / 2f - padX,
            baseline + fillPaint.ascent() - padY,
            centerX + textWidth / 2f + padX,
            baseline + fillPaint.descent() + padY,
        )
        val radius = textSize * BOX_CORNER_RATIO
        canvas.drawRoundRect(rect, radius, radius, boxPaint)
    }

    private fun drawOutline(canvas: Canvas, s: Spec, text: String, centerX: Float, baseline: Float) {
        if (s.outlineWidthPx <= 0f || Color.alpha(s.outlineColor) == 0) return
        strokePaint.color = s.outlineColor
        strokePaint.strokeWidth = s.outlineWidthPx * 2f
        strokePaint.clearShadowLayer()
        canvas.drawText(text, centerX, baseline, strokePaint)
    }

    private fun drawFill(canvas: Canvas, s: Spec, text: String, centerX: Float, baseline: Float) {
        if (s.shadowRadiusPx > 0f) {
            fillPaint.setShadowLayer(s.shadowRadiusPx, 0f, s.shadowOffsetPx, s.shadowColor)
        } else {
            fillPaint.clearShadowLayer()
        }
        fillPaint.color = s.textColor
        canvas.drawText(text, centerX, baseline, fillPaint)
    }

    companion object {
        private const val TEXT_HEIGHT_RATIO = 0.46f
        private const val BOX_PAD_X_RATIO = 0.4f
        private const val BOX_PAD_Y_RATIO = 0.22f
        private const val BOX_CORNER_RATIO = 0.14f
    }
}
