package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.game.ParticleSystem
import com.example.model.ScorePopup
import com.example.model.Tile

@Composable
fun GameBoard(
    board: List<List<Tile>>,
    selectedTile: Tile?,
    scorePopups: List<ScorePopup>,
    particleSystem: ParticleSystem,
    hintedTiles: Set<Pair<Int, Int>> = emptySet(),
    onTileClicked: (Tile) -> Unit,
    onSwipe: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (board.isEmpty()) return

    val rows = board.size
    val cols = board[0].size

    // Tropical bamboo/wooden board styling
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF00A896))
            .testTag("game_board"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xDD08241A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(Color(0xFF2EC4B6), Color(0xFFFFB703), Color(0xFF0F4C3A))
            ),
            width = 3.dp
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            val boardWidthPx = constraints.maxWidth.toFloat()
            val boardHeightPx = constraints.maxHeight.toFloat()
            val cellWidthPx = boardWidthPx / cols
            val cellHeightPx = boardHeightPx / rows

            val cellWidthDp = maxWidth / cols
            val cellHeightDp = maxHeight / rows

            val currentOnSwipe by rememberUpdatedState(onSwipe)

            // اللوحة دائمًا يسار→يمين كي تتطابق إحداثيات اللمس مع أماكن الفواكه في الواجهة العربية
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // رسم الشبكة الخلفية في مرحلة الرسم فقط بدل 64 Box
                    .drawBehind {
                        val inset = 1.5.dp.toPx()
                        val corner = CornerRadius(8.dp.toPx())
                        val cellSize = Size(cellWidthPx - inset * 2, cellHeightPx - inset * 2)
                        for (r in 0 until rows) {
                            for (c in 0 until cols) {
                                drawRoundRect(
                                    color = if ((r + c) % 2 == 0) CELL_EVEN else CELL_ODD,
                                    topLeft = Offset(c * cellWidthPx + inset, r * cellHeightPx + inset),
                                    size = cellSize,
                                    cornerRadius = corner
                                )
                            }
                        }
                    }
                    // المفاتيح ثابتة: لا نعيد تشغيل الإيماءة عند كل تغيير في اللوحة
                    .pointerInput(rows, cols, cellWidthPx, cellHeightPx) {
                        // السحب يُكتشف في المرحلة الأولى قبل لمسات الفواكه؛ التبديل بسحب الفاكهة نحو جارتها
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            val startC = (down.position.x / cellWidthPx).toInt().coerceIn(0, cols - 1)
                            val startR = (down.position.y / cellHeightPx).toInt().coerceIn(0, rows - 1)
                            val threshold = cellWidthPx * 0.28f
                            var swiped = false
                            while (true) {
                                val ev = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                                if (!ch.pressed) break
                                if (swiped) { ch.consume(); continue }
                                val dx = ch.position.x - down.position.x
                                val dy = ch.position.y - down.position.y
                                val ax = kotlin.math.abs(dx); val ay = kotlin.math.abs(dy)
                                if (ax > threshold || ay > threshold) {
                                    swiped = true
                                    ch.consume()
                                    if (ax > ay) {
                                        if (dx > 0 && startC + 1 < cols) currentOnSwipe(startR, startC, startR, startC + 1)
                                        else if (dx < 0 && startC - 1 >= 0) currentOnSwipe(startR, startC, startR, startC - 1)
                                    } else {
                                        if (dy > 0 && startR + 1 < rows) currentOnSwipe(startR, startC, startR + 1, startC)
                                        else if (dy < 0 && startR - 1 >= 0) currentOnSwipe(startR, startC, startR - 1, startC)
                                    }
                                }
                            }
                        }
                    }
            ) {
                // Render Tiles with identity keys to animate gravity falling and prevent disappearing tiles
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val tile = board[r][c]
                        val isSelected = selectedTile?.row == r && selectedTile.col == c
                        val isHinted = (r to c) in hintedTiles

                        key(tile.id) {
                            AnimatedTile(
                                tile = tile,
                                isSelected = isSelected,
                                isHinted = isHinted,
                                rows = rows,
                                cellWidthDp = cellWidthDp,
                                cellHeightDp = cellHeightDp,
                                cellWidthPx = cellWidthPx,
                                cellHeightPx = cellHeightPx,
                                onClick = { onTileClicked(tile) }
                            )
                        }
                    }
                }

                // Score popups layer
                scorePopups.forEach { popup ->
                    ScorePopupItem(
                        popup = popup,
                        cellWidthPx = cellWidthPx,
                        cellHeightPx = cellHeightPx
                    )
                }

                // Particle overlay layer
                ParticleOverlay(system = particleSystem, cols = cols, rows = rows)
            }
            }
        }
    }
}

@Composable
private fun AnimatedTile(
    tile: Tile,
    isSelected: Boolean,
    isHinted: Boolean = false,
    rows: Int,
    cellWidthDp: androidx.compose.ui.unit.Dp,
    cellHeightDp: androidx.compose.ui.unit.Dp,
    cellWidthPx: Float,
    cellHeightPx: Float,
    onClick: () -> Unit
) {
    // Gravity Falling Animation:
    // If newly spawned at top, start above board; if surviving tile dropping, start from previous row
    val startY = remember(tile.id) {
        if (tile.isNew) {
            -cellHeightPx * ((rows - tile.row).coerceIn(1, 4))
        } else {
            tile.row * cellHeightPx
        }
    }
    val currentY = remember(tile.id) { Animatable(startY) }

    LaunchedEffect(tile.id, tile.row) {
        currentY.animateTo(
            targetValue = tile.row * cellHeightPx,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    // مؤثر واحد للتحجيم: دخول مرتد للجديد، انكماش عند المطابقة (تفادي تعارض Animatable)
    val scale = remember(tile.id) { Animatable(if (tile.isNew) 0.5f else 1f) }
    LaunchedEffect(tile.id, tile.isMatched) {
        if (tile.isMatched) {
            scale.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
        } else {
            scale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = if (tile.isNew) Spring.StiffnessLow else Spring.StiffnessMedium
                )
            )
        }
    }

    val selectionScale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_select"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (tile.col * cellWidthPx).toInt(),
                    currentY.value.toInt()
                )
            }
            .size(cellWidthDp, cellHeightDp)
            .graphicsLayer {
                // قراءة الحالة في مرحلة الرسم: لا إعادة تركيب لكل إطار
                val sc = (scale.value * selectionScale).coerceAtLeast(0f)
                scaleX = sc
                scaleY = sc
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        FruitComposable(
            fruitType = tile.type,
            specialType = tile.special,
            isSelected = isSelected,
            isHinted = isHinted,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private val CELL_EVEN = Color(0x33004D40)
private val CELL_ODD = Color(0x1A002820)
