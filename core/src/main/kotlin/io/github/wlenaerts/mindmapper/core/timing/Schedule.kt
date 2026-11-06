package io.github.wlenaerts.mindmapper.core.timing

import io.github.wlenaerts.mindmapper.core.model.MindMap

/** The time slot of main branch [topicId], from [startSeconds] (inclusive) to [endSeconds] (exclusive). */
data class Slot(val topicId: String, val startSeconds: Int, val endSeconds: Int)

/**
 * The presentation schedule: consecutive [slots] for the main branches with a planned duration, in the
 * order of the main branches, starting at 0. [totalSeconds] is the end of the last slot.
 */
data class Schedule(val slots: List<Slot>) {
    val totalSeconds: Int get() = slots.lastOrNull()?.endSeconds ?: 0

    val isEmpty: Boolean get() = slots.isEmpty()

    /** The main branch scheduled at [elapsedSeconds], or null before 0 or at and beyond the total. */
    fun scheduledAt(elapsedSeconds: Long): String? =
        slots.firstOrNull { elapsedSeconds >= it.startSeconds && elapsedSeconds < it.endSeconds }?.topicId
}

fun MindMap.schedule(): Schedule {
    var start = 0
    val slots = root.children.mapNotNull { main ->
        main.durationSeconds?.takeIf { it > 0 }?.let { duration ->
            Slot(main.id, start, start + duration).also { start += duration }
        }
    }
    return Schedule(slots)
}
