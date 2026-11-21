package com.example.rescue

import kotlin.random.Random

/** نوع الخلية في لوحة الإنقاذ. */
enum class Kind { FRUIT, SEED, WALL, LAVA, TRUNK, QUEEN }

/**
 * خلية واحدة. الكائنات متعددة الأجزاء (الملكة 2×4 والجذع 1×4) تتكوّن من خلايا لكل جزء،
 * و[anchor] تعلّم الخلية العلوية-اليسرى التي يُرسم منها الكائن كاملاً.
 */
class Cell(
    val id: Long,
    val kind: Kind,
    var fruit: Int = -1,
    var heavy: Boolean = false,
    val variant: Int = 0,
    val anchor: Boolean = false
)

data class RescueLevel(
    val id: Int,
    val name: String,
    val moves: Int,
    val snakeEvery: Int,
    val rows: Int = 12,
    val cols: Int = 8,
    val fruitKinds: Int = 5,
    val queenRow: Int = 3,
    val ledgeRow: Int = 7,
    val trunkCol: Int = 2,
    val trunkTop: Int = 3,
    val trunkBottom: Int = 6,
    /** أعلى صف للفواكه في الأعمدة 3..7 (درجات متصاعدة نحو اليمين لتتدفق البذور يساراً). */
    val fruitTop: List<Int> = listOf(9, 8, 7, 6, 5),
    val heavy: List<Pair<Int, Int>> = emptyList()
) {
    companion object {
        val ALL = listOf(
            RescueLevel(1, "إنقاذ ملكة الفواكه", moves = 30, snakeEvery = 4),
            RescueLevel(2, "إنقاذ ملكة الفواكه ٢ — البذور الثقيلة", moves = 32, snakeEvery = 3, heavy = listOf(8 to 3, 7 to 3)),
            RescueLevel(3, "إنقاذ ٣ — الجبل العالي", moves = 34, snakeEvery = 3, fruitTop = listOf(10, 8, 7, 5, 4)),
            RescueLevel(4, "إنقاذ ٤ — ثلاثة حجارة", moves = 34, snakeEvery = 3, heavy = listOf(8 to 3, 7 to 3, 7 to 4)),
            RescueLevel(5, "إنقاذ ٥ — عاصفة الثعبان", moves = 32, snakeEvery = 2, fruitKinds = 6, fruitTop = listOf(10, 8, 7, 5, 4), heavy = listOf(9 to 3, 8 to 3, 7 to 4)),
            RescueLevel(6, "إنقاذ ٦ — الغابة المظلمة", moves = 32, snakeEvery = 3, fruitKinds = 5, fruitTop = listOf(10, 8, 7, 5, 4), heavy = listOf(9 to 3, 8 to 3)),
            RescueLevel(7, "إنقاذ ٧ — جسر الصخور", moves = 32, snakeEvery = 3, fruitKinds = 6, fruitTop = listOf(10, 8, 7, 5, 4), heavy = listOf(9 to 3, 8 to 3, 7 to 3)),
            RescueLevel(8, "إنقاذ ٨ — وادي الأفاعي", moves = 32, snakeEvery = 2, fruitKinds = 5, fruitTop = listOf(10, 9, 7, 6, 5), heavy = listOf(9 to 3, 8 to 3, 7 to 3)),
            RescueLevel(9, "إنقاذ ٩ — كهف الحجارة", moves = 32, snakeEvery = 2, fruitKinds = 6, fruitTop = listOf(10, 9, 7, 6, 5), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4)),
            RescueLevel(10, "إنقاذ ١٠ — الشلال الغاضب", moves = 30, snakeEvery = 2, fruitKinds = 6, fruitTop = listOf(10, 9, 8, 7, 6), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4)),
            RescueLevel(11, "إنقاذ ١١ — جبل البركان", moves = 30, snakeEvery = 2, fruitKinds = 6, fruitTop = listOf(11, 10, 9, 8, 7), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4)),
            RescueLevel(12, "إنقاذ ١٢ — ليلة بلا قمر", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 9, 8, 7), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5)),
            RescueLevel(13, "إنقاذ ١٣ — فخ الثعبان", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 9, 8), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5)),
            RescueLevel(14, "إنقاذ ١٤ — العاصفة الاستوائية", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 9, 8), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5, 9 to 5)),
            RescueLevel(15, "إنقاذ ١٥ — قلب الجزيرة", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 10, 9), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5, 9 to 5, 10 to 3)),
            RescueLevel(16, "إنقاذ ١٦ — حصن الحجارة", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 10, 9), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5, 9 to 5, 10 to 3, 10 to 4)),
            RescueLevel(17, "إنقاذ ١٧ — غضب الثعبان الأكبر", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 10, 10), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5, 9 to 5, 10 to 3, 10 to 4, 6 to 4)),
            RescueLevel(18, "إنقاذ ١٨ — المعركة الأخيرة", moves = 30, snakeEvery = 2, fruitKinds = 7, fruitTop = listOf(11, 10, 10, 10, 10), heavy = listOf(9 to 3, 8 to 3, 7 to 3, 7 to 4, 8 to 4, 9 to 4, 8 to 5, 7 to 5, 9 to 5, 10 to 3, 10 to 4, 6 to 4, 6 to 5, 10 to 5))
        )

        fun get(id: Int): RescueLevel = ALL.first { it.id == id }
    }
}

class RescueState(val level: RescueLevel, val rng: Random) {
    val rows = level.rows
    val cols = level.cols
    val grid = Array(rows) { arrayOfNulls<Cell>(cols) }
    var movesLeft = level.moves
    var snake = 0
    var snakeIn = level.snakeEvery
    var escaped = false
    var score = 0
    var seed0 = 1
    private var nextId = 1L
    fun newId() = nextId++
}

data class Move(val id: Long, val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int)
data class RemovedCell(val id: Long, val row: Int, val col: Int, val kind: Kind, val fruit: Int)

enum class Outcome { WON, LOST }

/** حدث واحد يصفه المحرك لواجهة الرسوم؛ اللوحة تكون عندئذ قد تحدّثت إلى الحالة اللاحقة. */
sealed interface Step {
    data class Removed(val cells: List<RemovedCell>, val gained: Int, val chain: Int) : Step
    data class Cracked(val cells: List<Pair<Int, Int>>) : Step
    data class Flow(val moves: List<Move>) : Step
    data class Drained(val cells: List<RemovedCell>) : Step
    data class SnakeAdvanced(val snake: Int) : Step
    data object TrunkFell : Step
    data object Reshuffled : Step
    data class Ended(val outcome: Outcome, val stars: Int) : Step
}

object RescueEngine {
    const val QUEEN_W = 2
    const val QUEEN_H = 4

    // ───────────── إنشاء اللوحة ─────────────
    fun newState(level: RescueLevel, rng: Random = Random.Default): RescueState {
        val s = RescueState(level, rng)
        repeat(200) {
            for (r in 0 until s.rows) for (c in 0 until s.cols) s.grid[r][c] = null
            fillFruits(s)
            layout(s)
            if (findRuns(s).isEmpty() && findHint(s) != null) {
                s.seed0 = maxOf(1, seedsInColumn(s, level.trunkCol + 1))
                return s
            }
        }
        error("تعذّر توليد لوحة صالحة")
    }

    private fun fillFruits(s: RescueState) {
        for (r in 0 until s.rows) for (c in 0 until s.cols) {
            var f: Int
            var guard = 0
            do {
                f = s.rng.nextInt(s.level.fruitKinds); guard++
            } while (guard < 30 && (
                    (c >= 2 && s.grid[r][c - 1]?.fruit == f && s.grid[r][c - 2]?.fruit == f) ||
                            (r >= 2 && s.grid[r - 1][c]?.fruit == f && s.grid[r - 2][c]?.fruit == f)))
            s.grid[r][c] = Cell(s.newId(), Kind.FRUIT, fruit = f)
        }
    }

    private fun layout(s: RescueState) {
        val l = s.level
        for (r in 0 until s.rows) for (c in 0 until s.cols) {
            s.grid[r][c] = when {
                c <= 1 -> when {
                    r == l.ledgeRow -> Cell(s.newId(), Kind.WALL)
                    r > l.ledgeRow -> Cell(s.newId(), Kind.LAVA, variant = r + c)
                    else -> null
                }
                c == l.trunkCol ->
                    if (r in l.trunkTop..l.trunkBottom) Cell(s.newId(), Kind.TRUNK, anchor = r == l.trunkTop)
                    else Cell(s.newId(), Kind.WALL)
                else -> {
                    val ft = l.fruitTop[c - 3]
                    if (r < ft) Cell(s.newId(), Kind.SEED, heavy = (r to c) in l.heavy, variant = (r * 7 + c * 3) % 6)
                    else s.grid[r][c]
                }
            }
        }
        for (dr in 0 until QUEEN_H) for (dc in 0 until QUEEN_W) {
            s.grid[l.queenRow + dr][dc] = Cell(s.newId(), Kind.QUEEN, anchor = dr == 0 && dc == 0)
        }
    }

    // ───────────── المطابقة ─────────────
    /** إحداثيات كل الفواكه ضمن سلاسل ≥ 3 (أفقي/عمودي). تُرمَّز r*cols+c. */
    fun findRuns(s: RescueState): Set<Int> {
        val out = HashSet<Int>()
        for (r in 0 until s.rows) {
            var c = 0
            while (c < s.cols) {
                val f = fruitAt(s, r, c)
                if (f < 0) { c++; continue }
                var k = c + 1
                while (k < s.cols && fruitAt(s, r, k) == f) k++
                if (k - c >= 3) for (i in c until k) out += r * s.cols + i
                c = k
            }
        }
        for (c in 0 until s.cols) {
            var r = 0
            while (r < s.rows) {
                val f = fruitAt(s, r, c)
                if (f < 0) { r++; continue }
                var k = r + 1
                while (k < s.rows && fruitAt(s, k, c) == f) k++
                if (k - r >= 3) for (i in r until k) out += i * s.cols + c
                r = k
            }
        }
        return out
    }

    private fun fruitAt(s: RescueState, r: Int, c: Int): Int =
        s.grid[r][c]?.takeIf { it.kind == Kind.FRUIT }?.fruit ?: -1

    private fun swapRaw(s: RescueState, r1: Int, c1: Int, r2: Int, c2: Int) {
        val t = s.grid[r1][c1]; s.grid[r1][c1] = s.grid[r2][c2]; s.grid[r2][c2] = t
    }

    fun isValidSwap(s: RescueState, r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        if (kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2) != 1) return false
        if (fruitAt(s, r1, c1) < 0 || fruitAt(s, r2, c2) < 0) return false
        swapRaw(s, r1, c1, r2, c2)
        val ok = findRuns(s).isNotEmpty()
        swapRaw(s, r1, c1, r2, c2)
        return ok
    }

    fun findHint(s: RescueState): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        for (r in 0 until s.rows) for (c in 0 until s.cols) {
            if (c + 1 < s.cols && isValidSwap(s, r, c, r, c + 1)) return (r to c) to (r to c + 1)
            if (r + 1 < s.rows && isValidSwap(s, r, c, r + 1, c)) return (r to c) to (r + 1 to c)
        }
        return null
    }

    // ───────────── الفيزياء: جاذبية وانسياب البذور ─────────────
    private fun movable(t: Cell) = t.kind == Kind.FRUIT || (t.kind == Kind.SEED && !t.heavy)

    /** خطوة متوازية واحدة: سقوط عمودي، أو انزلاق قطري للبذور إن وُجد ممرّ فارغ بجانبها. */
    fun settleStep(s: RescueState): List<Move> {
        val moves = ArrayList<Move>()
        for (r in s.rows - 2 downTo 0) {
            val order = (0 until s.cols).toMutableList()
            if (s.rng.nextBoolean()) order.reverse()
            for (c in order) {
                val t = s.grid[r][c] ?: continue
                if (!movable(t)) continue
                var nc = c
                if (s.grid[r + 1][c] != null) {
                    nc = -1
                    if (t.kind == Kind.SEED) {
                        val dirs = if (s.rng.nextBoolean()) intArrayOf(-1, 1) else intArrayOf(1, -1)
                        for (d in dirs) {
                            val cc = c + d
                            if (cc in 0 until s.cols && s.grid[r][cc] == null && s.grid[r + 1][cc] == null) { nc = cc; break }
                        }
                    }
                    if (nc < 0) continue
                }
                s.grid[r + 1][nc] = t; s.grid[r][c] = null
                moves += Move(t.id, r, c, r + 1, nc)
            }
        }
        return moves
    }

    /** البذور التي بلغت الصف الأخير تتصرّف خارج اللوحة. */
    fun drainBottom(s: RescueState): List<RemovedCell> {
        val out = ArrayList<RemovedCell>()
        val r = s.rows - 1
        for (c in 0 until s.cols) {
            val t = s.grid[r][c]
            if (t != null && t.kind == Kind.SEED) { out += RemovedCell(t.id, r, c, t.kind, -1); s.grid[r][c] = null }
        }
        return out
    }

    /** البذور الثقيلة تتفتّت عند إزالة أي خلية في جوارها (8 اتجاهات). */
    fun crackHeavy(s: RescueState, removed: Collection<Pair<Int, Int>>): List<Pair<Int, Int>> {
        val out = LinkedHashSet<Pair<Int, Int>>()
        for ((r, c) in removed) for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val rr = r + dr; val cc = c + dc
            if (rr !in 0 until s.rows || cc !in 0 until s.cols) continue
            val h = s.grid[rr][cc]
            if (h != null && h.kind == Kind.SEED && h.heavy) { h.heavy = false; out += rr to cc }
        }
        return out.toList()
    }

    // ───────────── الجذع والتقدّم ─────────────
    fun seedsInColumn(s: RescueState, col: Int): Int = (0 until s.rows).count { s.grid[it][col]?.kind == Kind.SEED }

    fun seedsBesideTrunk(s: RescueState): Int {
        val l = s.level
        return (l.trunkTop..l.trunkBottom).count { s.grid[it][l.trunkCol + 1]?.kind == Kind.SEED }
    }

    /** 0..1 لشريط التقدّم: نسبة تفريغ العمود المجاور للجذع. */
    fun trunkProgress(s: RescueState): Float =
        if (s.escaped) 1f else (1f - seedsInColumn(s, s.level.trunkCol + 1).toFloat() / s.seed0).coerceIn(0f, 1f)

    // ───────────── حلقة الحل ─────────────
    private fun settleAll(s: RescueState): Sequence<Step> = sequence {
        var guard = 0
        while (guard++ < 200) {
            val moves = settleStep(s)
            if (moves.isEmpty()) break
            yield(Step.Flow(moves))
            val dr = drainBottom(s)
            if (dr.isNotEmpty()) { s.score += dr.size * 30; yield(Step.Drained(dr)) }
        }
    }

    private fun resolve(s: RescueState): Sequence<Step> = sequence {
        var chain = 0
        while (true) {
            val matched = findRuns(s)
            if (matched.isEmpty()) break
            chain++
            val removed = matched.map { k ->
                val r = k / s.cols; val c = k % s.cols
                val t = s.grid[r][c]!!
                RemovedCell(t.id, r, c, t.kind, t.fruit)
            }
            matched.forEach { k -> s.grid[k / s.cols][k % s.cols] = null }
            val gained = (removed.size * 40 * (1 + .35 * (chain - 1))).toInt()
            s.score += gained
            yield(Step.Removed(removed, gained, chain))
            val cracked = crackHeavy(s, removed.map { it.row to it.col })
            if (cracked.isNotEmpty()) yield(Step.Cracked(cracked))
            yieldAll(settleAll(s))
        }
    }

    /** نهاية الدور: سقوط الجذع → تقدّم الثعبان → الحركات → إعادة الخلط. */
    private fun endTurn(s: RescueState): Sequence<Step> = sequence {
        val l = s.level
        val orphans = crackOrphans(s)
        if (orphans.isNotEmpty()) { yield(Step.Cracked(orphans)); yieldAll(settleAll(s)) }
        if (!s.escaped && seedsBesideTrunk(s) == 0) {
            s.escaped = true
            for (r in l.trunkTop..l.trunkBottom) s.grid[r][l.trunkCol] = null
            yield(Step.TrunkFell)
        }
        if (s.escaped) {
            val used = l.moves - s.movesLeft
            yield(Step.Ended(Outcome.WON, if (used <= 9) 3 else if (used <= 12) 2 else 1))
            return@sequence
        }
        s.snakeIn--
        if (s.snakeIn <= 0) {
            s.snakeIn = l.snakeEvery; s.snake++
            yield(Step.SnakeAdvanced(s.snake))
            if (l.queenRow < s.snake) { yield(Step.Ended(Outcome.LOST, 0)); return@sequence }
        }
        if (s.movesLeft <= 0) { yield(Step.Ended(Outcome.LOST, 0)); return@sequence }
        if (findHint(s) == null) {
            // لا حركات والحجارة محاصَرة: تتصدّع كلها حتى لا تتعطّل الجولة، وإلا فإعادة خلط
            val trapped = crackAllHeavy(s)
            if (trapped.isNotEmpty()) {
                yield(Step.Cracked(trapped)); yieldAll(settleAll(s))
                if (!s.escaped && seedsBesideTrunk(s) == 0) {
                    s.escaped = true
                    for (r in l.trunkTop..l.trunkBottom) s.grid[r][l.trunkCol] = null
                    yield(Step.TrunkFell)
                    val used = l.moves - s.movesLeft
                    yield(Step.Ended(Outcome.WON, if (used <= 9) 3 else if (used <= 12) 2 else 1))
                    return@sequence
                }
            }
            // إن بقيت بلا حركات حتى بعد التصدّع، يُعاد توزيع الفواكه تلقائياً
            if (findHint(s) == null) { reshuffle(s); yield(Step.Reshuffled) }
        }
    }

    /** حجر ثقيل بلا فاكهة في جواره الثماني لن يتفتّت بالمطابقة أبداً: يتصدّع تلقائياً. */
    private fun crackOrphans(s: RescueState): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (r in 0 until s.rows) for (c in 0 until s.cols) {
            val h = s.grid[r][c] ?: continue
            if (!h.heavy) continue
            var fruit = false
            for (dr in -1..1) for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                if (s.grid.getOrNull(r + dr)?.getOrNull(c + dc)?.kind == Kind.FRUIT) fruit = true
            }
            if (!fruit) { h.heavy = false; out += r to c }
        }
        return out
    }

    private fun crackAllHeavy(s: RescueState): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        for (r in 0 until s.rows) for (c in 0 until s.cols) s.grid[r][c]?.let { if (it.heavy) { it.heavy = false; out += r to c } }
        return out
    }

    /** يبدّل فاكهتين (إن كانت الحركة صالحة) ثم يحلّ كل شيء. التسلسل كسول: كل عنصر يُنتَج بعد تحديث الحالة. */
    fun playMove(s: RescueState, r1: Int, c1: Int, r2: Int, c2: Int): Sequence<Step> = sequence {
        if (s.movesLeft <= 0 || !isValidSwap(s, r1, c1, r2, c2)) return@sequence
        swapRaw(s, r1, c1, r2, c2)
        s.movesLeft--
        yieldAll(resolve(s))
        yieldAll(endTurn(s))
    }

    /** المطرقة: تزيل أي فاكهة أو بذرة (حتى الثقيلة). لا تستهلك حركة ولا تقدّم الثعبان. */
    fun useHammer(s: RescueState, r: Int, c: Int): Sequence<Step> = sequence {
        val t = s.grid[r][c] ?: return@sequence
        if (t.kind != Kind.FRUIT && t.kind != Kind.SEED) return@sequence
        s.grid[r][c] = null
        yield(Step.Removed(listOf(RemovedCell(t.id, r, c, t.kind, t.fruit)), 0, 1))
        val cracked = crackHeavy(s, listOf(r to c))
        if (cracked.isNotEmpty()) yield(Step.Cracked(cracked))
        yieldAll(settleAll(s))
        yieldAll(resolve(s))
        if (!s.escaped && seedsBesideTrunk(s) == 0) yieldAll(endTurnFreeOnly(s))
    }

    private fun endTurnFreeOnly(s: RescueState): Sequence<Step> = sequence {
        val l = s.level
        s.escaped = true
        for (r in l.trunkTop..l.trunkBottom) s.grid[r][l.trunkCol] = null
        yield(Step.TrunkFell)
        val used = l.moves - s.movesLeft
        yield(Step.Ended(Outcome.WON, if (used <= 9) 3 else if (used <= 12) 2 else 1))
    }

    /** بعد إعلان المكافأة: يتراجع الثعبان خطوتين، وتُمنح ٥ حركات إن نفدت، ويُعاد التوزيع إن لزم. */
    fun revive(s: RescueState) {
        s.snake = maxOf(0, s.snake - 2)
        s.snakeIn = s.level.snakeEvery
        if (s.movesLeft <= 0) s.movesLeft = 5
        if (findHint(s) == null) reshuffle(s)
    }

    fun reshuffle(s: RescueState) {
        val cells = ArrayList<Pair<Int, Int>>()
        for (r in 0 until s.rows) for (c in 0 until s.cols) if (s.grid[r][c]?.kind == Kind.FRUIT) cells += r to c
        val fruits = cells.map { (r, c) -> s.grid[r][c]!!.fruit }.toMutableList()
        repeat(80) {
            fruits.shuffle(s.rng)
            cells.forEachIndexed { i, (r, c) -> s.grid[r][c]!!.fruit = fruits[i] }
            if (findRuns(s).isEmpty() && findHint(s) != null) return
        }
    }
}
