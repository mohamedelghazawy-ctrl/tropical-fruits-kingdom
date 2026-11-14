package com.example

import com.example.game.GameEngine
import com.example.model.FruitType
import com.example.model.SpecialType
import com.example.model.Tile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineComboTest {

    /** لوحة بلا أي مطابقة: ألوان تتكرر بنمط 2×2 لتفادي ثلاثيات. */
    private fun calmBoard(): List<List<Tile>> {
        val fruits = listOf(FruitType.PINEAPPLE, FruitType.MANGO, FruitType.KIWI, FruitType.STRAWBERRY)
        return List(8) { r ->
            List(8) { c ->
                Tile(id = (r * 8 + c).toLong(), row = r, col = c, type = fruits[((r / 1) % 2) * 2 + (c % 2)])
            }
        }
    }

    private fun Tile.withSpecial(s: SpecialType) = copy(special = s)

    private fun List<List<Tile>>.with(r: Int, c: Int, f: (Tile) -> Tile) =
        mapIndexed { rr, row -> row.mapIndexed { cc, t -> if (rr == r && cc == c) f(t) else t } }

    @After
    fun restore() {
        GameEngine.activeFruits = FruitType.standardFruits
    }

    @Test
    fun `calm board has no matches`() {
        assertTrue(GameEngine.findRawMatches(calmBoard()).isEmpty())
    }

    @Test
    fun `two stripes swapped clear a full cross`() {
        val b = calmBoard()
            .with(3, 3) { it.withSpecial(SpecialType.STRIPED_HORIZONTAL) }
            .with(3, 4) { it.withSpecial(SpecialType.STRIPED_VERTICAL) }
        val res = GameEngine.detectAndResolveMatches(b, 3 to 3, 3 to 4)
        // صف كامل (8) + عمود كامل (8) - تقاطع واحد
        assertEquals(15, res.matchedTiles.size)
        assertTrue(res.scoreEarned >= 1000)
    }

    @Test
    fun `rainbow with stripe converts all fruits of that color`() {
        val base = calmBoard()
        val target = base[3][4].type
        val b = base
            .with(3, 3) { it.withSpecial(SpecialType.STARFRUIT_RAINBOW) }
            .with(3, 4) { it.withSpecial(SpecialType.STRIPED_HORIZONTAL) }
        val res = GameEngine.detectAndResolveMatches(b, 3 to 3, 3 to 4)
        val sameColor = b.flatten().count { it.type == target && it.special != SpecialType.STARFRUIT_RAINBOW }
        assertTrue(res.matchedTiles.size > sameColor)
        assertTrue(res.scoreEarned >= 1800)
    }

    @Test
    fun `rainbow tile never matches by color and is not counted as a fruit`() {
        val b = calmBoard()
            .with(0, 0) { it.copy(type = FruitType.KIWI, special = SpecialType.STARFRUIT_RAINBOW) }
            .with(0, 1) { it.copy(type = FruitType.KIWI) }
            .with(0, 2) { it.copy(type = FruitType.KIWI) }
        assertFalse((0 to 0) in GameEngine.findRawMatches(b))
        val res = GameEngine.explode(b, setOf(0 to 0))
        // النجمة نفسها لا تُحتسب ضمن الفواكه المجموعة
        assertEquals(res.matchedTiles.size - 1, res.collectedByType.values.sum())
    }

    @Test
    fun `new boards only use the active fruits`() {
        GameEngine.activeFruits = listOf(FruitType.PINEAPPLE, FruitType.MANGO, FruitType.KIWI, FruitType.STRAWBERRY, FruitType.COCONUT)
        repeat(20) {
            val board = GameEngine.createInitialBoard()
            assertTrue(board.flatten().all { it.type in GameEngine.activeFruits })
        }
    }

    @Test
    fun `level one uses five fruits that include the goal fruit`() {
        val level = com.example.model.LevelConfig.getLevel(1)
        assertEquals(5, level.activeFruits.size)
        assertTrue(level.targetFruits.keys.all { it in level.activeFruits })
        // كل المستويات: فواكه الأهداف دائماً موجودة
        (1..40).forEach { n ->
            val l = com.example.model.LevelConfig.getLevel(n)
            assertTrue("level $n", l.targetFruits.keys.all { it in l.activeFruits })
        }
    }

    @Test
    fun `each island carries its fruit and only the legendary island carries a star`() {
        val lvl = com.example.model.LevelConfig
        assertEquals(FruitType.PINEAPPLE, lvl.getLevel(1).islandFruit)
        assertEquals(FruitType.MANGO, lvl.getLevel(2).islandFruit)
        assertTrue(lvl.getLevel(10).islandFruit == null)
        assertTrue(lvl.getLevel(22).islandFruit == null) // الجزء الثاني يكرر نفس الجزر
        (1..40).filter { it % 12 != 10 }.forEach { n -> assertTrue("level $n", lvl.getLevel(n).islandFruit != null) }
    }
}
