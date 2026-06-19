package app.mpvnova.player

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.io.File

private val SUBTITLE_NAME_UNSAFE = Regex("[^A-Za-z0-9._-]")

// Nuvio and the like hand subtitles over as content:// URIs from their own cache, which
// mpv can't open. Copy them locally while we still hold the read grant and relay file URIs.
@Suppress("DEPRECATION")
internal fun Activity.materializeContentSubtitles(source: Intent, target: Intent) {
    val subs = source.getParcelableArrayExtra("subs")?.filterIsInstance<Uri>().orEmpty()
    if (subs.none { it.scheme == "content" }) return
    clearExternalSubtitleCache()
    val rewritten = HashMap<Uri, Uri>()
    val localSubs = subs.mapIndexed { index, uri ->
        if (uri.scheme == "content") copyContentSubtitle(index, uri)?.also { rewritten[uri] = it } ?: uri
        else uri
    }
    target.putExtra("subs", localSubs.toTypedArray())
    val enable = source.getParcelableArrayExtra("subs.enable")?.filterIsInstance<Uri>().orEmpty()
    if (enable.isNotEmpty())
        target.putExtra("subs.enable", enable.map { rewritten[it] ?: it }.toTypedArray())
}

private fun Activity.copyContentSubtitle(index: Int, uri: Uri): Uri? = runCatching {
    val dest = File(externalSubtitleCacheDir(), "${index}_${subtitleCacheName(uri)}")
    contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    } ?: return@runCatching null
    Uri.fromFile(dest)
}.getOrNull()

private fun Activity.externalSubtitleCacheDir(): File = File(cacheDir, "external_subs").apply { mkdirs() }

private fun Activity.clearExternalSubtitleCache() {
    externalSubtitleCacheDir().listFiles()?.forEach { it.delete() }
}

private fun subtitleCacheName(uri: Uri): String {
    val raw = (uri.lastPathSegment?.substringAfterLast('/') ?: "").ifBlank { "subtitle" }
    val cleaned = raw.replace(SUBTITLE_NAME_UNSAFE, "_")
    return if (cleaned.contains('.')) cleaned else "$cleaned.srt"
}
