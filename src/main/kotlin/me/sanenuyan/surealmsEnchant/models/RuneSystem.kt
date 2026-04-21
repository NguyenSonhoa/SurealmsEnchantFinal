package me.sanenuyan.surealmsEnchant.models

import org.bukkit.Material

/**
 * Represents different types of runes
 */
enum class RuneType(val id: String) {
    POWER_RUNE("power_rune"),
    PROTECTION_RUNE("protection_rune")
}

/**
 * Configuration for a specific rune item's display properties.
 */
data class RuneItemConfig(
    val displayName: String,
    val lore: List<String>,
    val material: Material,
    val customModelData: Int
)

/**
 * Represents a rune item with its properties
 */
data class Rune(
    val type: RuneType,
    val level: Int = 1,
    val maxLevel: Int = 1
)

/**
 * Result of applying a rune to an item
 */
sealed class RuneApplicationResult {
    object Success : RuneApplicationResult()
    object IncompatibleItem : RuneApplicationResult()
    object AlreadyProtected : RuneApplicationResult()
    object NoEnchantmentsToUpgrade : RuneApplicationResult()
    object AllEnchantmentsMaxLevel : RuneApplicationResult()
    object InvalidRune : RuneApplicationResult()
    object InvalidItem : RuneApplicationResult()
    data class Failure(val reason: String) : RuneApplicationResult()
}

/**
 * Represents the state of a protected item
 */
data class ProtectionState(
    val isProtected: Boolean,
    val runeAppliedTime: Long,
    val originalDurability: Int
)

/**
 * Configuration for rune system
 */
data class RuneConfig(
    val enableRunes: Boolean,
    val powerRuneEnabled: Boolean,
    val protectionRuneEnabled: Boolean,
    val powerRuneCost: Double,
    val protectionRuneCost: Double,
    val allowRuneStacking: Boolean,
    val maxProtectionRunes: Int,
    val shatterMessage: String,
    val protectionMessage: String,
    val powerRuneItemConfig: RuneItemConfig,
    val protectionRuneItemConfig: RuneItemConfig
)
