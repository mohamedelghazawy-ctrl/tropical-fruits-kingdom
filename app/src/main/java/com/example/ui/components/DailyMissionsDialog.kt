package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.daily.MissionView

/** نافذة المهام اليومية. [views] و[chest*] تُقرأ من جديد عند كل استلام. */
@Composable
fun DailyMissionsDialog(
    views: () -> List<MissionView>,
    streak: () -> Int,
    chestReady: () -> Boolean,
    chestClaimed: () -> Boolean,
    chestReward: () -> Int,
    onClaim: (Int) -> Unit,
    onClaimChest: () -> Unit,
    onClose: () -> Unit
) {
    var tick by remember { mutableIntStateOf(0) }
    val list = remember(tick) { views() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = onClose) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF0E382A), border = BorderStroke(2.dp, Color(0xFF02C39A))) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📋 المهام اليومية", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    val st = streak()
                    Text(if (st > 0) "🔥 سلسلة $st ${if (st == 1) "يوم" else "أيام"} متتالية" else "أنجز المهام الثلاث لتبدأ سلسلة أيام",
                        fontSize = 13.sp, color = Color(0xFFFFD166), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    list.forEach { m ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0x33FFFFFF)) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.type.icon, fontSize = 26.sp)
                                    Spacer(Modifier.padding(horizontal = 6.dp))
                                    Text(m.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                }
                                LinearProgressIndicator(
                                    progress = { (m.progress.toFloat() / m.target).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF02C39A), trackColor = Color(0x33FFFFFF)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${m.progress} / ${m.target}", fontSize = 12.sp, color = Color(0xFFBFE8DA), modifier = Modifier.weight(1f))
                                    when {
                                        m.claimed -> Text("✔ تم الاستلام", fontSize = 13.sp, color = Color(0xFF7FD184), fontWeight = FontWeight.Bold)
                                        m.done -> Button(onClick = { onClaim(m.index); tick++ }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE76F51))) { Text("استلم 💎 ${m.reward}", fontWeight = FontWeight.Bold) }
                                        else -> Text("💎 ${m.reward}", fontSize = 13.sp, color = Color(0xFFFFD166), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = if (chestReady()) Color(0x55FFD166) else Color(0x22FFFFFF)) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🎁", fontSize = 30.sp)
                            Spacer(Modifier.padding(horizontal = 6.dp))
                            Column(Modifier.weight(1f)) {
                                Text("صندوق اليوم", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("💎 ${chestReward()} + 🔨 مطرقة (يزيد مع السلسلة)", fontSize = 12.sp, color = Color(0xFFBFE8DA))
                            }
                            when {
                                chestClaimed() -> Text("✔ تم", color = Color(0xFF7FD184), fontWeight = FontWeight.Bold)
                                chestReady() -> Button(onClick = { onClaimChest(); tick++ }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE76F51))) { Text("افتح", fontWeight = FontWeight.Bold) }
                                else -> Text("أنهِ المهام الثلاث", fontSize = 12.sp, color = Color(0xFFBFE8DA))
                            }
                        }
                    }
                    Text("تتجدد المهام كل يوم عند منتصف الليل.", fontSize = 11.sp, color = Color(0xFF8FBFAF), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("إغلاق", color = Color.White, fontSize = 16.sp) }
                }
            }
        }
    }
}
