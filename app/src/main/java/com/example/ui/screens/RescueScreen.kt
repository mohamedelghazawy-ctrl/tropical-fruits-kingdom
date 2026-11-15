package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.BoosterType
import com.example.model.FruitType
import com.example.ads.AdGateDialog
import com.example.rescue.*
import com.example.ui.components.FruitComposable
import kotlin.math.abs
import kotlin.math.sin

private val Dirt = Color(0xFF5A3B22)
private val Stone = Color(0xFF6D6A66)
private val SeedColors = listOf(Color(0xFF3FB8AF), Color(0xFF8E6BBF), Color(0xFFE0A93B))

@Composable
fun RescueScreen(
    levelId: Int,
    onExit: () -> Unit,
    onQuitToMenu: () -> Unit,
    onGrantReward: (BoosterType, Int) -> Unit,
    onRescueWon: () -> Unit = {},
    vm: RescueViewModel = viewModel()
) {
    LaunchedEffect(levelId) { vm.startLevel(levelId) }
    var showAd by remember { mutableStateOf(false) }
    var adPurpose by remember { mutableStateOf(0) } // 0 = إنقاذ بعد الخسارة، 1 = مضاعفة الجائزة
    // منح الجائزة الأساسية مرة واحدة عند الفوز
    LaunchedEffect(vm.status, vm.reward) { if (vm.status == RescueStatus.WON) { vm.reward?.let { onGrantReward(it, 1) }; onRescueWon() } }
    val l = vm.state.level
    val queenImgs = QueenPose.entries.associateWith { p ->
        ImageBitmap.imageResource(
            when (p) {
                QueenPose.IDLE -> R.drawable.queen_idle
                QueenPose.CHEER -> R.drawable.queen_cheer
                QueenPose.LOOK -> R.drawable.queen_look
                QueenPose.HOLD -> R.drawable.queen_hold
                QueenPose.PUSH -> R.drawable.queen_push
                QueenPose.PUSH2 -> R.drawable.queen_push2
                QueenPose.CROUCH -> R.drawable.queen_crouch
                QueenPose.SCARED -> R.drawable.queen_scared
                QueenPose.RELIEF -> R.drawable.queen_relief
            }
        )
    }
    var frame by remember { mutableLongStateOf(0L) }
    // إطار مستمر خفيف لاهتزاز الجذع/الثعبان فقط
    LaunchedEffect(Unit) { while (true) { androidx.compose.runtime.withFrameMillis { frame = it } } }

    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1E5B4F), Color(0xFF0E2A2A)))).statusBarsPadding().navigationBarsPadding().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = Color.White, fontSize = 26.sp, modifier = Modifier.clickable { onExit() }.padding(8.dp))
            Text(l.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("🐍 ${vm.snakeIn}", color = if (vm.danger) Color(0xFFFF6B6B) else Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            Text("👣 ${vm.movesLeft}", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x55000000))) {
            Box(Modifier.fillMaxWidth(vm.progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color(0xFFFFD166)))
        }
        Spacer(Modifier.height(6.dp))

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cellDp = minOf(maxWidth / l.cols, maxHeight / l.rows)
            val boardW = cellDp * l.cols
            val boardH = cellDp * l.rows
            var cellPx by remember { mutableFloatStateOf(1f) }
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            Box(
                Modifier.size(boardW, boardH)
                    .onSizeChanged { cellPx = it.width / l.cols.toFloat() }
                    .pointerInput(l.id) {
                        detectTapGestures { o -> vm.onTap((o.y / cellPx).toInt().coerceIn(0, l.rows - 1), (o.x / cellPx).toInt().coerceIn(0, l.cols - 1)) }
                    }
                    .pointerInput(l.id) {
                        var start = Offset.Zero; var acc = Offset.Zero
                        detectDragGestures(
                            onDragStart = { start = it; acc = Offset.Zero },
                            onDrag = { _, d -> acc += d },
                            onDragEnd = {
                                if (maxOf(abs(acc.x), abs(acc.y)) > cellPx * 0.3f) {
                                    val r1 = (start.y / cellPx).toInt().coerceIn(0, l.rows - 1)
                                    val c1 = (start.x / cellPx).toInt().coerceIn(0, l.cols - 1)
                                    val horiz = abs(acc.x) > abs(acc.y)
                                    val r2 = r1 + if (!horiz) (if (acc.y > 0) 1 else -1) else 0
                                    val c2 = c1 + if (horiz) (if (acc.x > 0) 1 else -1) else 0
                                    if (r2 in 0 until l.rows && c2 in 0 until l.cols) vm.onSwipe(r1, c1, r2, c2)
                                }
                            }
                        )
                    }
            ) {
                // الخلفية: جدران، حمم، بذور، جذع، ملكة، ثعبان
                Canvas(Modifier.fillMaxSize()) {
                    frame
                    vm.version; vm.animT
                    val cs = size.width / l.cols
                    drawRect(Color(0xFF16302E))
                    val st = vm.state
                    // الثعبان (جسم أخضر متعرّج من الأعلى حتى snakeVis)
                    drawSnake(vm, cs, frame)
                    for (r in 0 until st.rows) for (c in 0 until st.cols) {
                        val cell = st.grid[r][c] ?: continue
                        when (cell.kind) {
                            Kind.WALL -> drawRect(Stone, Offset(c * cs, r * cs), Size(cs + 1, cs + 1)).also {
                                drawRect(Dirt.copy(alpha = .35f), Offset(c * cs, r * cs), Size(cs + 1, cs + 1))
                            }
                            Kind.LAVA -> drawRect(Brush.verticalGradient(listOf(Color(0xFFFF9E00), Color(0xFFD62828)), r * cs, (r + 1) * cs), Offset(c * cs, r * cs), Size(cs + 1, cs + 1))
                            Kind.SEED -> {
                                val (fx, fy) = vm.animFrom[cell.id]?.let { (fr, fc) -> fc to fr } ?: (c.toFloat() to r.toFloat())
                                val t = vm.animT
                                val x = (fx + (c - fx) * t) * cs; val y = (fy + (r - fy) * t) * cs
                                drawSeed(cell, x, y, cs)
                            }
                            else -> {}
                        }
                    }
                    drawTrunkAndQueen(vm, cs, queenImgs, frame)
                }
                // الفواكه: عناصر Compose مع إزاحة تُقرأ داخل graphicsLayer (دون إعادة تركيب كل إطار)
                val cellPxLocal = with(androidx.compose.ui.platform.LocalDensity.current) { cellDp.toPx() }
                val fruits = remember(vm.version) {
                    buildList { for (r in 0 until vm.state.rows) for (c in 0 until vm.state.cols) vm.state.grid[r][c]?.takeIf { it.kind == Kind.FRUIT }?.let { add(Triple(it, r, c)) } }
                }
                for ((cell, r, c) in fruits) key(cell.id) {
                    FruitComposable(
                        fruitType = FruitType.entries[cell.fruit % FruitType.entries.size],
                        isSelected = vm.selected == (r to c),
                        modifier = Modifier.size(cellDp).padding(cellDp * 0.06f).graphicsLayer {
                            val from = vm.animFrom[cell.id]
                            val t = vm.animT
                            val fr = from?.first ?: r.toFloat(); val fc = from?.second ?: c.toFloat()
                            translationX = (fc + (c - fc) * t) * cellPxLocal
                            translationY = (fr + (r - fr) * t) * cellPxLocal
                        }.then(Modifier.offset { IntOffset.Zero })
                    )
                }
            }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { vm.toggleHammer() }, enabled = vm.hammers > 0 && !vm.busy) {
                Text(if (vm.hammerMode) "🔨 اختر خلية" else "🔨 مطرقة × ${vm.hammers}")
            }
        }
    }

    if (vm.status != RescueStatus.PLAYING) {
        val won = vm.status == RescueStatus.WON
        Box(Modifier.fillMaxSize().background(Color(0xAA000000)), contentAlignment = Alignment.Center) {
            Column(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF1E3A36)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (won) "نجت الملكة! 👑" else if (vm.movesLeft <= 0) "انتهت الحركات 😢" else "أمسك بها الثعبان 🐍", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (won) {
                    Text("★".repeat(vm.stars) + "☆".repeat(3 - vm.stars), color = Color(0xFFFFD166), fontSize = 32.sp)
                    val rw = vm.reward
                    if (rw != null) {
                        val n = if (vm.rewardDoubled) 2 else 1
                        Text("🎁 ${rw.icon} ${rw.titleAr} × $n${if (vm.rewardDoubled) " (مضاعفة!)" else ""}", color = Color(0xFFFFD166), fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
                        if (!vm.rewardDoubled) {
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = { adPurpose = 1; showAd = true }) { Text("📺 ضاعف الجائزة بمشاهدة إعلان") }
                        }
                    }
                } else if (!vm.revived) {
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { adPurpose = 0; showAd = true }) { Text("📺 شاهد إعلاناً: يتراجع الثعبان خطوتين وأكمل") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (won || vm.revived) Button(onClick = { vm.restart() }) { Text("إعادة") }
                    if (won) Button(onClick = onExit) { Text("متابعة إلى الجزيرة التالية ◀") }
                    else Button(onClick = onQuitToMenu) { Text("استسلام والخروج للقائمة الرئيسية") }
                }
            }
        }
        if (showAd) {
            AdGateDialog(
                title = if (adPurpose == 1) "شاهد الإعلان لمضاعفة الجائزة" else "شاهد الإعلان لإبعاد الثعبان",
                onReward = {
                    showAd = false
                    if (adPurpose == 1) { vm.reward?.let { onGrantReward(it, 1) }; vm.markRewardDoubled() } else vm.reviveAfterAd()
                },
                onCancel = { showAd = false }
            )
        }
    }
}

private fun DrawScope.drawSeed(cell: Cell, x: Float, y: Float, cs: Float) {
    val col = SeedColors[cell.variant.mod(SeedColors.size)]
    val p = cs * 0.08f
    if (cell.heavy) {
        drawRoundRect(Color(0xFF4A4947), Offset(x + p, y + p), Size(cs - 2 * p, cs - 2 * p), CornerRadius(cs * 0.3f))
        drawRoundRect(Color(0xFF8A8784), Offset(x + p * 2, y + p * 2), Size(cs * 0.45f, cs * 0.3f), CornerRadius(cs * 0.15f))
    } else {
        drawCircle(col, cs * 0.38f, Offset(x + cs / 2, y + cs / 2))
        drawCircle(Color.White.copy(alpha = .45f), cs * 0.1f, Offset(x + cs * 0.4f, y + cs * 0.38f))
    }
}

private fun DrawScope.drawSnake(vm: RescueViewModel, cs: Float, frame: Long) {
    val l = vm.state.level
    val bottom = (vm.snakeVis + 1f).coerceAtMost(l.queenRow + 2f) * cs
    val x0 = cs * 0.5f
    var y = 0f
    var prev = Offset(x0, 0f)
    while (y < bottom) {
        y += cs * 0.2f
        val p = Offset(x0 + sin(y / cs * 2.2f + frame / 300f) * cs * 0.18f, y)
        drawLine(Color(0xFF4CAF50), prev, p, cs * 0.5f)
        prev = p
    }
    drawCircle(Color(0xFF2E7D32), cs * 0.36f, prev)
    drawCircle(Color.White, cs * 0.09f, prev + Offset(-cs * 0.12f, -cs * 0.04f))
    drawCircle(Color.White, cs * 0.09f, prev + Offset(cs * 0.12f, -cs * 0.04f))
}

private fun DrawScope.drawTrunkAndQueen(vm: RescueViewModel, cs: Float, imgs: Map<QueenPose, ImageBitmap>, frame: Long) {
    val l = vm.state.level
    val tx = l.trunkCol * cs
    val top = l.trunkTop * cs
    val h = (l.trunkBottom - l.trunkTop + 1) * cs
    val angle = vm.trunkLean(System.currentTimeMillis())
    withTransform({ rotate(Math.toDegrees(angle.toDouble()).toFloat(), Offset(tx + cs / 2, top + h)) }) {
        drawRoundRect(Color(0xFF7B4A24), Offset(tx + cs * 0.1f, top), Size(cs * 0.8f, h), CornerRadius(cs * 0.2f))
        for (i in 1..3) drawLine(Color(0xFF55321A), Offset(tx + cs * 0.2f, top + h * i / 4f), Offset(tx + cs * 0.8f, top + h * i / 4f), cs * 0.05f)
    }
    val q = imgs.getValue(vm.pose)
    val qh = cs * 4f
    val qw = qh * q.width / q.height
    drawImage(
        q, srcSize = IntSize(q.width, q.height),
        dstOffset = IntOffset(((1f + vm.queenDx) * cs - qw / 2).toInt(), ((l.queenRow + vm.queenDy) * cs).toInt()),
        dstSize = IntSize(qw.toInt(), qh.toInt())
    )
}
