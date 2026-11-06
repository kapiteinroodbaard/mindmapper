package io.github.wlenaerts.mindmapper.ui.presentation

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The count-up timer of presentation mode. Elapsed time is read from the monotonic [clock] (milliseconds)
 * instead of counting ticks, so it stays correct when frames are skipped or the screen is recreated.
 */
class PresentationTimer(private val clock: () -> Long = SystemClock::elapsedRealtime) {

    /** [accumulatedMillis] ran before the current run; [runningSince] is the clock at its start, or null when paused. */
    data class State(val accumulatedMillis: Long = 0, val runningSince: Long? = null) {
        val running: Boolean get() = runningSince != null
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun elapsedMillis(): Long = _state.value.let { s -> s.accumulatedMillis + (s.runningSince?.let { clock() - it } ?: 0) }

    /** Starts the timer when paused and pauses it when running. */
    fun toggle() {
        val now = clock()
        _state.value = _state.value.let { s ->
            if (s.runningSince == null) s.copy(runningSince = now) else State(s.accumulatedMillis + now - s.runningSince)
        }
    }

    /** Back to 0:00, paused. */
    fun reset() {
        _state.value = State()
    }
}

/** Formats elapsed time as `m:ss`, or `h:mm:ss` from one hour on. */
fun formatElapsed(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
