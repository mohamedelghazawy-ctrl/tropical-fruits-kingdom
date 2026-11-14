package com.example.game

import com.example.model.FruitType
import com.example.model.SpecialType
import com.example.model.Tile
import kotlin.random.Random

/**
 * محرك اللعبة (منطق خالص بلا اعتماد على الواجهة).
 * النسخة المطابقة لمنطق نسخة الويب: دمج الفواكه الخاصة، وتدرّج عدد الفواكه حسب المستوى.
 */
object GameEngine {
    private var nextTileId = 1000L

    /** الفواكه المستخدمة في المستوى الحالي. تُضبط من الـ ViewModel عند بدء كل مستوى. */
    @Volatile
    var activeFruits: List<FruitType> = FruitType.standardFruits

    fun getNextId(): Long = ++nextTileId

    private fun randomFruit(): FruitType = activeFruits.random()

    /** فاكهة النجمة لا تُطابَق مع غيرها ولا تُحسب فاكهةً عند الجمع. */
    private fun Tile.isRainbow() = special == SpecialType.STARFRUIT_RAINBOW

    // ------------------------------------------------------------------
    // إنشاء اللوحة
    // ------------------------------------------------------------------

    fun createInitialBoard(rows: Int = 8, cols: Int = 8): List<List<Tile>> {
        var board: List<List<Tile>>
        do {
            val grid = Array(rows) { arrayOfNulls<Tile>(cols) }
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    grid[r][c] = Tile(
                        id = getNextId(),
                        row = r,
                        col = c,
                        type = getRandomFruitExcluding(grid, r, c),
                        special = SpecialType.NONE
                    )
                }
            }
            board = List(rows) { r -> List(cols) { c -> grid[r][c]!! } }
        } while (!hasPossibleMoves(board))
        return board
    }

    private fun getRandomFruitExcluding(grid: Array<Array<Tile?>>, row: Int, col: Int): FruitType {
        val excluded = mutableSetOf<FruitType>()
        if (col >= 2) {
            val f1 = grid[row][col - 1]?.type
            if (f1 != null && f1 == grid[row][col - 2]?.type) excluded.add(f1)
        }
        if (row >= 2) {
            val f1 = grid[row - 1][col]?.type
            if (f1 != null && f1 == grid[row - 2][col]?.type) excluded.add(f1)
        }
        val pool = activeFruits.filter { it !in excluded }
        return if (pool.isNotEmpty()) pool.random() else randomFruit()
    }

    // ------------------------------------------------------------------
    // التبديل
    // ------------------------------------------------------------------

    fun isAdjacent(r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val dr = kotlin.math.abs(r1 - r2)
        val dc = kotlin.math.abs(c1 - c2)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }

    fun canSwap(r1: Int, c1: Int, r2: Int, c2: Int, board: List<List<Tile>>): Boolean {
        if (!isAdjacent(r1, c1, r2, c2)) return false
        val tile1 = board.getOrNull(r1)?.getOrNull(c1) ?: return false
        val tile2 = board.getOrNull(r2)?.getOrNull(c2) ?: return false

        if (tile1.isRainbow() || tile2.isRainbow()) return true
        if (tile1.hasSpecial() && tile2.hasSpecial()) return true

        return findRawMatches(swapTiles(r1, c1, r2, c2, board)).isNotEmpty()
    }

    fun swapTiles(r1: Int, c1: Int, r2: Int, c2: Int, board: List<List<Tile>>): List<List<Tile>> {
        val newBoard = board.map { it.toMutableList() }.toMutableList()
        val t1 = newBoard[r1][c1]
        val t2 = newBoard[r2][c2]
        newBoard[r1][c1] = t2.copy(row = r1, col = c1)
        newBoard[r2][c2] = t1.copy(row = r2, col = c2)
        return newBoard
    }

    // ------------------------------------------------------------------
    // كشف المطابقات
    // ------------------------------------------------------------------

    data class MatchResult(
        val matchedTiles: Set<Pair<Int, Int>>,
        val newSpecialTiles: Map<Pair<Int, Int>, SpecialType>,
        val scoreEarned: Int,
        val collectedByType: Map<FruitType, Int>
    )

    private val EMPTY_RESULT = MatchResult(emptySet(), emptyMap(), 0, emptyMap())

    private class Runs(
        val horizontal: List<List<Pair<Int, Int>>>,
        val vertical: List<List<Pair<Int, Int>>>
    ) {
        fun isEmpty() = horizontal.isEmpty() && vertical.isEmpty()
    }

    /** مسح واحد للوحة لإيجاد كل الخطوط (3+) الأفقية والعمودية. */
    private fun scanRuns(board: List<List<Tile>>): Runs {
        val rows = board.size
        val cols = board[0].size
        val h = ArrayList<List<Pair<Int, Int>>>()
        val v = ArrayList<List<Pair<Int, Int>>>()

        for (r in 0 until rows) {
            var c = 0
            while (c < cols) {
                val t = board[r][c]
                if (t.isRainbow()) { c++; continue }
                var len = 1
                while (c + len < cols && !board[r][c + len].isRainbow() && board[r][c + len].type == t.type) len++
                if (len >= 3) h.add((c until c + len).map { r to it })
                c += len
            }
        }
        for (c in 0 until cols) {
            var r = 0
            while (r < rows) {
                val t = board[r][c]
                if (t.isRainbow()) { r++; continue }
                var len = 1
                while (r + len < rows && !board[r + len][c].isRainbow() && board[r + len][c].type == t.type) len++
                if (len >= 3) v.add((r until r + len).map { it to c })
                r += len
            }
        }
        return Runs(h, v)
    }

    fun findRawMatches(board: List<List<Tile>>): Set<Pair<Int, Int>> {
        val runs = scanRuns(board)
        val out = mutableSetOf<Pair<Int, Int>>()
        runs.horizontal.forEach { out.addAll(it) }
        runs.vertical.forEach { out.addAll(it) }
        return out
    }

    // ------------------------------------------------------------------
    // توسيع تأثير الفواكه الخاصة (تفاعل متسلسل)
    // ------------------------------------------------------------------

    /** النوع الأكثر ظهوراً على اللوحة (هدف فاكهة النجمة عند إطلاقها ضمن سلسلة). */
    private fun mostCommonType(board: List<List<Tile>>): FruitType? =
        board.flatten().filter { !it.isRainbow() }.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key

    private fun expandSpecials(
        board: List<List<Tile>>,
        start: Set<Pair<Int, Int>>,
        protectedCoords: Set<Pair<Int, Int>> = emptySet()
    ): Set<Pair<Int, Int>> {
        val rows = board.size
        val cols = board[0].size
        val expanded = start.toMutableSet()
        val queue = ArrayDeque(start)

        fun add(coord: Pair<Int, Int>) {
            if (coord !in protectedCoords && expanded.add(coord)) queue.add(coord)
        }

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            when (board[r][c].special) {
                SpecialType.BOMB -> getAreaCoords(r, c, 1, rows, cols).forEach(::add)
                SpecialType.STRIPED_HORIZONTAL -> for (cc in 0 until cols) add(r to cc)
                SpecialType.STRIPED_VERTICAL -> for (rr in 0 until rows) add(rr to c)
                SpecialType.STARFRUIT_RAINBOW -> {
                    val target = mostCommonType(board)
                    if (target != null) {
                        for (rr in 0 until rows) for (cc in 0 until cols) {
                            val t = board[rr][cc]
                            if (!t.isRainbow() && t.type == target) add(rr to cc)
                        }
                    }
                }
                SpecialType.NONE -> Unit
            }
        }
        return expanded
    }

    /** يفجّر مجموعة خلايا (مع تفعيل الفواكه الخاصة بداخلها). يُستخدم للأدوات والمكافأة والدمج. */
    fun explode(board: List<List<Tile>>, coords: Set<Pair<Int, Int>>, bonus: Int = 0): MatchResult {
        val expanded = expandSpecials(board, coords)
        return tally(board, expanded, emptyMap(), bonus)
    }

    private fun tally(
        board: List<List<Tile>>,
        coords: Set<Pair<Int, Int>>,
        newSpecials: Map<Pair<Int, Int>, SpecialType>,
        bonus: Int
    ): MatchResult {
        val collected = mutableMapOf<FruitType, Int>()
        coords.forEach { (r, c) ->
            val t = board[r][c]
            if (!t.isRainbow()) collected[t.type] = (collected[t.type] ?: 0) + 1
        }
        var score = coords.size * TILE_SCORE + bonus
        newSpecials.values.forEach {
            score += when (it) {
                SpecialType.STARFRUIT_RAINBOW -> 300
                SpecialType.BOMB -> 200
                SpecialType.STRIPED_HORIZONTAL, SpecialType.STRIPED_VERTICAL -> 100
                SpecialType.NONE -> 0
            }
        }
        return MatchResult(coords, newSpecials, score, collected)
    }

    private const val TILE_SCORE = 40

    // ------------------------------------------------------------------
    // الدمج بين فاكهتين خاصتين (بالتبديل)
    // ------------------------------------------------------------------

    private fun comboResult(
        board: List<List<Tile>>,
        a: Pair<Int, Int>,
        b: Pair<Int, Int>
    ): MatchResult? {
        val rows = board.size
        val cols = board[0].size
        val tA = board[a.first][a.second]
        val tB = board[b.first][b.second]
        val bothSpecial = tA.hasSpecial() && tB.hasSpecial()
        if (!bothSpecial && !tA.isRainbow() && !tB.isRainbow()) return null

        // نجمتان: مسح اللوحة بالكامل
        if (tA.isRainbow() && tB.isRainbow()) {
            val all = (0 until rows).flatMap { r -> (0 until cols).map { c -> r to c } }.toSet()
            return tally(board, all, emptyMap(), 2000)
        }

        // نجمة + فاكهة أخرى
        if (tA.isRainbow() || tB.isRainbow()) {
            val rainbowPos = if (tA.isRainbow()) a else b
            val other = if (tA.isRainbow()) tB else tA
            val targets = mutableSetOf(rainbowPos)
            for (r in 0 until rows) for (c in 0 until cols) {
                val t = board[r][c]
                if (!t.isRainbow() && t.type == other.type) targets.add(r to c)
            }
            return when (other.special) {
                SpecialType.NONE -> tally(board, targets, emptyMap(), 500)
                else -> {
                    // نحوّل كل فواكه هذا اللون إلى النوع الخاص نفسه ثم نفجّرها
                    val converted = board.mapIndexed { r, row ->
                        row.mapIndexed { c, t ->
                            when {
                                (r to c) == rainbowPos -> t.copy(special = SpecialType.NONE)
                                (r to c) in targets -> t.copy(
                                    special = if (other.special == SpecialType.BOMB) SpecialType.BOMB
                                    else if (Random.nextBoolean()) SpecialType.STRIPED_HORIZONTAL else SpecialType.STRIPED_VERTICAL
                                )
                                else -> t
                            }
                        }
                    }
                    val bonus = if (other.special == SpecialType.BOMB) 2200 else 1800
                    val expanded = expandSpecials(converted, targets)
                    tally(board, expanded, emptyMap(), bonus)
                }
            }
        }

        val isStripeA = tA.special == SpecialType.STRIPED_HORIZONTAL || tA.special == SpecialType.STRIPED_VERTICAL
        val isStripeB = tB.special == SpecialType.STRIPED_HORIZONTAL || tB.special == SpecialType.STRIPED_VERTICAL
        val (cr, cc) = b

        return when {
            tA.special == SpecialType.BOMB && tB.special == SpecialType.BOMB ->
                explode(board, getAreaCoords(cr, cc, 2, rows, cols), bonus = 1200)

            (tA.special == SpecialType.BOMB && isStripeB) || (tB.special == SpecialType.BOMB && isStripeA) -> {
                val affected = mutableSetOf<Pair<Int, Int>>()
                for (r in (cr - 1)..(cr + 1)) if (r in 0 until rows) for (c in 0 until cols) affected.add(r to c)
                for (c in (cc - 1)..(cc + 1)) if (c in 0 until cols) for (r in 0 until rows) affected.add(r to c)
                explode(board, affected, bonus = 1500)
            }

            isStripeA && isStripeB -> {
                val cross = mutableSetOf<Pair<Int, Int>>()
                for (c in 0 until cols) cross.add(cr to c)
                for (r in 0 until rows) cross.add(r to cc)
                explode(board, cross, bonus = 1000)
            }

            else -> null
        }
    }

    // ------------------------------------------------------------------
    // الحل الرئيسي لخطوة واحدة
    // ------------------------------------------------------------------

    fun detectAndResolveMatches(
        board: List<List<Tile>>,
        lastSwappedA: Pair<Int, Int>? = null,
        lastSwappedB: Pair<Int, Int>? = null
    ): MatchResult {
        val rows = board.size
        val cols = board[0].size

        if (lastSwappedA != null && lastSwappedB != null) {
            comboResult(board, lastSwappedA, lastSwappedB)?.let { return it }
        }

        val runs = scanRuns(board)
        if (runs.isEmpty()) return EMPTY_RESULT

        val newSpecials = mutableMapOf<Pair<Int, Int>, SpecialType>()
        fun spawn(pos: Pair<Int, Int>, type: SpecialType) {
            if (board[pos.first][pos.second].hasSpecial()) return // الخاصة تنفجر ولا تُستبدل
            val current = newSpecials[pos]
            if (current == null || rank(type) > rank(current)) newSpecials[pos] = type
        }

        (runs.horizontal + runs.vertical).filter { it.size >= 5 }.forEach {
            spawn(findBestSpawnCoord(it, lastSwappedA, lastSwappedB), SpecialType.STARFRUIT_RAINBOW)
        }
        for (h in runs.horizontal) for (v in runs.vertical) {
            h.intersect(v.toSet()).firstOrNull()?.let { spawn(it, SpecialType.BOMB) }
        }
        runs.horizontal.filter { it.size == 4 }.forEach {
            spawn(findBestSpawnCoord(it, lastSwappedA, lastSwappedB), SpecialType.STRIPED_VERTICAL)
        }
        runs.vertical.filter { it.size == 4 }.forEach {
            spawn(findBestSpawnCoord(it, lastSwappedA, lastSwappedB), SpecialType.STRIPED_HORIZONTAL)
        }

        val matched = mutableSetOf<Pair<Int, Int>>()
        runs.horizontal.forEach { matched.addAll(it) }
        runs.vertical.forEach { matched.addAll(it) }

        // مواضع الميلاد محمية: لا يمسحها تفجير سلسلة في نفس الخطوة
        val expanded = expandSpecials(board, matched, protectedCoords = newSpecials.keys)
        return tally(board, expanded, newSpecials, 0)
    }

    private fun rank(s: SpecialType) = when (s) {
        SpecialType.STARFRUIT_RAINBOW -> 3
        SpecialType.BOMB -> 2
        SpecialType.NONE -> 0
        else -> 1
    }

    private fun getAreaCoords(centerR: Int, centerC: Int, radius: Int, rows: Int, cols: Int): Set<Pair<Int, Int>> {
        val result = mutableSetOf<Pair<Int, Int>>()
        for (r in (centerR - radius)..(centerR + radius)) {
            for (c in (centerC - radius)..(centerC + radius)) {
                if (r in 0 until rows && c in 0 until cols) result.add(r to c)
            }
        }
        return result
    }

    private fun findBestSpawnCoord(
        line: List<Pair<Int, Int>>,
        a: Pair<Int, Int>?,
        b: Pair<Int, Int>?
    ): Pair<Int, Int> {
        if (a != null && a in line) return a
        if (b != null && b in line) return b
        return line[line.size / 2]
    }

    // ------------------------------------------------------------------
    // الجاذبية وإعادة التعبئة
    // ------------------------------------------------------------------

    fun applyGravityAndRefill(
        board: List<List<Tile>>,
        matchedCoords: Set<Pair<Int, Int>>,
        newSpecials: Map<Pair<Int, Int>, SpecialType>
    ): List<List<Tile>> {
        val rows = board.size
        val cols = board[0].size
        val newGrid = Array(rows) { arrayOfNulls<Tile>(cols) }

        for (c in 0 until cols) {
            val column = ArrayList<Tile>(rows) // من الأسفل إلى الأعلى
            for (r in rows - 1 downTo 0) {
                val coord = r to c
                val special = newSpecials[coord]
                when {
                    special != null -> column.add(
                        Tile(id = getNextId(), row = r, col = c, type = board[r][c].type, special = special)
                    )
                    coord !in matchedCoords -> column.add(board[r][c])
                }
            }
            while (column.size < rows) {
                column.add(Tile(id = getNextId(), row = 0, col = c, type = randomFruit(), special = SpecialType.NONE, isNew = true))
            }
            for (r in rows - 1 downTo 0) {
                newGrid[r][c] = column[rows - 1 - r].copy(row = r, col = c, isMatched = false)
            }
        }
        return List(rows) { r -> List(cols) { c -> newGrid[r][c]!! } }
    }

    // ------------------------------------------------------------------
    // الحركات المتاحة والتلميح والخلط
    // ------------------------------------------------------------------

    fun hasPossibleMoves(board: List<List<Tile>>): Boolean = findHint(board) != null

    fun findHint(board: List<List<Tile>>): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val rows = board.size
        val cols = board[0].size
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (c + 1 < cols && canSwap(r, c, r, c + 1, board)) return (r to c) to (r to c + 1)
                if (r + 1 < rows && canSwap(r, c, r + 1, c, board)) return (r to c) to (r + 1 to c)
            }
        }
        return null
    }


    /**
     * اقتراح ذكي بالترتيب: ١) فواكه الهدف ٢) تطابق ٤+ أو L/T ٣) الفاكهة الخاصة
     * ٤) فتح الطريق للفواكه المطلوبة ٥) أي حركة. في آخر ٣ حركات يفضّل ما يُنهي الأهداف.
     * [needs] = ما تبقّى من كل هدف (قيم > 0 فقط)، [movesLeft] = الحركات المتبقية.
     */
    fun findSmartHint(
        board: List<List<Tile>>,
        needs: Map<FruitType, Int>,
        movesLeft: Int
    ): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
        val rows = board.size
        val cols = board[0].size
        var best: Pair<Pair<Int, Int>, Pair<Int, Int>>? = null
        var bestScore = -1.0
        for (r in 0 until rows) for (c in 0 until cols) {
            for ((dr, dc) in listOf(0 to 1, 1 to 0)) {
                val r2 = r + dr
                val c2 = c + dc
                if (r2 >= rows || c2 >= cols || !canSwap(r, c, r2, c2, board)) continue
                val sc = scoreSwap(board, r, c, r2, c2, needs, movesLeft)
                if (sc > bestScore) { bestScore = sc; best = (r to c) to (r2 to c2) }
            }
        }
        return best
    }

    private fun scoreSwap(
        board: List<List<Tile>>, r1: Int, c1: Int, r2: Int, c2: Int,
        needs: Map<FruitType, Int>, movesLeft: Int
    ): Double {
        val rows = board.size
        val cols = board[0].size
        val a = board[r1][c1]
        val b = board[r2][c2]
        val cells = HashSet<Pair<Int, Int>>()
        var sc = 0.0
        if (a.hasSpecial() || b.hasSpecial()) {
            sc += 2000
            fun fire(t: Tile, r: Int, c: Int, other: Tile) {
                when (t.special) {
                    SpecialType.STRIPED_HORIZONTAL -> for (i in 0 until cols) cells.add(r to i)
                    SpecialType.STRIPED_VERTICAL -> for (i in 0 until rows) cells.add(i to c)
                    SpecialType.BOMB -> for (dr in -1..1) for (dc in -1..1) {
                        if (r + dr in 0 until rows && c + dc in 0 until cols) cells.add((r + dr) to (c + dc))
                    }
                    SpecialType.STARFRUIT_RAINBOW -> for (row in board) for (x in row) if (x.type == other.type) cells.add(x.row to x.col)
                    else -> {}
                }
            }
            if (a.hasSpecial()) fire(a, r2, c2, b)
            if (b.hasSpecial()) fire(b, r1, c1, a)
        }
        val swapped = swapTiles(r1, c1, r2, c2, board)
        val runs = scanRuns(swapped)
        var mx = 0
        var cross = false
        val seen = HashSet<Pair<Int, Int>>()
        for (run in runs.horizontal + runs.vertical) {
            mx = maxOf(mx, run.size)
            for (k in run) { if (!seen.add(k)) cross = true; cells.add(k) }
        }
        var goal = 0
        for ((r, c) in cells) {
            val t = swapped[r][c]
            if ((needs[t.type] ?: 0) > 0) goal++
        }
        val late = movesLeft <= 3
        val allMet = needs.isNotEmpty() && needs.all { (t, n) -> cells.count { swapped[it.first][it.second].type == t } >= n }
        sc += minOf(goal, 12) * 1000 + (if (mx >= 4 || cross) 700 else 0) + (if (mx >= 5) 200 else 0) +
            cells.size * (if (late) 150 else 1) + (if (late && allMet) 5000 else 0)
        return sc
    }

    /**
     * يعيد ترتيب الفواكه الحالية (مع الحفاظ على الخاصة) إلى لوحة بلا مطابقات فورية
     * وفيها حركة صالحة على الأقل.
     */
    fun reshuffleBoardWhenNoMoves(board: List<List<Tile>>): List<List<Tile>> {
        val rows = board.size
        val cols = board[0].size
        val pool = board.flatten().map {
            it.copy(id = getNextId(), isMatched = false, isSelected = false, isNew = true)
        }

        repeat(150) {
            val shuffled = pool.shuffled()
            var idx = 0
            val candidate = List(rows) { r -> List(cols) { c -> shuffled[idx++].copy(row = r, col = c) } }
            if (findRawMatches(candidate).isEmpty() && hasPossibleMoves(candidate)) return candidate
        }

        // احتياط: لوحة جديدة مع إعادة توزيع الفواكه الخاصة
        val specials = pool.filter { it.hasSpecial() }
        val grid = Array(rows) { arrayOfNulls<Tile>(cols) }
        for (r in 0 until rows) for (c in 0 until cols) {
            grid[r][c] = Tile(id = getNextId(), row = r, col = c, type = getRandomFruitExcluding(grid, r, c), isNew = true)
        }
        specials.forEach { s ->
            val r = (0 until rows).random()
            val c = (0 until cols).random()
            grid[r][c] = grid[r][c]!!.copy(special = s.special)
        }
        var result = List(rows) { r -> List(cols) { c -> grid[r][c]!! } }

        if (!hasPossibleMoves(result)) {
            val m = result.map { it.toMutableList() }
            val t = m[0][0].type
            val other = activeFruits.first { it != t }
            m[0][1] = m[0][1].copy(type = t, special = SpecialType.NONE)
            m[0][2] = m[0][2].copy(type = other, special = SpecialType.NONE)
            m[1][2] = m[1][2].copy(type = t, special = SpecialType.NONE)
            result = m
        }
        return result
    }

    fun shuffleBoard(board: List<List<Tile>>): List<List<Tile>> = reshuffleBoardWhenNoMoves(board)
}
