package com.example.model

/** جائزة واحدة في عجلة الحظ: جواهر أو أداة (أو الاثنان). */
data class WheelPrize(
    val short: String,
    val icon: String,
    val gems: Int = 0,
    val booster: BoosterType? = null,
    val amount: Int = 0,
    val weight: Int = 10
) {
    val label: String get() = if (gems > 0) "$gems جوهرة" else "${booster?.titleAr ?: ""} × $amount"
}

object LuckyWheel {
    /** مرة كل ١٢ ساعة ولا تتراكم. */
    const val COOLDOWN_MS = 12L * 60 * 60 * 1000

    // الترتيب متناوب (جواهر/أداة) ليختلف لون كل قطعتين متجاورتين
    val PRIZES: List<WheelPrize> = listOf(
        WheelPrize("٢٥", "💎", gems = 25, weight = 22),
        WheelPrize("مطرقة", "🔨", booster = BoosterType.HAMMER, amount = 1, weight = 14),
        WheelPrize("٦٠", "💎", gems = 60, weight = 12),
        WheelPrize("إعصار", "🌪️", booster = BoosterType.SHUFFLE, amount = 1, weight = 14),
        WheelPrize("٤٠", "💎", gems = 40, weight = 18),
        WheelPrize("شعاع", "⚡", booster = BoosterType.ROW_BLAST, amount = 1, weight = 14),
        WheelPrize("١٢٠", "💎", gems = 120, weight = 4),
        WheelPrize("شلال", "🌊", booster = BoosterType.COL_BLAST, amount = 1, weight = 14)
    )

    /** اختيار مرجّح بالأوزان. */
    fun pickIndex(rnd: kotlin.random.Random): Int {
        val total = PRIZES.sumOf { it.weight }
        var x = rnd.nextInt(total)
        PRIZES.forEachIndexed { i, p -> if (x < p.weight) return i; x -= p.weight }
        return 0
    }
}
