package io.github.wlenaerts.mindmapper.document

import android.content.Context
import io.github.wlenaerts.mindmapper.core.recent.RecentFiles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persists the recent-files list in private preferences and exposes it as a flow. */
class RecentFilesStore(context: Context) {
    private val prefs = context.getSharedPreferences("recent_files", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(RecentFiles.decode(prefs.getString(KEY, null)))

    val files: StateFlow<RecentFiles> = state.asStateFlow()

    fun touch(uri: String, name: String) = update { it.touch(uri, name, System.currentTimeMillis()) }

    fun setPinned(uri: String, pinned: Boolean) = update { it.setPinned(uri, pinned) }

    fun remove(uri: String) = update { it.remove(uri) }

    @Synchronized
    private fun update(change: (RecentFiles) -> RecentFiles) {
        val next = change(state.value)
        state.value = next
        prefs.edit().putString(KEY, next.encode()).apply()
    }

    private companion object {
        const val KEY = "entries"
    }
}

/** App-wide preferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var autosave: Boolean
        get() = prefs.getBoolean("autosave", true)
        set(value) = prefs.edit().putBoolean("autosave", value).apply()
}
