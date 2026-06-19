package app.mpvnova.player

internal fun MPVActivity.persistedAudioFilterEnabled(active: Boolean): Boolean {
    return persistAudioFilters && active
}

internal fun MPVActivity.persistedAudioFilterLevel(level: Int): Int {
    return if (persistAudioFilters) level else 0
}

internal fun MPVActivity.persistedSubFilterLevel(level: Int, defaultLevel: Int): Int {
    return if (persistSubFilters) level else defaultLevel
}

internal fun MPVActivity.persistedSubPositionLevel(level: Int, defaultPosition: Int): Int {
    return if (persistSubFilters) level else defaultPosition
}
