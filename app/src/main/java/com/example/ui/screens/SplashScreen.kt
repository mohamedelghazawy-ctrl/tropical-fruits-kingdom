package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.island.IslandData
import kotlinx.coroutines.delay

/**
 * افتتاحية اللعبة: شاشة "GameXBreak" السوداء ٣ ثوانٍ، ثم صورة الملكة ٧ ثوانٍ مع شريط تحميل
 * وجملتين متغيّرتين في كل مرة.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    val lines = remember { IslandData.SPLASH_LINES.shuffled().take(2) }
    var lineIdx by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val lineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(3000)
        phase = 1
        progress.animateTo(1f, tween(7000, easing = LinearEasing))
    }
    // الجملتان تتبدّلان بتلاشٍ متتابع (لا تتداخلان) حتى لا تظهر جملة مشوّهة بينهما
    LaunchedEffect(phase) {
        if (phase == 1) {
            lineAlpha.animateTo(1f, tween(300))
            delay(2900)
            lineAlpha.animateTo(0f, tween(250))
            lineIdx = 1
            lineAlpha.animateTo(1f, tween(300))
            delay(3250)
            onFinished()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = phase, animationSpec = tween(600), label = "splash") { p ->
            if (p == 0) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.splash_gxb), contentDescription = "GameXBreak",
                        modifier = Modifier.fillMaxWidth(0.86f).widthIn(max = 520.dp).aspectRatio(640f / 1146f), contentScale = ContentScale.Fit
                    )
                }
            } else {
                Image(
                    painter = painterResource(R.drawable.splash_queen), contentDescription = null,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                )
            }
        }
        if (phase == 1) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    lines[lineIdx], color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                    modifier = Modifier
                        .graphicsLayer { alpha = lineAlpha.value }
                        .clip(RoundedCornerShape(14.dp)).background(Color(0x80000000)).padding(horizontal = 16.dp, vertical = 6.dp)
                )
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x73000000))) {
                    Box(Modifier.fillMaxWidth(progress.value).fillMaxHeight().background(Color(0xFFFFB703)))
                }
            }
        }
    }
}
