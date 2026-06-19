package app.mpvnova.player

import androidx.recyclerview.widget.RecyclerView

internal fun RecyclerView.Adapter<*>.notifyRangeRefresh(oldCount: Int, newCount: Int) {
    val changedCount = minOf(oldCount, newCount)
    if (changedCount > 0) {
        notifyItemRangeChanged(0, changedCount)
    }
    if (newCount > oldCount) {
        notifyItemRangeInserted(oldCount, newCount - oldCount)
    } else if (oldCount > newCount) {
        notifyItemRangeRemoved(newCount, oldCount - newCount)
    }
}
