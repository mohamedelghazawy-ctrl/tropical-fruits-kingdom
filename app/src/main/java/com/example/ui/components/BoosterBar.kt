package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BoosterType

@Composable
fun BoosterBar(
    inventory: Map<BoosterType, Int>,
    activeBooster: BoosterType?,
    onBoosterClick: (BoosterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active booster prompt
        AnimatedVisibility(
            visible = activeBooster != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xEEFFB703),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "انقر على أي خلية لاستخدام ${activeBooster?.titleAr}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF092318),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Booster Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterType.entries.forEach { booster ->
                val count = inventory[booster] ?: 0
                val isSelected = activeBooster == booster

                BoosterItem(
                    booster = booster,
                    count = count,
                    isSelected = isSelected,
                    onClick = { onBoosterClick(booster) }
                )
            }
        }
    }
}

@Composable
private fun BoosterItem(
    booster: BoosterType,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = Modifier.testTag("booster_${booster.name.lowercase()}")
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) Color(0xFF00A896) else Color(0xCC0E382A)
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    if (isSelected) listOf(Color(0xFFFFD166), Color.White)
                    else listOf(Color(0x6602C39A), Color(0x22FFFFFF))
                ),
                width = if (isSelected) 2.5.dp else 1.dp
            ),
            modifier = Modifier
                .size(60.dp)
                .clickable(enabled = count > 0, onClick = onClick)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = booster.icon,
                    fontSize = 26.sp
                )
            }
        }

        // Count Badge
        Box(
            modifier = Modifier
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .background(Color(0xFFE76F51), CircleShape)
                .border(1.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
