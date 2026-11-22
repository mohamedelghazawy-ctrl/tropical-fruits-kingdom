package com.example.island

/** بيانات عرض الجزيرة (إحداثيات المباني على صورة الجزيرة 1792×2400 وقياسات الصور). */
object IslandData {
    const val ISL_W = 1792f
    const val ISL_H = 2400f
    const val BDS = 0.83f
    const val WORLD_W = 1400f
    const val WORLD_H = 1875f           // = round(1400 * 2400 / 1792)
    const val IFAC = WORLD_W / ISL_W

    /** مركز القاعدة لكل مبنى (id -> x,y). القصر id = 0. */
    val POS: Map<Int, Pair<Float, Float>> = mapOf(
        1 to (381f to 803f),
        8 to (1400f to 792f),
        3 to (549f to 1061f),
        4 to (1243f to 1061f),
        5 to (302f to 1280f),
        10 to (1467f to 1280f),
        6 to (874f to 1258f),
        2 to (470f to 1536f),
        9 to (1299f to 1536f),
        7 to (883f to 1849f),
        11 to (594f to 1782f),
        12 to (1165f to 1782f),
        0 to (873f to 497f)
    ).mapValues { (id, p) -> if (id == 0) p else p.first to (p.second + 48f) }

    /** قياسات صور المباني المرمَّمة (BMETA) والأطلال (RMETA): العرض والارتفاع وإزاحة القاعدة. */
    data class Meta(val w: Int, val h: Int, val off: Int)
    val BMETA: Map<Int, Meta> = mapOf(
        1 to Meta(460, 397, 96),
        2 to Meta(460, 421, 93),
        3 to Meta(366, 300, 68),
        4 to Meta(387, 436, 87),
        5 to Meta(404, 344, 78),
        6 to Meta(405, 280, 85),
        7 to Meta(231, 522, 51),
        8 to Meta(426, 383, 80),
        9 to Meta(468, 361, 87),
        10 to Meta(426, 472, 80),
        11 to Meta(332, 351, 67),
        12 to Meta(478, 489, 96)
    )
    val RMETA: Map<Int, Meta> = mapOf(
        1 to Meta(436, 366, 89),
        2 to Meta(455, 417, 92),
        3 to Meta(360, 304, 68),
        4 to Meta(392, 400, 80),
        5 to Meta(395, 346, 79),
        6 to Meta(394, 280, 85),
        7 to Meta(243, 509, 50),
        8 to Meta(427, 382, 80),
        9 to Meta(461, 351, 85),
        10 to Meta(400, 461, 78),
        11 to Meta(304, 307, 58),
        12 to Meta(545, 477, 94)
    )

    val RESIDENTS = listOf(
        "مايا", "ديفيد", "عائشة", "روبرت", "سارة", "جيمس", "إيميلي", "بن", "كلوي", "آرثر", "أوليفيا",
        "مايكل", "صوفيا", "توم", "مارغريت", "جون", "إيزابيلا", "رايان", "أليس", "فرانك", "غريس"
    )

    val SPLASH_LINES = listOf(
        "المملكة بانتظارك لإعادة ترميمها", "أنقذ الملكة وستحظى بالكثير…", "المعصرة تجهّز لك مشروب الفاكهة الذي تحبه",
        "لا تترك المباني بدون عمال… أرباح المباني لا تعوّض", "المركب في انتظار إشارتك لتبادل البضائع",
        "الحديقة تزيد جواهر نجومك", "ارفع قصر الملكة لتحصل على حركات إضافية", "المخبز يُطعم السكان فيسرعون في العمل"
    )
}
