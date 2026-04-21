package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.models.ProtectionState
import me.sanenuyan.surealmsEnchant.models.RuneApplicationResult
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneItem
import me.sanenuyan.surealmsEnchant.models.RuneType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.persistence.PersistentDataType

/**
 * Manages rune creation, application, and effects
 */
class RuneManager(
    private val plugin: SurealmsEnchant,
    private val enchantmentSystem: EnchantmentSystem,
    private val runeConfig: RuneConfig
) {

    private val runeTypeKey = NamespacedKey(plugin, "rune_type")
    private val protectionKey = NamespacedKey(plugin, "protection_rune")
    private val protectionTimeKey = NamespacedKey(plugin, "protection_time")
    private val originalDurabilityKey = NamespacedKey(plugin, "original_durability")

    /**
     * Create a rune item
     */
    fun createRune(runeType: RuneType): ItemStack {
        return when (runeType) {
            RuneType.POWER_RUNE -> enchantmentSystem.createPowerRune()
            RuneType.PROTECTION_RUNE -> enchantmentSystem.createProtectionRune()
        }
    }

    /**
     * Check if an item is a rune
     */
    fun isRune(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(runeTypeKey)
    }

    /**
     * Get rune type from item
     */
    fun getRuneType(item: ItemStack): RuneType? {
        val meta = item.itemMeta ?: return null
        val runeId = meta.persistentDataContainer.get(runeTypeKey, PersistentDataType.STRING) ?: return null

        return RuneType.values().find { it.id == runeId }
    }

    /**
     * Get rune from item (for GUI usage) - returns RuneItem model
     */
    fun getRuneFromItem(item: ItemStack): RuneItem? {
        val runeType = getRuneType(item) ?: return null

        val runeItemConfig = when (runeType) {
            RuneType.POWER_RUNE -> runeConfig.powerRuneItemConfig
            RuneType.PROTECTION_RUNE -> runeConfig.protectionRuneItemConfig
        }

        return RuneItem(
            id = runeType.id,
            displayName = runeItemConfig.displayName,
            description = runeItemConfig.lore,
            material = runeItemConfig.material,
            customModelData = runeItemConfig.customModelData
        )
    }

    /**
     * Apply a rune to an item (for GUI usage) - returns enhanced item
     */
    fun applyRuneToItem(item: ItemStack, rune: RuneItem): ItemStack {
        val enhancedItem = item.clone()
        val runeType = RuneType.values().find { it.id == rune.id } ?: return enhancedItem

        when (runeType) {
            RuneType.POWER_RUNE -> {
                applyPowerRune(enhancedItem)
            }
            RuneType.PROTECTION_RUNE -> {
                applyProtectionRune(enhancedItem)
            }
        }

        return enhancedItem
    }

    /**
     * Apply a rune to an item
     */
    fun applyRune(targetItem: ItemStack, runeItem: ItemStack): RuneApplicationResult {
        val runeType = getRuneType(runeItem) ?: return RuneApplicationResult.InvalidRune

        return when (runeType) {
            RuneType.POWER_RUNE -> applyPowerRune(targetItem)
            RuneType.PROTECTION_RUNE -> applyProtectionRune(targetItem)
        }
    }

    /**
     * Apply Power Rune - upgrades all enchantments by 1 level
     */
    private fun applyPowerRune(item: ItemStack): RuneApplicationResult {
        if (!isValidItemForRunes(item)) {
            return RuneApplicationResult.IncompatibleItem
        }

        val enchantments = item.enchantments
        if (enchantments.isEmpty()) {
            return RuneApplicationResult.NoEnchantmentsToUpgrade
        }

        var upgraded = false

        for ((enchantment, currentLevel) in enchantments) {
            val maxLevel = enchantment.maxLevel
            if (currentLevel < maxLevel) {
                item.addUnsafeEnchantment(enchantment, currentLevel + 1)
                upgraded = true
            }
        }

        if (!upgraded) {
            return RuneApplicationResult.AllEnchantmentsMaxLevel
        }


        return RuneApplicationResult.Success
    }

    /**
     * Apply Protection Rune - prevents item destruction
     */
    private fun applyProtectionRune(item: ItemStack): RuneApplicationResult {
        if (!isValidItemForRunes(item)) {
            return RuneApplicationResult.IncompatibleItem
        }

        if (isProtected(item)) {
            return RuneApplicationResult.AlreadyProtected
        }

        val meta = item.itemMeta!!
        val currentTime = System.currentTimeMillis()
        val originalDurability = item.type.maxDurability.toInt()

        // Set protection data
        meta.persistentDataContainer.set(protectionKey, PersistentDataType.BYTE, 1.toByte())
        meta.persistentDataContainer.set(protectionTimeKey, PersistentDataType.LONG, currentTime)
        meta.persistentDataContainer.set(originalDurabilityKey, PersistentDataType.INTEGER, originalDurability)

        // Add protection message to lore
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        lore.add(Component.text(""))
        lore.add(MiniMessage.miniMessage().deserialize(runeConfig.protectionMessage))
        meta.lore(lore)

        item.itemMeta = meta

        return RuneApplicationResult.Success
    }

    /**
     * Check if an item is protected by a Protection Rune
     */
    fun isProtected(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(protectionKey)
    }

    /**
     * Handle item breaking - check for protection
     */
    fun handleItemBreaking(item: ItemStack): Boolean {
        if (!isProtected(item)) return false

        // Remove protection and set to 1 durability (shattered state)
        removeProtection(item)

        val meta = item.itemMeta ?: return false
        if (meta is Damageable) {
            meta.damage = item.type.maxDurability - 1
        }

        // Add shattered effect to lore
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        lore.add(Component.text(""))
        lore.add(MiniMessage.miniMessage().deserialize(runeConfig.shatterMessage))
        meta.lore(lore)

        item.itemMeta = meta

        return true // Item was protected
    }

    /**
     * Remove protection from an item
     */
    private fun removeProtection(item: ItemStack) {
        val meta = item.itemMeta ?: return

        meta.persistentDataContainer.remove(protectionKey)
        meta.persistentDataContainer.remove(protectionTimeKey)
        meta.persistentDataContainer.remove(originalDurabilityKey)

        // Remove protection message from lore
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()
        val protectedLoreComponent = MiniMessage.miniMessage().deserialize(runeConfig.protectionMessage)
        lore.remove(protectedLoreComponent)
        meta.lore(lore)

        item.itemMeta = meta
    }

    /**
     * Add power rune effect to item lore
     */
    private fun addPowerRuneEffect(item: ItemStack) {
        val meta = item.itemMeta ?: return
        val lore = meta.lore()?.toMutableList() ?: mutableListOf()

        val powerRuneMessage = "Power Rune Applied" // This should be from config
        val plainSerializer = PlainComponentSerializer.plain()

        val hasEffect = lore.any { plainSerializer.serialize(it).contains(powerRuneMessage) }
        if (!hasEffect) {
            lore.add(Component.text(""))
            lore.add(MiniMessage.miniMessage().deserialize(runeConfig.powerRuneItemConfig.displayName + " Applied")) // Example
            lore.add(MiniMessage.miniMessage().deserialize("§7All enchantments have been upgraded")) // This too
        }

        meta.lore(lore)
        item.itemMeta = meta
    }

    /**
     * Check if an item is valid for rune application
     */
    private fun isValidItemForRunes(item: ItemStack): Boolean {
        // Check if item has durability (tools, weapons, armor)
        return item.type.maxDurability > 0
    }

    /**
     * Get all available rune types
     */
    fun getAllRuneTypes(): Array<RuneType> = RuneType.values()

    /**
     * Get rune type key for NBT storage
     */
    fun getRuneTypeKey(): NamespacedKey = runeTypeKey

    /**
     * Get protection key for NBT storage
     */
    fun getProtectionKey(): NamespacedKey = protectionKey

    /**
     * Check if item is shattered (durability = 1 and has shattered lore)
     */
    fun isShattered(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val lore = meta.lore() ?: return false
        val damageable = meta as? Damageable ?: return false

        val shatterMessagePlainText = MiniMessage.miniMessage().stripTags(runeConfig.shatterMessage)
        val plainSerializer = PlainComponentSerializer.plain()

        return damageable.damage >= (item.type.maxDurability - 1) &&
                lore.any { plainSerializer.serialize(it).contains(shatterMessagePlainText) }
    }

    /**
     * Get protection state of an item
     */
    fun getProtectionState(item: ItemStack): ProtectionState {
        val meta = item.itemMeta
        if (meta == null || !isProtected(item)) {
            return ProtectionState(false, 0L, 0)
        }

        val appliedTime = meta.persistentDataContainer.get(protectionTimeKey, PersistentDataType.LONG) ?: 0L
        val originalDurability = meta.persistentDataContainer.get(originalDurabilityKey, PersistentDataType.INTEGER) ?: 0

        return ProtectionState(true, appliedTime, originalDurability)
    }
}
