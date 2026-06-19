package app.mpvnova.player

// Translucent player variant for callers (e.g. Stremio) whose result / auto-next handling
// breaks when their activity is stopped — a translucent window keeps the caller merely paused
// so its registerForActivityResult callback stays alive. Behaviourally identical to
// MPVActivity; the translucent window flag is merged on top of the color theme in onCreate.
// The trampoline picks this vs ExternalOpaquePlayerActivity by caller package.
class TranslucentMPVActivity : MPVActivity() {
    override val useTranslucentPlayerWindow: Boolean get() = true
}
