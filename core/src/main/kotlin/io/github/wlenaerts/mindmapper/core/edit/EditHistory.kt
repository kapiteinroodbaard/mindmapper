package io.github.wlenaerts.mindmapper.core.edit

import io.github.wlenaerts.mindmapper.core.model.MindMap

/** Undo/redo as a list of immutable snapshots; a new change after undo discards the redo part. */
class EditHistory(initial: MindMap, private val limit: Int = 100) {
    private val states = ArrayList<MindMap>().apply { add(initial) }
    private var index = 0

    val current: MindMap get() = states[index]
    val canUndo: Boolean get() = index > 0
    val canRedo: Boolean get() = index < states.size - 1

    /** Records [map] as the new current state; no-op when it equals the current state. */
    fun push(map: MindMap) {
        if (map == current) return
        while (states.size > index + 1) states.removeAt(states.size - 1)
        states += map
        if (states.size > limit + 1) states.removeAt(0)
        index = states.size - 1
    }

    fun undo(): MindMap {
        if (canUndo) index--
        return current
    }

    fun redo(): MindMap {
        if (canRedo) index++
        return current
    }
}
