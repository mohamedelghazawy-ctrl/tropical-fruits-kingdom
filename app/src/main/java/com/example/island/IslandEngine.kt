package com.example.island

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * محرّك جزيرة القصر (Kotlin نقي بلا اعتماد على أندرويد): مطابق لنسخة الويب (المرحلة ٣).
 * كل الدوال تأخذ الزمن `now` بالمللي ثانية صراحةً ليسهل الاختبار.
 */
const val PALACE = 0            // معرّف القصر
const val MAX_PALACE = 30
const val IDLE_HOURS = 12
const val AD_SPEED_MS = 30 * 60_000L
const val COST_G = 1.05
const val COST_M = 1.2
const val FREE_MARKET_LV = 5     // السوق يعمل بلا عامل حتى هذا المستوى
const val GEMS_PER_STAR = 10
const val GATE_GIFT = 0.3       // هدية الزعيم: تغطي حتى ٣٠٪ من تكلفة مبنى البوابة التالية
const val GEMS_RESCUE = 30
const val DAY_MS = 86_400_000L

data class Site(
    val id: Int, val name: String, val base: Int, val gate: Int = 0, val max: Int = 30,
    val needsWorker: Boolean = false, val need: Int = 0, val soon: Boolean = false, val perk: String = ""
)

object Sites {
    const val GARDEN = 1; const val BAKERY = 2; const val MARKET = 3; const val SCHOOL = 4; const val WORKSHOP = 5
    const val FOUNTAIN = 6; const val LIGHTHOUSE = 7; const val LIBRARY = 8; const val STABLE = 9; const val MILL = 10
    const val PORT = 11; const val HALL = 12

    val PALACE_SITE = Site(PALACE, "قصر الملكة", base = 200, max = MAX_PALACE, perk = "كل مستويين: +١ حركة · بنّاء ثابت كل ١٠ مستويات")
    val ALL: List<Site> = listOf(
        Site(GARDEN, "حديقة القصر", 60, gate = 10, needsWorker = true, perk = "جواهر النجوم +٢٪ لكل مستوى"),
        Site(BAKERY, "المخبز", 80, needsWorker = true, perk = "يُطعم السكان: الجوع يُبطئ البناء والمراكب"),
        Site(MARKET, "السوق", 150, gate = 20, needsWorker = true, perk = "جواهر تتجمّع كل دقيقة"),
        Site(SCHOOL, "المدرسة", 200, needsWorker = true, perk = "يقلّل وقت البناء ١٪ لكل مستوى"),
        Site(WORKSHOP, "الورشة", 100, needsWorker = true, perk = "تصنع مطارق"),
        Site(FOUNTAIN, "ساحة النافورة", 90, perk = "عجلة أمنيات يومية"),
        Site(LIGHTHOUSE, "المنارة", 380, soon = true, perk = "قريبًا"),
        Site(LIBRARY, "المكتبة", 140, max = 10, need = 3, perk = "بيت السكان: +٣ سكان كل مستويين"),
        Site(STABLE, "الإسطبل", 140, needsWorker = true, need = 3, perk = "إحياء مجاني يوميًا (من المستوى ١٠)"),
        Site(MILL, "المعصرة", 180, needsWorker = true, need = 5, perk = "تعصر أعاصير"),
        Site(PORT, "المرفأ", 260, gate = 30, max = 20, needsWorker = true, perk = "مراكب للتبادل وبيع الحقيبة"),
        Site(HALL, "قاعة الاحتفالات", 220, needsWorker = true, need = 5, perk = "جواهر إضافية عند إنقاذ الملكة")
    )
    fun of(id: Int): Site = if (id == PALACE) PALACE_SITE else ALL.first { it.id == id }
    /** المباني التي تدخل في شروط ترقية القصر (بلا المنارة). */
    val ACTIVE: List<Site> get() = ALL.filter { !it.soon }
}

/** مستويات القصر المشترطة لفتح الطريق بعد الجزر ٤٠، ٥٠، … */
val PAL_GATES: Map<Int, Int> = mapOf(40 to 4, 50 to 6, 60 to 9, 70 to 13, 80 to 17, 90 to 25)

enum class Item(val id: String, val sell: Int) { HAMMER("hammer", 10), SHUFFLE("shuffle", 12), ROW("row", 10), COL("col", 10) }

data class Build(val id: Int, val n: Int, val start: Long, var end: Long)
data class Boat(val sell: Boolean, val give: Item, val get: Item?, val n: Int, var end: Long)
class Acc(var v: Double = 0.0, var t: Long = 0L)

/** سبب منع الترقية أو نجاحها. */
data class Up(val ok: Boolean, val why: String, val n: Int, val cost: Int, val ms: Long, val maxed: Boolean = false, val needGems: Boolean = false)

class IslandState {
    var gems = 0
    val lv = mutableMapOf<Int, Int>()
    var pal = 1
    val q = mutableListOf<Build>()
    val prod = mutableMapOf<Int, Acc>()
    val wk = mutableMapOf<Int, Int>()       // مبنى -> (رقم الساكن + ١)
    val boats = mutableListOf<Boat?>()
    var ad24 = 0L
    val news = mutableListOf<String>()
    var revDay = -1L; var revUsed = 0
    var wel = 0                              // عدد السكان الذين رُحّب بهم
    var wishDay = -1L
    val inv = mutableMapOf(Item.HAMMER to 0, Item.SHUFFLE to 0, Item.ROW to 0, Item.COL to 0)
    var seen = false
}

class IslandEngine(val s: IslandState = IslandState(), val rng: Random = Random.Default) {
    fun lvOf(id: Int) = if (id == PALACE) s.pal else (s.lv[id] ?: 0)
    fun has(id: Int) = lvOf(id) >= 1
    private fun maxOf(site: Site) = site.max
    val residents get() = 3 + 3 * (lvOf(Sites.LIBRARY) / 2)
    fun workerOf(id: Int) = (s.wk[id] ?: 0) - 1
    val assigned get() = Sites.ALL.count { it.needsWorker && has(it.id) && (s.wk[it.id] ?: 0) > 0 }
    fun freeResidents(): List<Int> { val used = s.wk.values.filter { it > 0 }.toSet(); return (0 until residents).filter { (it + 1) !in used } }
    fun workerFree(id: Int) = id == Sites.MARKET && lvOf(Sites.MARKET) <= FREE_MARKET_LV
    fun noWorker() = Sites.ALL.filter { it.needsWorker && has(it.id) && (s.wk[it.id] ?: 0) == 0 && !workerFree(it.id) }
    fun worked(id: Int): Boolean { val site = Sites.of(id); return !site.needsWorker || (has(id) && ((s.wk[id] ?: 0) > 0 || workerFree(id))) }
    fun foodRatio(): Double = if (has(Sites.BAKERY) && worked(Sites.BAKERY)) min(1.0, (3 + 2 * lvOf(Sites.BAKERY)).toDouble() / residents) else .4
    fun penalty() = 1 + (1 - foodRatio()) * .5
    fun builders(now: Long) = 1 + s.pal / 10 + (if (s.ad24 > now) 1 else 0)
    fun costOf(id: Int, n: Int): Int = max(5, (Sites.of(id).base * COST_M * COST_G.pow(n - 1) / 5).roundToInt() * 5)
    fun durMs(id: Int, n: Int): Long {
        var m = if (n <= 1) 1.0 else min(18.0 * 60, 5 * 1.2.pow(n - 2)); if (id == PALACE) m *= 1.5
        val sc = if (worked(Sites.SCHOOL)) min(.3, .01 * lvOf(Sites.SCHOOL)) else 0.0
        return max(30_000L, (m * 60_000 * (1 - sc) * penalty()).roundToInt().toLong())
    }
    fun inQ(id: Int) = s.q.firstOrNull { it.id == id }
    fun builtCount() = Sites.ACTIVE.count { has(it.id) }
    fun palReq(): List<Int> {
        val n = s.pal; val e = Sites.ACTIVE.filter { it.max >= n }.map { it.id }; val i = (2 * (n - 1)) % e.size
        return listOf(e[i], e[(i + 1) % e.size])
    }

    fun canUp(id: Int, now: Long): Up {
        val x = Sites.of(id); val n = lvOf(id) + 1; val cost = costOf(id, n); val ms = durMs(id, n)
        fun no(w: String, maxed: Boolean = false, g: Boolean = false) = Up(false, w, n, cost, ms, maxed, g)
        if (x.soon) return no("قريبًا")
        if (n > x.max) return no("بلغ أقصى مستوى", maxed = true)
        if (inQ(id) != null) return no("قيد البناء")
        if (x.need > 0 && builtCount() < x.need && n == 1) return no("يُفتح بعد ترميم ${x.need} مبانٍ")
        if (id == PALACE) {
            val bad = palReq().filter { lvOf(it) < s.pal }
            if (bad.isNotEmpty()) return no("ارفع أولًا إلى المستوى ${s.pal}: " + bad.joinToString(" و") { Sites.of(it).name })
        } else if (n > s.pal) return no("ارفع القصر أولًا إلى المستوى $n")
        if (s.q.size >= builders(now)) return no("كل البنّائين مشغولون")
        if (cost > s.gems) return no("ينقصك ${cost - s.gems} جوهرة", g = true)
        return Up(true, "", n, cost, ms)
    }
    fun startBuild(id: Int, now: Long): Boolean {
        val c = canUp(id, now); if (!c.ok) return false
        s.gems -= c.cost; s.q.add(Build(id, c.n, now, now + c.ms)); return true
    }
    fun speedUpByAd(id: Int) { inQ(id)?.let { it.end -= AD_SPEED_MS } }
    fun grantAdBuilder(now: Long) { s.ad24 = now + DAY_MS }

    // ---- الإنتاج: السوق جواهر، الورشة مطارق، المعصرة أعاصير
    private fun rate(id: Int, l: Int): Double = when (id) {
        Sites.MARKET -> 1.5 + .8 * (l - 1)
        Sites.WORKSHOP -> (1 + .15 * (l - 1)) / 12
        Sites.MILL -> (1 + .12 * (l - 1)) / 12
        else -> 0.0
    }
    fun produces(id: Int) = id == Sites.MARKET || id == Sites.WORKSHOP || id == Sites.MILL
    fun prodVal(id: Int, now: Long): Double {
        if (!produces(id) || !has(id)) return 0.0; val p = s.prod[id] ?: Acc(0.0, now)
        if (!worked(id)) return p.v; val r = rate(id, lvOf(id))
        return min(r * IDLE_HOURS, p.v + r * max(0L, now - p.t) / 3_600_000.0)
    }
    fun prodCap(id: Int) = if (produces(id) && has(id) && worked(id)) rate(id, lvOf(id)) * IDLE_HOURS else 0.0
    private fun settle(id: Int, now: Long) { val p = s.prod.getOrPut(id) { Acc(0.0, now) }; p.v = prodVal(id, now); p.t = now }
    fun setWorker(id: Int, idx: Int, now: Long): Boolean {
        if (idx >= 0 && (idx !in freeResidents() || !has(id))) return false
        if (produces(id)) settle(id, now)
        if (idx >= 0) s.wk[id] = idx + 1 else s.wk.remove(id)
        if (produces(id)) s.prod[id]!!.t = now
        return true
    }
    fun collect(id: Int, doubled: Boolean, now: Long): Int {
        val v = prodVal(id, now); val amt = floor(v).toInt(); if (!produces(id) || amt < 1) return 0
        val p = s.prod.getOrPut(id) { Acc(0.0, now) }; p.v = v - amt; p.t = now
        val got = amt * (if (doubled) 2 else 1)
        when (id) { Sites.MARKET -> s.gems += got; Sites.WORKSHOP -> s.inv[Item.HAMMER] = s.inv.getValue(Item.HAMMER) + got; else -> s.inv[Item.SHUFFLE] = s.inv.getValue(Item.SHUFFLE) + got }
        return got
    }
    fun readyList(now: Long) = listOf(Sites.MARKET, Sites.WORKSHOP, Sites.MILL).filter { has(it) && worked(it) && prodVal(it, now) >= (if (it == Sites.MARKET) 10 else 1) }

    // ---- المرفأ والإسطبل والقاعة والحديقة والحركات
    fun boatCount() = if (has(Sites.PORT)) 1 + lvOf(Sites.PORT) / 5 else 0
    fun tripQty() = 2 + lvOf(Sites.PORT) / 4
    fun tripMs() = (max(2.0, 5 * (1 - .02 * lvOf(Sites.PORT))) * 60_000 * penalty()).toLong()
    fun boatGo(i: Int, sell: Boolean, give: Item, get: Item?, n: Int, now: Long): Boolean {
        while (s.boats.size < boatCount()) s.boats.add(null)
        if (i !in s.boats.indices || s.boats[i] != null || !worked(Sites.PORT) || s.inv.getValue(give) < n || n > tripQty()) return false
        s.inv[give] = s.inv.getValue(give) - n; s.boats[i] = Boat(sell, give, get, n, now + tripMs()); return true
    }
    fun dayOf(now: Long, tzOffsetMs: Long = 0) = (now + tzOffsetMs).floorDiv(DAY_MS)
    fun freeRevives(now: Long, tz: Long = 0): Int {
        val l = lvOf(Sites.STABLE); val n = if (!worked(Sites.STABLE)) 0 else if (l >= 30) 3 else if (l >= 20) 2 else if (l >= 10) 1 else 0
        return max(0, n - (if (s.revDay == dayOf(now, tz)) s.revUsed else 0))
    }
    fun useRevive(now: Long, tz: Long = 0) { val d = dayOf(now, tz); if (s.revDay != d) { s.revDay = d; s.revUsed = 0 }; s.revUsed++ }
    fun hallBonus() = if (worked(Sites.HALL)) 5 * lvOf(Sites.HALL) else 0
    fun gardenMul() = if (worked(Sites.GARDEN)) 1 + min(.6, .02 * lvOf(Sites.GARDEN)) else 1.0
    fun palMoves() = s.pal / 2
    /** جواهر الفوز بنجوم جديدة (مع حافز الحديقة). */
    fun gemsForStars(newStars: Int) = (max(0, newStars) * GEMS_PER_STAR * gardenMul()).roundToInt()

    /** عجلة الأمنيات: تعيد وصف الجائزة، أو null إن كانت قد استُخدمت اليوم. */
    fun wish(now: Long, tz: Long = 0): Pair<String, Int>? {
        val l = lvOf(Sites.FOUNTAIN); val d = dayOf(now, tz); if (l < 1 || s.wishDay == d) return null
        data class W(val w: Double, val kind: String, val amt: Int)
        val ws = listOf(W(30.0, "gems", 10 + 3 * l), W(30.0, "gems", 20 + 6 * l), W(5 + l / 2.0, "gems", 40 + 12 * l), W(15.0, "hammer", 1 + l / 10), W(15.0, "shuffle", 1 + l / 10))
        var r = rng.nextDouble() * ws.sumOf { it.w }; var pick = ws[0]
        for (z in ws) { r -= z.w; if (r <= 0) { pick = z; break } }
        s.wishDay = d
        when (pick.kind) { "gems" -> s.gems += pick.amt; "hammer" -> s.inv[Item.HAMMER] = s.inv.getValue(Item.HAMMER) + pick.amt; else -> s.inv[Item.SHUFFLE] = s.inv.getValue(Item.SHUFFLE) + pick.amt }
        return pick.kind to pick.amt
    }

    /** يعالج البناء والمراكب المنتهية (حتى أثناء غياب اللاعب). يعيد true إن تغيّر شيء. */
    fun tick(now: Long): Boolean {
        var ch = false
        for (b in s.q.filter { it.end <= now }) {
            if (b.id == PALACE) s.pal = b.n else {
                if (produces(b.id)) settle(b.id, now); s.lv[b.id] = b.n
                if (b.n == 1 && Sites.of(b.id).needsWorker) s.news.add("👷 ${Sites.of(b.id).name} يحتاج إلى عامل — عيّنه من السكان")
                if (produces(b.id)) s.prod[b.id]!!.t = now
            }
            s.news.add("🏗️ اكتمل: ${Sites.of(b.id).name} — المستوى ${b.n}"); ch = true
        }
        if (ch) s.q.removeAll { it.end <= now }
        while (s.boats.size < boatCount()) s.boats.add(null)
        for (i in s.boats.indices) {
            val bt = s.boats[i] ?: continue
            if (bt.end <= now) {
                if (bt.sell) { s.gems += bt.n * bt.give.sell; s.news.add("⛵ بيعت ${bt.n} مقابل ${bt.n * bt.give.sell} جوهرة") }
                else { val g = bt.get!!; s.inv[g] = s.inv.getValue(g) + bt.n; s.news.add("⛵ وصلت ${bt.n} قطع") }
                s.boats[i] = null; ch = true
            }
        }
        return ch
    }
    fun takeNews(): List<String> { val n = s.news.toList(); s.news.clear(); return n }
    /** عدد السكان الجدد الذين يستحقون ترحيبًا (الفهارس). */
    fun newResidents(): List<Int> = (s.wel until residents).toList().also { if (it.isNotEmpty()) s.wel = residents }

    // ---- بوابات الطريق
    sealed class Gate { data class SiteGate(val site: Site) : Gate(); data class PalaceGate(val reqLv: Int) : Gate() }
    fun gateFor(n: Int): Gate? {
        if (n < 2 || n >= 100) return null; val k = (n - 1) / 10; if (k < 1) return null
        if (k >= 4) { val req = PAL_GATES[k * 10]; return if (req != null && s.pal < req) Gate.PalaceGate(req) else null }
        val site = Sites.ALL.firstOrNull { it.gate == k * 10 && !it.soon }
        return if (site != null && !has(site.id)) Gate.SiteGate(site) else null
    }

    /** هدية الزعيم عند أول فوز بمستوى زعيم قبل بوابة مبنى: تُضاف للجواهر وتُعاد قيمتها. */
    fun bossGift(levelNum: Int): Int {
        val g = gateFor(levelNum + 1) as? Gate.SiteGate ?: return 0
        val cost = costOf(g.site.id, 1); val need = cost - s.gems
        if (need <= 0) return 0
        val gift = min(need, kotlin.math.ceil(cost * GATE_GIFT).toInt()); s.gems += gift; return gift
    }

    // ---- الحفظ (نص بسيط سطرًا سطرًا)
    fun encode(): String = buildString {
        appendLine("gems=${s.gems}"); appendLine("pal=${s.pal}"); appendLine("ad24=${s.ad24}"); appendLine("wel=${s.wel}"); appendLine("wishDay=${s.wishDay}")
        appendLine("rev=${s.revDay},${s.revUsed}"); appendLine("seen=${if (s.seen) 1 else 0}")
        s.lv.forEach { (k, v) -> appendLine("lv.$k=$v") }; s.wk.forEach { (k, v) -> appendLine("wk.$k=$v") }
        s.prod.forEach { (k, a) -> appendLine("prod.$k=${a.v},${a.t}") }; s.q.forEach { appendLine("q=${it.id},${it.n},${it.start},${it.end}") }
        s.inv.forEach { (k, v) -> appendLine("inv.${k.id}=$v") }
        s.boats.forEachIndexed { i, b -> if (b != null) appendLine("boat.$i=${b.sell},${b.give.id},${b.get?.id ?: "-"},${b.n},${b.end}") }
        s.news.forEach { appendLine("news=${it.replace("\n", " ")}") }
    }
    companion object {
        fun decode(text: String, rng: Random = Random.Default): IslandEngine {
            val st = IslandState(); val items = Item.values().associateBy { it.id }
            for (line in text.lines()) {
                val i = line.indexOf('='); if (i < 0) continue; val k = line.substring(0, i); val v = line.substring(i + 1)
                try { when {
                    k == "gems" -> st.gems = v.toInt(); k == "pal" -> st.pal = v.toInt(); k == "ad24" -> st.ad24 = v.toLong(); k == "wel" -> st.wel = v.toInt()
                    k == "wishDay" -> st.wishDay = v.toLong(); k == "seen" -> st.seen = v == "1"
                    k == "rev" -> v.split(",").let { st.revDay = it[0].toLong(); st.revUsed = it[1].toInt() }
                    k.startsWith("lv.") -> st.lv[k.substring(3).toInt()] = v.toInt(); k.startsWith("wk.") -> st.wk[k.substring(3).toInt()] = v.toInt()
                    k.startsWith("prod.") -> v.split(",").let { st.prod[k.substring(5).toInt()] = Acc(it[0].toDouble(), it[1].toLong()) }
                    k == "q" -> v.split(",").let { st.q.add(Build(it[0].toInt(), it[1].toInt(), it[2].toLong(), it[3].toLong())) }
                    k.startsWith("inv.") -> items[k.substring(4)]?.let { st.inv[it] = v.toInt() }
                    k.startsWith("boat.") -> v.split(",").let { st.boats.add(Boat(it[0] == "true", items.getValue(it[1]), items[it[2]], it[3].toInt(), it[4].toLong())) }
                    k == "news" -> st.news.add(v)
                } } catch (e: Exception) { /* سطر تالف: يُتجاهل */ }
            }
            return IslandEngine(st, rng)
        }
    }
}
