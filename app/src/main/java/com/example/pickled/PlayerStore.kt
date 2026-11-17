package com.example.pickled

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/** Persists the roster and session settings in SharedPreferences. */
class PlayerStore(context: Context) {

    private val prefs = context.getSharedPreferences("pickled", Context.MODE_PRIVATE)

    fun loadRoster(): List<String> {
        val json = prefs.getString(KEY_ROSTER, null) ?: return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { array.getString(it) }
    }

    fun loadActive(): Set<String> = prefs.getStringSet(KEY_ACTIVE, null)?.toSet() ?: emptySet()

    fun loadMode(): GameMode =
        prefs.getString(KEY_MODE, null)?.let { runCatching { GameMode.valueOf(it) }.getOrNull() }
            ?: GameMode.DOUBLES

    fun save(roster: List<String>, active: Set<String>, mode: GameMode) {
        prefs.edit {
            putString(KEY_ROSTER, JSONArray(roster).toString())
            putStringSet(KEY_ACTIVE, active)
            putString(KEY_MODE, mode.name)
        }
    }

    private companion object {
        const val KEY_ROSTER = "roster"
        const val KEY_ACTIVE = "active"
        const val KEY_MODE = "mode"
    }
}
