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

/** One court: its game type and `mode.perCourt` slots. */
data class Court(
    val mode: GameMode = GameMode.DOUBLES,
    val slots: List<Placement?> = List(mode.perCourt) { null },
) {
    fun teamA(): IntRange = 0 until mode.perSide
    fun teamB(): IntRange = mode.perSide until mode.perCourt
}

typealias Courts = List<Court>

/** Pure functions for court assignment. Kept free of Android so it is easy to unit test. */
object CourtAssigner {

    fun empty(courtCount: Int): Courts = List(courtCount) { Court() }

    /** Takes everyone off every court, keeping each court's game type. */
    fun clear(courts: Courts): Courts = courts.map { Court(it.mode) }

    fun placedNames(courts: Courts): Set<String> =
        courts.flatMap { it.slots }.mapNotNullTo(mutableSetOf()) { it?.name }

    fun find(courts: Courts, name: String): Slot? {
        courts.forEachIndexed { c, court ->
            court.slots.forEachIndexed { i, p -> if (p?.name == name) return Slot(c, i) }
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
        val slots = courts.map { court -> court.slots.map { p -> p?.takeIf { it.pinned } }.toMutableList() }
        val placed = slots.flatten().mapNotNullTo(mutableSetOf()) { it?.name }
        val pool = players.filter { it !in placed }.shuffled(random).toMutableList()

        val courtOrder = slots.indices.sortedByDescending { c -> slots[c].count { it != null } }
        loop@ for (c in courtOrder) {
            for (i in fillOrder(courts[c].mode)) {
                if (pool.isEmpty()) break@loop
                if (slots[c][i] == null) slots[c][i] = Placement(pool.removeAt(0), pinned = false)
            }
        }
        return courts.mapIndexed { c, court -> court.copy(slots = slots[c]) }
    }

    /**
     * Moves [name] to [target] (pinning it there), or back to the waiting list when
     * [target] is null. If the target is occupied: when dragged from another slot the
     * two players swap; when dragged from the waiting list the occupant goes to waiting.
     */
    fun place(courts: Courts, name: String, target: Slot?): Courts {
        val from = find(courts, name)
        val slots = courts.map { it.slots.toMutableList() }
        if (target == null) {
            from?.let { slots[it.court][it.index] = null }
        } else {
            val occupant = slots[target.court][target.index]
            if (from != null && from != target) {
                slots[from.court][from.index] = occupant?.copy(pinned = true)
            }
            slots[target.court][target.index] = Placement(name, pinned = true)
        }
        return courts.mapIndexed { c, court -> court.copy(slots = slots[c]) }
    }

    fun unpin(courts: Courts, name: String): Courts = mapSlots(courts) { p ->
        if (p?.name == name) p.copy(pinned = false) else p
    }

    fun remove(courts: Courts, name: String): Courts = mapSlots(courts) { p ->
        p?.takeUnless { it.name == name }
    }

    /**
     * Switches one court between doubles and singles. Each side keeps its first
     * player; a doubles partner who no longer fits goes back to waiting.
     */
    fun setMode(courts: Courts, courtIndex: Int, mode: GameMode): Courts {
        val old = courts[courtIndex]
        if (old.mode == mode) return courts
        val new = Court(mode).let { court ->
            val slots = court.slots.toMutableList()
            val keep = minOf(old.mode.perSide, mode.perSide)
            for (i in 0 until keep) {
                slots[court.teamA().first + i] = old.slots[old.teamA().first + i]
                slots[court.teamB().first + i] = old.slots[old.teamB().first + i]
            }
            court.copy(slots = slots)
        }
        return courts.mapIndexed { c, court -> if (c == courtIndex) new else court }
    }

    /** Adds empty doubles courts or drops trailing ones (their players go back to waiting). */
    fun resize(courts: Courts, newCount: Int): Courts =
        if (newCount <= courts.size) courts.take(newCount)
        else courts + empty(newCount - courts.size)

    private fun mapSlots(courts: Courts, transform: (Placement?) -> Placement?): Courts =
        courts.map { court -> court.copy(slots = court.slots.map(transform)) }

    private fun fillOrder(mode: GameMode): List<Int> =
        (0 until mode.perSide).flatMap { listOf(it, mode.perSide + it) }
}
