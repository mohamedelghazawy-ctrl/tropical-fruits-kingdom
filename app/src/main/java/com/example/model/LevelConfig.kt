package com.example.model

data class LevelConfig(
    val levelNumber: Int,
    val titleAr: String,
    val islandName: String,
    val targetScore: Int,
    val maxMoves: Int,
    val targetFruits: Map<FruitType, Int> = emptyMap(),
    val difficultyLabel: String = "عادي 🌴",
    val completionCount: Int = 0,
    val starThresholds: Triple<Int, Int, Int> = Triple(
        targetScore,
        (targetScore * 1.35).toInt(),
        (targetScore * 1.85).toInt()
    )
) {
    /** عدد أنواع الفواكه على اللوحة: يبدأ بـ5 ويزداد مع المستوى لرفع الصعوبة تدريجياً. */
    val fruitCount: Int
        get() = (5 + (if (levelNumber >= 4) 1 else 0) + (if (levelNumber >= 8) 1 else 0))
            .coerceAtMost(FruitType.standardFruits.size)

    /** فاكهة الجزيرة المرسومة على خريطة الجزر. null = نجمة قوس قزح (جنة الفواكه الأسطورية). */
    val islandFruit: FruitType?
        get() = ISLAND_FRUITS[(levelNumber - 1) % ISLAND_FRUITS.size]

    /** الفواكه النشطة: تتضمن دائماً فواكه أهداف المستوى ثم تُكمَّل بترتيب التعداد. */
    val activeFruits: List<FruitType>
        get() = (targetFruits.keys.toList() + FruitType.standardFruits)
            .distinct()
            .take(maxOf(fruitCount, targetFruits.size))

    companion object {
        /** بنفس ترتيب ISLAND_NAMES: كل جزيرة تحمل فاكهة اسمها. */
        private val ISLAND_FRUITS: List<FruitType?> = listOf(
            FruitType.PINEAPPLE, FruitType.MANGO, FruitType.DRAGONFRUIT, FruitType.STRAWBERRY,
            FruitType.COCONUT, FruitType.MANGO, FruitType.KIWI, FruitType.WATERMELON,
            FruitType.PINEAPPLE, null, FruitType.DRAGONFRUIT, FruitType.COCONUT
        )

        // Pre-defined island names and lore
        private val ISLAND_NAMES = listOf(
            "Palm Beach" to "شاطئ النخيل الذهبي",
            "Mango Falls" to "شلال المانجو العذب",
            "Dragonfruit Jungle" to "غابة الدراجون الساحرة",
            "Strawberry Reef" to "بستان الفراولة المرجاني",
            "Coconut Cove" to "كهف جوز الهند السري",
            "Fruit Volcano" to "بركان الفواكه المتفجر",
            "Emerald Kiwi Valley" to "وادي الكيوي الزمردي",
            "Watermelon Oasis" to "واحة البطيخ المنعشة",
            "Royal Pineapple Peak" to "قمة الأناناس الملكية",
            "Legendary Paradise" to "جنة الفواكه الأسطورية",
            "Sunset Coral Bay" to "خليج المرجان الوردي",
            "Crystal Lagoon" to "بحيرة الكريستال الاستوائية"
        )

        /**
         * نظام إدارة وتوليد المستويات المتصاعد في الصعوبة.
         * تزداد الأهداف (مثل جمع 20 فراولة، ثم 25، 30...)
         * وتتزايد المتطلبات بعد كل إكمال ناجح (completionCount) ومع تقدم رقم المستوى.
         */
        fun getLevel(num: Int, completionCount: Int = 0): LevelConfig {
            val levelNumber = num.coerceAtLeast(1)
            // Scaling factor increases with each successful completion
            val replayBoost = 1.0f + (completionCount * 0.15f)

            val islandIndex = (levelNumber - 1) % ISLAND_NAMES.size
            val (baseEnName, baseArTitle) = ISLAND_NAMES[islandIndex]
            val tier = ((levelNumber - 1) / ISLAND_NAMES.size) + 1
            val titleAr = if (tier > 1) "$baseArTitle - الجزء $tier" else baseArTitle
            val islandName = if (tier > 1) "$baseEnName Pt.$tier" else baseEnName

            // Progressive score formula
            // Move budget scales tightly as levels advance
            val moveReduction = ((levelNumber - 1) / 3).coerceAtMost(8)
            val maxMoves = (22 - moveReduction).coerceAtLeast(14)

            // معايَر بمحاكاة لاعب آلي: ~٣٠٠ نقطة لكل حركة (مطابق لنسخة الويب)
            val scaledTargetScore = (maxMoves * (130 + levelNumber) * replayBoost).toInt()

            // Dynamic fruit target objectives based on level and completion mastery
            val targetFruits = generateTargetFruits(levelNumber, replayBoost)

            // Difficulty tag
            val difficultyLabel = when {
                levelNumber % 5 == 0 -> "زعيم الجزيرة 👑"
                levelNumber >= 8 -> "تحدي ناري 🔥"
                levelNumber >= 5 -> "صعب ⚡"
                levelNumber >= 3 -> "متوسط 🍍"
                else -> "سهل 🌴"
            }

            return LevelConfig(
                levelNumber = levelNumber,
                titleAr = titleAr,
                islandName = islandName,
                targetScore = scaledTargetScore,
                maxMoves = maxMoves,
                targetFruits = targetFruits,
                difficultyLabel = difficultyLabel,
                completionCount = completionCount
            )
        }

        private fun generateTargetFruits(levelNumber: Int, boost: Float): Map<FruitType, Int> {
            val goals = mutableMapOf<FruitType, Int>()
            val three = levelNumber == 5 || levelNumber == 10 || (levelNumber > 10 && levelNumber % 3 == 0)
            val ns = needScale(levelNumber) * (if (three) 0.85 else 1.0)
            fun scaled(v: Float): Int = maxOf(MIN_NEED, (v * ns).toInt())

            when (levelNumber) {
                1 -> {
                    // Level 1: Introduction to Pineapple
                    goals[FruitType.PINEAPPLE] = scaled(10 * boost)
                }
                2 -> {
                    // Level 2: Mango & Watermelon
                    goals[FruitType.MANGO] = scaled(14 * boost)
                    goals[FruitType.WATERMELON] = scaled(10 * boost)
                }
                3 -> {
                    // Level 3: Dragonfruit & Kiwi
                    goals[FruitType.DRAGONFRUIT] = scaled(16 * boost)
                    goals[FruitType.KIWI] = scaled(12 * boost)
                }
                4 -> {
                    // Level 4: Strawberry milestone (e.g., collect 20 strawberries!)
                    goals[FruitType.STRAWBERRY] = scaled(20 * boost)
                    goals[FruitType.PINEAPPLE] = scaled(15 * boost)
                }
                5 -> {
                    // Level 5: Island Boss - Triple goal with Strawberries
                    goals[FruitType.STRAWBERRY] = scaled(22 * boost)
                    goals[FruitType.DRAGONFRUIT] = scaled(18 * boost)
                    goals[FruitType.COCONUT] = scaled(16 * boost)
                }
                6 -> {
                    // Level 6: High challenge
                    goals[FruitType.MANGO] = scaled(24 * boost)
                    goals[FruitType.STRAWBERRY] = scaled(22 * boost)
                }
                7 -> {
                    // Level 7: Watermelon & Kiwi challenge
                    goals[FruitType.WATERMELON] = scaled(26 * boost)
                    goals[FruitType.KIWI] = scaled(24 * boost)
                }
                8 -> {
                    // Level 8: Strawberries & Pineapples
                    goals[FruitType.STRAWBERRY] = scaled(28 * boost)
                    goals[FruitType.PINEAPPLE] = scaled(25 * boost)
                }
                9 -> {
                    // Level 9: Dragonfruits & Coconuts
                    goals[FruitType.DRAGONFRUIT] = scaled(30 * boost)
                    goals[FruitType.COCONUT] = scaled(26 * boost)
                }
                10 -> {
                    // Level 10: Grand Volcano Boss
                    goals[FruitType.STRAWBERRY] = scaled(32 * boost)
                    goals[FruitType.MANGO] = scaled(28 * boost)
                    goals[FruitType.WATERMELON] = scaled(26 * boost)
                }
                else -> {
                    // Procedural levels: dynamically pick 2-3 fruits with growing counts
                    val all = FruitType.standardFruits
                    val fruitA = all[(levelNumber) % all.size]
                    val fruitB = all[(levelNumber + 2) % all.size]
                    val baseCountA = scaled((20 + (levelNumber * 1.5f) * boost).coerceAtMost(45f))
                    val baseCountB = scaled((18 + (levelNumber * 1.2f) * boost).coerceAtMost(40f))

                    goals[fruitA] = baseCountA
                    goals[fruitB] = baseCountB

                    if (levelNumber % 3 == 0) {
                        val fruitC = all[(levelNumber + 4) % all.size]
                        goals[fruitC] = scaled((16 + levelNumber * boost).coerceAtMost(35f))
                    }
                }
            }

            return goals
        }

        const val MAX_LEVEL = 95
        const val MIN_NEED = 6

        /** نقاط معايرة صعوبة الأهداف (مطابقة لنسخة الويب NS_DEFAULT). */
        private val NS_POINTS = listOf(
            1 to 1.0, 5 to 1.0, 6 to 0.85, 7 to 0.7, 8 to 0.45, 12 to 0.33, 15 to 0.28,
            20 to 0.26, 30 to 0.23, 40 to 0.25, 60 to 0.26, 95 to 0.27
        )

        fun needScale(n: Int): Double {
            if (n <= NS_POINTS.first().first) return NS_POINTS.first().second
            for (i in 1 until NS_POINTS.size) {
                val (b, fb) = NS_POINTS[i]
                if (n <= b) { val (a, fa) = NS_POINTS[i - 1]; return fa + (fb - fa) * (n - a) / (b - a).toDouble() }
            }
            return NS_POINTS.last().second
        }

        // Default list of levels for convenience or fallbacks
        val ALL_LEVELS: List<LevelConfig> get() = getAvailableLevels(count = MAX_LEVEL)

        // Returns a preview list of levels for the Map screen
        fun getAvailableLevels(completions: Map<Int, Int> = emptyMap(), count: Int = MAX_LEVEL): List<LevelConfig> {
            return (1..count).map { lvl ->
                getLevel(lvl, completions[lvl] ?: 0)
            }
        }
    }
}
