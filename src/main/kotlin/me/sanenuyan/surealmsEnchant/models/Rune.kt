package me.sanenuyan.surealmsEnchant.models

import org.bukkit.Material

/**
 * Represents a rune that can be applied to items
 * Used for GUI-based rune application system
 */
data class RuneItem(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val material: Material,
    val customModelData: Int,
)
