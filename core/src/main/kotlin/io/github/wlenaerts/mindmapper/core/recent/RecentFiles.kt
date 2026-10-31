package io.github.wlenaerts.mindmapper.core.recent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RecentFile(val uri: String, val name: String, val lastUsed: Long, val pinned: Boolean = false)

/** The recent-files list: pinned entries first, then the rest by last use, at most [limit] unpinned. */
data class RecentFiles(val entries: List<RecentFile> = emptyList(), val limit: Int = DEFAULT_LIMIT) {

    /** Entries in display order. */
    val ordered: List<RecentFile>
        get() = entries.filter { it.pinned }.sortedByDescending { it.lastUsed } +
            entries.filterNot { it.pinned }.sortedByDescending { it.lastUsed }

    /** Records that [uri] was opened or saved; keeps its pinned state and drops the oldest unpinned overflow. */
    fun touch(uri: String, name: String, now: Long): RecentFiles {
        val pinned = entries.firstOrNull { it.uri == uri }?.pinned ?: false
        val updated = entries.filterNot { it.uri == uri } + RecentFile(uri, name, now, pinned)
        val unpinned = updated.filterNot { it.pinned }.sortedByDescending { it.lastUsed }
        val dropped = unpinned.drop(limit).map { it.uri }.toSet()
        return copy(entries = updated.filterNot { it.uri in dropped })
    }

    fun setPinned(uri: String, pinned: Boolean): RecentFiles =
        copy(entries = entries.map { if (it.uri == uri) it.copy(pinned = pinned) else it }).let {
            // Unpinning may push the list over the limit.
            if (pinned) it else it.trim()
        }

    fun remove(uri: String): RecentFiles = copy(entries = entries.filterNot { it.uri == uri })

    private fun trim(): RecentFiles {
        val dropped = entries.filterNot { it.pinned }.sortedByDescending { it.lastUsed }.drop(limit).map { it.uri }.toSet()
        return copy(entries = entries.filterNot { it.uri in dropped })
    }

    fun encode(): String = json.encodeToString(entries)

    companion object {
        const val DEFAULT_LIMIT = 15
        private val json = Json { ignoreUnknownKeys = true }

        fun decode(text: String?): RecentFiles =
            if (text.isNullOrBlank()) RecentFiles() else runCatching { RecentFiles(json.decodeFromString<List<RecentFile>>(text)) }.getOrDefault(RecentFiles())
    }
}
