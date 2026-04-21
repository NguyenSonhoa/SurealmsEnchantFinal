package me.sanenuyan.surealmsEnchant.config

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import java.io.File

class GUIConfig(private val plugin: SurealmsEnchant) {

    private lateinit var config: YamlConfiguration
    private val configFile = File(plugin.dataFolder, "gui.yml")

    fun load() {

        if (!configFile.exists()) {
            plugin.saveResource("gui.yml", false)
            plugin.logger.info("Created default gui.yml configuration")
        }

        try {
            config = YamlConfiguration.loadConfiguration(configFile)
            plugin.logger.info("GUI configuration loaded successfully from ${configFile.path}")
        } catch (e: Exception) {
            plugin.logger.severe("Failed to load gui.yml: ${e.message}")

            plugin.saveResource("gui.yml", true)
            config = YamlConfiguration.loadConfiguration(configFile)
        }
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(configFile)
        plugin.logger.info("GUI configuration reloaded")
    }

    fun getTitle(guiType: String): String {
        return config.getString("$guiType.title", "&6&lGUI")?.replace("&", "§") ?: "§6§lGUI"
    }

    fun getTitle(guiType: String, player: org.bukkit.entity.Player): String {
        var title = getTitle(guiType)
        val placeholders = plugin.config.getConfigurationSection("gui_placeholders")?.getValues(false) ?: emptyMap<String, Any>()
        placeholders.forEach { (k, v) ->
            title = title.replace("{$k}", v.toString())
        }
        return try {
            if (plugin.placeholderAPIIntegration?.isEnabled() == true) {
                plugin.placeholderAPIIntegration?.parsePlaceholders(player, title) ?: title
            } else {
                title
            }
        } catch (e: Exception) {
            title
        }
    }

    fun getSize(guiType: String): Int {
        return config.getInt("$guiType.size", 54)
    }

    fun getItemConfig(guiType: String, itemKey: String): ItemConfig? {
        val path = "$guiType.items.$itemKey"
        if (!config.contains(path)) return null

        val material = Material.valueOf(config.getString("$path.material", "STONE")!!.uppercase())
        val displayName = config.getString("$path.display_name", "")?.replace("&", "§")
        val lore = config.getStringList("$path.lore").map { it.replace("&", "§") }
        val customModelData = config.getInt("$path.custom_model_data", 0)
        val nbtId = config.getString("$path.nbt_id") ?: itemKey
        val slot = config.getInt("$path.slot", -1)
        val slots = config.getIntegerList("$path.slots")

        return ItemConfig(
            material = material,
            displayName = displayName,
            lore = lore,
            customModelData = customModelData,
            nbtId = nbtId ?: itemKey,
            slot = slot,
            slots = slots
        )
    }

    fun createItem(guiType: String, itemKey: String, placeholders: Map<String, String> = emptyMap()): ItemStack? {
        val itemConfig = getItemConfig(guiType, itemKey)
        if (itemConfig == null) {
            plugin.logger.warning("No config found for guiType=$guiType, itemKey=$itemKey")
            return null
        }
        val item = ItemStack(itemConfig.material)
        val meta = item.itemMeta ?: return item

        if (!itemConfig.displayName.isNullOrEmpty()) {
            var displayName = itemConfig.displayName!!
            placeholders.forEach { (key, value) ->
                displayName = displayName.replace("{$key}", value)
            }
            meta.setDisplayName(displayName)
        }

        if (itemConfig.lore.isNotEmpty()) {
            val processedLore = itemConfig.lore.map { loreLine ->
                var line = loreLine
                placeholders.forEach { (key, value) ->
                    line = line.replace("{$key}", value)
                }
                line
            }
            meta.lore = processedLore
        }

        if (itemConfig.customModelData > 0) {
            meta.setCustomModelData(itemConfig.customModelData)
        }

        val container = meta.persistentDataContainer
        val nbtKey = org.bukkit.NamespacedKey(plugin, "gui_item_id")
        container.set(nbtKey, PersistentDataType.STRING, itemConfig.nbtId)

        item.itemMeta = meta
        return item
    }

    fun getItemNBTId(item: ItemStack): String? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val nbtKey = org.bukkit.NamespacedKey(plugin, "gui_item_id")
        return container.get(nbtKey, PersistentDataType.STRING)
    }

    fun hasNBTId(item: ItemStack, nbtId: String): Boolean {
        return getItemNBTId(item) == nbtId
    }

    fun getSlots(guiType: String, slotKey: String): List<Int> {
        return config.getIntegerList("$guiType.$slotKey")
    }

    fun getTierColor(tier: Int): String {
        return config.getString("enchantment_display.tier_colors.$tier", "&f")?.replace("&", "§") ?: "§f"
    }

    fun getSourceColor(source: String): String {
        return config.getString("enchantment_display.source_colors.$source", "&f")?.replace("&", "§") ?: "§f"
    }

    fun getRarityColor(rarity: String): String {
        return config.getString("enchantment_display.rarity_colors.$rarity", "&f")?.replace("&", "§") ?: "§f"
    }

    fun getSpecialIndicator(type: String): String {
        return config.getString("enchantment_display.special_indicators.$type", "")?.replace("&", "§") ?: ""
    }

    fun getSound(soundKey: String): String {
        return config.getString("sounds.$soundKey", "UI_BUTTON_CLICK") ?: "UI_BUTTON_CLICK"
    }

    fun isAnimationEnabled(animationType: String): Boolean {
        return config.getBoolean("animations.$animationType.enabled", false)
    }

    fun getAnimationDuration(animationType: String): Int {
        return config.getInt("animations.$animationType.duration", 20)
    }

    fun getAnimationInterval(animationType: String): Int {
        return config.getInt("animations.$animationType.interval", 2)
    }

    fun getEnchantsPerPage(): Int {
        return config.getInt("enchant_index.enchantments_per_page", 27)
    }

    fun getTierMaterial(tier: Int): org.bukkit.Material {
        val materialName = config.getString("enchantment_display.tier_materials.$tier", "ENCHANTED_BOOK")
        return try {
            org.bukkit.Material.valueOf(materialName!!.uppercase())
        } catch (e: Exception) {
            org.bukkit.Material.ENCHANTED_BOOK
        }
    }

    fun getTierCustomModelData(tier: Int): Int {
        return config.getInt("enchantment_display.tier_custom_models.$tier", 0)
    }

    fun shouldTierGlow(tier: Int): Boolean {
        return config.getBoolean("enchantment_display.tier_glow.$tier", false)
    }

}

data class ItemConfig(
    val material: Material,
    val displayName: String?,
    val lore: List<String>,
    val customModelData: Int,
    val nbtId: String,
    val slot: Int,
    val slots: List<Int>
)

