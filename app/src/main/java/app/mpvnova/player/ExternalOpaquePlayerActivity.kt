package app.mpvnova.player

// Opaque player variant for external callers (Nuvio and everyone except Stremio) that auto-next
// off the full stop->restart lifecycle: an opaque window stops the caller. Standard launch mode
// (unlike the internal singleTask MPVActivity) so it stays in the caller's task and the result
// flows back through the trampoline. Behaviourally identical to MPVActivity; only the manifest
// declaration differs.
class ExternalOpaquePlayerActivity : MPVActivity()
