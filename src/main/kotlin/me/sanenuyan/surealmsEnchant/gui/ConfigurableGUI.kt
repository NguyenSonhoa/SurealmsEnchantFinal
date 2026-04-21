package me.sanenuyan.surealmsEnchant.gui

import net.kyori.adventure.text.Component
import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.config.GUIConfig
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * Base class for configurable GUIs
 * Uses YAML configuration for all GUI elements
 */
abstract class ConfigurableGUI(
    protected val plugin: SurealmsEnchant,
    protected val guiType: String,
    protected val player: Player
) {
    
    protected val guiConfig: GUIConfig = plugin.guiConfig
    protected lateinit var inventory: Inventory
    
    /**
     * Initialize the GUI
     */
    open fun initialize() {
        val title = guiConfig.getTitle(guiType, player)
        val size = guiConfig.getSize(guiType)

        inventory = Bukkit.createInventory(null, size, title)
        setupItems()
    }
    
    /**
     * Setup GUI items from configuration
     */
    protected open fun setupItems() {
        // Clear inventory
        inventory.clear()

        // Setup border items first
        setupBorderItems()

        // Setup static items (these should override border items if needed)
        setupStaticItems()

        // Setup dynamic content last
        setupDynamicContent()
    }
    
    /**
     * Setup border items
     */
    protected open fun setupBorderItems() {
        val borderItem = guiConfig.createItem(guiType, "border")
        if (borderItem != null) {
            val borderConfig = guiConfig.getItemConfig(guiType, "border")
            borderConfig?.slots?.forEach { slot ->
                if (slot in 0 until inventory.size) {
                    inventory.setItem(slot, borderItem.clone())
                }
            }
        }
    }
    
    /**
     * Setup static items (buttons, decorations, etc.)
     */
    protected open fun setupStaticItems() {
        // Override in subclasses to add specific static items
    }
    
    /**
     * Setup dynamic content (enchantments, pages, etc.)
     */
    protected open fun setupDynamicContent() {
        // Override in subclasses to add dynamic content
    }
    
    /**
     * Get the inventory instance
     */
    fun getGUIInventory(): Inventory = inventory

    /**
     * Get GUI type identifier
     */
    open fun getGUITypeId(): String = guiType

    /**
     * Open GUI for player
     */
    fun open() {
        // Register with GUI manager
        plugin.configurableGUIManager.registerGUI(player, this)
        player.openInventory(inventory)
        playSound("open_gui")
    }
    
    /**
     * Close GUI
     */
    fun close() {
        plugin.configurableGUIManager.unregisterGUI(player)
        player.closeInventory()
        playSound("close_gui")

        // Clear PlaceholderAPI state
        try {
            if (plugin.placeholderAPIIntegration?.isEnabled() == true) {
                plugin.placeholderAPIIntegration?.clearPlayerState(player)
            }
        } catch (e: Exception) {
            // Ignore PlaceholderAPI errors
        }
    }

    /**
     * Called when GUI is closed (for cleanup)
     */
    open fun onClose() {
        // Override in subclasses for cleanup
    }

    /**
     * Refresh GUI content
     */
    fun refresh() {
        setupItems()
    }
    
    /**
     * Check if click should be cancelled
     */
    open fun shouldCancelClick(slot: Int, item: ItemStack?, player: Player): Boolean {
        // By default, cancel all clicks
        return true
    }

    /**
     * Called after a click that wasn't cancelled
     */
    open fun updateAfterClick(slot: Int, player: Player) {
        // Override in subclasses if needed
    }

    /**
     * Check if shift+click should be blocked for this item
     */
    open fun shouldBlockShiftClick(item: ItemStack?, player: Player): Boolean {
        // By default, block all shift+clicks
        return true
    }

    /**
     * Handle item click
     */
    abstract fun handleClick(slot: Int, item: ItemStack?, player: Player)
    
    /**
     * Play sound effect
     */
    protected fun playSound(soundKey: String) {
        try {
            val soundName = guiConfig.getSound(soundKey)
            val sound = org.bukkit.Sound.valueOf(soundName)
            player.playSound(player.location, sound, 1.0f, 1.0f)
        } catch (e: Exception) {
            // Fallback to default sound
            player.playSound(player.location, org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f)
        }
    }
    
    /**
     * Create item with placeholders
     */
    protected fun createItem(itemKey: String, placeholders: Map<String, String> = emptyMap()): ItemStack? {
        return guiConfig.createItem(guiType, itemKey, placeholders)
    }
    
    /**
     * Check if item has specific NBT ID
     */
    protected fun hasNBTId(item: ItemStack?, nbtId: String): Boolean {
        return item?.let { guiConfig.hasNBTId(it, nbtId) } ?: false
    }
    
    /**
     * Get item NBT ID
     */
    protected fun getItemNBTId(item: ItemStack?): String? {
        return item?.let { guiConfig.getItemNBTId(it) }
    }
    fun Int.toRoman2(): String {
        if (this <= 0) return this.toString()
        val numerals = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
            100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
            10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
        )
        var n = this
        val sb = StringBuilder()
        for ((value, numeral) in numerals) {
            while (n >= value) {
                sb.append(numeral)
                n -= value
            }
        }
        return sb.toString()
    }
    /**
     * Set item in slot with NBT ID check
     */
    protected fun setItem(slot: Int, item: ItemStack?) {
        if (slot in 0 until inventory.size && item != null) {
            inventory.setItem(slot, item)
        }
    }
    
    /**
     * Get slots from configuration
     */
    protected fun getSlots(slotKey: String): List<Int> {
        return guiConfig.getSlots(guiType, slotKey)
    }
    
    /**
     * Create enchantment display item with tier-specific configuration
     */
    protected fun createEnchantmentItem(
        enchantment: me.sanenuyan.surealmsEnchant.models.CustomEnchantment,
        additionalLore: List<String> = emptyList(),
        tier: Int? = null,
        enchantmentLevel: Int? = null

    ): ItemStack {
        // Determine tier-specific settings
        val actualTier = tier ?: enchantment.tier
        val tierItemKey = when (actualTier) {
            1 -> "tier_1_enchant_item"
            2 -> "tier_2_enchant_item"
            3 -> "tier_3_enchant_item"
            else -> "enchantment_item"
        }

        val tierConfig = guiConfig.getItemConfig(guiType, tierItemKey)
        val material = tierConfig?.material ?: guiConfig.getTierMaterial(actualTier)

        val item = ItemStack(material)
        val meta = item.itemMeta!!

        // Get tier color
        val tierColor = guiConfig.getTierColor(enchantment.tier)
        val displayLevel = enchantmentLevel ?: enchantment.level
        val levelStr = if (enchantment.maxLevel > 1) " ${displayLevel.toRoman2()}" else ""


        // Set display name
        meta.displayName(ChatUtils.parse("$tierColor${enchantment.name}$levelStr"))

        val lore = mutableListOf<Component>()
        lore.addAll(enchantment.description.map { ChatUtils.parse(it) })
        lore.add(Component.empty())
        lore.add(Component.empty())
        lore.add(ChatUtils.parse("<gray>Tier: ${enchantment.tier}</gray>"))
        lore.add(ChatUtils.parse("<gray>Cấp Phù phép tối đa: ${enchantment.maxLevel}</gray>"))
        if (enchantment.maxLevel > 1) {
            lore.add(ChatUtils.parse("<gray>Cấp phù phép: $displayLevel</gray>"))
        }
        if (enchantment.isTreasure) {
            lore.add(ChatUtils.parse(guiConfig.getSpecialIndicator("treasure")))
        }
        if (enchantment.isCursed) {
            lore.add(ChatUtils.parse(guiConfig.getSpecialIndicator("cursed")))
        }
        if (enchantment.isExcellentEnchant) {
            lore.add(ChatUtils.parse(guiConfig.getSpecialIndicator("excellent")))
        }
        if (additionalLore.isNotEmpty()) {
            lore.add(Component.empty())
            lore.addAll(additionalLore.map { ChatUtils.parse(it) })
        }
        meta.lore(lore)

        // Set tier-specific custom model data
        val customModelData = if (tierConfig != null && tierConfig.customModelData > 0) {
            tierConfig.customModelData
        } else {
            guiConfig.getTierCustomModelData(actualTier)
        }

        if (customModelData > 0) {
            meta.setCustomModelData(customModelData)
        }

        // Add glow effect if configured
        if (guiConfig.shouldTierGlow(actualTier)) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true)
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS)
        }

        // set NBT ID for enchantment items
        val container = meta.persistentDataContainer
        val nbtKey = org.bukkit.NamespacedKey(plugin, "gui_item_id")
        val nbtId = tierConfig?.nbtId ?: "enchantment_item"
        container.set(nbtKey, org.bukkit.persistence.PersistentDataType.STRING, nbtId)

        // store enchantment ID for identification
        val enchantIdKey = org.bukkit.NamespacedKey(plugin, "enchantment_id")
        container.set(enchantIdKey, org.bukkit.persistence.PersistentDataType.STRING, enchantment.id)

        // Store tier for identification
        val tierKey = org.bukkit.NamespacedKey(plugin, "enchantment_tier")
        container.set(tierKey, org.bukkit.persistence.PersistentDataType.INTEGER, tier ?: enchantment.tier)

        item.itemMeta = meta
        return item
    }
    
    /**
     * Get enchantment ID from item
     */
    protected fun getEnchantmentId(item: ItemStack?): String? {
        val meta = item?.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val enchantIdKey = org.bukkit.NamespacedKey(plugin, "enchantment_id")
        return container.get(enchantIdKey, org.bukkit.persistence.PersistentDataType.STRING)
    }

    /**
     * Get enchantment tier from item
     */
    protected fun getEnchantmentTier(item: ItemStack?): Int? {
        val meta = item?.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val tierKey = org.bukkit.NamespacedKey(plugin, "enchantment_tier")
        return container.get(tierKey, org.bukkit.persistence.PersistentDataType.INTEGER)
    }
    
    /**
     * Start glow animation for item
     */
    protected fun startGlowAnimation(slot: Int) {
        if (!guiConfig.isAnimationEnabled("enchant_glow")) return
        
        val duration = guiConfig.getAnimationDuration("enchant_glow")
        val interval = guiConfig.getAnimationInterval("enchant_glow")
        
        // Implementation for glow animation
        // This would require a more complex animation system
    }

    open fun handleClose() {
        onClose()
    }
}
