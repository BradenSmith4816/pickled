package com.braden.pickleplayers

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/** Persists the roster and who's checked in, in SharedPreferences. */
class PlayerStore(context: Context) {

    private val prefs = context.getSharedPreferences("pickled", Context.MODE_PRIVATE)

    fun loadRoster(): List<String> {
        val json = prefs.getString(KEY_ROSTER, null) ?: return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { array.getString(it) }
    }

    fun loadActive(): Set<String> = prefs.getStringSet(KEY_ACTIVE, null)?.toSet() ?: emptySet()

    fun save(roster: List<String>, active: Set<String>) {
        prefs.edit {
            putString(KEY_ROSTER, JSONArray(roster).toString())
            putStringSet(KEY_ACTIVE, active)
        }
    }

    private companion object {
        const val KEY_ROSTER = "roster"
        const val KEY_ACTIVE = "active"
    }
}
