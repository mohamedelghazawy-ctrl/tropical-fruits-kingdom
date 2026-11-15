package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.FruitType
import com.example.model.SpecialType
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FruitComposable(
    fruitType: FruitType,
    specialType: SpecialType = SpecialType.NONE,
    isSelected: Boolean = false,
    isHinted: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Performance optimization: only run continuous animations on active, hinted or special tiles
    val needsAnimation = isSelected || isHinted || specialType != SpecialType.NONE
    val (pulseScale, glowRotation) = if (needsAnimation) {
        val infiniteTransition = rememberInfiniteTransition(label = "fruit_fx")
        val pulse = infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (isHinted) 1.15f else 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (isHinted) 450 else 600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        ).value
        val rotation = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        ).value
        Pair(pulse, rotation)
    } else {
        Pair(1f, 0f)
    }

    // صور ثابتة لكل الأجهزة (بدل إيموجي الخط الذي يختلف من هاتف لآخر)
    val emoRes = emoResOf(fruitType)
    val emoBitmap = if (emoRes != null) ImageBitmap.imageResource(id = emoRes) else null

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Selection halo glow
            if (isSelected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99FFD166), Color(0x33FFB703), Color.Transparent),
                        center = center,
                        radius = w * 0.55f * pulseScale
                    ),
                    radius = w * 0.55f * pulseScale
                )
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = w * 0.46f,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Hint luminous breathing glow
            if (isHinted && !isSelected) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xBBFFE500), Color(0x66FFB703), Color.Transparent),
                        center = center,
                        radius = w * 0.60f * pulseScale
                    ),
                    radius = w * 0.60f * pulseScale
                )
                drawCircle(
                    color = Color(0xFFFFD166),
                    radius = w * 0.46f * pulseScale,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Draw base fruit graphics (فاكهة النجمة تُرسم نجمةً فقط بلا فاكهة تحتها)
            if (specialType != SpecialType.STARFRUIT_RAINBOW) when (fruitType) {
                FruitType.PINEAPPLE -> emoBitmap?.let { drawEmoImage(it, w, center) }
                FruitType.DRAGONFRUIT -> drawDragonfruit(w, h, center)
                FruitType.WATERMELON -> emoBitmap?.let { drawEmoImage(it, w, center) }
                FruitType.MANGO -> emoBitmap?.let { drawEmoImage(it, w, center) }
                FruitType.COCONUT -> emoBitmap?.let { drawEmoImage(it, w, center) }
                FruitType.KIWI -> emoBitmap?.let { drawEmoImage(it, w, center) }
                FruitType.STRAWBERRY -> emoBitmap?.let { drawEmoImage(it, w, center) }
            }

            // Draw special overlays
            when (specialType) {
                SpecialType.STRIPED_HORIZONTAL -> drawStripedEffect(w, h, center, isHorizontal = true, pulseScale)
                SpecialType.STRIPED_VERTICAL -> drawStripedEffect(w, h, center, isHorizontal = false, pulseScale)
                SpecialType.BOMB -> drawBombEffect(w, h, center, pulseScale)
                SpecialType.STARFRUIT_RAINBOW -> drawRainbowStarEffect(w, h, center, glowRotation, pulseScale)
                SpecialType.NONE -> { /* no overlay */ }
            }
        }
    }
}

private fun DrawScope.drawPineapple(w: Float, h: Float, center: Offset) {
    // Green leafy crown
    val crownPath = Path().apply {
        moveTo(center.x, h * 0.08f)
        quadraticBezierTo(center.x - w * 0.18f, h * 0.16f, center.x - w * 0.25f, h * 0.28f)
        quadraticBezierTo(center.x - w * 0.08f, h * 0.26f, center.x, h * 0.35f)
        quadraticBezierTo(center.x + w * 0.08f, h * 0.26f, center.x + w * 0.25f, h * 0.28f)
        quadraticBezierTo(center.x + w * 0.18f, h * 0.16f, center.x, h * 0.08f)
        close()
    }
    drawPath(
        path = crownPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFF2D6A4F), Color(0xFF52B788)),
            startY = h * 0.08f,
            endY = h * 0.35f
        )
    )

    // Center tall leaf
    val centerLeaf = Path().apply {
        moveTo(center.x, h * 0.04f)
        quadraticBezierTo(center.x - w * 0.08f, h * 0.18f, center.x - w * 0.06f, h * 0.34f)
        lineTo(center.x + w * 0.06f, h * 0.34f)
        quadraticBezierTo(center.x + w * 0.08f, h * 0.18f, center.x, h * 0.04f)
        close()
    }
    drawPath(
        path = centerLeaf,
        brush = Brush.verticalGradient(
            listOf(Color(0xFF40916C), Color(0xFF74C69D)),
            startY = h * 0.04f,
            endY = h * 0.34f
        )
    )

    // Golden Pineapple Oval Body
    val bodyRadiusX = w * 0.34f
    val bodyRadiusY = h * 0.36f
    val bodyCenter = Offset(center.x, h * 0.58f)

    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD166), Color(0xFFFFB703), Color(0xFFFB8500)),
            center = bodyCenter + Offset(-w * 0.08f, -h * 0.08f),
            radius = bodyRadiusX * 1.3f
        ),
        topLeft = Offset(bodyCenter.x - bodyRadiusX, bodyCenter.y - bodyRadiusY),
        size = Size(bodyRadiusX * 2, bodyRadiusY * 2)
    )

    // Diamond Scales pattern
    val scaleStroke = Stroke(width = 1.8.dp.toPx())
    val scaleColor = Color(0x44D97706)
    for (i in -2..2) {
        val yOffset = i * h * 0.12f
        drawLine(
            color = scaleColor,
            start = Offset(bodyCenter.x - bodyRadiusX * 0.7f, bodyCenter.y + yOffset - h * 0.1f),
            end = Offset(bodyCenter.x + bodyRadiusX * 0.7f, bodyCenter.y + yOffset + h * 0.1f),
            strokeWidth = scaleStroke.width,
            cap = StrokeCap.Round
        )
        drawLine(
            color = scaleColor,
            start = Offset(bodyCenter.x - bodyRadiusX * 0.7f, bodyCenter.y + yOffset + h * 0.1f),
            end = Offset(bodyCenter.x + bodyRadiusX * 0.7f, bodyCenter.y + yOffset - h * 0.1f),
            strokeWidth = scaleStroke.width,
            cap = StrokeCap.Round
        )
    }

    // Glossy highlight
    drawOval(
        color = Color(0x66FFFFFF),
        topLeft = Offset(bodyCenter.x - bodyRadiusX * 0.65f, bodyCenter.y - bodyRadiusY * 0.7f),
        size = Size(w * 0.18f, h * 0.12f)
    )
}

private fun DrawScope.drawDragonfruit(w: Float, h: Float, center: Offset) {
    // Outer magenta dragon skin
    val bodyRadius = w * 0.37f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF4D94), Color(0xFFFF007F), Color(0xFFB5179E)),
            center = center + Offset(-w * 0.06f, -h * 0.06f),
            radius = bodyRadius * 1.2f
        ),
        radius = bodyRadius,
        center = center
    )

    // Green outer flame-like bract tips
    val greenTipColor = Color(0xFF70E000)
    val tipAngles = listOf(35f, 90f, 145f, 215f, 270f, 325f)
    tipAngles.forEach { deg ->
        val rad = Math.toRadians(deg.toDouble()).toFloat()
        val tipX = center.x + (bodyRadius * 1.05f) * cos(rad)
        val tipY = center.y + (bodyRadius * 1.05f) * sin(rad)
        drawCircle(
            color = greenTipColor,
            radius = w * 0.07f,
            center = Offset(tipX, tipY)
        )
    }

    // Inner white speckled pulp
    val innerRadius = bodyRadius * 0.74f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF7F7F7), Color(0xFFFFE5EC)),
            center = center,
            radius = innerRadius
        ),
        radius = innerRadius,
        center = center
    )

    // Dragonfruit black seeds
    val seedColor = Color(0xFF222222)
    val seedOffsets = listOf(
        Offset(-0.25f, -0.2f), Offset(0.2f, -0.25f), Offset(-0.1f, 0.28f),
        Offset(0.25f, 0.15f), Offset(-0.3f, 0.08f), Offset(0.05f, -0.05f),
        Offset(-0.12f, -0.1f), Offset(0.14f, 0.28f), Offset(-0.02f, 0.18f)
    )
    seedOffsets.forEach { rel ->
        drawCircle(
            color = seedColor,
            radius = w * 0.022f,
            center = center + Offset(rel.x * innerRadius * 1.8f, rel.y * innerRadius * 1.8f)
        )
    }

    // Gloss
    drawCircle(
        color = Color(0x66FFFFFF),
        radius = w * 0.08f,
        center = center + Offset(-innerRadius * 0.45f, -innerRadius * 0.45f)
    )
}

private fun DrawScope.drawWatermelon(w: Float, h: Float, center: Offset) {
    // Sliced crescent of watermelon
    val slicePath = Path().apply {
        moveTo(center.x - w * 0.42f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, h * 0.88f, center.x + w * 0.42f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, center.y - h * 0.18f, center.x - w * 0.42f, center.y - h * 0.05f)
        close()
    }

    // Green Rind (outer layer)
    drawPath(
        path = slicePath,
        color = Color(0xFF2D6A4F)
    )

    // White/Lime inner rind
    val limeRindPath = Path().apply {
        moveTo(center.x - w * 0.38f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, h * 0.82f, center.x + w * 0.38f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, center.y - h * 0.16f, center.x - w * 0.38f, center.y - h * 0.05f)
        close()
    }
    drawPath(
        path = limeRindPath,
        color = Color(0xFFD8F3DC)
    )

    // Red Juicy Pulp
    val redPulp = Path().apply {
        moveTo(center.x - w * 0.35f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, h * 0.76f, center.x + w * 0.35f, center.y - h * 0.05f)
        quadraticBezierTo(center.x, center.y - h * 0.14f, center.x - w * 0.35f, center.y - h * 0.05f)
        close()
    }
    drawPath(
        path = redPulp,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFF2A6D), Color(0xFFFF0054), Color(0xFFD90429)),
            startY = center.y - h * 0.15f,
            endY = h * 0.8f
        )
    )

    // Watermelon seeds
    val seedColor = Color(0xFF1E1E24)
    val seeds = listOf(
        Offset(center.x - w * 0.18f, center.y + h * 0.15f),
        Offset(center.x, center.y + h * 0.28f),
        Offset(center.x + w * 0.18f, center.y + h * 0.15f),
        Offset(center.x - w * 0.09f, center.y + h * 0.08f),
        Offset(center.x + w * 0.09f, center.y + h * 0.08f)
    )
    seeds.forEach { pt ->
        drawOval(
            color = seedColor,
            topLeft = Offset(pt.x - w * 0.02f, pt.y - h * 0.035f),
            size = Size(w * 0.04f, h * 0.07f)
        )
    }

    // Top shine
    drawLine(
        color = Color(0x66FFFFFF),
        start = Offset(center.x - w * 0.25f, center.y - h * 0.08f),
        end = Offset(center.x + w * 0.1f, center.y - h * 0.12f),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun emoResOf(t: FruitType): Int? = when (t) {
    FruitType.MANGO -> com.example.R.drawable.emo_mango
    FruitType.WATERMELON -> com.example.R.drawable.emo_watermelon
    FruitType.PINEAPPLE -> com.example.R.drawable.emo_pineapple
    FruitType.STRAWBERRY -> com.example.R.drawable.emo_strawberry
    FruitType.KIWI -> com.example.R.drawable.emo_kiwi
    FruitType.COCONUT -> com.example.R.drawable.emo_coconut
    else -> null
}

private fun DrawScope.drawEmoImage(img: androidx.compose.ui.graphics.ImageBitmap, w: Float, center: Offset) {
    val d = (w * 0.82f).toInt()
    drawImage(
        image = img,
        dstOffset = androidx.compose.ui.unit.IntOffset((center.x - d / 2f).toInt(), (center.y - d / 2f).toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(d, d),
        filterQuality = androidx.compose.ui.graphics.FilterQuality.High
    )
}

private fun DrawScope.drawMangoOld(w: Float, h: Float, center: Offset) {
    // Mango body - smooth teardrop / kidney shape
    val mangoCenter = center + Offset(0f, h * 0.04f)
    val rX = w * 0.36f
    val rY = h * 0.38f

    val mangoPath = Path().apply {
        moveTo(mangoCenter.x, mangoCenter.y - rY)
        cubicTo(
            mangoCenter.x + rX * 1.15f, mangoCenter.y - rY * 0.6f,
            mangoCenter.x + rX * 1.25f, mangoCenter.y + rY * 0.6f,
            mangoCenter.x + rX * 0.2f, mangoCenter.y + rY * 0.95f
        )
        cubicTo(
            mangoCenter.x - rX * 0.7f, mangoCenter.y + rY * 1.1f,
            mangoCenter.x - rX * 1.1f, mangoCenter.y + rY * 0.3f,
            mangoCenter.x - rX * 0.85f, mangoCenter.y - rY * 0.4f
        )
        cubicTo(
            mangoCenter.x - rX * 0.6f, mangoCenter.y - rY * 0.8f,
            mangoCenter.x - rX * 0.2f, mangoCenter.y - rY,
            mangoCenter.x, mangoCenter.y - rY
        )
        close()
    }

    drawPath(
        path = mangoPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFFFF5400), Color(0xFFD90429)),
            center = mangoCenter + Offset(-rX * 0.2f, -rY * 0.2f),
            radius = rX * 1.6f
        )
    )

    // Cute Green leaf at top stem
    val leafPath = Path().apply {
        moveTo(mangoCenter.x, mangoCenter.y - rY * 0.95f)
        quadraticBezierTo(mangoCenter.x + w * 0.22f, mangoCenter.y - rY * 1.25f, mangoCenter.x + w * 0.28f, mangoCenter.y - rY * 1.4f)
        quadraticBezierTo(mangoCenter.x + w * 0.12f, mangoCenter.y - rY * 1.05f, mangoCenter.x, mangoCenter.y - rY * 0.95f)
        close()
    }
    drawPath(leafPath, color = Color(0xFF38B000))

    // Glossy reflection
    drawOval(
        color = Color(0x66FFFFFF),
        topLeft = Offset(mangoCenter.x - rX * 0.55f, mangoCenter.y - rY * 0.6f),
        size = Size(w * 0.2f, h * 0.12f)
    )
}

private fun DrawScope.drawCoconut(w: Float, h: Float, center: Offset) {
    val r = w * 0.37f

    // Brown hairy husk
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF8B5A2B), Color(0xFF5C3A1E), Color(0xFF3A210F)),
            center = center + Offset(-w * 0.05f, -h * 0.05f),
            radius = r * 1.3f
        ),
        radius = r,
        center = center
    )

    // Inner coconut meat (snowy white rim)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFF6F3EE), Color(0xFFE5DDD0)),
            center = center,
            radius = r * 0.75f
        ),
        radius = r * 0.72f,
        center = center
    )

    // Coconut water translucent center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xBBDEF2F1), Color(0x8898D8D8), Color(0x442B7A78)),
            center = center + Offset(0f, r * 0.1f),
            radius = r * 0.5f
        ),
        radius = r * 0.48f,
        center = center
    )

    // Three coconut germination pores (eyes)
    val eyeColor = Color(0xFF4A2810)
    drawCircle(eyeColor, radius = w * 0.035f, center = center + Offset(-r * 0.22f, -r * 0.2f))
    drawCircle(eyeColor, radius = w * 0.035f, center = center + Offset(r * 0.22f, -r * 0.2f))
    drawCircle(eyeColor, radius = w * 0.04f, center = center + Offset(0f, -r * 0.38f))

    // Water shine
    drawCircle(Color(0x88FFFFFF), radius = w * 0.06f, center = center + Offset(-r * 0.15f, r * 0.1f))
}

private fun DrawScope.drawKiwi(w: Float, h: Float, center: Offset) {
    val r = w * 0.37f

    // Fuzzy Brown peel
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF7F4F24), Color(0xFF582F0E)),
            center = center,
            radius = r
        ),
        radius = r,
        center = center
    )

    // Bright lime green flesh
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFB5E48C), Color(0xFF70E000), Color(0xFF38B000)),
            center = center,
            radius = r * 0.92f
        ),
        radius = r * 0.9f,
        center = center
    )

    // Cream white central core
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFE0), Color(0xFFE9F5DB)),
            center = center,
            radius = r * 0.28f
        ),
        topLeft = Offset(center.x - r * 0.25f, center.y - r * 0.32f),
        size = Size(r * 0.5f, r * 0.64f)
    )

    // Radiating kiwi seed spokes
    val seedColor = Color(0xFF1E1E24)
    val numSeeds = 12
    for (i in 0 until numSeeds) {
        val angle = (i * (360f / numSeeds)) + 15f
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val dist = r * 0.52f
        val seedX = center.x + dist * cos(rad)
        val seedY = center.y + dist * sin(rad)
        drawCircle(
            color = seedColor,
            radius = w * 0.025f,
            center = Offset(seedX, seedY)
        )
    }

    // Gloss
    drawCircle(
        color = Color(0x66FFFFFF),
        radius = w * 0.07f,
        center = center + Offset(-r * 0.45f, -r * 0.45f)
    )
}

// Special power overlays:
private fun DrawScope.drawStripedEffect(w: Float, h: Float, center: Offset, isHorizontal: Boolean, pulse: Float) {
    val beamColor = Color(0xEEFFFFFF)
    val glowColor = Color(0xAA00F5D4)

    if (isHorizontal) {
        // Horizontal energy beam
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, glowColor, beamColor, glowColor, Color.Transparent)
            ),
            topLeft = Offset(0f, center.y - 6.dp.toPx() * pulse),
            size = Size(w, 12.dp.toPx() * pulse),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Arrows
        drawLine(
            color = Color.White,
            start = Offset(w * 0.15f, center.y),
            end = Offset(w * 0.05f, center.y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.85f, center.y),
            end = Offset(w * 0.95f, center.y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    } else {
        // Vertical energy beam
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, glowColor, beamColor, glowColor, Color.Transparent)
            ),
            topLeft = Offset(center.x - 6.dp.toPx() * pulse, 0f),
            size = Size(12.dp.toPx() * pulse, h),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        // Arrows
        drawLine(
            color = Color.White,
            start = Offset(center.x, h * 0.15f),
            end = Offset(center.x, h * 0.05f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White,
            start = Offset(center.x, h * 0.85f),
            end = Offset(center.x, h * 0.95f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawBombEffect(w: Float, h: Float, center: Offset, pulse: Float) {
    // Fiery pulsing bomb ring
    drawCircle(
        color = Color(0xFFFF5400),
        radius = w * 0.44f * pulse,
        style = Stroke(width = 3.5.dp.toPx())
    )
    drawCircle(
        color = Color(0xFFFFD166),
        radius = w * 0.40f * pulse,
        style = Stroke(width = 2.dp.toPx())
    )
    // Mini sparks at 4 corners
    val sparkCoords = listOf(
        Offset(center.x - w * 0.32f, center.y - h * 0.32f),
        Offset(center.x + w * 0.32f, center.y - h * 0.32f),
        Offset(center.x - w * 0.32f, center.y + h * 0.32f),
        Offset(center.x + w * 0.32f, center.y + h * 0.32f)
    )
    sparkCoords.forEach { pt ->
        drawCircle(Color(0xFFFFEA00), radius = 3.dp.toPx() * pulse, center = pt)
    }
}

private fun DrawScope.drawRainbowStarEffect(w: Float, h: Float, center: Offset, rotation: Float, pulse: Float) {
    // Rotating rainbow aura
    val rainbowColors = listOf(
        Color(0xFFFF007F), Color(0xFFFFB703), Color(0xFF00F5D4),
        Color(0xFF70E000), Color(0xFF7209B7), Color(0xFFFF007F)
    )
    drawCircle(
        brush = Brush.sweepGradient(rainbowColors, center = center),
        radius = w * 0.48f * pulse,
        style = Stroke(width = 4.dp.toPx())
    )

    // 5-Pointed Star
    val starPoints = 5
    val outerRadius = w * 0.44f * pulse
    val innerRadius = outerRadius * 0.42f
    val starPath = Path()

    for (i in 0 until (starPoints * 2)) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = Math.toRadians((i * (360.0 / (starPoints * 2)) - 90.0)).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
    }
    starPath.close()

    drawPath(
        path = starPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFE600), Color(0xFFFF9E00)),
            center = center,
            radius = outerRadius
        )
    )
    drawPath(
        path = starPath,
        color = Color(0xFFFFFAEB),
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawStrawberry(w: Float, h: Float, center: Offset) {
    val strawberryCenter = center + Offset(0f, h * 0.05f)
    val rX = w * 0.35f
    val rY = h * 0.38f

    // Strawberry heart/conical shape
    val berryPath = Path().apply {
        moveTo(strawberryCenter.x, strawberryCenter.y - rY * 0.7f)
        cubicTo(
            strawberryCenter.x + rX * 1.15f, strawberryCenter.y - rY * 0.7f,
            strawberryCenter.x + rX * 0.95f, strawberryCenter.y + rY * 0.3f,
            strawberryCenter.x, strawberryCenter.y + rY
        )
        cubicTo(
            strawberryCenter.x - rX * 0.95f, strawberryCenter.y + rY * 0.3f,
            strawberryCenter.x - rX * 1.15f, strawberryCenter.y - rY * 0.7f,
            strawberryCenter.x, strawberryCenter.y - rY * 0.7f
        )
        close()
    }

    drawPath(
        path = berryPath,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF2A6D), Color(0xFFFF0054), Color(0xFFC9184A)),
            center = strawberryCenter + Offset(-rX * 0.2f, -rY * 0.2f),
            radius = rX * 1.5f
        )
    )

    // Green leafy crown (sepals)
    val calyxPath = Path().apply {
        moveTo(strawberryCenter.x, strawberryCenter.y - rY * 0.75f)
        quadraticBezierTo(strawberryCenter.x - w * 0.2f, strawberryCenter.y - rY * 1.1f, strawberryCenter.x - w * 0.25f, strawberryCenter.y - rY * 0.6f)
        lineTo(strawberryCenter.x - w * 0.08f, strawberryCenter.y - rY * 0.65f)
        quadraticBezierTo(strawberryCenter.x, strawberryCenter.y - rY * 1.25f, strawberryCenter.x + w * 0.08f, strawberryCenter.y - rY * 0.65f)
        lineTo(strawberryCenter.x + w * 0.25f, strawberryCenter.y - rY * 0.6f)
        quadraticBezierTo(strawberryCenter.x + w * 0.2f, strawberryCenter.y - rY * 1.1f, strawberryCenter.x, strawberryCenter.y - rY * 0.75f)
        close()
    }
    drawPath(calyxPath, color = Color(0xFF38B000))

    // Tiny golden seeds
    val seedColor = Color(0xFFFFD166)
    val seeds = listOf(
        Offset(-0.4f, -0.2f), Offset(0f, -0.3f), Offset(0.4f, -0.2f),
        Offset(-0.25f, 0.1f), Offset(0.25f, 0.1f), Offset(0f, 0.15f),
        Offset(-0.15f, 0.45f), Offset(0.15f, 0.45f), Offset(0f, 0.65f)
    )
    seeds.forEach { rel ->
        drawOval(
            color = seedColor,
            topLeft = Offset(strawberryCenter.x + rel.x * rX - 1.5.dp.toPx(), strawberryCenter.y + rel.y * rY - 2.5.dp.toPx()),
            size = Size(3.dp.toPx(), 5.dp.toPx())
        )
    }

    // Glossy reflection
    drawOval(
        color = Color(0x66FFFFFF),
        topLeft = Offset(strawberryCenter.x - rX * 0.5f, strawberryCenter.y - rY * 0.4f),
        size = Size(w * 0.18f, h * 0.12f)
    )
}
