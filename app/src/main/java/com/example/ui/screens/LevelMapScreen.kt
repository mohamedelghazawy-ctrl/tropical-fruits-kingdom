package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.example.R
import com.example.model.FruitType
import com.example.model.LevelConfig
import com.example.model.SpecialType
import com.example.ui.components.FruitComposable
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val LEVEL_COUNT = 95
private const val RESCUE_EVERY = 5

/** مواضع الخريطة: الجزر بالترتيب، وبعد كل ٥ جزر عقدة إنقاذ (قيمة سالبة = -(رقم الإنقاذ)). */
private fun buildSlots(rescueCount: Int): List<Int> = buildList {
    for (n in 1..LEVEL_COUNT) {
        add(n)
        if (n % RESCUE_EVERY == 0 && n / RESCUE_EVERY <= rescueCount) add(-(n / RESCUE_EVERY))
    }
}
private val GAP = 240.dp          // المسافة الرأسية بين جزيرتين
private val FOG_HEIGHT = 440.dp   // منطقة الضباب فوق آخر جزيرة
private val BOTTOM_PAD = 190.dp   // هامش أسفل الجزيرة الأولى
private const val ZIGZAG = 0.26f  // اتساع تعرّج المسار (نسبة من العرض)

/** موضع مركز الجزيرة i (من 0) أفقياً كنسبة من العرض. */
private fun islandX(i: Int): Float = 0.5f + ZIGZAG * sin(i * 1.1f)

/**
 * خريطة الجزر: بحر فعلي تُسحب رأسياً من الجزيرة الأولى في الأسفل إلى الأعلى،
 * وتنتهي بضباب وعبارة «قريباً» بعد آخر مرحلة. لكل جزيرة فاكهة اسمها.
 */
@Composable
fun LevelMapScreen(
    levelStars: Map<Int, Int>,
    highScores: Map<Int, Int>,
    levelCompletions: Map<Int, Int> = emptyMap(),
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit,
    rescueStars: Map<Int, Int> = emptyMap(),
    rescueCount: Int = 0,
    onSelectRescue: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val slots = remember(rescueCount) { buildSlots(rescueCount) }
    fun slotStars(slot: Int) = if (slot < 0) (rescueStars[-slot] ?: 0) else (levelStars[slot] ?: 0)
    val levels = remember(levelCompletions) { LevelConfig.getAvailableLevels(levelCompletions, count = LEVEL_COUNT) }

    // مرحلة إنقاذ الملكة بعد كل ٥ جزر شرطٌ لفتح الجزيرة التالية
    fun rescueBlocks(n: Int): Boolean =
        n > RESCUE_EVERY && (n - 1) % RESCUE_EVERY == 0 && (n - 1) / RESCUE_EVERY <= rescueCount &&
            (rescueStars[(n - 1) / RESCUE_EVERY] ?: 0) == 0

    fun isUnlocked(n: Int): Boolean = n == 1 || (
        ((levelStars[n - 1] ?: 0) > 0 ||
            (highScores[n - 1] ?: 0) > 0 ||
            (levelCompletions[n - 1] ?: 0) > 0) && !rescueBlocks(n)
        )

    val currentLevel = levels.firstOrNull { isUnlocked(it.levelNumber) && (levelStars[it.levelNumber] ?: 0) == 0 }
        ?.levelNumber ?: LEVEL_COUNT
    val totalStars = levelStars.values.sum()
    val currentIndex = slots.indexOf(currentLevel).coerceAtLeast(0)
    val doneFlags = remember(levelStars, rescueStars, slots) { slots.map { slotStars(it) > 0 } }

    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val phase by rememberInfiniteTransition(label = "sea").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "phase"
    )

    val sea = ImageBitmap.imageResource(R.drawable.sea_strip)
    Box(modifier = modifier.fillMaxSize().background(Color(0xFF053247))) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val widthDp = maxWidth
            val density = LocalDensity.current
            val viewportPx = with(density) { maxHeight.toPx() }
            val totalH = FOG_HEIGHT + GAP * (slots.size - 1) + BOTTOM_PAD
            fun centerY(i: Int): Dp = totalH - BOTTOM_PAD - GAP * i

            // ابدأ من الجزيرة الحالية في منتصف الشاشة
            LaunchedEffect(currentLevel) {
                snapshotFlow { scroll.maxValue }.first { it > 0 }
                val target = with(density) { centerY(currentIndex).toPx() } - viewportPx / 2f
                scroll.scrollTo(target.toInt().coerceIn(0, scroll.maxValue))
            }

            // الخريطة بإحداثيات مطلقة (يسار→يمين) حتى لا تنقلب في الواجهة العربية
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalH)
                        .background(Color(0xFF1590A8))
                        .drawBehind {
                            // نرسم المقطع الظاهر فقط لتوفير الأداء
                            val from = scroll.value - 120.dp.toPx()
                            val to = scroll.value + viewportPx + 120.dp.toPx()
                            drawSeaTiles(sea, from, to)
                            drawRoute(doneFlags, from, to, GAP.toPx(), BOTTOM_PAD.toPx())
                        }
                ) {
                    slots.forEachIndexed { i, slot ->
                        if (slot < 0) {
                            val k = -slot
                            val unlockedR = (levelStars[k * RESCUE_EVERY] ?: 0) > 0
                            RescueNode(
                                index = k,
                                unlocked = unlockedR,
                                stars = rescueStars[k] ?: 0,
                                onClick = { if (unlockedR) onSelectRescue(k) },
                                modifier = Modifier.absoluteOffset(
                                    x = widthDp * islandX(i) - ISLAND_COLUMN_WIDTH / 2,
                                    y = centerY(i) - 58.dp
                                )
                            )
                            return@forEachIndexed
                        }
                        val level = levels[slot - 1]
                        val unlocked = isUnlocked(level.levelNumber)
                        IslandNode(
                            level = level,
                            unlocked = unlocked,
                            isCurrent = level.levelNumber == currentLevel,
                            stars = levelStars[level.levelNumber] ?: 0,
                            onClick = { if (unlocked) onSelectLevel(level.levelNumber) },
                            modifier = Modifier.absoluteOffset(
                                x = widthDp * islandX(i) - ISLAND_COLUMN_WIDTH / 2,
                                y = centerY(i) - 58.dp
                            )
                        )
                    }

                    // الضباب عند نهاية الخريطة
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(FOG_HEIGHT)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFAF6FCFC), Color(0xEBEAF7F7), Color(0x00EAF7F7))
                                )
                            )
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) { drawClouds(phase) }
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 120.dp, start = 24.dp, end = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "قريباً…",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0B6A87)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مزيد من الجزر خلف الغيوم، وقد تزول قريباً ☁️",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B6A87),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            }

            // أزرار التنقل السريع: لآخر جزيرة، للجزيرة الحالية، وللجزيرة الأولى
            fun jumpTo(index: Int) {
                val target = with(density) { centerY(index).toPx() } - viewportPx / 2f
                scope.launch { scroll.animateScrollTo(target.toInt().coerceIn(0, scroll.maxValue), tween(700)) }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MapNavButton(modifier = Modifier.testTag("map_nav_top"), onClick = { jumpTo(slots.lastIndex) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "آخر جزيرة", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                MapNavButton(modifier = Modifier.testTag("map_nav_current"), onClick = { jumpTo(currentIndex) }) {
                    Text("📍", fontSize = 20.sp)
                }
                MapNavButton(modifier = Modifier.testTag("map_nav_bottom"), onClick = { jumpTo(0) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "أول جزيرة", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        // شريط علوي عائم فوق الخريطة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color(0xB30E382A), RoundedCornerShape(18.dp))
                .border(1.5.dp, Color(0x40FFFFFF), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xCC0E382A), CircleShape)
                    .border(1.5.dp, Color(0x6602C39A), CircleShape)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "الرجوع",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "خريطة الجزر 🌴",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFB703),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD166), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "$totalStars", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0F2E9))
        }
    }
}

private val ISLAND_COLUMN_WIDTH = 180.dp

/** زر دائري عائم للتنقل السريع على الخريطة. */
@Composable
private fun MapNavButton(modifier: Modifier = Modifier, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(46.dp)
            .background(Color(0xCC0E382A), CircleShape)
            .border(1.5.dp, Color(0x6602C39A), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** عقدة إنقاذ ملكة الفواكه بين الجزر. */
@Composable
private fun RescueNode(
    index: Int,
    unlocked: Boolean,
    stars: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rw = 180.dp
    val rh = rw * 0.904f
    val dim = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.2f) }) }
    Column(
        modifier = modifier
            .offset(y = -(rh - 102.dp))
            .width(ISLAND_COLUMN_WIDTH)
            .alpha(if (unlocked) 1f else 0.7f)
            .clickable(
                enabled = unlocked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("rescue_node_$index"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(width = rw, height = rh), contentAlignment = Alignment.TopCenter) {
            Image(
                painter = painterResource(R.drawable.map_isle_queen),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = if (unlocked) null else dim,
                modifier = Modifier.fillMaxSize()
            )
            // صورة الملكة فوق المنصة الذهبية في قمة الجزيرة
            Image(
                painter = painterResource(R.drawable.queen_scared),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .absoluteOffset(x = rw * 0.507f - 25.dp, y = rh * 0.21f - 25.dp)
                    .size(50.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFFFFD166), CircleShape)
                    .background(Color(0xFF1E3A36))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFD6F2), Color(0xFFFF70A6))), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) { Text("👑", fontSize = 14.sp) }
            if (!unlocked) {
                Icon(Icons.Default.Lock, contentDescription = "مقفلة", tint = Color.White, modifier = Modifier.align(Alignment.Center).size(34.dp).background(Color(0x99053247), CircleShape).padding(6.dp))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.background(Color(0x99053247), RoundedCornerShape(50)).padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            repeat(3) { i ->
                Icon(Icons.Default.Star, contentDescription = null, tint = if (i < stars) Color(0xFFFFD166) else Color.White.copy(alpha = 0.35f), modifier = Modifier.size(15.dp))
            }
        }
        Text(
            "إنقاذ ملكة الفواكه", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEAFFF7), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp).background(Color(0xA0053247), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun IslandNode(
    level: LevelConfig,
    unlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val boss = level.levelNumber % 5 == 0
    val w = if (boss) 176.dp else 146.dp
    val h = if (boss) w * 0.96f else w * 0.70f
    val fruit = level.islandFruit
    val art = if (boss) R.drawable.map_isle_boss else when (fruit) {
        FruitType.STRAWBERRY -> R.drawable.map_isle_straw
        FruitType.PINEAPPLE -> R.drawable.map_isle_pine
        FruitType.COCONUT -> R.drawable.map_isle_coco
        FruitType.WATERMELON -> R.drawable.map_isle_melon
        FruitType.DRAGONFRUIT -> R.drawable.map_isle_dragon
        FruitType.MANGO -> R.drawable.map_isle_mango
        FruitType.KIWI -> R.drawable.map_isle_kiwi
        null -> R.drawable.map_isle_legend
        else -> R.drawable.map_isle_pine
    }
    val dim = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.2f) }) }

    Column(
        modifier = modifier
            .offset(y = if (boss) -(h - 102.dp) else 0.dp)
            .width(ISLAND_COLUMN_WIDTH)
            .clickable(
                enabled = unlocked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag("level_item_${level.levelNumber}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(width = w, height = h), contentAlignment = Alignment.TopCenter) {
            if (isCurrent) CurrentRing()
            Image(
                painter = painterResource(art),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                colorFilter = if (unlocked) null else dim,
                alpha = if (unlocked) 1f else 0.7f,
                modifier = Modifier.fillMaxSize()
            )
            // رقم المرحلة
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(30.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFFE08A), Color(0xFFFFB703))),
                        CircleShape
                    )
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${level.levelNumber}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF3A2400)
                )
            }
            if (boss) Text(text = "👑", fontSize = 20.sp, modifier = Modifier.align(Alignment.TopEnd))
            if (!unlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "مقفلة",
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .background(Color(0x99053247), CircleShape)
                        .padding(6.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .background(Color(0x99053247), RoundedCornerShape(50))
                .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
            repeat(3) { index ->
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (index < stars) Color(0xFFFFD166) else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Text(
            text = level.titleAr,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEAFFF7),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(Color(0xA0053247), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 1.dp)
        )
    }
}

/** حلقة نابضة حول الجزيرة الحالية (تُركَّب لها وحدها كي لا تعمل 40 حركة معاً). */
@Composable
private fun CurrentRing() {
    val t by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ringT"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val grow = 1f + 0.4f * t
        drawOval(
            color = Color.White.copy(alpha = 0.9f * (1f - t)),
            topLeft = Offset(w * (1f - grow) / 2f, h * 0.36f + h * 0.28f * (1f - grow)),
            size = Size(w * grow, h * 0.56f * grow),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// ---------------------------------------------------------------------------
// الرسم
// ---------------------------------------------------------------------------

private fun DrawScope.drawIsland(boss: Boolean) {
    val w = size.width
    val h = size.height
    // ظل
    drawOval(Color(0x33000000), topLeft = Offset(w * 0.02f, h * 0.46f), size = Size(w * 0.96f, h * 0.5f))
    // رغوة
    drawOval(
        Color.White.copy(alpha = 0.6f),
        topLeft = Offset(0f, h * 0.36f),
        size = Size(w, h * 0.56f),
        style = Stroke(width = 3.dp.toPx())
    )
    // رمل
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFBEBC2), Color(0xFFE2BF7D)),
            center = Offset(w / 2f, h * 0.62f),
            radius = w * 0.55f
        ),
        topLeft = Offset(w * 0.04f, h * 0.40f),
        size = Size(w * 0.92f, h * 0.50f)
    )
    // عشب
    drawOval(
        brush = Brush.radialGradient(
            listOf(Color(0xFF86E29A), Color(0xFF2B9A56)),
            center = Offset(w / 2f, h * 0.55f),
            radius = w * 0.42f
        ),
        topLeft = Offset(w * 0.16f, h * 0.44f),
        size = Size(w * 0.68f, h * 0.34f)
    )
    if (boss) {
        drawOval(
            Color(0xFFFFD166),
            topLeft = Offset(-2f, h * 0.34f),
            size = Size(w + 4f, h * 0.6f),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

/** بحر مرسوم: شريط صورة قابل للتكرار رأسياً يُمدّ على عرض الشاشة، ويُرسم المقطع الظاهر فقط. */
private fun DrawScope.drawSeaTiles(sea: ImageBitmap, from: Float, to: Float) {
    val w = size.width
    val tileH = w * sea.height / sea.width
    val first = (from / tileH).toInt().coerceAtLeast(0)
    val last = (to / tileH).toInt()
    for (k in first..last) {
        drawImage(
            image = sea,
            dstOffset = IntOffset(0, (k * tileH).toInt()),
            dstSize = IntSize(w.toInt(), tileH.toInt() + 1)
        )
    }
}

/** مسار منقّط يصل الجزر؛ الذهبي للمراحل المنجزة. */
private fun DrawScope.drawRoute(
    doneFlags: List<Boolean>,
    from: Float,
    to: Float,
    gapPx: Float,
    bottomPx: Float
) {
    fun point(i: Int) = Offset(size.width * islandX(i), size.height - bottomPx - gapPx * i)
    val dots = PathEffect.dashPathEffect(floatArrayOf(2f, 12.dp.toPx()))
    for (i in 0 until doneFlags.size - 1) {
        val a = point(i)
        val b = point(i + 1)
        if (a.y < from || b.y > to) continue
        val midY = (a.y + b.y) / 2f
        val path = Path().apply {
            moveTo(a.x, a.y)
            cubicTo(a.x, midY, b.x, midY, b.x, b.y)
        }
        val done = doneFlags[i]
        drawPath(
            path = path,
            color = Color(0x66053247),
            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, pathEffect = dots)
        )
        drawPath(
            path = path,
            color = if (done) Color(0xFFFFD166) else Color.White.copy(alpha = 0.9f),
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dots)
        )
    }
}

/** غيوم ضبابية تنجرف ببطء فوق آخر جزيرة. */
private fun DrawScope.drawClouds(phase: Float) {
    val drift = sin(phase * 2f * PI.toFloat()) * 22.dp.toPx()
    // (موضع أفقي كنسبة، موضع رأسي dp، نصف قطر dp)
    val blobs = listOf(
        Triple(0.12f, 40f, 105f), Triple(0.50f, 22f, 135f), Triple(0.88f, 56f, 115f),
        Triple(0.28f, 150f, 125f), Triple(0.72f, 165f, 145f), Triple(0.05f, 240f, 105f),
        Triple(0.95f, 260f, 115f), Triple(0.50f, 310f, 150f)
    )
    blobs.forEachIndexed { i, (fx, yDp, rDp) ->
        val c = Offset(size.width * fx + if (i % 2 == 0) drift else -drift, yDp.dp.toPx())
        val r = rDp.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0f)),
                center = c,
                radius = r
            ),
            radius = r,
            center = c
        )
    }
}
