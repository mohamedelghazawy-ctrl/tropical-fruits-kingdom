package com.example

import com.example.island.*
import org.junit.Assert.*
import org.junit.Test

class IslandEngineTest {
    @Test fun costsAndBuild() {
        val e = IslandEngine(); var now = 0L
        assertEquals(70, e.costOf(Sites.GARDEN, 1)); assertEquals(240, e.costOf(PALACE, 1))
        e.s.gems = 1000
        assertTrue(e.startBuild(Sites.GARDEN, now)); assertFalse(e.startBuild(Sites.BAKERY, now))
        now += 10 * 60_000; e.tick(now)
        assertEquals(1, e.lvOf(Sites.GARDEN)); assertTrue(e.noWorker().any { it.id == Sites.GARDEN })
        assertTrue(e.setWorker(Sites.GARDEN, 0, now)); assertTrue(e.noWorker().isEmpty())
    }
    @Test fun marketWorksWithoutWorkerEarly() {
        val e = IslandEngine(); e.s.pal = 5; e.s.lv[Sites.MARKET] = 1; e.s.prod[Sites.MARKET] = Acc(0.0, 0)
        assertTrue(e.worked(Sites.MARKET)); assertTrue(e.prodVal(Sites.MARKET, 100 * 3_600_000) <= 18.001)
    }
    @Test fun gatesAndSave() {
        val e = IslandEngine(); assertTrue(e.gateFor(11) is IslandEngine.Gate.SiteGate)
        e.s.pal = 5; assertTrue(e.gateFor(41) is IslandEngine.Gate.PalaceGate)
        e.s.gems = 77; val d = IslandEngine.decode(e.encode()); assertEquals(77, d.s.gems)
    }
}
