package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FruitCoconutColor
import com.example.ui.theme.FruitDragonfruitColor
import com.example.ui.theme.FruitKiwiColor
import com.example.ui.theme.FruitMangoColor
import com.example.ui.theme.FruitPineappleColor
import com.example.ui.theme.FruitStarfruitColor
import com.example.ui.theme.FruitWatermelonColor

enum class FruitType(
    val arabicName: String,
    val englishName: String,
    val primaryColor: Color,
    val splashColor: Color,
    val emoji: String
) {
    PINEAPPLE("أناناس", "Pineapple", FruitPineappleColor, Color(0xFFFFD166), "🍍"),
    DRAGONFRUIT("دراجون فروت", "Dragonfruit", FruitDragonfruitColor, Color(0xFFFF70A6), "🌺"),
    WATERMELON("بطيخ", "Watermelon", FruitWatermelonColor, Color(0xFFFF5964), "🍉"),
    MANGO("مانجو", "Mango", FruitMangoColor, Color(0xFFFF9E00), "🥭"),
    COCONUT("جوز الهند", "Coconut", FruitCoconutColor, Color(0xFFF4EDE4), "🥥"),
    KIWI("كيوي", "Kiwi", FruitKiwiColor, Color(0xFF9EF01A), "🥝"),
    STRAWBERRY("فراولة", "Strawberry", Color(0xFFFF1E56), Color(0xFFFF597B), "🍓");

    companion object {
        val standardFruits: List<FruitType> = entries
    }
}
