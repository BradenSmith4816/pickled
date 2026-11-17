package com.example.pickled

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CourtAssignerTest {

    private val players = (1..10).map { "P$it" }

    private fun Courts.names() = flatMap { it.slots }.filterNotNull().map { it.name }

    @Test
    fun courtsDefaultToDoubles() {
        assertTrue(CourtAssigner.empty(2).all { it.mode == GameMode.DOUBLES && it.slots.size == 4 })
    }

    @Test
    fun roll_fillsCourtsWithUniquePlayers() {
        val courts = CourtAssigner.roll(CourtAssigner.empty(2), players, Random(1))
        assertEquals(8, courts.names().size)
        assertEquals(8, courts.names().toSet().size)
    }

    @Test
    fun roll_mixedSinglesAndDoubles() {
        val courts = CourtAssigner.setMode(CourtAssigner.empty(2), 1, GameMode.SINGLES)
        val rolled = CourtAssigner.roll(courts, players, Random(2))
        assertEquals(4, rolled[0].slots.count { it != null })
        assertEquals(2, rolled[1].slots.count { it != null })
        assertEquals(6, rolled.names().toSet().size)
    }

    @Test
    fun roll_keepsPinnedPairsAndFillsTheirCourtFirst() {
        var courts = CourtAssigner.empty(3)
        courts = CourtAssigner.place(courts, "P1", Slot(2, 0))
        courts = CourtAssigner.place(courts, "P2", Slot(2, 1))

        val rolled = CourtAssigner.roll(courts, players.take(6), Random(7))

        assertEquals("P1", rolled[2].slots[0]?.name)
        assertEquals("P2", rolled[2].slots[1]?.name)
        assertTrue(rolled[2].slots.all { it != null }) // pinned court got opponents
    }

    @Test
    fun roll_partialDoublesCourtBecomesSingles() {
        val rolled = CourtAssigner.roll(CourtAssigner.empty(1), listOf("A", "B"), Random(3))
        // Slots 0 (Team A) and 2 (Team B) are filled first.
        assertTrue(rolled[0].slots[0] != null && rolled[0].slots[2] != null)
    }

    @Test
    fun setMode_doublesToSinglesKeepsFirstPlayerOfEachSide() {
        var courts = CourtAssigner.roll(CourtAssigner.empty(1), listOf("A", "B", "C", "D"), Random(4))
        val a1 = courts[0].slots[0]
        val b1 = courts[0].slots[2]
        courts = CourtAssigner.setMode(courts, 0, GameMode.SINGLES)
        assertEquals(listOf(a1, b1), courts[0].slots)
    }

    @Test
    fun clear_keepsCourtModes() {
        var courts = CourtAssigner.setMode(CourtAssigner.empty(2), 0, GameMode.SINGLES)
        courts = CourtAssigner.clear(CourtAssigner.roll(courts, players, Random(5)))
        assertEquals(GameMode.SINGLES, courts[0].mode)
        assertTrue(courts.names().isEmpty())
    }

    @Test
    fun place_swapsWhenMovingBetweenSlots() {
        var courts = CourtAssigner.setMode(CourtAssigner.empty(1), 0, GameMode.SINGLES)
        courts = CourtAssigner.place(courts, "A", Slot(0, 0))
        courts = CourtAssigner.place(courts, "B", Slot(0, 1))
        courts = CourtAssigner.place(courts, "A", Slot(0, 1))
        assertEquals("B", courts[0].slots[0]?.name)
        assertEquals("A", courts[0].slots[1]?.name)
    }

    @Test
    fun unpin_keepsPlayerInSlot() {
        var courts = CourtAssigner.place(CourtAssigner.empty(1), "A", Slot(0, 0))
        courts = CourtAssigner.unpin(courts, "A")
        assertEquals(Placement("A", pinned = false), courts[0].slots[0])
    }

    @Test
    fun place_nullTargetUnassigns() {
        var courts = CourtAssigner.place(CourtAssigner.empty(1), "A", Slot(0, 0))
        courts = CourtAssigner.place(courts, "A", null)
        assertNull(courts[0].slots[0])
    }
}
