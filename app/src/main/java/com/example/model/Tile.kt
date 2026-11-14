package com.example.model

data class Tile(
    val id: Long,
    val row: Int,
    val col: Int,
    val type: FruitType,
    val special: SpecialType = SpecialType.NONE,
    val isMatched: Boolean = false,
    val isSelected: Boolean = false,
    val isNew: Boolean = false
) {
    fun hasSpecial(): Boolean = special != SpecialType.NONE
}
