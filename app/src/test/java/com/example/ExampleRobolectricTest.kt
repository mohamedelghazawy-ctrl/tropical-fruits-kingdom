package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.GameEngine
import com.example.audio.SoundHelper
import com.example.model.BoosterType
import com.example.model.FruitType
import com.example.model.GameState
import com.example.model.LevelConfig
import com.example.model.SpecialType
import com.example.model.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("مملكة الفواكه الاستوائية", appName)
    }

    @Test
    fun `game engine creates initial board with valid moves`() {
        val board = GameEngine.createInitialBoard(rows = 8, cols = 8)
        assertEquals(8, board.size)
        assertEquals(8, board[0].size)
        assertTrue(GameEngine.hasPossibleMoves(board))
        assertTrue(GameEngine.findRawMatches(board).isEmpty())
    }

    @Test
    fun `reshuffleBoardWhenNoMoves guarantees valid moves and no initial matches`() {
        // Construct a locked board with alternating fruits where no move is possible
        val rows = 8
        val cols = 8
        val lockedBoard = List(rows) { r ->
            List(cols) { c ->
                val fruit = if ((r + c) % 2 == 0) FruitType.PINEAPPLE else FruitType.WATERMELON
                Tile(
                    id = (r * cols + c).toLong(),
                    row = r,
                    col = c,
                    type = fruit,
                    special = if (r == 0 && c == 0) SpecialType.BOMB else SpecialType.NONE
                )
            }
        }

        val reshuffled = GameEngine.reshuffleBoardWhenNoMoves(lockedBoard)

        // Must have valid moves
        assertTrue(GameEngine.hasPossibleMoves(reshuffled))
        // Must have no pre-existing auto-exploding 3-in-a-row matches
        assertTrue(GameEngine.findRawMatches(reshuffled).isEmpty())
        // Special tile (Bomb) must be preserved
        assertTrue(reshuffled.flatten().any { it.special == SpecialType.BOMB })
    }

    @Test
    fun `hint system finds valid candidate swap`() {
        val board = GameEngine.createInitialBoard(rows = 8, cols = 8)
        val hint = GameEngine.findHint(board)
        assertNotNull(hint)
        val (first, second) = hint!!
        assertTrue(GameEngine.canSwap(first.first, first.second, second.first, second.second, board))
    }

    @Test
    fun `level configs are defined`() {
        assertTrue(LevelConfig.ALL_LEVELS.isNotEmpty())
        val level1 = LevelConfig.getLevel(1)
        assertNotNull(level1)
        assertEquals(1, level1.levelNumber)
    }

    @Test
    fun `level management scales strawberry objectives and target score with completion count`() {
        // Base Level 4 objective requires 20 Strawberries
        val level4Base = LevelConfig.getLevel(4, completionCount = 0)
        assertEquals(20, level4Base.targetFruits[FruitType.STRAWBERRY])

        // After 1st successful completion, strawberry objective increases
        val level4Repeat1 = LevelConfig.getLevel(4, completionCount = 1)
        val strawberryCount1 = level4Repeat1.targetFruits[FruitType.STRAWBERRY] ?: 0
        assertTrue(strawberryCount1 > 20)
        assertTrue(level4Repeat1.targetScore > level4Base.targetScore)

        // After 2nd completion, difficulty scales even higher
        val level4Repeat2 = LevelConfig.getLevel(4, completionCount = 2)
        val strawberryCount2 = level4Repeat2.targetFruits[FruitType.STRAWBERRY] ?: 0
        assertTrue(strawberryCount2 > strawberryCount1)
        assertTrue(level4Repeat2.targetScore > level4Repeat1.targetScore)
    }

    @Test
    fun `level progression scales difficulty across higher levels`() {
        val level1 = LevelConfig.getLevel(1)
        val level4 = LevelConfig.getLevel(4)
        val level5 = LevelConfig.getLevel(5)
        val level10 = LevelConfig.getLevel(10)
        val level15 = LevelConfig.getLevel(15)

        // الأهداف معايَرة (~٣٠٠ نقطة لكل حركة) وتتناقص الحركات تدريجيًا
        assertEquals(22 * 131, level1.targetScore)
        assertTrue(level4.maxMoves <= level1.maxMoves && level15.maxMoves < level5.maxMoves)
        assertTrue(level10.targetScore > 2000 && level15.targetScore > 2000)

        // أهداف الفواكه تُضبط بمقياس الصعوبة (needScale) بعد المستوى ٥
        assertEquals(20, level4.targetFruits[FruitType.STRAWBERRY])
        assertEquals(18, level5.targetFruits[FruitType.STRAWBERRY])
        assertEquals(10, level10.targetFruits[FruitType.STRAWBERRY])
        assertEquals(95, LevelConfig.MAX_LEVEL)

        // Available levels list respects completion tracking
        val completions = mapOf(1 to 2, 4 to 1)
        val list = LevelConfig.getAvailableLevels(completions, count = 10)
        assertEquals(10, list.size)
        assertEquals(2, list[0].completionCount)
        assertEquals(1, list[3].completionCount)
    }

    @Test
    fun `gameState tracks screen shake properties`() {
        val state = GameState(levelConfig = LevelConfig.getLevel(1))
        assertEquals(0L, state.shakeTimestamp)
        assertEquals(1f, state.shakeIntensity, 0.01f)

        val shakenState = state.copy(
            shakeTimestamp = System.currentTimeMillis(),
            shakeIntensity = 1.5f
        )
        assertTrue(shakenState.shakeTimestamp > 0L)
        assertEquals(1.5f, shakenState.shakeIntensity, 0.01f)
    }

    @Test
    fun `detectAndResolveMatches identifies large group of more than 4 fruits`() {
        // Build board with 5 matching fruits in row 0
        val board = List(8) { r ->
            List(8) { c ->
                val fruit = if (r == 0 && c < 5) FruitType.PINEAPPLE else if ((r + c) % 2 == 0) FruitType.KIWI else FruitType.MANGO
                Tile(
                    id = (r * 8 + c).toLong(),
                    row = r,
                    col = c,
                    type = fruit
                )
            }
        }
        val result = GameEngine.detectAndResolveMatches(board)
        assertTrue(result.matchedTiles.size > 4)
    }

    @Test
    fun `SoundHelper executes fruit matching, clearing, and victory audio cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val soundHelper = SoundHelper(context)

        // Matching sounds with increasing combo
        soundHelper.playMatch(1)
        soundHelper.playMatch(3)
        soundHelper.playMatch(6)

        // Fruit squish and cascade refill sounds
        soundHelper.playFruitSquishPop()
        soundHelper.playCascadeDrop()

        // Explosions and large matches
        soundHelper.playSpecialExplosion()
        soundHelper.playLargeMatchImpact(7)

        // Level completion and defeat
        soundHelper.playVictory()
        soundHelper.playDefeat()

        // Booster sounds
        soundHelper.playBoosterUse(BoosterType.HAMMER)
        soundHelper.playBoosterUse(BoosterType.ROW_BLAST)
        soundHelper.playBoosterUse(BoosterType.SHUFFLE)

        // Basic interactions
        soundHelper.playTap()
        soundHelper.playSwap()
        soundHelper.playInvalidSwap()

        // Release resources
        soundHelper.release()
    }
}
