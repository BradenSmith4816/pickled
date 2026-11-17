package com.example.pickled

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CourtAssignerTest {

    private val players = (1..10).map { "P$it" }

    @Test
    fun roll_fillsCourtsWithUniquePlayers() {
        val courts = CourtAssigner.roll(CourtAssigner.empty(2, GameMode.DOUBLES), players, Random(1))
        val placed = courts.flatten().filterNotNull().map { it.name }
        assertEquals(8, placed.size)
        assertEquals(8, placed.toSet().size)
    }

    @Test
    fun roll_keepsPinnedPairsAndFillsTheirCourtFirst() {
        var courts = CourtAssigner.empty(3, GameMode.DOUBLES)
        courts = CourtAssigner.place(courts, "P1", Slot(2, 0))
        courts = CourtAssigner.place(courts, "P2", Slot(2, 1))

        val rolled = CourtAssigner.roll(courts, players.take(6), Random(7))

        assertEquals("P1", rolled[2][0]?.name)
        assertEquals("P2", rolled[2][1]?.name)
        assertTrue(rolled[2].all { it != null }) // pinned court got opponents
    }

    @Test
    fun roll_partialDoublesCourtBecomesSingles() {
        val rolled = CourtAssigner.roll(CourtAssigner.empty(1, GameMode.DOUBLES), listOf("A", "B"), Random(3))
        // Slots 0 (Team A) and 2 (Team B) are filled first.
        assertTrue(rolled[0][0] != null && rolled[0][2] != null)
    }

    @Test
    fun place_swapsWhenMovingBetweenSlots() {
        var courts = CourtAssigner.empty(1, GameMode.SINGLES)
        courts = CourtAssigner.place(courts, "A", Slot(0, 0))
        courts = CourtAssigner.place(courts, "B", Slot(0, 1))
        courts = CourtAssigner.place(courts, "A", Slot(0, 1))
        assertEquals("B", courts[0][0]?.name)
        assertEquals("A", courts[0][1]?.name)
    }

    @Test
    fun place_nullTargetUnassigns() {
        var courts = CourtAssigner.place(CourtAssigner.empty(1, GameMode.SINGLES), "A", Slot(0, 0))
        courts = CourtAssigner.place(courts, "A", null)
        assertNull(courts[0][0])
    }
}
