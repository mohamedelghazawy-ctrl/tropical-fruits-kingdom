package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundHelper
import com.example.game.GameEngine
import com.example.game.ParticleSystem
import com.example.island.IslandEngine
import com.example.island.Item
import com.example.island.Sites
import com.example.model.BoosterType
import com.example.model.LuckyWheel
import com.example.model.WheelPrize
import com.example.notify.ReadyNotifier
import com.example.rescue.RescueLevel
import com.example.model.FruitType
import com.example.model.GameState
import com.example.model.GameStatus
import com.example.model.LevelConfig
import com.example.model.ScorePopup
import com.example.model.SpecialType
import com.example.model.Tile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class ScreenState {
    SPLASH,
    ISLAND,
    MAIN_MENU,
    LEVEL_MAP,
    GAMEPLAY,
    RESCUE
}

class TropicalGameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("tropical_crush_prefs", Context.MODE_PRIVATE)
    private val soundHelper = SoundHelper(application)
    val daily = com.example.daily.DailyMissions(prefs)
    private val _dailyVer = MutableStateFlow(0)
    val dailyVersion: StateFlow<Int> = _dailyVer.asStateFlow()
    private fun rescueUnlocked() = prefs.getInt("stars_level_5", 0) > 0
    private fun rec(t: com.example.daily.MType, n: Int = 1, p: Int = -1) { daily.ensureToday(rescueUnlocked()); daily.record(t, n, p); _dailyVer.value++ }
    fun dailyViews() = daily.views(rescueUnlocked())
    fun dailyPending() = daily.pending(rescueUnlocked())
    fun claimDaily(i: Int) { val g = daily.claim(i); if (g > 0) { island.s.gems += g; islandChanged() }; _dailyVer.value++ }
    fun claimDailyChest() { val g = daily.claimChest(); if (g > 0) { island.s.gems += g; grantBooster(BoosterType.HAMMER, 1); islandChanged() }; _dailyVer.value++ }

    private val _screen = MutableStateFlow(ScreenState.SPLASH)
    val screen: StateFlow<ScreenState> = _screen.asStateFlow()

    private val _gameState = MutableStateFlow(
        GameState(
            levelConfig = LevelConfig.getLevel(1),
            board = emptyList(),
            highScores = loadHighScores(),
            levelStars = loadLevelStars(),
            levelCompletions = loadLevelCompletions(),
            boosterInventory = loadInventory()
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    // ───── جزيرة القصر ─────
    var island: IslandEngine = IslandEngine.decode(prefs.getString("island_save", "") ?: "")
        private set
    private val _islandVer = MutableStateFlow(0)
    /** يزداد عند أي تغيّر في الجزيرة لتُعاد تركيب الشاشات. */
    val islandVersion: StateFlow<Int> = _islandVer.asStateFlow()
    private val _gateMsg = MutableStateFlow<String?>(null)
    /** رسالة البوابة المغلقة (تُعرض على الخريطة) أو null. */
    val gateMessage: StateFlow<String?> = _gateMsg.asStateFlow()

    private fun bagKey(i: Item) = when (i) {
        Item.HAMMER -> BoosterType.HAMMER; Item.SHUFFLE -> BoosterType.SHUFFLE
        Item.ROW -> BoosterType.ROW_BLAST; Item.COL -> BoosterType.COL_BLAST
    }

    /** الحقيبة = مخزون الأدوات في اللعبة: تُزامَن قبل أي عملية جزيرة وبعدها. */
    fun <T> withBag(block: IslandEngine.() -> T): T {
        val inv = _gameState.value.boosterInventory
        Item.values().forEach { island.s.inv[it] = inv[bagKey(it)] ?: 0 }
        val r = island.block()
        val out = inv.toMutableMap()
        Item.values().forEach { out[bagKey(it)] = island.s.inv[it] ?: 0 }
        if (out != inv) { saveInventory(out); _gameState.update { it.copy(boosterInventory = out) } }
        islandChanged()
        return r
    }

    private var lastBuildAt = -1L
    private var lastBoatAt = -1L

    fun islandChanged() {
        prefs.edit().putString("island_save", island.encode()).apply()
        _islandVer.update { it + 1 }
        syncReadyNotifications()
    }

    /** يجدول إشعار "جاهز" لأقرب بناء ومركب (يُعاد الجدولة فقط عند تغيّر الوقت). */
    private fun syncReadyNotifications() {
        try {
            val app = getApplication<Application>()
            val b = island.s.q.minOfOrNull { it.end } ?: 0L
            if (b != lastBuildAt) {
                lastBuildAt = b
                if (b > 0) ReadyNotifier.schedule(app, ReadyNotifier.ID_BUILD, b, "🏗️ اكتمل البناء في جزيرة القصر!")
                else ReadyNotifier.cancel(app, ReadyNotifier.ID_BUILD)
            }
            val bo = island.s.boats.filterNotNull().minOfOrNull { it.end } ?: 0L
            if (bo != lastBoatAt) {
                lastBoatAt = bo
                if (bo > 0) ReadyNotifier.schedule(app, ReadyNotifier.ID_BOAT, bo, "⛵ وصل المركب! اجمع بضاعتك")
                else ReadyNotifier.cancel(app, ReadyNotifier.ID_BOAT)
            }
        } catch (e: Exception) { }
    }

    // ───── عجلة الحظ ─────
    fun wheelReadyAt(): Long = prefs.getLong("wheel_last", 0L) + LuckyWheel.COOLDOWN_MS

    /** تدور العجلة إن كانت جاهزة؛ تعيد (فهرس القطعة, الجائزة) أو null. */
    fun spinWheel(): Pair<Int, WheelPrize>? {
        val now = System.currentTimeMillis()
        if (now < wheelReadyAt()) return null
        val idx = LuckyWheel.pickIndex(Random)
        val prize = LuckyWheel.PRIZES[idx]
        prefs.edit().putLong("wheel_last", now).apply()
        rec(com.example.daily.MType.SPIN_WHEEL)
        applyPrize(prize)
        ReadyNotifier.schedule(getApplication<Application>(), ReadyNotifier.ID_WHEEL, now + LuckyWheel.COOLDOWN_MS, "🎡 عجلة الحظ جاهزة للدوران!")
        return idx to prize
    }

    /** مضاعفة الجائزة بعد مشاهدة إعلان. */
    fun doublePrize(p: WheelPrize) = applyPrize(p)

    private fun applyPrize(p: WheelPrize) {
        if (p.gems > 0) { island.s.gems += p.gems; islandChanged() }
        p.booster?.let { grantBooster(it, p.amount) }
    }
    /** جواهر قاعة الاحتفالات عند إنقاذ الملكة. */
    fun onRescueWon() { rec(com.example.daily.MType.WIN_RESCUE); val b = island.hallBonus(); if (b > 0) { island.s.gems += b; islandChanged() } }
    fun dismissGate() { _gateMsg.value = null }

    init {
        val w = wheelReadyAt()
        if (w > System.currentTimeMillis()) ReadyNotifier.schedule(getApplication<Application>(), ReadyNotifier.ID_WHEEL, w, "🎡 عجلة الحظ جاهزة للدوران!")
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val changed = withBag { tick(System.currentTimeMillis()) }
                if (!changed) _islandVer.update { it + 1 }
            }
        }
    }

    /** يفتح مستوى الخريطة بعد فحص بوابة الجزر (حديقة/سوق/مرفأ/قصر). */
    /** رقم مرحلة الإنقاذ المطلوبة قبل هذه الجزيرة (0 = لا شيء). */
    fun rescueNeeded(levelNum: Int): Int {
        if (levelNum <= 5 || (levelNum - 1) % 5 != 0) return 0
        val k = (levelNum - 1) / 5
        return if (k <= RescueLevel.ALL.size && prefs.getInt("rescue_stars_$k", 0) == 0) k else 0
    }

    fun requestLevel(levelNum: Int) {
        val k = rescueNeeded(levelNum)
        if (k > 0) { startRescue(k); return }
        when (val g = island.gateFor(levelNum)) {
            null -> startLevel(levelNum)
            is IslandEngine.Gate.SiteGate -> _gateMsg.value = "الطريق مغلق: رمّم ${g.site.name} في جزيرة القصر أولًا"
            is IslandEngine.Gate.PalaceGate -> _gateMsg.value = "الطريق مغلق: ارفع قصر الملكة إلى المستوى ${g.reqLv}"
        }
    }

    /** حركات القصر الإضافية + قراءة الإحياء المجاني من الإسطبل. */
    fun freeReviveAvailable(): Boolean = island.freeRevives(System.currentTimeMillis()) > 0

    /** حالة الجسيمات خارج StateFlow لتفادي إعادة التركيب كل إطار. */
    val particleSystem = ParticleSystem()

    private var lastInteractionTimestamp: Long = System.currentTimeMillis()

    /** أثناء جولة المكافأة تُجمَّد مدخلات اللاعب. */
    private var inBonusRound = false

    init {
        // Automatic 5-second idle hint loop
        viewModelScope.launch {
            while (true) {
                delay(500)
                val state = _gameState.value
                val isEligibleForHint = _screen.value == ScreenState.GAMEPLAY &&
                        state.status == GameStatus.PLAYING &&
                        !state.isBusyAnimating &&
                        state.selectedTile == null &&
                        state.activeBooster == null

                if (isEligibleForHint) {
                    val idleMillis = System.currentTimeMillis() - lastInteractionTimestamp
                    if (idleMillis >= 5000L && state.hintedTiles.isEmpty()) {
                        val needs = state.levelConfig.targetFruits
                            .mapValues { (t, n) -> n - (state.collectedFruits[t] ?: 0) }
                            .filterValues { it > 0 }
                        val hint = GameEngine.findSmartHint(state.board, needs, state.movesRemaining)
                            ?: GameEngine.findHint(state.board)
                        if (hint != null) {
                            _gameState.update {
                                it.copy(hintedTiles = setOf(hint.first, hint.second))
                            }
                        }
                    }
                } else {
                    if (state.hintedTiles.isNotEmpty()) {
                        _gameState.update { it.copy(hintedTiles = emptySet()) }
                    }
                }
            }
        }
    }

    private fun registerUserInteraction() {
        lastInteractionTimestamp = System.currentTimeMillis()
        if (_gameState.value.hintedTiles.isNotEmpty()) {
            _gameState.update { it.copy(hintedTiles = emptySet()) }
        }
    }

    /** رقم مستوى الإنقاذ المختار من الخريطة. */
    var rescueLevelId: Int = 1
        private set

    fun startRescue(id: Int) { rescueLevelId = id; _screen.value = ScreenState.RESCUE }

    fun loadRescueStars(): Map<Int, Int> =
        RescueLevel.ALL.associate { it.id to prefs.getInt("rescue_stars_${it.id}", 0) }.filterValues { it > 0 }

    fun navigateTo(newScreen: ScreenState) {
        if (_screen.value == ScreenState.GAMEPLAY && newScreen != ScreenState.GAMEPLAY) {
            val st = _gameState.value
            if (st.status == GameStatus.PLAYING) com.example.analytics.Track.levelQuit(st.levelConfig.levelNumber, st.movesRemaining, st.score, st.levelConfig.targetScore)
        }
        _screen.value = newScreen
    }

    fun startLevel(levelNum: Int) {
        daily.ensureToday(rescueUnlocked())
        val completions = _gameState.value.levelCompletions[levelNum] ?: prefs.getInt("completions_level_$levelNum", 0)
        val config = LevelConfig.getLevel(levelNum, completions)
        GameEngine.activeFruits = config.activeFruits
        inBonusRound = false
        particleSystem.clear()
        val initialBoard = GameEngine.createInitialBoard()

        _gameState.update {
            it.copy(
                levelConfig = config,
                board = initialBoard,
                score = 0,
                movesRemaining = config.maxMoves + island.palMoves(),
                collectedFruits = emptyMap(),
                status = GameStatus.PLAYING,
                selectedTile = null,
                activeBooster = null,
                comboCount = 0,
                comboMessage = null,
                scorePopups = emptyList(),
                isBusyAnimating = false,
                hintedTiles = emptySet(),
                revived = false
            )
        }
        registerUserInteraction()
        _screen.value = ScreenState.GAMEPLAY
        com.example.analytics.Track.levelStart(levelNum)
    }

    fun restartCurrentLevel() {
        val cur = _gameState.value
        if (cur.status == GameStatus.PLAYING) com.example.analytics.Track.levelRestart(cur.levelConfig.levelNumber)
        startLevel(cur.levelConfig.levelNumber)
    }

    fun nextLevel() {
        val next = _gameState.value.levelConfig.levelNumber + 1
        requestLevel(next)
    }

    fun onTileClicked(tile: Tile) {
        val state = _gameState.value
        if (inBonusRound || state.isBusyAnimating || state.status != GameStatus.PLAYING) return
        registerUserInteraction()

        // 1. Check Booster mode
        if (state.activeBooster != null) {
            applyBooster(state.activeBooster, tile)
            return
        }

        // 1b. بلاط خاص: النقر يفعّله مباشرة في مكانه
        val selected = state.selectedTile
        if (selected == null && state.board[tile.row][tile.col].hasSpecial()) {
            useSpecial(tile.row, tile.col)
            return
        }

        // 2. Normal Tap & Selection
        if (selected == null) {
            _gameState.update { it.copy(selectedTile = tile) }
            if (state.soundEnabled) soundHelper.playTap()
        } else if (selected.row == tile.row && selected.col == tile.col) {
            // Deselect
            _gameState.update { it.copy(selectedTile = null) }
        } else if (GameEngine.isAdjacent(selected.row, selected.col, tile.row, tile.col)) {
            // Attempt swap
            _gameState.update { it.copy(selectedTile = null) }
            handleSwap(selected.row, selected.col, tile.row, tile.col)
        } else {
            // Pick new tile
            _gameState.update { it.copy(selectedTile = tile) }
            if (state.soundEnabled) soundHelper.playTap()
        }
    }

    fun onSwipe(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val state = _gameState.value
        if (inBonusRound || state.isBusyAnimating || state.status != GameStatus.PLAYING) return
        registerUserInteraction()
        _gameState.update { it.copy(selectedTile = null) }
        // سحب بلاط خاص لأي جهة (حتى بدون مطابقة) يفعّله مباشرة
        val inB = fromRow in state.board.indices && fromCol in state.board[0].indices
        if (inB && state.board[fromRow][fromCol].hasSpecial() &&
            GameEngine.isAdjacent(fromRow, fromCol, toRow, toCol)) {
            useSpecial(fromRow, fromCol)
            return
        }
        handleSwap(fromRow, fromCol, toRow, toCol)
    }

    private fun useSpecial(r: Int, c: Int) {
        rec(com.example.daily.MType.USE_SPECIAL)
        viewModelScope.launch {
            _gameState.update { it.copy(isBusyAnimating = true, selectedTile = null) }
            resolveMatchesCascade(forced = setOf(r to c), consumeMove = true)
        }
    }

    private fun handleSwap(r1: Int, c1: Int, r2: Int, c2: Int) {
        val state = _gameState.value
        if (!GameEngine.canSwap(r1, c1, r2, c2, state.board)) {
            if (state.soundEnabled) soundHelper.playInvalidSwap()
            val inBounds = r1 in state.board.indices && r2 in state.board.indices &&
                c1 in state.board[0].indices && c2 in state.board[0].indices
            if (inBounds && GameEngine.isAdjacent(r1, c1, r2, c2)) {
                // حركة غير صالحة: تبديل ثم عودة بصرية
                viewModelScope.launch {
                    _gameState.update { it.copy(isBusyAnimating = true, board = GameEngine.swapTiles(r1, c1, r2, c2, state.board)) }
                    delay(140)
                    _gameState.update { it.copy(board = state.board) }
                    delay(140)
                    _gameState.update { it.copy(isBusyAnimating = false) }
                }
            }
            return
        }

        viewModelScope.launch {
            _gameState.update { it.copy(isBusyAnimating = true) }
            if (state.soundEnabled) soundHelper.playSwap()

            // Perform swap
            val swappedBoard = GameEngine.swapTiles(r1, c1, r2, c2, state.board)
            _gameState.update { it.copy(board = swappedBoard) }
            delay(150)

            // Resolve match chain
            resolveMatchesCascade(Pair(r1, c1), Pair(r2, c2))
        }
    }

    /**
     * @param forced خلايا تُفجَّر في أول خطوة (الأدوات المساعدة وجولة المكافأة).
     * @param consumeMove هل تُحسب هذه السلسلة حركة؟ (الأدوات والمكافأة مجانية).
     */
    private suspend fun resolveMatchesCascade(
        initialSwapA: Pair<Int, Int>? = null,
        initialSwapB: Pair<Int, Int>? = null,
        forced: Set<Pair<Int, Int>>? = null,
        consumeMove: Boolean = true
    ) {
        var currentSwapA = initialSwapA
        var currentSwapB = initialSwapB
        var pendingForced = forced
        var combo = 0

        while (true) {
            val state = _gameState.value
            val matchResult = pendingForced?.let { GameEngine.explode(state.board, it) }
                ?: GameEngine.detectAndResolveMatches(
                    board = state.board,
                    lastSwappedA = currentSwapA,
                    lastSwappedB = currentSwapB
                )
            pendingForced = null

            // Clear swap coordinates after first turn
            currentSwapA = null
            currentSwapB = null

            if (matchResult.matchedTiles.isEmpty()) {
                break
            }

            combo++
            // مضاعف السلاسل: كل تفاعل متتالٍ يرفع النقاط 35%
            val earnedScore = (matchResult.scoreEarned * (1f + 0.35f * (combo - 1))).toInt()
            val clearedCount = matchResult.matchedTiles.size
            val isLargeGroup = clearedCount > 4
            val shakeIntensity = when {
                clearedCount >= 10 -> 2.0f
                clearedCount >= 7 -> 1.5f
                clearedCount > 4 -> 1.2f
                else -> 0f
            }

            if (state.soundEnabled) {
                // Play juicy fruit popping sound for clearing animations
                soundHelper.playFruitSquishPop()

                if (isLargeGroup) {
                    soundHelper.playLargeMatchImpact(clearedCount)
                } else if (matchResult.newSpecialTiles.isNotEmpty() || combo >= 3) {
                    soundHelper.playSpecialExplosion()
                } else {
                    soundHelper.playMatch(combo)
                }
            }

            // Spawn Particles & Popups
            spawnMatchParticles(matchResult.matchedTiles, state.board)

            val comboMessage = when {
                isLargeGroup && combo < 3 -> "تطهير خارق! 💥"
                combo == 2 -> "رائع! 🍍"
                combo == 3 -> "كومبو استوائي! 🌴"
                combo == 4 -> "عاصفة الفواكه! ⚡"
                combo >= 5 -> "سحق خارق للعادة! 🌟"
                else -> null
            }

            // Add score popup at center of match
            val centerMatch = matchResult.matchedTiles.first()
            val popup = ScorePopup(
                id = System.currentTimeMillis() + Random.nextInt(1000),
                text = "+$earnedScore",
                row = centerMatch.first,
                col = centerMatch.second,
                color = state.board[centerMatch.first][centerMatch.second].type.splashColor
            )

            // Update collected counts & score
            val updatedCollected = state.collectedFruits.toMutableMap()
            matchResult.collectedByType.forEach { (fruit, count) ->
                updatedCollected[fruit] = (updatedCollected[fruit] ?: 0) + count
                daily.record(com.example.daily.MType.COLLECT_FRUIT, count, fruit.ordinal)
            }
            daily.record(com.example.daily.MType.SCORE, earnedScore)

            // Mark matched tiles for shrink animation
            val shrinkingBoard = state.board.map { row ->
                row.map { tile ->
                    if (tile.row to tile.col in matchResult.matchedTiles) {
                        tile.copy(isMatched = true)
                    } else tile
                }
            }

            _gameState.update {
                it.copy(
                    board = shrinkingBoard,
                    score = it.score + earnedScore,
                    collectedFruits = updatedCollected,
                    comboCount = combo,
                    comboMessage = comboMessage,
                    scorePopups = it.scorePopups + popup,
                    shakeTimestamp = if (isLargeGroup) System.currentTimeMillis() else it.shakeTimestamp,
                    shakeIntensity = if (isLargeGroup) shakeIntensity else it.shakeIntensity
                )
            }

            delay(200)

            // Apply gravity and refill
            val refilledBoard = GameEngine.applyGravityAndRefill(
                board = state.board,
                matchedCoords = matchResult.matchedTiles,
                newSpecials = matchResult.newSpecialTiles
            )

            _gameState.update {
                it.copy(board = refilledBoard)
            }

            if (state.soundEnabled) {
                soundHelper.playCascadeDrop()
            }

            delay(340)
        }

        // Moves and Win/Loss check
        val finalState = _gameState.value
        val newMoves = if (consumeMove) (finalState.movesRemaining - 1).coerceAtLeast(0) else finalState.movesRemaining

        _gameState.update {
            it.copy(
                movesRemaining = newMoves,
                comboCount = 0,
                comboMessage = if (inBonusRound) it.comboMessage else null,
                isBusyAnimating = inBonusRound
            )
        }

        registerUserInteraction()
        if (!inBonusRound) checkEndGameConditions()
    }

    private suspend fun checkEndGameConditions() {
        val state = _gameState.value
        if (state.isGoalsMet) {
            if (state.movesRemaining > 0) runBonusRound()
            declareVictory()
        } else if (state.movesRemaining <= 0) {
            _gameState.update { it.copy(status = GameStatus.LOST) }
            com.example.analytics.Track.levelLose(state.levelConfig.levelNumber, state.score, state.levelConfig.targetScore,
                state.levelConfig.targetFruits.entries.sumOf { (f, n) -> (n - (state.collectedFruits[f] ?: 0)).coerceAtLeast(0) })
            if (state.soundEnabled) soundHelper.playDefeat()
        } else if (!GameEngine.hasPossibleMoves(state.board)) {
            // Auto-shuffle with animation and player feedback
            triggerNoMovesReshuffle()
        }
    }

    private fun declareVictory() {
        val state = _gameState.value
        val earnedStars = state.currentStars.coerceAtLeast(1)
        rec(com.example.daily.MType.WIN_LEVELS); rec(com.example.daily.MType.EARN_STARS, earnedStars)
        val oldStars = prefs.getInt("stars_level_${state.levelConfig.levelNumber}", 0)
        val lvl = state.levelConfig.levelNumber
        val gift = if (lvl % 5 == 0 && oldStars == 0) island.bossGift(lvl) else 0
        val gain = island.gemsForStars(earnedStars - oldStars)
        if (gain > 0) island.s.gems += gain
        if (gain > 0 || gift > 0) islandChanged()
        saveScoreAndStars(state.levelConfig.levelNumber, state.score, earnedStars)
        _gameState.update { it.copy(status = GameStatus.WON, isBusyAnimating = false) }
        com.example.analytics.Track.levelWin(lvl, earnedStars, state.score, state.movesRemaining)
        if (state.soundEnabled) soundHelper.playVictory()
    }

    /**
     * جولة المكافأة: كل حركة متبقية (حتى 6) تتحول إلى فاكهة خاصة عشوائية وتنفجر،
     * فتُضاف نقاط إضافية قبل شاشة الفوز.
     */
    private suspend fun runBonusRound() {
        inBonusRound = true
        _gameState.update { it.copy(isBusyAnimating = true, selectedTile = null, activeBooster = null, comboMessage = "حركات مكافأة! 🎉") }
        delay(500)
        val rounds = _gameState.value.movesRemaining.coerceAtMost(6)
        repeat(rounds) {
            val board = _gameState.value.board
            val plain = board.flatten().filter { !it.hasSpecial() }
            val pick = plain.randomOrNull() ?: return@repeat
            val special = listOf(
                SpecialType.STRIPED_HORIZONTAL, SpecialType.STRIPED_VERTICAL, SpecialType.BOMB
            ).random()
            val upgraded = board.map { row ->
                row.map { t -> if (t.id == pick.id) t.copy(special = special) else t }
            }
            _gameState.update { it.copy(board = upgraded, movesRemaining = (it.movesRemaining - 1).coerceAtLeast(0)) }
            delay(320)
            resolveMatchesCascade(forced = setOf(pick.row to pick.col), consumeMove = false)
        }
        inBonusRound = false
    }

    /**
     * إعادة ترتيب شبكة الفواكه تلقائياً عند نفاذ الحركات مع مؤثرات وتنبيه مرئي
     */
    fun triggerNoMovesReshuffle() {
        val state = _gameState.value
        if (state.isBusyAnimating || state.status != GameStatus.PLAYING) return

        viewModelScope.launch {
            _gameState.update {
                it.copy(
                    isBusyAnimating = true,
                    comboMessage = "لا توجد حركات ممكنة! 🔄 جاري خلط الفواكه..."
                )
            }

            if (state.soundEnabled) soundHelper.playBoosterUse(BoosterType.SHUFFLE)
            delay(750)

            val reshuffled = GameEngine.reshuffleBoardWhenNoMoves(state.board)

            _gameState.update {
                it.copy(
                    board = reshuffled,
                    comboMessage = "تمت إعادة الترتيب! لديك حركات جديدة 🍍✨",
                    isBusyAnimating = false
                )
            }

            delay(1500)
            _gameState.update {
                if (it.comboMessage?.contains("تمت إعادة الترتيب") == true) {
                    it.copy(comboMessage = null)
                } else it
            }
        }
    }

    fun selectBooster(booster: BoosterType) {
        val state = _gameState.value
        if (inBonusRound || state.isBusyAnimating || state.status != GameStatus.PLAYING) return
        registerUserInteraction()

        if (booster == BoosterType.SHUFFLE) {
            // Immediately use shuffle
            useShuffleBooster()
            return
        }

        // Toggle booster selection
        _gameState.update {
            it.copy(activeBooster = if (it.activeBooster == booster) null else booster)
        }
    }

    private fun useShuffleBooster() {
        val state = _gameState.value
        val currentCount = state.boosterInventory[BoosterType.SHUFFLE] ?: 0
        if (currentCount <= 0) return

        rec(com.example.daily.MType.USE_BOOSTER)
        val shuffled = GameEngine.shuffleBoard(state.board)
        val updatedInv = state.boosterInventory.toMutableMap()
        updatedInv[BoosterType.SHUFFLE] = currentCount - 1

        if (state.soundEnabled) soundHelper.playBoosterUse(BoosterType.SHUFFLE)

        saveInventory(updatedInv)
        _gameState.update {
            it.copy(
                board = shuffled,
                boosterInventory = updatedInv,
                comboMessage = "إعصار الفواكه! 🌪️"
            )
        }
        // تختفي الرسالة بعد لحظة بدل أن تبقى حتى المطابقة التالية
        viewModelScope.launch {
            kotlinx.coroutines.delay(1400)
            _gameState.update { if (it.comboMessage == "إعصار الفواكه! 🌪️") it.copy(comboMessage = null) else it }
        }
    }

    private fun applyBooster(booster: BoosterType, targetTile: Tile) {
        val state = _gameState.value
        val count = state.boosterInventory[booster] ?: 0
        if (count <= 0) return

        rec(com.example.daily.MType.USE_BOOSTER)
        val updatedInv = state.boosterInventory.toMutableMap()
        updatedInv[booster] = count - 1

        saveInventory(updatedInv)
        viewModelScope.launch {
            _gameState.update {
                it.copy(
                    activeBooster = null,
                    boosterInventory = updatedInv,
                    isBusyAnimating = true
                )
            }
            if (state.soundEnabled) soundHelper.playBoosterUse(booster)

            val rows = state.board.size
            val cols = state.board[0].size
            val clearedCoords = mutableSetOf<Pair<Int, Int>>()

            when (booster) {
                BoosterType.HAMMER -> {
                    clearedCoords.add(targetTile.row to targetTile.col)
                }
                BoosterType.ROW_BLAST -> {
                    for (c in 0 until cols) clearedCoords.add(targetTile.row to c)
                }
                BoosterType.COL_BLAST -> {
                    for (r in 0 until rows) clearedCoords.add(r to targetTile.col)
                }
                BoosterType.SHUFFLE -> {}
            }

            // الأدوات مجانية (لا تستهلك حركة)، وتُحتسب نقاطها وأهدافها عبر السلسلة
            resolveMatchesCascade(forced = clearedCoords, consumeMove = false)
        }
    }

    private fun spawnMatchParticles(coords: Set<Pair<Int, Int>>, board: List<List<Tile>>) {
        coords.forEach { (r, c) ->
            val tile = board.getOrNull(r)?.getOrNull(c) ?: return@forEach
            particleSystem.emit(c + 0.5f, r + 0.5f, tile.type.splashColor.toArgb())
        }
        particleSystem.notifySpawned()
    }

    fun toggleSound(enabled: Boolean) {
        _gameState.update { it.copy(soundEnabled = enabled) }
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
    }

    fun toggleHaptics(enabled: Boolean) {
        _gameState.update { it.copy(hapticsEnabled = enabled) }
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun pauseGame() {
        _gameState.update { it.copy(status = GameStatus.PAUSED) }
    }

    fun resumeGame() {
        registerUserInteraction()
        _gameState.update { it.copy(status = GameStatus.PLAYING) }
    }

    private fun saveScoreAndStars(level: Int, score: Int, stars: Int) {
        val currentBestScore = prefs.getInt("score_level_$level", 0)
        val currentBestStars = prefs.getInt("stars_level_$level", 0)
        val currentCompletions = prefs.getInt("completions_level_$level", 0) + 1

        val editor = prefs.edit()
        if (score > currentBestScore) {
            editor.putInt("score_level_$level", score)
        }
        if (stars > currentBestStars) {
            editor.putInt("stars_level_$level", stars)
        }
        editor.putInt("completions_level_$level", currentCompletions)
        editor.apply()

        _gameState.update {
            it.copy(
                highScores = loadHighScores(),
                levelStars = loadLevelStars(),
                levelCompletions = loadLevelCompletions()
            )
        }
    }

    private fun loadHighScores(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (lvl in 1..LevelConfig.MAX_LEVEL) {
            val score = prefs.getInt("score_level_$lvl", 0)
            if (score > 0) map[lvl] = score
        }
        return map
    }

    // ───── مخزون الأدوات (يُحفظ بين الجلسات) والجوائز ─────
    private fun loadInventory(): Map<BoosterType, Int> = BoosterType.entries.associateWith { b ->
        prefs.getInt("inv_${b.name}", GameState(levelConfig = LevelConfig.getLevel(1)).boosterInventory[b] ?: 0)
    }

    private fun saveInventory(inv: Map<BoosterType, Int>) {
        prefs.edit().apply { inv.forEach { (b, n) -> putInt("inv_${b.name}", n) } }.apply()
    }

    /** يمنح أداة (جائزة إنقاذ الملكة أو غيرها) ويحفظها. */
    fun grantBooster(booster: BoosterType, amount: Int) {
        val inv = _gameState.value.boosterInventory.toMutableMap()
        inv[booster] = (inv[booster] ?: 0) + amount
        saveInventory(inv)
        _gameState.update { it.copy(boosterInventory = inv) }
    }

    /** بعد مشاهدة إعلان بعد الخسارة: ٥ حركات إضافية لإكمال المستوى (مرة واحدة لكل محاولة). */
    /** إحياء مجاني من الإسطبل (يوميًا) بلا إعلان. */
    fun reviveWithStable() {
        val st = _gameState.value
        if (st.status != GameStatus.LOST || st.revived || !freeReviveAvailable()) return
        island.useRevive(System.currentTimeMillis()); islandChanged()
        reviveWithAd()
    }

    fun reviveWithAd() {
        val st = _gameState.value
        if (st.status != GameStatus.LOST || st.revived) return
        _gameState.update { it.copy(status = GameStatus.PLAYING, movesRemaining = it.movesRemaining + 5, revived = true, isBusyAnimating = false) }
        com.example.analytics.Track.levelRevive(st.levelConfig.levelNumber)
        registerUserInteraction()
        viewModelScope.launch { if (!GameEngine.hasPossibleMoves(_gameState.value.board)) triggerNoMovesReshuffle() }
    }

    private fun loadLevelStars(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (lvl in 1..LevelConfig.MAX_LEVEL) {
            val stars = prefs.getInt("stars_level_$lvl", 0)
            if (stars > 0) map[lvl] = stars
        }
        return map
    }

    private fun loadLevelCompletions(): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        for (lvl in 1..LevelConfig.MAX_LEVEL) {
            val count = prefs.getInt("completions_level_$lvl", 0)
            if (count > 0) map[lvl] = count
        }
        return map
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.release()
    }
}
