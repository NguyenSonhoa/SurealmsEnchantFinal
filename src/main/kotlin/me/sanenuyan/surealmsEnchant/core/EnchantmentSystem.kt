package me.sanenuyan.surealmsEnchant.core

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import me.sanenuyan.surealmsEnchant.models.CustomEnchantment
import me.sanenuyan.surealmsEnchant.models.EnchantTier
import me.sanenuyan.surealmsEnchant.models.EnchantmentRarity
import me.sanenuyan.surealmsEnchant.models.SpecializedBookType
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneType
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.NamespacedKey

/**
 * Core enchantment system that handles all enchanting logic
 */
class EnchantmentSystem(
    private val plugin: SurealmsEnchant,
    private val customEnchantmentManager: CustomEnchantmentManager,
    private val specializedBookManager: SpecializedBookManager,
    private val runeConfig: RuneConfig
) {

    // NamespacedKey for the "protected" status on items
    val protectedKey = NamespacedKey(plugin, "protected_item")
    val protectionRuneKey = NamespacedKey(plugin, "protection_rune")
    val powerRuneKey = NamespacedKey(plugin, "power_rune")
    val runeTypeKey = NamespacedKey(plugin, "rune_type")

    /**
     * Get available enchant tiers for a player based on their XP level
     */
    fun getAvailableTiers(player: Player): List<EnchantTier> {
        val playerLevel = player.level
        val tiers = mutableListOf<EnchantTier>()

        // Tier 1
        val requiredLevel1 = plugin.config.getInt("enchanting.tier1.required-level", 5)
        if (playerLevel >= requiredLevel1) {
            tiers.add(EnchantTier(
                tier = 1,
                requiredLevel = requiredLevel1,
                baseCost = plugin.config.getDouble("enchanting.tier1.base-cost", 100.0),
                displayName = "§eTier I",
                description = listOf("§7Basic enchantments", "§7Requires level $requiredLevel1+")
            ))
        }

        // Tier 2
        val requiredLevel2 = plugin.config.getInt("enchanting.tier2.required-level", 15)
        if (playerLevel >= requiredLevel2) {
            tiers.add(EnchantTier(
                tier = 2,
                requiredLevel = requiredLevel2,
                baseCost = plugin.config.getDouble("enchanting.tier2.base-cost", 250.0),
                displayName = "§eTier II",
                description = listOf("§7Improved enchantments", "§7Requires level $requiredLevel2+")
            ))
        }

        // Tier 3
        val requiredLevel3 = plugin.config.getInt("enchanting.tier3.required-level", 30)
        if (playerLevel >= requiredLevel3) {
            tiers.add(EnchantTier(
                tier = 3,
                requiredLevel = requiredLevel3,
                baseCost = plugin.config.getDouble("enchanting.tier3.base-cost", 500.0),
                displayName = "§eTier III",
                description = listOf("§7Powerful enchantments", "§7Requires level $requiredLevel3+")
            ))
        }

        return tiers
    }

    /**
     * Get available enchantments for a specific tier and book type
     */
    fun getApplicableEnchantments(
        item: ItemStack,
        tier: Int,
        bookType: SpecializedBookType? = null
    ): List<CustomEnchantment> {
        val applicableEnchantments = mutableListOf<CustomEnchantment>()

        if (bookType == null && item.type == Material.BOOK) {
            // Handle vanilla books: get all vanilla enchants that match the tier
            applicableEnchantments.addAll(getVanillaEnchantmentsAsCustom().filter { it.tier == tier })
        } else {
            // Handle specialized books
            if (bookType != null) {
                // Custom enchantments from this plugin
                applicableEnchantments.addAll(
                    customEnchantmentManager.getEnchantmentsByTier(tier).filter { enchant ->
                        enchant.isCompatibleWith(bookType)
                    }
                )
            } else {
                // Handle enchanting other items directly (not books)
                applicableEnchantments.addAll(
                    customEnchantmentManager.getEnchantmentsByTier(tier).filter { enchant ->
                        enchant.bukkitEnchantment.canEnchantItem(item)
                    }
                )
            }
        }

        val itemEnchants = item.enchantments
        return applicableEnchantments.distinctBy { it.id }.filter { enchant ->
            val currentLevel = itemEnchants[enchant.bukkitEnchantment] ?: 0
            if (currentLevel >= enchant.maxLevel) {
                return@filter false // Already at max level
            }

            // If it's a new enchant, check for conflicts
            if (currentLevel == 0) {
                val conflictsWithBukkit = itemEnchants.keys.any { existingEnchant -> enchant.bukkitEnchantment.conflictsWith(existingEnchant) }
                val conflictsWithCustom = enchant.conflictsWith.any { conf -> itemEnchants.containsKey(conf) }
                !conflictsWithBukkit && !conflictsWithCustom
            } else {
                true // It's an upgrade, no need to check conflicts again
            }
        }
    }


    private fun isEnchantmentOnItem(item: ItemStack, enchantment: Enchantment): Boolean {
        return item.enchantments.containsKey(enchantment)
    }

    private fun isCompatible(enchant: CustomEnchantment, item: ItemStack): Boolean {
        return enchant.compatibleItems.contains(item.type)
    }

    private fun getUniversalEnchantmentList(): List<CustomEnchantment> {
        val masterList = mutableListOf<CustomEnchantment>()
        masterList.addAll(getVanillaEnchantmentsAsCustom())
        if (plugin.excellentEnchantsIntegration.isEnabled()) {
            masterList.addAll(getExcellentEnchantsAsCustom())
        }
        masterList.addAll(customEnchantmentManager.getAllEnchantments())
        return masterList
    }

    private fun getVanillaEnchantmentsAsCustom(): List<CustomEnchantment> {
        return Enchantment.values().map { enchant ->
            val properTier = when (enchant.maxLevel) {
                1 -> if (enchant.isTreasure) 3 else 1
                2, 3 -> 2
                4, 5 -> 3
                else -> 3
            }

            CustomEnchantment(
                id = enchant.key.key,
                displayName = enchant.key.key.replace('_', ' ').split(' ').joinToString(" ") { it.replaceFirstChar(Char::titlecase) },
                description = listOf(),
                bukkitEnchantment = enchant,
                level = 1,
                maxLevel = enchant.maxLevel,
                tier = properTier,
                compatibleItems = Material.values().toList(),
                conflictsWith = listOf(), // Will be handled by Bukkit's `conflictsWith`
                costMultiplier = 1.0,
                rarity = EnchantmentRarity.COMMON, // Not really used for vanilla
                isCustom = false,
                isTreasure = enchant.isTreasure,
                isCursed = enchant.isCursed,
                isExcellentEnchant = false
            )
        }
    }

    fun getVanillaAsCustom(enchantId: String): CustomEnchantment? {
        return getVanillaEnchantmentsAsCustom().find { it.id.equals(enchantId, ignoreCase = true) }
    }

    private fun getExcellentEnchantsAsCustom(): List<CustomEnchantment> {
        return plugin.excellentEnchantsIntegration.getAvailableEnchantments().mapNotNull { enchant ->
            convertExcellentEnchant(enchant)
        }
    }

    private fun convertExcellentEnchant(enchant: Any): CustomEnchantment? {
        val id = plugin.excellentEnchantsIntegration.getEnchantmentProperty(enchant, "id") as? String ?: return null
        val displayName = plugin.excellentEnchantsIntegration.getEnchantmentProperty(enchant, "displayName") as? String ?: id
        val maxLevel = plugin.excellentEnchantsIntegration.getEnchantmentProperty(enchant, "maxLevel") as? Int ?: 1
        val rarityStr = plugin.excellentEnchantsIntegration.getEnchantmentProperty(enchant, "rarity")?.toString()?.uppercase() ?: "EPIC"

        val rarity = try {
            EnchantmentRarity.valueOf(rarityStr)
        } catch (e: Exception) {
            EnchantmentRarity.EPIC
        }

        val tier = when (rarity) {
            EnchantmentRarity.COMMON, EnchantmentRarity.UNCOMMON -> 1
            EnchantmentRarity.RARE, EnchantmentRarity.EPIC -> 2
            EnchantmentRarity.EPIC, EnchantmentRarity.LEGENDARY, EnchantmentRarity.MYTHIC -> 3
        }

        val compatibleItemNames = plugin.excellentEnchantsIntegration.getEnchantmentProperty(enchant, "applicableItems") as? Collection<String> ?: listOf()
        val compatibleItems = compatibleItemNames.mapNotNull { materialName ->
            try {
                Material.valueOf(materialName.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        return CustomEnchantment(
            id = id,
            displayName = displayName,
            description = listOf(""),
            bukkitEnchantment = Enchantment.getByName(id.uppercase()) ?: Enchantment.UNBREAKING,
            level = 1,
            maxLevel = maxLevel,
            tier = tier,
            compatibleItems = if (compatibleItems.isNotEmpty()) compatibleItems else Material.values().toList(),
            conflictsWith = listOf(),
            costMultiplier = 1.0,
            rarity = rarity,
            isCustom = false,
            isTreasure = false,
            isCursed = false,
            isExcellentEnchant = true,
            excellentEnchantData = enchant
        )
    }

    private fun getCategoryFromBookType(bookType: SpecializedBookType): String {
        val id = bookType.id.lowercase()
        return when {
            id.contains("bow") || id.contains("crossbow") -> "bow"
            id.contains("sword") || id.contains("axe") -> "weapon"
            id.contains("pickaxe") || id.contains("shovel") -> "tool"
            id.contains("helmet") || id.contains("chestplate") || id.contains("leggings") || id.contains("boots") || id.contains("armor") -> "armor"
            id.contains("fishing_rod") -> "fishing"
            else -> ""
        }
    }

    /**
     * Create an enchanted book with the specified enchantment
     */
    fun createEnchantedBook(
        originalBook: ItemStack,
        enchantment: CustomEnchantment,
        level: Int,
        bookType: SpecializedBookType? = null
    ): ItemStack {
        val book = ItemStack(Material.ENCHANTED_BOOK)
        val meta = book.itemMeta as EnchantmentStorageMeta

        if (originalBook.hasItemMeta()) {
            val originalMeta = originalBook.itemMeta
            if (originalMeta != null && originalMeta.hasCustomModelData()) {
                meta.setCustomModelData(originalMeta.customModelData)
            }
        }

        val tierColor = when (level) {
            1 -> "§a"
            2 -> "§b"
            3 -> "§d"
            else -> "§f"
        }

        val romanLevel = when (level) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> level.toString()
        }

        val bookTypePrefix = bookType?.let { "§7[${it.displayName}§7] " } ?: ""
        meta.setDisplayName("$bookTypePrefix$tierColor${enchantment.displayName} $romanLevel")

        val lore = mutableListOf<String>()
        lore.add("")

        if (bookType != null) {
            lore.add("§7Loại sách §e${bookType.displayName}")
            lore.add("§7Hợp lệ với:")
            bookType.compatibleItems.take(3).forEach { material ->
                lore.add("  §8- §7${material.name.lowercase().replace("_", " ")}")
            }
            if (bookType.compatibleItems.size > 3) {
                lore.add("  §8- §7và ${bookType.compatibleItems.size - 3} thêm...")
            }
        }

        lore.add("")
        lore.add("§7Đặt sách vào đe")
        lore.add("§7với trang bị hợp lệ để ép!")

        meta.lore = lore

        meta.addStoredEnchant(enchantment.bukkitEnchantment, level, true)
        meta.setEnchantmentGlintOverride(false)
        val container = meta.persistentDataContainer
        container.set(
            specializedBookManager.getEnchantmentIdKey(),
            PersistentDataType.STRING,
            enchantment.id
        )

        if (bookType != null) {
            container.set(
                specializedBookManager.getBookTypeKey(),
                PersistentDataType.STRING,
                bookType.id
            )
        }

        container.set(
            specializedBookManager.getGlintOverrideKey(),
            PersistentDataType.BYTE,
            1.toByte()
        )

        book.itemMeta = meta
        return book
    }

    /**
     * Creates a Protection Rune item.
     */
    fun createProtectionRune(): ItemStack {
        val runeItemConfig = runeConfig.protectionRuneItemConfig
        val rune = ItemStack(runeItemConfig.material)
        val meta = rune.itemMeta

        meta.setDisplayName(runeItemConfig.displayName)
        meta.lore = runeItemConfig.lore
        meta.setCustomModelData(runeItemConfig.customModelData)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        // Mark this item as a Protection Rune
        meta.persistentDataContainer.set(protectionRuneKey, PersistentDataType.BYTE, 1.toByte())
        meta.persistentDataContainer.set(NamespacedKey(plugin, "rune_type"), PersistentDataType.STRING, RuneType.PROTECTION_RUNE.id)

        rune.itemMeta = meta
        return rune
    }

    /**
     * Creates a Power Rune item.
     */
    fun createPowerRune(): ItemStack {
        val runeItemConfig = runeConfig.powerRuneItemConfig
        val rune = ItemStack(runeItemConfig.material)
        val meta = rune.itemMeta

        meta.setDisplayName(runeItemConfig.displayName)
        meta.lore = runeItemConfig.lore
        meta.setCustomModelData(runeItemConfig.customModelData)
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

        // Mark this item as a Power Rune
        meta.persistentDataContainer.set(powerRuneKey, PersistentDataType.BYTE, 1.toByte())
        meta.persistentDataContainer.set(NamespacedKey(plugin, "rune_type"), PersistentDataType.STRING, RuneType.POWER_RUNE.id)

        rune.itemMeta = meta
        return rune
    }

    /**
     * Check if an item can be enchanted with a specific enchantment
     */
    fun canEnchantItem(item: ItemStack, enchantment: CustomEnchantment): Boolean {
        if (!enchantment.compatibleItems.contains(item.type)) {
            return false
        }

        if (item.enchantments.containsKey(enchantment.bukkitEnchantment)) {
            return false
        }

        for (conflictingEnchant in enchantment.conflictsWith) {
            if (item.enchantments.containsKey(conflictingEnchant)) {
                return false
            }
        }

        return true
    }

    /**
     * Apply an enchanted book to an item
     */
    fun applyEnchantedBook(item: ItemStack, enchantedBook: ItemStack): Boolean {
        val bookMeta = enchantedBook.itemMeta as? EnchantmentStorageMeta ?: return false
        val storedEnchants = bookMeta.storedEnchants

        if (storedEnchants.isEmpty()) return false

        val enchantmentId = bookMeta.persistentDataContainer.get(
            specializedBookManager.getEnchantmentIdKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        ) ?: return false

        val customEnchantment = customEnchantmentManager.getEnchantmentById(enchantmentId) ?: return false

        if (!canEnchantItem(item, customEnchantment)) {
            return false
        }

        for ((enchant, level) in storedEnchants) {
            item.addUnsafeEnchantment(enchant, level)
        }

        return true
    }

    /**
     * Calculate the cost for enchanting based on tier and enchantment
     */
    fun calculateEnchantCost(enchantment: CustomEnchantment, tier: Int): Double {
        val baseCost = when (tier) {
            1 -> plugin.config.getDouble("enchanting.tier1.base-cost", 100.0)
            2 -> plugin.config.getDouble("enchanting.tier2.base-cost", 250.0)
            3 -> plugin.config.getDouble("enchanting.tier3.base-cost", 500.0)
            else -> 100.0
        }
        return baseCost * enchantment.costMultiplier
    }

    /**
     * Get the required level for a specific enchantment tier
     */
    fun getRequiredLevel(tier: Int): Int {
        return when (tier) {
            1 -> plugin.config.getInt("enchanting.tier1.required-level", 5)
            2 -> plugin.config.getInt("enchanting.tier2.required-level", 15)
            3 -> plugin.config.getInt("enchanting.tier3.required-level", 30)
            else -> 0
        }
    }

    /**
     * Check if a book is already enchanted
     */
    fun isBookEnchanted(book: ItemStack): Boolean {
        if (book.type != Material.ENCHANTED_BOOK) return false

        val meta = book.itemMeta as? EnchantmentStorageMeta ?: return false
        return meta.storedEnchants.isNotEmpty()
    }

    /**
     * Remove all enchantments from a book (for grindstone)
     */
    fun disenchantBook(enchantedBook: ItemStack): ItemStack? {
        if (!isBookEnchanted(enchantedBook)) return null

        val meta = enchantedBook.itemMeta as EnchantmentStorageMeta

        val bookTypeId = meta.persistentDataContainer.get(
            specializedBookManager.getBookTypeKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        )


        return if (bookTypeId != null) {
            val bookType = specializedBookManager.getBookTypeById(bookTypeId)
            bookType?.let { specializedBookManager.createSpecializedBook(it) }
        } else {
            ItemStack(Material.BOOK)
        }
    }
}
