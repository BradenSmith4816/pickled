package com.example.pickled

import kotlin.random.Random

enum class GameMode(val perSide: Int, val label: String) {
    DOUBLES(perSide = 2, label = "Doubles"),
    SINGLES(perSide = 1, label = "Singles");

    val perCourt: Int get() = perSide * 2
}

/**
 * A position on a court. Indices `0 until perSide` are Team A, the rest are Team B.
 */
data class Slot(val court: Int, val index: Int)

/**
 * A player sitting in a slot. [pinned] players were placed by hand (drag & drop)
 * and survive a re-roll; non-pinned players were placed randomly.
 */
data class Placement(val name: String, val pinned: Boolean)

/** One entry per court, each holding `mode.perCourt` slots. */
typealias Courts = List<List<Placement?>>

/** Pure functions for court assignment. Kept free of Android so it is easy to unit test. */
object CourtAssigner {

    fun empty(courtCount: Int, mode: GameMode): Courts =
        List(courtCount) { List(mode.perCourt) { null } }

    fun placedNames(courts: Courts): Set<String> =
        courts.flatten().mapNotNullTo(mutableSetOf()) { it?.name }

    fun find(courts: Courts, name: String): Slot? {
        courts.forEachIndexed { c, court ->
            court.forEachIndexed { i, p -> if (p?.name == name) return Slot(c, i) }
        }
        return null
    }

    /**
     * Keeps every pinned player where they are, clears everyone else, and randomly
     * fills the open slots with the remaining [players]. Courts that already have
     * pinned players are filled first so hand-picked pairs always get opponents.
     * Within a court, slots fill alternating sides (A1, B1, A2, B2) so a partially
     * filled doubles court still produces a fair game (e.g. 1 v 1).
     */
    fun roll(courts: Courts, players: List<String>, random: Random = Random.Default): Courts {
        val result = courts.map { court -> court.map { p -> p?.takeIf { it.pinned } }.toMutableList() }
        val placed = placedNames(result)
        val pool = players.filter { it !in placed }.shuffled(random).toMutableList()

        val courtOrder = result.indices.sortedByDescending { c -> result[c].count { it != null } }
        for (c in courtOrder) {
            for (i in fillOrder(result[c].size)) {
                if (pool.isEmpty()) return result
                if (result[c][i] == null) result[c][i] = Placement(pool.removeAt(0), pinned = false)
            }
        }
        return result
    }

    /**
     * Moves [name] to [target] (pinning it there), or back to the waiting list when
     * [target] is null. If the target is occupied: when dragged from another slot the
     * two players swap; when dragged from the waiting list the occupant goes to waiting.
     */
    fun place(courts: Courts, name: String, target: Slot?): Courts {
        val from = find(courts, name)
        val result = courts.map { it.toMutableList() }
        if (target == null) {
            from?.let { result[it.court][it.index] = null }
            return result
        }
        val occupant = result[target.court][target.index]
        if (from != null && from != target) {
            result[from.court][from.index] = occupant?.copy(pinned = true)
        }
        result[target.court][target.index] = Placement(name, pinned = true)
        return result
    }

    fun remove(courts: Courts, name: String): Courts =
        courts.map { court -> court.map { p -> p?.takeUnless { it.name == name } } }

    /** Adds empty courts or drops trailing ones (their players go back to waiting). */
    fun resize(courts: Courts, newCount: Int, mode: GameMode): Courts =
        if (newCount <= courts.size) courts.take(newCount)
        else courts + empty(newCount - courts.size, mode)

    private fun fillOrder(slotCount: Int): List<Int> {
        val perSide = slotCount / 2
        return (0 until perSide).flatMap { listOf(it, perSide + it) }
    }
}
