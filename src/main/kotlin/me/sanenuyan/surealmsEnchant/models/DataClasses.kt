package me.sanenuyan.surealmsEnchant.models

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import kotlin.collections.contains

/**
 * Represents an enchant tier with its requirements and properties
 */
data class EnchantTier(
    val tier: Int,
    val requiredLevel: Int,
    val baseCost: Double,
    val displayName: String,
    val description: List<String>
)

/**
 * Represents a custom enchantment with all its properties
 */
data class CustomEnchantment(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val bukkitEnchantment: Enchantment,
    val level: Int,
    val maxLevel: Int,
    val tier: Int,
    val compatibleItems: List<Material>,
    val conflictsWith: List<Enchantment>,
    val costMultiplier: Double,
    val rarity: EnchantmentRarity,
    val isCustom: Boolean = false,
    val isTreasure: Boolean = false,
    val isCursed: Boolean = false,
    val isExcellentEnchant: Boolean = false,
    val excellentEnchantData: Any? = null,
    val vanillaEnchant: Enchantment? = null
) {
    val name: String get() = displayName.replace("§[0-9a-fk-or]".toRegex(), "")
    val descriptionText: String get() = description.joinToString(" ")

    fun isCompatibleWith(bookType: SpecializedBookType?): Boolean {
        if (bookType == null) return true // No book type means it's a regular book, so any enchant is fine
        return compatibleItems.any { it in bookType.compatibleItems }
    }
}

/**
 * Represents the rarity of an enchantment
 */
enum class EnchantmentRarity(
    val displayName: String,
    val color: String,
    val weight: Int
) {
    COMMON("Common", "§f", 100),
    UNCOMMON("Uncommon", "§a", 75),
    RARE("Rare", "§9", 50),
    EPIC("Epic", "§5", 25),
    LEGENDARY("Legendary", "§6", 10),
    MYTHIC("Mythic", "§c", 5)
}

/**
 * Represents a specialized book type
 */
data class SpecializedBookType(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val compatibleItems: List<Material>,
    val texture: String? = null,
    val customModelData: Int? = null,
    val enchantment_glint_override: Boolean? = false
)

/**
 * Represents an enchanting option in the GUI
 */
data class EnchantingOption(
    val enchantment: CustomEnchantment,
    val description: List<String>,
    val tier: EnchantTier,
    val cost: Double,
    val slot: Int
)

/**
 * Represents the result of an enchanting attempt
 */
sealed class EnchantingResult {
    data class Success(val enchantment: CustomEnchantment, val chance: Double, val anvilFailureChance: Double) : EnchantingResult()
    data class Failure(val reason: String) : EnchantingResult()
    data class InsufficientFunds(val required: Double, val available: Double) : EnchantingResult()
    data class InsufficientLevel(val required: Int, val current: Int) : EnchantingResult()
    object BookAlreadyEnchanted : EnchantingResult()
    object InvalidBook : EnchantingResult()
    object NoEnchantmentSelected : EnchantingResult()
    data class Destruction(val successChance: Double, val destructionChance: Double) : EnchantingResult()
}

/**
 * Represents the result of applying an enchanted book to an item
 */
sealed class EnchantApplicationResult {
    object Success : EnchantApplicationResult()
    object IncompatibleItem : EnchantApplicationResult()
    object ConflictingEnchantment : EnchantApplicationResult()
    object AlreadyEnchanted : EnchantApplicationResult()
    object InvalidBook : EnchantApplicationResult()
    object InvalidItem : EnchantApplicationResult()
}

/**
 * Configuration for the Tome of Renewal item.
 */
data class TomeOfRenewal(
    val material: String,
    val customModelData: Int,
    val displayName: String,
    val lore: List<String>,
    val successMessage: String
)

/**
 * Configuration for the Ashes item.
 */
data class AshesItem(
    val material: String,
    val customModelData: Int,
    val displayName: String,
    val lore: List<String>,
    val chanceModifier: Double
)

/**
 * Configuration data for the enchanting system
 */
data class EnchantingConfig(
    val enableVaultIntegration: Boolean,
    val enableXpCosts: Boolean,
    val tier1RequiredLevel: Int,
    val tier2RequiredLevel: Int,
    val tier3RequiredLevel: Int,
    val tier1BaseCost: Double,
    val tier2BaseCost: Double,
    val tier3BaseCost: Double,
    val enchantingTableTitle: String,
    val enchantIndexTitle: String,
    val soundEffects: SoundConfig
)

/**
 * Sound configuration
 */
data class SoundConfig(
    val enchantSuccess: String,
    val enchantFailure: String,
    val bookCreate: String,
    val guiClick: String,
    val guiOpen: String,
    val anvilEnchant: String,
    val grindstoneDisenchant: String
)

/**
 * Represents a player's enchanting session
 */
data class EnchantingSession(
    val playerId: String,
    val bookSlotItem: Material?,
    val selectedTier: EnchantTier?,
    val availableOptions: List<EnchantingOption>,
    val timestamp: Long
)

/**
 * Statistics for enchantments
 */
data class EnchantmentStats(
    val enchantmentId: String,
    val timesApplied: Int,
    val totalCostPaid: Double,
    val averageCost: Double,
    val lastUsed: Long
)

/**
 * Player enchanting statistics
 */
data class PlayerEnchantingStats(
    val playerId: String,
    val totalEnchantments: Int,
    val totalMoneySpent: Double,
    val favoriteEnchantment: String?,
    val enchantmentCounts: Map<String, Int>,
    val firstEnchantTime: Long,
    val lastEnchantTime: Long
)
