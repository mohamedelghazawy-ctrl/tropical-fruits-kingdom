package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdGateDialog
import com.example.model.LuckyWheel
import com.example.model.WheelPrize
import kotlinx.coroutines.launch

/**
 * عجلة الحظ. onSpin تعيد (فهرس القطعة, الجائزة) أو null إن لم تكن جاهزة.
 * onDouble تُستدعى بعد مشاهدة الإعلان لمضاعفة الجائزة.
 */
@Composable
fun LuckyWheelDialog(
    onSpin: () -> Pair<Int, WheelPrize>?,
    onDouble: (WheelPrize) -> Unit,
    onClose: () -> Unit
) {
    val rot = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var spinning by remember { mutableStateOf(false) }
    var spun by remember { mutableStateOf(false) }
    var prize by remember { mutableStateOf<WheelPrize?>(null) }
    var doubled by remember { mutableStateOf(false) }
    var showAd by remember { mutableStateOf(false) }
    val n = LuckyWheel.PRIZES.size
    val seg = 360f / n

    if (showAd) {
        AdGateDialog(
            title = "شاهد الإعلان لمضاعفة الجائزة",
            onReward = { showAd = false; doubled = true; prize?.let(onDouble) },
            onCancel = { showAd = false }
        )
    }

    Dialog(onDismissRequest = { if (!spinning) onClose() }, properties = DialogProperties(dismissOnBackPress = !spinning, dismissOnClickOutside = false)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF0E382A)) {
            Column(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🎡 عجلة الحظ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD166))
                Text("▼", fontSize = 22.sp, color = Color(0xFFE76F51))
                Canvas(modifier = Modifier.size(260.dp)) {
                    val r = size.minDimension / 2f
                    rotate(rot.value, pivot = center) {
                        for (i in 0 until n) {
                            // القطعة i مركزها عند زاوية (i+0.5)*seg مقاسة من أعلى باتجاه عقارب الساعة
                            val start = i * seg - 90f
                            drawArc(
                                color = if (i % 2 == 0) Color(0xFF2A9D8F) else Color(0xFFF4A261),
                                startAngle = start, sweepAngle = seg, useCenter = true,
                                topLeft = Offset.Zero, size = Size(size.width, size.height)
                            )
                            val mid = Math.toRadians((start + seg / 2f).toDouble())
                            val lx = center.x + (r * 0.68f) * Math.cos(mid).toFloat()
                            val ly = center.y + (r * 0.68f) * Math.sin(mid).toFloat()
                            val p = LuckyWheel.PRIZES[i]
                            val paint = android.graphics.Paint().apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 30.sp.toPx() * 0.8f
                                isAntiAlias = true
                            }
                            drawContext.canvas.nativeCanvas.apply {
                                save()
                                rotate(start + seg / 2f + 90f, lx, ly)
                                drawText(p.icon, lx, ly, paint)
                                paint.textSize = 12.sp.toPx()
                                paint.color = android.graphics.Color.WHITE
                                paint.isFakeBoldText = true
                                drawText(p.short, lx, ly + 16.sp.toPx(), paint)
                                restore()
                            }
                        }
                    }
                }
                val pz = prize
                if (pz != null) {
                    val mult = if (doubled) 2 else 1
                    val txt = if (pz.gems > 0) "💎 ${pz.gems * mult} جوهرة" else "${pz.icon} ${pz.booster?.titleAr} × ${pz.amount * mult}"
                    Text("ربحت: $txt", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    if (!spinning && !doubled) {
                        Button(onClick = { showAd = true }) { Text("📺 ضاعف الجائزة (إعلان)") }
                    }
                } else if (spun && !spinning) {
                    Text("العجلة غير جاهزة بعد", color = Color.White)
                } else {
                    Spacer(Modifier.height(6.dp))
                }
                if (!spun) {
                    Button(enabled = !spinning, onClick = {
                        val res = onSpin()
                        spun = true
                        if (res != null) {
                            spinning = true
                            val target = 360f * 6 + (360f - (res.first + 0.5f) * seg)
                            scope.launch {
                                rot.animateTo(target, tween(4200, easing = FastOutSlowInEasing))
                                prize = res.second
                                spinning = false
                            }
                        }
                    }) { Text("🎡 ادور العجلة", fontSize = 16.sp) }
                } else {
                    TextButton(enabled = !spinning, onClick = onClose) { Text("إغلاق", color = Color.White) }
                }
            }
        }
    }
}
