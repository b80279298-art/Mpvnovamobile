package app.mpvnova.player

import kotlin.math.abs

private val LEGACY_SUB_SCALE_STEPS = doubleArrayOf(
    0.5,
    0.65,
    0.8,
    DEFAULT_SUB_SCALE,
    1.15,
    1.3,
    1.5,
    1.75,
    2.0,
)

internal fun buildSubScaleSteps(): DoubleArray =
    (SUB_SCALE_MIN_HUNDREDTHS..SUB_SCALE_MAX_HUNDREDTHS step SUB_SCALE_STEP_HUNDREDTHS)
        .map { hundredths -> hundredths / PERCENT_SCALE_DOUBLE }
        .toDoubleArray()

internal fun nearestSubScaleIndex(scale: Double): Int =
    SUB_SCALE_STEPS.indices.minBy { index -> abs(SUB_SCALE_STEPS[index] - scale) }

internal fun migrateSubScaleLevel(level: Int, version: Int): Int {
    if (version >= SUB_SCALE_STEPS_VERSION)
        return level.coerceIn(0, SUB_SCALE_STEPS.lastIndex)
    val legacyScale = LEGACY_SUB_SCALE_STEPS.getOrNull(level) ?: DEFAULT_SUB_SCALE
    return nearestSubScaleIndex(legacyScale)
}
