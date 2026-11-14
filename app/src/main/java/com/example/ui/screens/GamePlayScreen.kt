package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BoosterType
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.LevelConfig
import com.example.model.Tile
import com.example.game.ParticleSystem
import com.example.ui.components.BoosterBar
import com.example.ui.components.ComboBanner
import com.example.ui.components.DefeatDialog
import com.example.ui.components.GameBoard
import com.example.ui.components.GameTopBar
import com.example.ui.components.PauseDialog
import com.example.ui.components.VictoryDialog
import kotlinx.coroutines.launch

@Composable
fun GamePlayScreen(
    gameState: GameState,
    particleSystem: ParticleSystem,
    onTileClicked: (Tile) -> Unit,
    onSwipe: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit,
    onBoosterClick: (BoosterType) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onMainMenuClick: () -> Unit,
    onSoundToggle: (Boolean) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onReviveWithAd: () -> Unit = {},
    freeReviveAvailable: Boolean = false,
    onReviveWithStable: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAd by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    // Android Back button handler
    BackHandler {
        if (gameState.status == GameStatus.PLAYING) {
            onPauseClick()
        } else {
            onMainMenuClick()
        }
    }

    // High-impact screen shake animations for clearing large groups (> 4 fruits)
    val shakeOffsetX = remember { Animatable(0f) }
    val shakeOffsetY = remember { Animatable(0f) }
    val shakeRotation = remember { Animatable(0f) }
    val impactFlashAlpha = remember { Animatable(0f) }

    LaunchedEffect(gameState.shakeTimestamp) {
        if (gameState.shakeTimestamp > 0L) {
            val intensity = gameState.shakeIntensity.coerceIn(0.8f, 2.5f)
            val maxOffset = 18f * intensity
            val maxAngle = 1.6f * intensity

            // Impact flash overlay
            launch {
                impactFlashAlpha.snapTo(0.26f * (intensity / 1.5f).coerceAtMost(1f))
                impactFlashAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
            }

            // Rapid decaying multi-directional spring oscillation for tactile power
            launch {
                shakeOffsetX.animateTo(maxOffset, tween(25, easing = LinearEasing))
                shakeOffsetX.animateTo(-maxOffset * 0.85f, tween(30, easing = LinearEasing))
                shakeOffsetX.animateTo(maxOffset * 0.65f, tween(35, easing = LinearEasing))
                shakeOffsetX.animateTo(-maxOffset * 0.45f, tween(40, easing = LinearEasing))
                shakeOffsetX.animateTo(maxOffset * 0.25f, tween(45, easing = LinearEasing))
                shakeOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            }
            launch {
                shakeOffsetY.animateTo(-maxOffset * 0.75f, tween(25, easing = LinearEasing))
                shakeOffsetY.animateTo(maxOffset * 0.65f, tween(30, easing = LinearEasing))
                shakeOffsetY.animateTo(-maxOffset * 0.45f, tween(35, easing = LinearEasing))
                shakeOffsetY.animateTo(maxOffset * 0.25f, tween(40, easing = LinearEasing))
                shakeOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            }
            launch {
                shakeRotation.animateTo(maxAngle, tween(30, easing = LinearEasing))
                shakeRotation.animateTo(-maxAngle * 0.7f, tween(35, easing = LinearEasing))
                shakeRotation.animateTo(maxAngle * 0.4f, tween(40, easing = LinearEasing))
                shakeRotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Subtle global screen shake for camera feel
                translationX = shakeOffsetX.value * 0.35f
                translationY = shakeOffsetY.value * 0.35f
            }
    ) {
        // Jungle Background image
        Image(
            painter = painterResource(id = R.drawable.bg_tropical_jungle),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Backdrop gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xCC051812),
                            Color(0x9908241A),
                            Color(0xEE051812)
                        )
                    )
                )
        )

        // Impact shockwave flash overlay for large group clears
        if (impactFlashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFFD166).copy(alpha = impactFlashAlpha.value))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Bar
            GameTopBar(
                levelConfig = gameState.levelConfig,
                score = gameState.score,
                movesRemaining = gameState.movesRemaining,
                collectedFruits = gameState.collectedFruits,
                currentStars = gameState.currentStars,
                onPauseClick = onPauseClick
            )

            // Center: Game Board & Combo Banner with powerful shockwave animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationX = shakeOffsetX.value
                        translationY = shakeOffsetY.value
                        rotationZ = shakeRotation.value
                    },
                contentAlignment = Alignment.Center
            ) {
                GameBoard(
                    board = gameState.board,
                    selectedTile = gameState.selectedTile,
                    scorePopups = gameState.scorePopups,
                    particleSystem = particleSystem,
                    hintedTiles = gameState.hintedTiles,
                    onTileClicked = onTileClicked,
                    onSwipe = onSwipe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp)
                        .padding(horizontal = 4.dp)
                )

                // Combo banner floating on center
                ComboBanner(
                    comboText = gameState.comboMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Bottom: Tropical Boosters
            BoosterBar(
                inventory = gameState.boosterInventory,
                activeBooster = gameState.activeBooster,
                onBoosterClick = onBoosterClick
            )
        }

        // Dialogs Overlay
        when (gameState.status) {
            GameStatus.WON -> {
                val hasNext = gameState.levelConfig.levelNumber < 50
                VictoryDialog(
                    score = gameState.score,
                    stars = gameState.currentStars,
                    levelConfig = gameState.levelConfig,
                    hasNextLevel = hasNext,
                    onNextLevel = onNextLevelClick,
                    onRestart = onRestartClick,
                    onMainMenu = onMainMenuClick
                )
            }
            GameStatus.LOST -> {
                DefeatDialog(
                    score = gameState.score,
                    targetScore = gameState.levelConfig.targetScore,
                    onRestart = onRestartClick,
                    onMainMenu = onMainMenuClick,
                    canWatchAd = !gameState.revived,
                    freeRevive = freeReviveAvailable,
                    onWatchAd = { if (freeReviveAvailable) onReviveWithStable() else showAd = true }
                )
                if (showAd) {
                    com.example.ads.AdGateDialog(
                        title = "شاهد الإعلان للحصول على حركات إضافية",
                        onReward = { showAd = false; onReviveWithAd() },
                        onCancel = { showAd = false }
                    )
                }
            }
            GameStatus.PAUSED -> {
                PauseDialog(
                    soundEnabled = gameState.soundEnabled,
                    hapticsEnabled = gameState.hapticsEnabled,
                    onSoundToggle = onSoundToggle,
                    onHapticsToggle = onHapticsToggle,
                    onResume = onResumeClick,
                    onRestart = onRestartClick,
                    onMainMenu = onMainMenuClick
                )
            }
            GameStatus.PLAYING -> {
                // No dialog
            }
        }
    }
}
