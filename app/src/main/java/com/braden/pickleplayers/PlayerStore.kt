package com.braden.pickleplayers

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray

/** Persists the player list in SharedPreferences. */
class PlayerStore(context: Context) {

    private val prefs = context.getSharedPreferences("pickled", Context.MODE_PRIVATE)

    fun loadRoster(): List<String> {
        val json = prefs.getString(KEY_ROSTER, null) ?: return emptyList()
        val array = JSONArray(json)
        return List(array.length()) { array.getString(it) }
    }

    fun save(roster: List<String>) {
        prefs.edit {
            putString(KEY_ROSTER, JSONArray(roster).toString())
            remove(KEY_LEGACY_ACTIVE) // "checked in" set from v1.1 and earlier; no longer used
        }
    }

    private companion object {
        const val KEY_ROSTER = "roster"
        const val KEY_LEGACY_ACTIVE = "active"
    }
}
