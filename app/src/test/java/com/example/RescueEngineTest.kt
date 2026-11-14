package com.example

import com.example.rescue.Kind
import com.example.rescue.Outcome
import com.example.rescue.RescueEngine
import com.example.rescue.RescueLevel
import com.example.rescue.RescueState
import com.example.rescue.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RescueEngineTest {
    private fun count(s: RescueState, k: Kind) = s.grid.sumOf { row -> row.count { it?.kind == k } }

    @Test
    fun `layout has queen trunk and stable seeds`() {
        val s = RescueEngine.newState(RescueLevel.get(1), Random(1))
        assertEquals(8, count(s, Kind.QUEEN))
        assertEquals(4, count(s, Kind.TRUNK))
        assertEquals(9 + 8 + 7 + 6 + 5, count(s, Kind.SEED))
        assertTrue(RescueEngine.settleStep(s).isEmpty())
        assertTrue(RescueEngine.findRuns(s).isEmpty())
        assertTrue(RescueEngine.findHint(s) != null)
    }

    @Test
    fun `clearing the channel next to trunk frees the queen`() {
        val s = RescueEngine.newState(RescueLevel.get(1), Random(2))
        val steps = ArrayList<Step>()
        for (r in 9..11) steps += RescueEngine.useHammer(s, r, 3).toList()
        assertTrue(s.escaped)
        assertEquals(0, RescueEngine.seedsBesideTrunk(s))
        assertTrue(steps.any { it is Step.Drained })
        val end = steps.filterIsInstance<Step.Ended>().last()
        assertEquals(Outcome.WON, end.outcome)
        assertEquals(0, count(s, Kind.TRUNK))
        assertEquals(8, count(s, Kind.QUEEN))
    }

    @Test
    fun `heavy seeds hold until a neighbour is removed`() {
        val s = RescueEngine.newState(RescueLevel.get(2), Random(3))
        assertTrue(s.grid[8][3]!!.heavy && s.grid[7][3]!!.heavy)
        RescueEngine.useHammer(s, 9, 3).toList()        // يجاور (8,3) فقط
        assertFalse(s.grid[8][3]?.heavy ?: false)
    }

    @Test
    fun `heavy second stone needs the diagonal fruit`() {
        val s = RescueEngine.newState(RescueLevel.get(2), Random(4))
        RescueEngine.useHammer(s, 9, 3).toList()
        // بعد تفتّت (8,3) تسقط البذور؛ (7,3) ما زالت ثقيلة ولم يجاورها شيء مُزال
        val still = (0 until s.rows).any { r -> s.grid[r][3]?.let { it.kind == Kind.SEED && it.heavy } == true }
        assertTrue(still)
        RescueEngine.useHammer(s, 8, 4).toList()
        val any = (0 until s.rows).any { r -> s.grid[r][3]?.let { it.kind == Kind.SEED && it.heavy } == true }
        assertFalse(any)
    }

    @Test
    fun `snake advances and can end the game`() {
        val s = RescueEngine.newState(RescueLevel.get(1), Random(5))
        var lost = false
        var guard = 0
        while (!lost && guard++ < 40) {
            val h = RescueEngine.findHint(s) ?: break
            // اختر حركة في الأعمدة البعيدة عن الجذع كي لا يُفرَّغ الممر
            var pick = h
            loop@ for (r in 0 until s.rows) for (c in 0 until s.cols - 1) if (c >= 5 && RescueEngine.isValidSwap(s, r, c, r, c + 1)) { pick = (r to c) to (r to c + 1); break@loop }
            val steps = RescueEngine.playMove(s, pick.first.first, pick.first.second, pick.second.first, pick.second.second).toList()
            val end = steps.filterIsInstance<Step.Ended>().firstOrNull()
            if (end != null) { lost = end.outcome == Outcome.LOST; if (end.outcome == Outcome.WON) break }
        }
        assertTrue(s.snake >= 1 || s.escaped || lost)
    }

    @Test
    fun `revive pushes snake back two steps and grants moves`() {
        val s = RescueEngine.newState(RescueLevel.get(1), Random(5))
        s.snake = 4; s.movesLeft = 0
        RescueEngine.revive(s)
        assertEquals(2, s.snake)
        assertEquals(5, s.movesLeft)
        s.snake = 1
        RescueEngine.revive(s)
        assertEquals(0, s.snake)
        assertTrue(RescueEngine.findHint(s) != null)
    }

    @Test
    fun `greedy bot wins most games without invariant breaks`() {
        var wins = 0
        val games = 250
        for (g in 0 until games) {
            val s = RescueEngine.newState(RescueLevel.get(1 + g % RescueLevel.ALL.size), Random(100 + g))
            var ended = false
            var guard = 0
            while (!ended && guard++ < 60) {
                var best: IntArray? = null
                var bs = -1
                for (r in 0 until s.rows) for (c in 0 until s.cols) {
                    for ((dr, dc) in listOf(0 to 1, 1 to 0)) {
                        val r2 = r + dr; val c2 = c + dc
                        if (r2 >= s.rows || c2 >= s.cols || !RescueEngine.isValidSwap(s, r, c, r2, c2)) continue
                        val score = 10 - kotlin.math.abs(c - 3) * 2 + r
                        if (score > bs) { bs = score; best = intArrayOf(r, c, r2, c2) }
                    }
                }
                val b = best ?: break
                val steps = RescueEngine.playMove(s, b[0], b[1], b[2], b[3]).toList()
                steps.filterIsInstance<Step.Ended>().firstOrNull()?.let { ended = true; if (it.outcome == Outcome.WON) wins++ }
                assertEquals(8, count(s, Kind.QUEEN))
                if (!s.escaped) assertEquals(4, count(s, Kind.TRUNK))
            }
        }
        println("bot wins $wins / $games")
        assertTrue(wins > games / 4)
    }
}
