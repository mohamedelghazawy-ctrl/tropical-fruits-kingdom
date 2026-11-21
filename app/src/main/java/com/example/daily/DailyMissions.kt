package com.example.daily

import android.content.SharedPreferences
import com.example.model.FruitType
import java.util.TimeZone
import kotlin.random.Random

enum class MType(val icon: String, val reward: Int) {
    WIN_LEVELS("🏆", 20), COLLECT_FRUIT("🍍", 20), USE_SPECIAL("✨", 25), EARN_STARS("⭐", 20),
    WIN_RESCUE("👑", 40), SPIN_WHEEL("🎡", 15), USE_BOOSTER("🔨", 15), SCORE("🎯", 20)
}

data class MissionView(
    val index: Int, val type: MType, val text: String, val progress: Int, val target: Int,
    val reward: Int, val claimed: Boolean
) {
    val done: Boolean get() = progress >= target
}

/** مهام يومية: ٣ مهام جديدة كل يوم (بتوقيت الجهاز)، مكافأة لكل مهمة + صندوق عند إنهاء الثلاث + سلسلة أيام. */
class DailyMissions(private val prefs: SharedPreferences) {

    private fun today(): Long { val now = System.currentTimeMillis(); return (now + TimeZone.getDefault().getOffset(now)) / 86_400_000L }

    /** يولّد مهام اليوم إن لم تكن مولّدة. [rescueUnlocked]: هل وصل اللاعب لأول مستوى إنقاذ؟ */
    fun ensureToday(rescueUnlocked: Boolean) {
        val day = today()
        if (prefs.getLong("dm_day", -1L) == day) return
        val rng = Random(day * 31 + 7)
        val pool = MType.entries.filter { rescueUnlocked || it != MType.WIN_RESCUE }.shuffled(rng).take(3)
        val e = prefs.edit().putLong("dm_day", day).putBoolean("dm_chest", false)
        pool.forEachIndexed { i, t ->
            val (param, target) = when (t) {
                MType.WIN_LEVELS -> -1 to (2 + rng.nextInt(3))
                MType.COLLECT_FRUIT -> rng.nextInt(FruitType.entries.size) to (25 + 5 * rng.nextInt(5))
                MType.USE_SPECIAL -> -1 to (3 + rng.nextInt(4))
                MType.EARN_STARS -> -1 to (4 + rng.nextInt(5))
                MType.WIN_RESCUE -> -1 to 1
                MType.SPIN_WHEEL -> -1 to 1
                MType.USE_BOOSTER -> -1 to (2 + rng.nextInt(2))
                MType.SCORE -> -1 to (8000 + 2000 * rng.nextInt(6))
            }
            e.putInt("dm_t$i", t.ordinal).putInt("dm_p$i", param).putInt("dm_n$i", target)
                .putInt("dm_g$i", 0).putBoolean("dm_c$i", false)
        }
        e.apply()
    }

    private fun textOf(t: MType, param: Int, n: Int): String = when (t) {
        MType.WIN_LEVELS -> "اربح $n مستويات"
        MType.COLLECT_FRUIT -> "اجمع $n × ${FruitType.entries[param.coerceIn(0, FruitType.entries.size - 1)].arabicName}"
        MType.USE_SPECIAL -> "فعّل $n فاكهة خاصة"
        MType.EARN_STARS -> "اكسب $n نجوم"
        MType.WIN_RESCUE -> "أنقذ الملكة مرة واحدة"
        MType.SPIN_WHEEL -> "ادر عجلة الحظ"
        MType.USE_BOOSTER -> "استخدم $n أدوات مساعدة"
        MType.SCORE -> "اجمع $n نقطة"
    }

    fun views(rescueUnlocked: Boolean): List<MissionView> {
        ensureToday(rescueUnlocked)
        return (0..2).map { i ->
            val t = MType.entries[prefs.getInt("dm_t$i", 0).coerceIn(0, MType.entries.size - 1)]
            val n = prefs.getInt("dm_n$i", 1)
            MissionView(i, t, textOf(t, prefs.getInt("dm_p$i", -1), n), prefs.getInt("dm_g$i", 0).coerceAtMost(n), n, t.reward, prefs.getBoolean("dm_c$i", false))
        }
    }

    /** يسجّل تقدّماً. [param] لمهمة جمع نوع فاكهة معيّن (رقم النوع). */
    fun record(type: MType, amount: Int = 1, param: Int = -1) {
        if (amount <= 0 || prefs.getLong("dm_day", -1L) != today()) return
        val e = prefs.edit(); var changed = false
        for (i in 0..2) {
            if (prefs.getInt("dm_t$i", -1) != type.ordinal) continue
            if (type == MType.COLLECT_FRUIT && prefs.getInt("dm_p$i", -2) != param) continue
            val n = prefs.getInt("dm_n$i", 1)
            val g = prefs.getInt("dm_g$i", 0)
            if (g < n) { e.putInt("dm_g$i", (g + amount).coerceAtMost(n)); changed = true }
        }
        if (changed) e.apply()
    }

    /** يستلم مكافأة مهمة منتهية؛ يعيد عدد الجواهر (٠ إن لم تكن قابلة للاستلام). */
    fun claim(index: Int): Int {
        if (index !in 0..2 || prefs.getBoolean("dm_c$index", false)) return 0
        val t = MType.entries[prefs.getInt("dm_t$index", 0)]
        if (prefs.getInt("dm_g$index", 0) < prefs.getInt("dm_n$index", 1)) return 0
        prefs.edit().putBoolean("dm_c$index", true).apply()
        return t.reward
    }

    fun chestReady(): Boolean = !prefs.getBoolean("dm_chest", false) && (0..2).all { prefs.getBoolean("dm_c$it", false) }
    fun chestClaimed(): Boolean = prefs.getBoolean("dm_chest", false)

    /** عدد الأيام المتتالية التي أُنجز فيها كل شيء (للعرض). */
    fun streak(): Int {
        val last = prefs.getLong("dm_streak_day", -10L)
        return if (last >= today() - 1) prefs.getInt("dm_streak", 0) else 0
    }

    fun chestReward(): Int = 50 + 10 * minOf(5, streakAfterClaim())
    private fun streakAfterClaim(): Int { val last = prefs.getLong("dm_streak_day", -10L); return if (last == today() - 1) prefs.getInt("dm_streak", 0) + 1 else if (last == today()) prefs.getInt("dm_streak", 1) else 1 }

    fun claimChest(): Int {
        if (!chestReady()) return 0
        val s = streakAfterClaim(); val r = chestReward()
        prefs.edit().putBoolean("dm_chest", true).putInt("dm_streak", s).putLong("dm_streak_day", today()).apply()
        return r
    }

    /** مهام جاهزة للاستلام (للشارة على الزر). */
    fun pending(rescueUnlocked: Boolean): Int = views(rescueUnlocked).count { it.done && !it.claimed } + if (chestReady()) 1 else 0
}
