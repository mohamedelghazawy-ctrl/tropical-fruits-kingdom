package com.example.rescue

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundHelper
import com.example.model.BoosterType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class QueenPose { IDLE, CHEER, LOOK, HOLD, PUSH, PUSH2, CROUCH, SCARED, RELIEF }

enum class RescueStatus { PLAYING, WON, LOST }

/** ظلّ فاكهة/بذرة أُزيلت: يُرسم متقلّصاً لحظياً. */
class Ghost(val id: Long, val row: Int, val col: Int, val kind: Kind, val fruit: Int, val startMs: Long)

/**
 * حالة اللعب والرسوم. اللوحة ([state]) هي المصدر؛ التحريك يعتمد على [animFrom] و[animT]
 * اللذين يُقرآن داخل طبقات الرسم فقط، فلا تُعاد تركيبة الواجهة كل إطار.
 * يُستدعى كل شيء من الـ Main thread.
 */
class RescueViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("tropical_crush_prefs", Context.MODE_PRIVATE)
    private val sound = SoundHelper(application)

    var state: RescueState = RescueEngine.newState(RescueLevel.get(1))
        private set

    /** يتغيّر بعد كل خطوة ليعيد تركيب قائمة الفواكه. */
    var version by mutableIntStateOf(0); private set
    var animT by mutableFloatStateOf(1f); private set
    val animFrom = HashMap<Long, Pair<Float, Float>>()
    var ghosts: List<Ghost> = emptyList(); private set
    var ghostTick by mutableIntStateOf(0); private set

    var status by mutableStateOf(RescueStatus.PLAYING); private set
    var stars by mutableIntStateOf(0); private set
    var pose by mutableStateOf(QueenPose.IDLE); private set
    var busy by mutableStateOf(false); private set
    var hammerMode by mutableStateOf(false); private set
    var hammers by mutableIntStateOf(3); private set
    var hammerCount = 0

    // الجائزة بعد الفوز (إعصار ثم مطرقة بالتناوب) وإعلان الإنقاذ بعد الخسارة
    var reward by mutableStateOf<BoosterType?>(null); private set
    var rewardDoubled by mutableStateOf(false); private set
    var revived by mutableStateOf(false); private set

    // الثعبان والجذع والملكة
    var snakeVis by mutableFloatStateOf(0f); private set
    var trunkAngle by mutableFloatStateOf(0f); private set
    var trunkFalling by mutableStateOf(false); private set
    var queenDx by mutableFloatStateOf(0f); private set
    var queenDy by mutableFloatStateOf(0f); private set
    var pushUntilMs by mutableStateOf(0L); private set
    var selected by mutableStateOf<Pair<Int, Int>?>(null); private set

    private var poseJob: Job? = null
    private var idleJob: Job? = null
    private var lastDrainSfx = 0L

    var soundEnabled = prefs.getBoolean("sound_enabled", true)

    init { startLevel(1) }

    fun startLevel(id: Int) {
        poseJob?.cancel(); idleJob?.cancel()
        state = RescueEngine.newState(RescueLevel.get(id))
        animFrom.clear(); ghosts = emptyList()
        status = RescueStatus.PLAYING; stars = 0; busy = false; hammerMode = false; hammers = 3
        snakeVis = 0f; trunkAngle = 0f; trunkFalling = false; queenDx = 0f; queenDy = 0f; selected = null
        reward = null; rewardDoubled = false; revived = false
        pose = QueenPose.IDLE; animT = 1f; version++
        startIdle()
    }

    fun restart() = startLevel(state.level.id)
    fun nextLevel() = startLevel(if (state.level.id < RescueLevel.ALL.size) state.level.id + 1 else 1)

    // ───────── الحالة المشتقة للواجهة ─────────
    val movesLeft get() = state.movesLeft
    val snakeIn get() = state.snakeIn
    val progress get() = RescueEngine.trunkProgress(state)
    val danger get() = status == RescueStatus.PLAYING && (state.level.queenRow - state.snake) <= 1

    /** ميل الجذع: يزداد كلما قلّت البذور بجانبه، مع اهتزاز عند دفع الملكة. */
    fun trunkLean(nowMs: Long): Float {
        if (trunkFalling || state.escaped) return trunkAngle
        val l = state.level
        val frac = (RescueEngine.seedsBesideTrunk(state).toFloat() / (l.trunkBottom - l.trunkTop + 1)).coerceIn(0f, 1f)
        val push = ((pushUntilMs - nowMs) / 1000f).coerceIn(0f, 1f)
        return 0.22f * (1f - frac) + push * (0.03f + sin(nowMs / 65f) * 0.02f)
    }

    // ───────── المدخلات ─────────
    fun onTap(r: Int, c: Int) {
        if (busy || status != RescueStatus.PLAYING) return
        if (hammerMode) { useHammer(r, c); return }
        val sel = selected
        if (sel == null) { selected = r to c; return }
        if (abs(sel.first - r) + abs(sel.second - c) == 1) { selected = null; onSwipe(sel.first, sel.second, r, c) } else selected = r to c
    }

    fun toggleHammer() { if (!busy && hammers > 0) hammerMode = !hammerMode }

    fun onSwipe(r1: Int, c1: Int, r2: Int, c2: Int) {
        if (busy || status != RescueStatus.PLAYING) return
        if (!RescueEngine.isValidSwap(state, r1, c1, r2, c2)) { sfx { sound.playInvalidSwap() }; return }
        sfx { sound.playSwap() }
        play(RescueEngine.playMove(state, r1, c1, r2, c2))
    }

    private fun useHammer(r: Int, c: Int) {
        val k = state.grid[r][c]?.kind
        if (k != Kind.FRUIT && k != Kind.SEED) return
        hammers--; hammerMode = false
        sfx { sound.playHammerSmash() }
        play(RescueEngine.useHammer(state, r, c))
    }

    private fun play(steps: Sequence<Step>) {
        busy = true; selected = null
        viewModelScope.launch {
            val it = steps.iterator()
            while (it.hasNext()) animate(it.next())
            busy = false
        }
    }

    // ───────── تحريك الخطوات ─────────
    private suspend fun animate(step: Step) {
        when (step) {
            is Step.Removed -> {
                val now = System.currentTimeMillis()
                ghosts = ghosts + step.cells.map { Ghost(it.id, it.row, it.col, it.kind, it.fruit, now) }
                ghostTick++
                version++
                sfx { sound.playFruitSquishPop() }
                if (step.cells.any { it.col == 3 }) push(900)
                delay(150)
                ghosts = emptyList(); ghostTick++
            }
            is Step.Cracked -> { sfx { sound.playLargeMatchImpact(6) }; version++; delay(120) }
            is Step.Flow -> {
                animFrom.clear()
                step.moves.forEach { animFrom[it.id] = it.fromRow.toFloat() to it.fromCol.toFloat() }
                version++
                tweenT(70)
                animFrom.clear(); animT = 1f
            }
            is Step.Drained -> {
                val now = System.currentTimeMillis()
                if (now - lastDrainSfx > 110) { sfx { sound.playCascadeDrop() }; lastDrainSfx = now }
                version++
            }
            is Step.SnakeAdvanced -> {
                setPose(QueenPose.SCARED, 1400); sfx { sound.playSpecialExplosion() }
                val from = snakeVis
                tweenValue(500) { snakeVis = from + (step.snake - from) * it }
                snakeVis = step.snake.toFloat(); version++
            }
            Step.TrunkFell -> freeQueen()
            Step.Reshuffled -> { version++; delay(200) }
            is Step.Ended -> {
                stars = step.stars
                if (step.outcome == Outcome.WON) { sfx { sound.playVictory() }; delay(900); status = RescueStatus.WON; saveStars(step.stars); rollReward() }
                else { setPose(QueenPose.SCARED, 60_000); sfx { sound.playDefeat() }; status = RescueStatus.LOST }
                idleJob?.cancel()
            }
        }
    }

    /** يسقط الجذع، تركض الملكة إلى المنتصف ثم ترتاح. */
    private suspend fun freeQueen() {
        idleJob?.cancel(); poseJob?.cancel()
        pose = QueenPose.PUSH2; trunkFalling = true
        val a0 = trunkAngle
        sfx { sound.playSpecialExplosion() }
        tweenValue(700) { trunkAngle = a0 + (PI.toFloat() / 2f - a0) * it * it }
        trunkAngle = PI.toFloat() / 2f; version++
        delay(250)
        pose = QueenPose.IDLE
        tweenValue(1150) { t -> queenDx = 3f * t; queenDy = -0.8f * minOf(1f, t * 6f) - abs(sin(t * 13f)) * 0.1f }
        queenDy = -0.8f; pose = QueenPose.RELIEF
        delay(900)
    }

    private suspend fun tweenT(ms: Int) {
        val t0 = System.nanoTime()
        while (true) {
            val e = ((System.nanoTime() - t0) / 1_000_000f / ms).coerceAtMost(1f)
            animT = e
            if (e >= 1f) break
            delay(16)
        }
    }

    private suspend fun tweenValue(ms: Int, set: (Float) -> Unit) {
        val t0 = System.nanoTime()
        while (true) {
            val e = ((System.nanoTime() - t0) / 1_000_000f / ms).coerceAtMost(1f)
            set(e)
            if (e >= 1f) break
            delay(16)
        }
    }

    // ───────── سلوك الملكة ─────────
    private fun push(ms: Long) { pushUntilMs = System.currentTimeMillis() + ms; setPose(QueenPose.PUSH2, ms) }

    private fun setPose(p: QueenPose, ms: Long) {
        poseJob?.cancel()
        pose = p
        poseJob = viewModelScope.launch { delay(ms); if (status == RescueStatus.PLAYING) pose = basePose() }
    }

    private fun basePose() = if (state.escaped) QueenPose.RELIEF else if (danger) QueenPose.HOLD else QueenPose.IDLE

    private fun startIdle() {
        idleJob = viewModelScope.launch {
            while (isActive) {
                delay(3800L + Random.nextLong(2600L))
                if (busy || status != RescueStatus.PLAYING) continue
                if (Random.nextFloat() < .65f) push(1100) else setPose(if (basePose() == QueenPose.HOLD) QueenPose.SCARED else QueenPose.LOOK, 1500)
            }
        }
    }

    private fun rollReward() {
        val wins = prefs.getInt("rescue_wins", 0)
        reward = if (wins % 2 == 0) BoosterType.SHUFFLE else BoosterType.HAMMER
        prefs.edit().putInt("rescue_wins", wins + 1).apply()
    }

    fun markRewardDoubled() { rewardDoubled = true }

    /** بعد مشاهدة الإعلان: يتراجع الثعبان خطوتين وتُكمل الجولة (مرة واحدة لكل محاولة). */
    fun reviveAfterAd() {
        if (status != RescueStatus.LOST || revived) return
        revived = true
        RescueEngine.revive(state)
        snakeVis = state.snake.toFloat()
        status = RescueStatus.PLAYING
        busy = false; version++
        setPose(QueenPose.IDLE, 400)
        idleJob?.cancel(); startIdle()
    }

    private fun saveStars(s: Int) {
        val key = "rescue_stars_${state.level.id}"
        if (s > prefs.getInt(key, 0)) prefs.edit().putInt(key, s).apply()
    }

    private inline fun sfx(block: () -> Unit) { if (soundEnabled) block() }

    override fun onCleared() {
        poseJob?.cancel(); idleJob?.cancel()
        sound.release()
        super.onCleared()
    }
}
