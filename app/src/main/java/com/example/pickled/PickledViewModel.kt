package com.example.pickled

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class PickledViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PlayerStore(app)

    /** Everyone ever added. Persists across sessions. */
    var roster by mutableStateOf(store.loadRoster())
        private set

    /** Players checked in for the current roll. */
    var active by mutableStateOf(store.loadActive())
        private set

    var courts by mutableStateOf(CourtAssigner.empty(DEFAULT_COURTS))
        private set

    val activePlayers: List<String> get() = roster.filter { it in active }

    /** Active players not currently on a court. */
    val waiting: List<String>
        get() {
            val placed = CourtAssigner.placedNames(courts)
            return activePlayers.filter { it !in placed }
        }

    fun addPlayer(raw: String) {
        val name = raw.trim()
        if (name.isEmpty()) return
        val existing = roster.firstOrNull { it.equals(name, ignoreCase = true) }
        if (existing != null) {
            active = active + existing
        } else {
            roster = roster + name
            active = active + name
        }
        persist()
    }

    fun deletePlayer(name: String) {
        roster = roster - name
        active = active - name
        courts = CourtAssigner.remove(courts, name)
        persist()
    }

    fun toggleActive(name: String) {
        if (name in active) {
            active = active - name
            courts = CourtAssigner.remove(courts, name)
        } else {
            active = active + name
        }
        persist()
    }

    fun setEveryoneActive(on: Boolean) {
        active = if (on) roster.toSet() else emptySet()
        if (!on) courts = CourtAssigner.clear(courts)
        persist()
    }

    fun setCourtMode(courtIndex: Int, mode: GameMode) {
        courts = CourtAssigner.setMode(courts, courtIndex, mode)
    }

    fun setCourtCount(count: Int) {
        courts = CourtAssigner.resize(courts, count.coerceIn(1, MAX_COURTS))
    }

    fun roll() {
        courts = CourtAssigner.roll(courts, activePlayers)
    }

    fun reset() {
        courts = CourtAssigner.clear(courts)
    }

    /** Drag & drop result: [target] null means "back to waiting". */
    fun move(name: String, target: Slot?) {
        courts = CourtAssigner.place(courts, name, target)
    }

    /** Clears the hand-placed status but leaves the player on the court. */
    fun unpin(name: String) {
        courts = CourtAssigner.unpin(courts, name)
    }

    private fun persist() = store.save(roster, active)

    companion object {
        const val MAX_COURTS = 12
        const val DEFAULT_COURTS = 2
    }
}
