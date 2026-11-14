package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.LevelConfig
import com.example.ui.screens.GamePlayScreen
import com.example.ui.screens.IslandScreen
import com.example.ui.screens.SplashScreen
import com.example.island.Item
import com.example.island.PALACE
import com.example.model.BoosterType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.RescueScreen
import com.example.ui.theme.TropicalCrushTheme
import com.example.viewmodel.ScreenState
import com.example.viewmodel.TropicalGameViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TropicalGameViewModel by viewModels()

    /** ملء الشاشة في كل اللعبة: تختفي أزرار النظام وشريط الحالة وتظهر مؤقتًا عند السحب من الحافة. */
    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).let {
            it.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            it.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        setContent {
            TropicalCrushTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TropicalCrushApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun TropicalCrushApp(viewModel: TropicalGameViewModel) {
    val currentScreen by viewModel.screen.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()

    val totalStars = gameState.levelStars.values.sum()

    val gateMsg by viewModel.gateMessage.collectAsStateWithLifecycle()
    val islandVer by viewModel.islandVersion.collectAsStateWithLifecycle()

    when (currentScreen) {
        ScreenState.SPLASH -> SplashScreen(onFinished = { viewModel.navigateTo(ScreenState.MAIN_MENU) })

        ScreenState.ISLAND -> {
            BackHandler { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            val eng = viewModel.island
            IslandScreen(
                engine = eng,
                version = islandVer,
                onChanged = { op -> viewModel.withBag { op() } },
                // TODO(AdMob): استبدل بإظهار إعلان مكافأة حقيقي ثم نفّذ المكافأة عند اكتماله
                onAdSpeedUp = { id -> viewModel.withBag { speedUpByAd(id); tick(System.currentTimeMillis()) } },
                onAdBuilder = { viewModel.withBag { grantAdBuilder(System.currentTimeMillis()) } },
                onAdDouble = { id -> viewModel.withBag { collect(id, true, System.currentTimeMillis()) } },
                bagCount = { item ->
                    gameState.boosterInventory[when (item) {
                        Item.HAMMER -> BoosterType.HAMMER; Item.SHUFFLE -> BoosterType.SHUFFLE
                        Item.ROW -> BoosterType.ROW_BLAST; Item.COL -> BoosterType.COL_BLAST
                    }] ?: 0
                },
                onBack = { viewModel.navigateTo(ScreenState.MAIN_MENU) }
            )
        }

        ScreenState.MAIN_MENU -> {
            MainMenuScreen(
                soundEnabled = gameState.soundEnabled,
                hapticsEnabled = gameState.hapticsEnabled,
                totalStars = totalStars,
                onPlayClick = {
                    // Start next uncompleted level or level 1
                    val nextLevel = (1..LevelConfig.MAX_LEVEL).firstOrNull { lvl ->
                        (gameState.levelStars[lvl] ?: 0) == 0
                    } ?: 1
                    viewModel.requestLevel(nextLevel)
                },
                onIslandClick = { viewModel.navigateTo(ScreenState.ISLAND) },
                onMapClick = {
                    viewModel.navigateTo(ScreenState.LEVEL_MAP)
                },
                onSoundToggle = { viewModel.toggleSound(it) },
                onHapticsToggle = { viewModel.toggleHaptics(it) }
            )
        }

        ScreenState.LEVEL_MAP -> {
            BackHandler {
                viewModel.navigateTo(ScreenState.MAIN_MENU)
            }
            LevelMapScreen(
                levelStars = gameState.levelStars,
                highScores = gameState.highScores,
                levelCompletions = gameState.levelCompletions,
                onSelectLevel = { levelNum ->
                    viewModel.requestLevel(levelNum)
                },
                rescueStars = viewModel.loadRescueStars(),
                rescueCount = com.example.rescue.RescueLevel.ALL.size,
                onSelectRescue = { viewModel.startRescue(it) },
                onBack = {
                    viewModel.navigateTo(ScreenState.MAIN_MENU)
                }
            )
        }

        ScreenState.GAMEPLAY -> {
            GamePlayScreen(
                gameState = gameState,
                particleSystem = viewModel.particleSystem,
                onTileClicked = { viewModel.onTileClicked(it) },
                onSwipe = { r1, c1, r2, c2 -> viewModel.onSwipe(r1, c1, r2, c2) },
                onBoosterClick = { viewModel.selectBooster(it) },
                onPauseClick = { viewModel.pauseGame() },
                onResumeClick = { viewModel.resumeGame() },
                onRestartClick = { viewModel.restartCurrentLevel() },
                onNextLevelClick = { viewModel.nextLevel() },
                onMainMenuClick = { viewModel.navigateTo(ScreenState.MAIN_MENU) },
                onSoundToggle = { viewModel.toggleSound(it) },
                onHapticsToggle = { viewModel.toggleHaptics(it) },
                onReviveWithAd = { viewModel.reviveWithAd() },
                freeReviveAvailable = viewModel.freeReviveAvailable(),
                onReviveWithStable = { viewModel.reviveWithStable() }
            )
        }

        ScreenState.RESCUE -> {
            BackHandler { viewModel.navigateTo(ScreenState.LEVEL_MAP) }
            RescueScreen(
                levelId = viewModel.rescueLevelId,
                onExit = { viewModel.navigateTo(ScreenState.LEVEL_MAP) },
                onQuitToMenu = { viewModel.navigateTo(ScreenState.MAIN_MENU) },
                onGrantReward = { type, n -> viewModel.grantBooster(type, n) },
                onRescueWon = { viewModel.onRescueWon() }
            )
        }
    }

    gateMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissGate() },
            title = { Text("🚧 الطريق مغلق") }, text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.dismissGate(); viewModel.navigateTo(ScreenState.ISLAND) }) { Text("اذهب إلى الجزيرة") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissGate() }) { Text("لاحقًا") } }
        )
    }
}
