package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.LevelConfig

@Composable
fun VictoryDialog(
    score: Int,
    stars: Int,
    levelConfig: LevelConfig,
    hasNextLevel: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF008241A)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFFFB703), Color(0xFF00A896), Color(0xFFFFD166))
                ),
                width = 3.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉 أحسنت صنعاً! 🎉",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFB703),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "أكملت ${levelConfig.titleAr}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0F2E9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // Animated Stars
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    for (i in 1..3) {
                        AnimatedStar(
                            earned = stars >= i,
                            delayMs = i * 160
                        )
                    }
                }

                // Score Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33004D40)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "مجموع النقاط",
                            fontSize = 13.sp,
                            color = Color(0xFF98D8C8)
                        )
                        Text(
                            text = "$score",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Dynamic Difficulty Banner
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33FFB703)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(0x66FFB703), Color(0x6602C39A))),
                        width = 1.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "⚡ ترقية الصعوبة: تزداد متطلبات الفواكه والأهداف بعد كل إنجاز!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD166),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                // Action Buttons
                if (hasNextLevel) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_level_button")
                    ) {
                        Text(
                            text = "المستوى التالي ➡️",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("إعادة", fontSize = 14.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onMainMenu,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("القائمة", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedStar(
    earned: Boolean,
    delayMs: Int
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(earned) {
        if (earned) {
            kotlinx.coroutines.delay(delayMs.toLong())
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .scale(if (earned) scale.value else 0.85f),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = if (earned) Color(0xFFFFD166) else Color(0x33FFFFFF),
            modifier = Modifier.size(46.dp)
        )
    }
}

@Composable
fun DefeatDialog(
    score: Int,
    targetScore: Int,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    canWatchAd: Boolean = false,
    onWatchAd: () -> Unit = {},
    freeRevive: Boolean = false
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF008241A)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(Color(0xFFD90429), Color(0xFFFB8500))
                ),
                width = 2.5.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("defeat_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "نفدت الحركات! 🥥",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFF5400),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "لم تحقق هدف المستوى هذه المرة، حاول مجدداً!",
                    fontSize = 14.sp,
                    color = Color(0xFFE0F2E9),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x33004D40)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("نقاطك", fontSize = 12.sp, color = Color(0xFF98D8C8))
                            Text("$score", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الهدف", fontSize = 12.sp, color = Color(0xFF98D8C8))
                            Text("$targetScore", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB703))
                        }
                    }
                }

                if (canWatchAd) {
                    Button(
                        onClick = onWatchAd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("watch_ad_button")
                    ) {
                        Text(if (freeRevive) "🐴 إحياء مجاني من الإسطبل: ٥ حركات إضافية" else "📺 شاهد إعلاناً: ٥ حركات إضافية وأكمل", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3A2400))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("retry_button")
                ) {
                    Text(
                        text = "إعادة المحاولة 🔄",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text(if (canWatchAd) "استسلام والخروج للقائمة الرئيسية" else "العودة للقائمة الرئيسية", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun PauseDialog(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF008241A)),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF00A896), Color(0xFFFFB703))
                ),
                width = 2.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إيقاف مؤقت 🌴",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFB703),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Settings Rows
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = null,
                            tint = Color(0xFF00A896)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الأصوات", fontSize = 15.sp, color = Color.White)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = onSoundToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00A896))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = null,
                            tint = Color(0xFF00A896)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("الاهتزاز", fontSize = 15.sp, color = Color.White)
                    }
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = onHapticsToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00A896))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A896)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("متابعة اللعب ▶️", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onRestart,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("إعادة المستوى 🔄", fontSize = 14.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onMainMenu,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("القائمة الرئيسية 🏠", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}
