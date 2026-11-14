package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class GameStatus {
    PLAYING,
    WON,
    LOST,
    PAUSED
}

enum class BoosterType(val titleAr: String, val icon: String, val descriptionAr: String) {
    HAMMER("مطرقة جوز الهند", "🔨", "إزالة أي فاكهة بلمسة واحدة"),
    SHUFFLE("إعصار الفواكه", "🌪️", "خلط كل الفواكه في اللوحة"),
    ROW_BLAST("شعاع الشمس", "⚡", "تدمير صف أفقي كامل"),
    COL_BLAST("شلال البرق", "🌊", "تدمير عمود كامل")
}

data class ScorePopup(
    val id: Long,
    val text: String,
    val row: Int,
    val col: Int,
    val color: Color,
    val startTimestamp: Long = System.currentTimeMillis()
)

data class GameState(
    val levelConfig: LevelConfig,
    val board: List<List<Tile>> = emptyList(),
    val score: Int = 0,
    val movesRemaining: Int = 20,
    val collectedFruits: Map<FruitType, Int> = emptyMap(),
    val status: GameStatus = GameStatus.PLAYING,
    val selectedTile: Tile? = null,
    val activeBooster: BoosterType? = null,
    val comboCount: Int = 0,
    val comboMessage: String? = null,
    val scorePopups: List<ScorePopup> = emptyList(),
    val boosterInventory: Map<BoosterType, Int> = mapOf(
        BoosterType.HAMMER to 3,
        BoosterType.SHUFFLE to 2,
        BoosterType.ROW_BLAST to 2,
        BoosterType.COL_BLAST to 2
    ),
    val isBusyAnimating: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val highScores: Map<Int, Int> = emptyMap(),
    val levelStars: Map<Int, Int> = emptyMap(),
    val levelCompletions: Map<Int, Int> = emptyMap(),
    val hintedTiles: Set<Pair<Int, Int>> = emptySet(),
    val shakeTimestamp: Long = 0L,
    val shakeIntensity: Float = 1f,
    /** استُخدم إعلان الإنقاذ في هذه المحاولة (مرة واحدة). */
    val revived: Boolean = false
) {
    val currentStars: Int
        get() {
            val (oneStar, twoStar, threeStar) = levelConfig.starThresholds
            return when {
                score >= threeStar -> 3
                score >= twoStar -> 2
                score >= oneStar -> 1
                else -> 0
            }
        }

    val isGoalsMet: Boolean
        get() {
            val scoreMet = score >= levelConfig.targetScore
            val fruitsMet = levelConfig.targetFruits.all { (fruit, target) ->
                (collectedFruits[fruit] ?: 0) >= target
            }
            return scoreMet && fruitsMet
        }
}
