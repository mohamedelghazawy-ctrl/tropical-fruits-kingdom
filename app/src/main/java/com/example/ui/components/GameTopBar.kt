package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FruitType
import com.example.model.LevelConfig

@Composable
fun GameTopBar(
    levelConfig: LevelConfig,
    score: Int,
    movesRemaining: Int,
    collectedFruits: Map<FruitType, Int>,
    currentStars: Int,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_moves")
    val lowMovesPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (movesRemaining <= 5) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "low_moves"
    )

    val movesBgColor by animateColorAsState(
        targetValue = when {
            movesRemaining <= 3 -> Color(0xFFD90429)
            movesRemaining <= 6 -> Color(0xFFFB8500)
            else -> Color(0xFF00A896)
        },
        label = "moves_bg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Top Row: Level Title, Moves Left, Pause Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xCC0E382A),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0x6602C39A))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المستوى ${levelConfig.levelNumber}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB703)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🌴",
                        fontSize = 14.sp
                    )
                }
            }

            // Moves Left Indicator (Pill)
            Box(
                modifier = Modifier
                    .scale(lowMovesPulse)
                    .background(movesBgColor, RoundedCornerShape(20.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("moves_indicator"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$movesRemaining",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "حركة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Pause button
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xCC0E382A), CircleShape)
                    .border(1.5.dp, Color(0x6602C39A), CircleShape)
                    .testTag("pause_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "إيقاف مؤقت",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Second Row: Score progress & Fruit Goals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Score & Stars
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "النقاط: $score",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF1FAEE)
                    )

                    // 3-Star Badges
                    Row {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $i",
                                tint = if (currentStars >= i) Color(0xFFFFD166) else Color(0x55FFFFFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Score Bar
                val progress = (score.toFloat() / levelConfig.starThresholds.third).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFFFB703),
                    trackColor = Color(0x44FFFFFF)
                )
            }

            // Fruit collection goals if any
            if (levelConfig.targetFruits.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    levelConfig.targetFruits.forEach { (fruit, target) ->
                        val collected = collectedFruits[fruit] ?: 0
                        val isDone = collected >= target

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDone) Color(0x442EC4B6) else Color(0x33000000),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDone) Color(0xFF2EC4B6) else Color(0x44FFFFFF)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fruit.emoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDone) "✓" else "$collected/$target",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDone) Color(0xFF00F5D4) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
