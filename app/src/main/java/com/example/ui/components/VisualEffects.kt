package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScorePopup
import com.example.game.ParticleSystem

/**
 * يرسم الجسيمات دون أي إعادة تركيب: حلقة إطارات تعمل فقط أثناء وجود جسيمات،
 * و`frame` يُقرأ داخل مرحلة الرسم فقط.
 */
@Composable
fun ParticleOverlay(
    system: ParticleSystem,
    cols: Int,
    rows: Int,
    modifier: Modifier = Modifier
) {
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(system) {
        snapshotFlow { system.spawnVersion }.collectLatest {
            var last = withFrameNanos { it }
            while (system.hasActive) {
                val now = withFrameNanos { it }
                system.update((now - last) / 1_000_000_000f)
                last = now
                frame = now // يبطل الرسم فقط
            }
            frame = last
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frame // اشتراك في مرحلة الرسم
        val cw = size.width / cols
        val ch = size.height / rows
        for (i in 0 until system.count) {
            val a = system.life[i].coerceIn(0f, 1f)
            drawCircle(
                color = Color(system.argb[i]).copy(alpha = a),
                radius = system.size[i].dp.toPx() * 0.5f * a,
                center = Offset(system.x[i] * cw, system.y[i] * ch)
            )
        }
    }
}

@Composable
fun ScorePopupItem(
    popup: ScorePopup,
    cellWidthPx: Float,
    cellHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val offsetY = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(1f) }
    val scaleAnim = remember { Animatable(0.5f) }

    LaunchedEffect(popup.id) {
        scaleAnim.animateTo(1.2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        offsetY.animateTo(-70f, tween(600, easing = FastOutSlowInEasing))
        alphaAnim.animateTo(0f, tween(300, easing = LinearEasing))
    }

    val posX = popup.col * cellWidthPx + cellWidthPx * 0.2f
    val posY = popup.row * cellHeightPx + offsetY.value

    Box(
        modifier = modifier
            .offset { IntOffset(posX.toInt(), posY.toInt()) }
            .graphicsLayer {
                alpha = alphaAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
    ) {
        Text(
            text = popup.text,
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = popup.color,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun ComboBanner(
    comboText: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !comboText.isNullOrEmpty(),
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut(tween(250)) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFE76F51), Color(0xFFFFB703), Color(0xFF00A896))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(2.dp, Color.White, RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comboText ?: "",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color(0x99000000),
                        offset = Offset(3f, 3f),
                        blurRadius = 6f
                    )
                )
            )
        }
    }
}
