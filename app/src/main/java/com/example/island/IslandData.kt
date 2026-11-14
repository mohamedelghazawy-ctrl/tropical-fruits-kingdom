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
        1 to (320f to 830f), 2 to (1400f to 830f), 3 to (492f to 1110f), 4 to (1254f to 1122f),
        5 to (198f to 1380f), 6 to (876f to 1368f), 7 to (1530f to 1392f), 8 to (430f to 1676f),
        9 to (1326f to 1674f), 10 to (534f to 1908f), 11 to (876f to 1980f), 12 to (1236f to 1932f),
        0 to (896f to 470f)
    )

    /** قياسات صور المباني المرمَّمة (BMETA) والأطلال (RMETA): العرض والارتفاع وإزاحة القاعدة. */
    data class Meta(val w: Int, val h: Int, val off: Int)
    val BMETA: Map<Int, Meta> = mapOf(
        1 to Meta(352, 288, 70),
        2 to Meta(357, 317, 70),
        3 to Meta(277, 240, 54),
        4 to Meta(304, 305, 61),
        5 to Meta(326, 285, 65),
        6 to Meta(327, 215, 65),
        7 to Meta(204, 418, 41),
        8 to Meta(335, 316, 66),
        9 to Meta(362, 295, 71),
        10 to Meta(292, 344, 58),
        11 to Meta(242, 252, 48),
        12 to Meta(358, 360, 71)
    )
    val RMETA: Map<Int, Meta> = mapOf(
        1 to Meta(350, 278, 70),
        2 to Meta(352, 312, 70),
        3 to Meta(271, 238, 54),
        4 to Meta(305, 293, 61),
        5 to Meta(322, 279, 64),
        6 to Meta(325, 213, 65),
        7 to Meta(203, 418, 41),
        8 to Meta(331, 310, 66),
        9 to Meta(356, 289, 71),
        10 to Meta(289, 320, 58),
        11 to Meta(238, 251, 48),
        12 to Meta(355, 352, 71)
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
