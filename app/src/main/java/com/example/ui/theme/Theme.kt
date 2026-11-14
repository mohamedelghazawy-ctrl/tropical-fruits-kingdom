package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val TropicalColorScheme = darkColorScheme(
    primary = TropicalMint,
    onPrimary = Color(0xFF00382B),
    primaryContainer = TropicalEmerald,
    onPrimaryContainer = TropicalMint,
    secondary = TropicalSunGold,
    onSecondary = Color(0xFF3B2800),
    secondaryContainer = TropicalWarmAmber,
    onSecondaryContainer = Color.White,
    tertiary = TropicalHibiscus,
    onTertiary = Color.White,
    background = TropicalDarkJungle,
    onBackground = TropicalSand,
    surface = TropicalEmerald,
    onSurface = TropicalSand,
    surfaceVariant = Color(0xFF144737),
    onSurfaceVariant = Color(0xFFE0F2E9)
)

@Composable
fun TropicalCrushTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TropicalColorScheme,
        typography = Typography,
        content = content
    )
}
