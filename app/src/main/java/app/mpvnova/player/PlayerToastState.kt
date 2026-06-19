package app.mpvnova.player

internal data class PlayerToastState(
    val title: String?,
    val detail: String,
    val hideAtMs: Long,
    val token: Int,
)
