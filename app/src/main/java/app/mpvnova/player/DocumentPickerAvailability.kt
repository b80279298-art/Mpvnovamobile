package app.mpvnova.player

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

internal fun hasDocumentTreePicker(context: Context): Boolean {
    return canResolveDocumentIntent(context, Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
}

internal fun hasDocumentFilePicker(context: Context): Boolean {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType("*/*")
    return canResolveDocumentIntent(context, intent)
}

private fun canResolveDocumentIntent(context: Context, intent: Intent): Boolean {
    val packageManager = context.packageManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        ).isNotEmpty()
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()
    }
}
