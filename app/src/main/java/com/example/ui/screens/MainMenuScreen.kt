package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FruitType
import com.example.ui.components.FruitComposable
import com.example.ui.components.PauseDialog

@Composable
fun MainMenuScreen(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    totalStars: Int,
    gems: Int = 0,
    wheelReadyAt: Long = 0L,
    onWheelSpin: () -> Pair<Int, com.example.model.WheelPrize>? = { null },
    onWheelDouble: (com.example.model.WheelPrize) -> Unit = {},
    onPlayClick: () -> Unit,
    onMapClick: () -> Unit,
    onIslandClick: () -> Unit = {},
    onSoundToggle: (Boolean) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettings by remember { mutableStateOf(false) }
    var showWheel by remember { mutableStateOf(false) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(1000); nowMs = System.currentTimeMillis() }
    }
    val wheelLeft = wheelReadyAt - nowMs
    if (showWheel) {
        com.example.ui.components.LuckyWheelDialog(
            onSpin = onWheelSpin, onDouble = onWheelDouble, onClose = { showWheel = false }
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "title_bounce")
    val bounceScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Jungle Background image
        Image(
            painter = painterResource(id = R.drawable.bg_tropical_jungle),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark gradient scrim for readability and high contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x99051812),
                            Color(0x7708241A),
                            Color(0xEE051812)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Stars count & Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xCC0E382A),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x6602C39A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD166),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$totalStars نجمة",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xCC0E382A),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x6602C39A))
                    ) {
                        Text(
                            text = "💎 $gems",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier.clickable { showWheel = true },
                        shape = RoundedCornerShape(26.dp),
                        color = if (wheelLeft <= 0) Color(0xFFE76F51) else Color(0xCC0E382A),
                        border = androidx.compose.foundation.BorderStroke(2.5.dp, if (wheelLeft <= 0) Color(0xFFFFD166) else Color(0x6602C39A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎡", fontSize = 40.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            val t = if (wheelLeft <= 0) "عجلة الحظ!" else {
                                val sec = wheelLeft / 1000
                                "%d:%02d:%02d".format(sec / 3600, (sec % 3600) / 60, sec % 60)
                            }
                            Text(text = t, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                }

                // Settings icon button
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xCC0E382A), CircleShape)
                        .border(1.5.dp, Color(0x6602C39A), CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "الإعدادات",
                        tint = Color.White
                    )
                }
            }

            // Title and Mascot Showcase
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(bounceScale)
            ) {
                // Fruit Showcase row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FruitComposable(
                        fruitType = FruitType.DRAGONFRUIT,
                        modifier = Modifier.size(44.dp)
                    )
                    FruitComposable(
                        fruitType = FruitType.PINEAPPLE,
                        modifier = Modifier.size(54.dp)
                    )
                    FruitComposable(
                        fruitType = FruitType.WATERMELON,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "مملكة الفواكه",
                    style = TextStyle(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFB703),
                        shadow = Shadow(
                            color = Color(0xFF00382B),
                            offset = Offset(3f, 4f),
                            blurRadius = 8f
                        )
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "الاستوائية 🌺",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00F5D4),
                        shadow = Shadow(
                            color = Color(0xFF00382B),
                            offset = Offset(3f, 4f),
                            blurRadius = 8f
                        )
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x55000000),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = "مغامرة المطابقة في الجزيرة الساحرة",
                        fontSize = 14.sp,
                        color = Color(0xFFE0F2E9),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Play Button
                Button(
                    onClick = onPlayClick,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00A896)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(12.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF00F5D4))
                        .testTag("play_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ابدأ المغامرة 🍍",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                // Level Map Button
                OutlinedButton(
                    onClick = onMapClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x990E382A)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFFB703), Color(0xFF00A896))
                        ),
                        width = 2.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("map_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color(0xFFFFB703),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "خريطة الجزر والمستويات 🗺️",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // جزيرة القصر
                OutlinedButton(
                    onClick = onIslandClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0x990E382A)),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFFFFB703), Color(0xFF00A896))), width = 2.dp
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("island_button")
                ) {
                    Text("👑 جزيرة القصر", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                // Fruits strip preview
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FruitType.standardFruits.forEach { fruit ->
                        FruitComposable(
                            fruitType = fruit,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Settings Dialog
        if (showSettings) {
            PauseDialog(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                onSoundToggle = onSoundToggle,
                onHapticsToggle = onHapticsToggle,
                onResume = { showSettings = false },
                onRestart = { showSettings = false },
                onMainMenu = { showSettings = false }
            )
        }
    }
}
