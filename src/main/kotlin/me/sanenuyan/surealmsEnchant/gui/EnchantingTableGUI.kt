package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.listeners.EnchantingTableListenerSimple
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Compatibility wrapper for old EnchantingTableGUI API
 * Delegates to new ConfigurableEnchantingTableGUI system
 */
class EnchantingTableGUI(
    private val plugin: SurealmsEnchant,
    private val enchantmentSystem: EnchantmentSystem,
    private val specializedBookManager: SpecializedBookManager,
    private val customEnchantmentManager: CustomEnchantmentManager,
    listener: EnchantingTableListenerSimple
) {
    
    companion object {
        const val TITLE = "§6§lEnchanting Table"
        const val SIZE = 54
        const val BOOK_SLOT = 22  // Default input slot
        const val ENCHANT_INDEX_SLOT = 40
        
        // Tier slots
        const val TIER_1_SLOT = 4
        const val TIER_2_SLOT = 13
        const val TIER_3_SLOT = 22
        
        // Option slots
        val TIER_1_OPTION_SLOTS = listOf(5, 6, 7, 8)
        val TIER_2_OPTION_SLOTS = listOf(14, 15, 16, 17)
        val TIER_3_OPTION_SLOTS = listOf(23, 24, 25, 26)
    }
    
    // Delegate to manager
    private val manager = EnchantingTableGUIManager(plugin, enchantmentSystem, specializedBookManager, customEnchantmentManager, listener)
    
    /**
     * Open GUI for player
     */
    fun openGUI(player: Player) {
        manager.openGUI(player)
    }
    
    /**
     * Check if inventory is enchanting table GUI
     */
    fun isEnchantingTableGUI(inventory: Inventory): Boolean {
        return manager.isEnchantingTableGUI(inventory)
    }
    
    /**
     * Get book slot (legacy)
     */
    fun getBookSlot(): Int = BOOK_SLOT

    /**
     * Get input slot (configurable)
     */
    fun getInputSlot(): Int {
        return try {
            plugin.guiConfig.getItemConfig("enchanting_table", "input_slot")?.slot ?: 31
        } catch (e: Exception) {
            31 // Fallback to slot 31
        }
    }
    
    /**
     * Check if slot is book slot
     */
    fun isBookSlot(slot: Int): Boolean = slot == BOOK_SLOT
    
    /**
     * Check if slot is enchant index slot
     */
    fun isEnchantIndexSlot(slot: Int): Boolean = slot == ENCHANT_INDEX_SLOT
    
    /**
     * Check if slot is enchantment option slot
     */
    fun isEnchantmentOptionSlot(slot: Int): Boolean {
        return slot in TIER_1_OPTION_SLOTS || 
               slot in TIER_2_OPTION_SLOTS || 
               slot in TIER_3_OPTION_SLOTS
    }
    
    /**
     * Update tier options (compatibility method)
     */
    fun updateTierOptions(inventory: Inventory, player: Player) {
        // Get GUI for player and refresh
        val gui = manager.getGUI(player)
        gui?.refresh()
    }
    
    /**
     * Handle click (compatibility method)
     */
    fun handleClick(slot: Int, item: ItemStack?, player: Player) {
        manager.handleClick(player, slot, item)
    }
    
    /**
     * Create enchantment option item (compatibility)
     */
    fun createEnchantmentOptionItem(
        enchantment: me.sanenuyan.surealmsEnchant.models.CustomEnchantment,
        tier: me.sanenuyan.surealmsEnchant.models.EnchantTier,
        cost: Double,
        player: Player
    ): ItemStack {
        // Create basic enchanted book
        val item = ItemStack(org.bukkit.Material.ENCHANTED_BOOK)
        val meta = item.itemMeta!!
        
        val tierColor = when (tier.tier) {
            1 -> "§a"
            2 -> "§b"
            3 -> "§d"
            else -> "§f"
        }
        
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize("$tierColor${enchantment.displayName}").decoration(TextDecoration.ITALIC, false))
        
        val loreStrings = mutableListOf<String>()
        loreStrings.addAll(enchantment.description)
        loreStrings.add("")
        loreStrings.add("§7Tier: ${tier.displayName}")
        loreStrings.add("§7Cost: §6${plugin.vaultIntegration.format(cost)}")
        loreStrings.add("")
        
        val hasEnoughMoney = plugin.vaultIntegration.hasEnough(player, cost)
        val hasEnoughLevel = player.level >= tier.requiredLevel
        
        if (hasEnoughMoney && hasEnoughLevel) {
            loreStrings.add("§aClick to enchant!")
        } else {
            if (!hasEnoughMoney) {
                loreStrings.add("§cNot enough money!")
            }
            if (!hasEnoughLevel) {
                loreStrings.add("§cNot enough XP levels!")
            }
        }
        
        val loreComponents = loreStrings.map { LegacyComponentSerializer.legacySection().deserialize(it).decoration(TextDecoration.ITALIC, false) }
        meta.lore(loreComponents)
        item.itemMeta = meta
        return item
    }
    
    /**
     * Create border item (compatibility)
     */
    fun createBorderItem(): ItemStack {
        val item = ItemStack(Material.BLACK_STAINED_GLASS_PANE)
        val meta = item.itemMeta!!
        meta.displayName(Component.text(" ").decoration(TextDecoration.ITALIC, false))
        item.itemMeta = meta
        return item
    }
    
    /**
     * Create book placeholder (compatibility)
     */
    fun createBookPlaceholder(): ItemStack {
        val item = ItemStack(Material.BOOK)
        val meta = item.itemMeta!!
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§e§lPlace Book Here").decoration(TextDecoration.ITALIC, false))
        val lore = listOf(
            "§7Place an enchanted book here",
            "§7to see available enchantments"
        )
        val loreComponents = lore.map { LegacyComponentSerializer.legacySection().deserialize(it).decoration(TextDecoration.ITALIC, false) }
        meta.lore(loreComponents)
        item.itemMeta = meta
        return item
    }
    
    /**
     * Create enchant index button (compatibility)
     */
    fun createEnchantIndexButton(): ItemStack {
        val item = ItemStack(Material.ENCHANTED_BOOK)
        val meta = item.itemMeta!!
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize("§6§lEnchantment Index").decoration(TextDecoration.ITALIC, false))
        val lore = listOf(
            "§7View all available enchantments",
            "§7and their descriptions",
            "",
            "§eClick to open!"
        )
        val loreComponents = lore.map { LegacyComponentSerializer.legacySection().deserialize(it).decoration(TextDecoration.ITALIC, false) }
        meta.lore(loreComponents)
        item.itemMeta = meta
        return item
    }
    
    /**
     * Get inventory size
     */
    fun getSize(): Int = SIZE
    
    /**
     * Get inventory title
     */
    fun getTitle(): String = TITLE
}
