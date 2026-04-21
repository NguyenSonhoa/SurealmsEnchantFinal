package me.sanenuyan.surealmsEnchant.nms

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class NMSEnchantmentRegistry(private val plugin: SurealmsEnchant) {

    data class CustomEnchantmentDef(
        val id: String,
        val displayName: String,
        val description: String,
        val maxLevel: Int,
        val applicableItems: Set<Material>
    )

    private val customEnchantments = mutableMapOf<String, CustomEnchantmentDef>()
    private var isInitialized = false

    private fun isExcellentEnchantsAvailable(): Boolean {
        return plugin.excellentEnchantsIntegration.isEnabled()
    }

    fun registerEnchantments(): Boolean {
        return try {
            if (isExcellentEnchantsAvailable()) {
                plugin.logger.info("ExcellentEnchants detected - skipping built-in enchantment registration")
                isInitialized = true
                return true
            }

            plugin.logger.info("Registering built-in custom enchantments using NBT storage...")

            registerAutoSmelting()
            registerBackstab()
            registerLuckyOrb()

            isInitialized = true
            plugin.logger.info("Successfully registered ${customEnchantments.size} built-in custom enchantments")
            true

        } catch (e: Exception) {
            plugin.logger.severe("Failed to register custom enchantments: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun registerAutoSmelting() {
        val enchantment = CustomEnchantmentDef(
            id = "auto_smelting",
            displayName = "§6Auto Smelting",
            description = "Automatically smelts ores when mining",
            maxLevel = 3,
            applicableItems = setOf(
                Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
                Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.STONE_PICKAXE
            )
        )
        customEnchantments["auto_smelting"] = enchantment
        plugin.logger.info("Registered Auto Smelting enchantment")
    }

    private fun registerBackstab() {
        val enchantment = CustomEnchantmentDef(
            id = "backstab",
            displayName = "§5Backstab",
            description = "Deal more damage when attacking from behind",
            maxLevel = 3,
            applicableItems = setOf(
                Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
                Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.STONE_SWORD
            )
        )
        customEnchantments["backstab"] = enchantment
        plugin.logger.info("Registered Backstab enchantment")
    }

    private fun registerLuckyOrb() {
        val enchantment = CustomEnchantmentDef(
            id = "lucky_orb",
            displayName = "§9Lucky Orb",
            description = "Gain more EXP when collecting orbs",
            maxLevel = 3,
            applicableItems = setOf(
                Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.LEATHER_HELMET,
                Material.CHAINMAIL_HELMET
            )
        )
        customEnchantments["lucky_orb"] = enchantment
        plugin.logger.info("Registered Lucky Orb enchantment")
    }

    fun addCustomEnchantment(item: ItemStack, enchantmentId: String, level: Int): ItemStack {

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchant = plugin.excellentEnchantsIntegration.getEnchantmentById(enchantmentId)
            if (excellentEnchant != null) {
                return plugin.excellentEnchantsIntegration.addEnchantment(item, excellentEnchant, level)
            }
        }

        val enchantment = customEnchantments[enchantmentId] ?: return item

        if (level < 1 || level > enchantment.maxLevel) return item
        if (!enchantment.applicableItems.contains(item.type)) return item

        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer

        val key = NamespacedKey(plugin, "custom_enchant_$enchantmentId")
        container.set(key, PersistentDataType.INTEGER, level)

        updateItemLore(meta, enchantmentId, level)

        item.itemMeta = meta
        return item
    }

    fun getCustomEnchantmentLevel(item: ItemStack, enchantmentId: String): Int {

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchant = plugin.excellentEnchantsIntegration.getEnchantmentById(enchantmentId)
            if (excellentEnchant != null) {
                return plugin.excellentEnchantsIntegration.getEnchantmentLevel(item, excellentEnchant)
            }
        }

        val meta = item.itemMeta ?: return 0
        val container = meta.persistentDataContainer
        val key = NamespacedKey(plugin, "custom_enchant_$enchantmentId")
        return container.get(key, PersistentDataType.INTEGER) ?: 0
    }

    fun removeCustomEnchantment(item: ItemStack, enchantmentId: String): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        val key = NamespacedKey(plugin, "custom_enchant_$enchantmentId")

        container.remove(key)

        removeFromItemLore(meta, enchantmentId)

        item.itemMeta = meta
        return item
    }

    private fun updateItemLore(meta: ItemMeta, enchantmentId: String, level: Int) {
        val enchantment = customEnchantments[enchantmentId] ?: return

        val lore = meta.lore?.toMutableList() ?: mutableListOf()

        lore.removeIf { it.contains(enchantment.displayName.replace("§[0-9a-fk-or]".toRegex(), "")) }

        val romanLevel = when (level) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> level.toString()
        }

        lore.add("${enchantment.displayName} $romanLevel")
        meta.lore = lore
    }

    private fun removeFromItemLore(meta: ItemMeta, enchantmentId: String) {
        val enchantment = customEnchantments[enchantmentId] ?: return

        val lore = meta.lore?.toMutableList() ?: return
        lore.removeIf { it.contains(enchantment.displayName.replace("§[0-9a-fk-or]".toRegex(), "")) }

        meta.lore = if (lore.isEmpty()) null else lore
    }

    fun getCustomEnchantments(item: ItemStack): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchants = plugin.excellentEnchantsIntegration.getEnchantments(item)
            excellentEnchants.forEach { (enchant, level) ->
                val enchantInfo = plugin.excellentEnchantsIntegration.getEnchantmentInfo(enchant)
                val enchantId = enchantInfo["id"] as? String ?: "unknown"
                result[enchantId] = level
            }
        }

        val meta = item.itemMeta ?: return result
        val container = meta.persistentDataContainer

        for (enchantmentId in customEnchantments.keys) {
            val key = NamespacedKey(plugin, "custom_enchant_$enchantmentId")
            val level = container.get(key, PersistentDataType.INTEGER)
            if (level != null && level > 0) {
                result[enchantmentId] = level
            }
        }

        return result
    }

    fun hasCustomEnchantments(item: ItemStack): Boolean {
        return getCustomEnchantments(item).isNotEmpty()
    }

    fun getCustomEnchantmentCount(item: ItemStack): Int {
        return getCustomEnchantments(item).size
    }

    fun getEnchantmentDef(enchantmentId: String): CustomEnchantmentDef? {
        return customEnchantments[enchantmentId]
    }

    fun getAllEnchantmentDefs(): Map<String, CustomEnchantmentDef> {
        return customEnchantments.toMap()
    }

    fun hasEnchantment(enchantmentId: String): Boolean {
        return customEnchantments.containsKey(enchantmentId)
    }

    fun getAutoSmeltingLevel(item: ItemStack): Int {
        return getCustomEnchantmentLevel(item, "auto_smelting")
    }

    fun getBackstabLevel(item: ItemStack): Int {
        return getCustomEnchantmentLevel(item, "backstab")
    }

    fun getLuckyOrbLevel(item: ItemStack): Int {
        return getCustomEnchantmentLevel(item, "lucky_orb")
    }

    fun getAutoSmeltingDef(): CustomEnchantmentDef? {
        return getEnchantmentDef("auto_smelting")
    }

    fun getBackstabDef(): CustomEnchantmentDef? {
        return getEnchantmentDef("backstab")
    }

    fun getLuckyOrbDef(): CustomEnchantmentDef? {
        return getEnchantmentDef("lucky_orb")
    }

    fun addAutoSmelting(item: ItemStack, level: Int): ItemStack {
        return addCustomEnchantment(item, "auto_smelting", level)
    }

    fun addBackstab(item: ItemStack, level: Int): ItemStack {
        return addCustomEnchantment(item, "backstab", level)
    }

    fun addLuckyOrb(item: ItemStack, level: Int): ItemStack {
        return addCustomEnchantment(item, "lucky_orb", level)
    }

    fun getEnchantmentsByCategory(category: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchants = plugin.excellentEnchantsIntegration.getEnchantmentsByCategory(category)
            excellentEnchants.forEach { enchant ->
                result.add(plugin.excellentEnchantsIntegration.getEnchantmentInfo(enchant))
            }
        }

        if (result.isEmpty()) {
            customEnchantments.values.forEach { enchant ->
                result.add(mapOf(
                    "id" to enchant.id,
                    "name" to enchant.displayName,
                    "description" to enchant.description,
                    "maxLevel" to enchant.maxLevel,
                    "applicableItems" to enchant.applicableItems.map { it.name }
                ))
            }
        }

        return result
    }

    fun searchEnchantments(query: String): List<Map<String, Any>> {
        val result = mutableListOf<Map<String, Any>>()

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchants = plugin.excellentEnchantsIntegration.searchEnchantments(query)
            excellentEnchants.forEach { enchant ->
                result.add(plugin.excellentEnchantsIntegration.getEnchantmentInfo(enchant))
            }
        }

        if (result.isEmpty()) {
            val lowerQuery = query.lowercase()
            customEnchantments.values.filter { enchant ->
                enchant.id.lowercase().contains(lowerQuery) ||
                        enchant.displayName.lowercase().contains(lowerQuery) ||
                        enchant.description.lowercase().contains(lowerQuery)
            }.forEach { enchant ->
                result.add(mapOf(
                    "id" to enchant.id,
                    "name" to enchant.displayName,
                    "description" to enchant.description,
                    "maxLevel" to enchant.maxLevel,
                    "applicableItems" to enchant.applicableItems.map { it.name }
                ))
            }
        }

        return result
    }

    fun getRandomEnchantments(item: ItemStack, count: Int = 3): List<Map<String, Any>> {

        if (isExcellentEnchantsAvailable()) {
            val excellentEnchants = plugin.excellentEnchantsIntegration.getRandomEnchantments(item, count)
            return excellentEnchants.map { enchant ->
                plugin.excellentEnchantsIntegration.getEnchantmentInfo(enchant)
            }
        }

        val compatibleEnchants = customEnchantments.values.filter { enchant ->
            enchant.applicableItems.contains(item.type)
        }

        return compatibleEnchants.shuffled().take(count).map { enchant ->
            mapOf(
                "id" to enchant.id,
                "name" to enchant.displayName,
                "description" to enchant.description,
                "maxLevel" to enchant.maxLevel,
                "applicableItems" to enchant.applicableItems.map { it.name }
            )
        }
    }

    fun getTotalEnchantmentsCount(): Int {
        return if (isExcellentEnchantsAvailable()) {
            plugin.excellentEnchantsIntegration.getAvailableEnchantments().size
        } else {
            customEnchantments.size
        }
    }

    fun unregisterEnchantments() {
        if (!isInitialized) return

        customEnchantments.clear()
        isInitialized = false
        plugin.logger.info("Unregistered custom enchantments")
    }
}

