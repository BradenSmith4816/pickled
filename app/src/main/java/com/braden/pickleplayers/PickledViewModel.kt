package com.braden.pickleplayers

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class PickledViewModel(app: Application) : AndroidViewModel(app) {

    private val store = PlayerStore(app)

    /** Everyone in the player list. Persists across sessions; all of them are available to roll. */
    var roster by mutableStateOf(store.loadRoster())
        private set

    var courts by mutableStateOf(CourtAssigner.empty(DEFAULT_COURTS))
        private set

    /** Players who aren't on a court: what the Players pane lists. */
    val offCourt: List<String>
        get() {
            val placed = CourtAssigner.placedNames(courts)
            return roster.filter { it !in placed }
        }

    fun addPlayer(raw: String) {
        val name = raw.trim()
        if (name.isEmpty() || roster.any { it.equals(name, ignoreCase = true) }) return
        roster = roster + name
        persist()
    }

    fun deletePlayer(name: String) {
        roster = roster - name
        courts = CourtAssigner.remove(courts, name)
        persist()
    }

    fun setCourtMode(courtIndex: Int, mode: GameMode) {
        courts = CourtAssigner.setMode(courts, courtIndex, mode)
    }

    fun addCourt() {
        if (courts.size < MAX_COURTS) courts = CourtAssigner.addCourt(courts)
    }

    /** Removes one court (never the last); its players go back to the Players pane. */
    fun removeCourt(index: Int) {
        if (courts.size > 1) courts = CourtAssigner.removeCourt(courts, index)
    }

    fun roll() {
        courts = CourtAssigner.roll(courts, roster)
    }

    fun reset() {
        courts = CourtAssigner.clear(courts)
    }

    /** Drag & drop result: [target] null means "back to the Players pane". */
    fun move(name: String, target: Slot?) {
        courts = CourtAssigner.place(courts, name, target)
    }

    /** Clears the hand-placed status but leaves the player on the court. */
    fun unpin(name: String) {
        courts = CourtAssigner.unpin(courts, name)
    }

    private fun persist() = store.save(roster)

    companion object {
        const val MAX_COURTS = 12
        const val DEFAULT_COURTS = 2
    }
}
