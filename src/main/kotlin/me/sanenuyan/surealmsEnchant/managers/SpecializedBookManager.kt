package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.managers.MessageManager
import me.sanenuyan.surealmsEnchant.models.SpecializedBookType
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType

/**
 * Manages specialized books for different item types
 */
class SpecializedBookManager(private val plugin: SurealmsEnchant, private val messageManager: MessageManager) {
    
    private val bookTypeKey = NamespacedKey(plugin, "book_type")
    private val enchantmentIdKey = NamespacedKey(plugin, "enchantment_id")
    private val glintOverrideKey = NamespacedKey(plugin, "enchantment_glint_override")
    
    private val specializedBookTypes = mutableMapOf<String, SpecializedBookType>()
    
    init {
        initializeBookTypes()
    }
    
    /**
     * Initialize all specialized book types
     */
    private fun initializeBookTypes() {
        // Weapon Books
        registerBookType(SpecializedBookType(
            id = "sword_book",
            displayName = "<red>Sword Enchant Book",
            description = listOf(
                "<gray>A specialized book for sword enchantments",
                "<gray>Can only receive enchantments compatible with swords"
            ),
            compatibleItems = listOf(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
            ),
            customModelData = 1001,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "axe_book",
            displayName = "<gold>Axe Enchant Book",
            description = listOf(
                "<gray>A specialized book for axe enchantments",
                "<gray>Can only receive enchantments compatible with axes"
            ),
            compatibleItems = listOf(
                Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
            ),
            customModelData = 1002,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "pickaxe_book",
            displayName = "<dark_gray>Pickaxe Enchant Book",
            description = listOf(
                "<gray>A specialized book for pickaxe enchantments",
                "<gray>Can only receive enchantments compatible with pickaxes"
            ),
            compatibleItems = listOf(
                Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
            ),
            customModelData = 1003,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "shovel_book",
            displayName = "<gray>Shovel Enchant Book",
            description = listOf(
                "<gray>A specialized book for shovel enchantments",
                "<gray>Can only receive enchantments compatible with shovels"
            ),
            compatibleItems = listOf(
                Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
            ),
            customModelData = 1004,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "hoe_book",
            displayName = "<dark_green>Hoe Enchant Book",
            description = listOf(
                "<gray>A specialized book for hoe enchantments",
                "<gray>Can only receive enchantments compatible with hoes"
            ),
            compatibleItems = listOf(
                Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
            ),
            customModelData = 1005,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "bow_book",
            displayName = "<yellow>Bow Enchant Book",
            description = listOf(
                "<gray>A specialized book for bow enchantments",
                "<gray>Can only receive enchantments compatible with bows"
            ),
            compatibleItems = listOf(Material.BOW),
            customModelData = 1006,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "crossbow_book",
            displayName = "<blue>Crossbow Enchant Book",
            description = listOf(
                "<gray>A specialized book for crossbow enchantments",
                "<gray>Can only receive enchantments compatible with crossbows"
            ),
            compatibleItems = listOf(Material.CROSSBOW),
            customModelData = 1007,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "trident_book",
            displayName = "<aqua>Trident Enchant Book",
            description = listOf(
                "<gray>A specialized book for trident enchantments",
                "<gray>Can only receive enchantments compatible with tridents"
            ),
            compatibleItems = listOf(Material.TRIDENT),
            customModelData = 1008,
               enchantment_glint_override = false
        ))
        
        // Armor Books
        registerBookType(SpecializedBookType(
            id = "helmet_book",
            displayName = "<white>Helmet Enchant Book",
            description = listOf(
                "<gray>A specialized book for helmet enchantments",
                "<gray>Can only receive enchantments compatible with helmets"
            ),
            compatibleItems = listOf(
                Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET,
                Material.TURTLE_HELMET
            ),
            customModelData = 1009,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "chestplate_book",
            displayName = "<white>Chestplate Enchant Book",
            description = listOf(
                "<gray>A specialized book for chestplate enchantments",
                "<gray>Can only receive enchantments compatible with chestplates"
            ),
            compatibleItems = listOf(
                Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE
            ),
            customModelData = 1010,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "leggings_book",
            displayName = "<white>Leggings Enchant Book",
            description = listOf(
                "<gray>A specialized book for leggings enchantments",
                "<gray>Can only receive enchantments compatible with leggings"
            ),
            compatibleItems = listOf(
                Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
            ),
            customModelData = 1011,
               enchantment_glint_override = false
        ))
        
        registerBookType(SpecializedBookType(
            id = "boots_book",
            displayName = "<white>Boots Enchant Book",
            description = listOf(
                "<gray>A specialized book for boots enchantments",
                "<gray>Can only receive enchantments compatible with boots"
            ),
            compatibleItems = listOf(
                Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
            ),
            customModelData = 1012,
               enchantment_glint_override = false
        ))

        registerBookType(SpecializedBookType(
            id = "fishing_rod_book",
            displayName = "<aqua>Fishing Rod Enchant Book",
            description = listOf(
                "<gray>A specialized book for fishing rod enchantments",
                "<gray>Can only receive enchantments compatible with fishing rods"
            ),
            compatibleItems = listOf(Material.FISHING_ROD),
            customModelData = 1013,
               enchantment_glint_override = false
        ))

        registerBookType(SpecializedBookType(
            id = "tome_of_renewal",
            displayName = "<light_purple>Tome of Renewal",
            description = listOf(
                "<gray>A special tome that can be used to",
                "<gray>renew enchantments on an item."
            ),
            compatibleItems = listOf(), // Not compatible with any specific item, it's a special item
            customModelData = 1014,
            enchantment_glint_override = true
        ))
    }
    
    /**
     * Register a new specialized book type
     */
    fun registerBookType(bookType: SpecializedBookType) {
        specializedBookTypes[bookType.id] = bookType
    }
    
    /**
     * Get all registered book types
     */
    fun getAllBookTypes(): Collection<SpecializedBookType> = specializedBookTypes.values
    
    /**
     * Get a book type by ID
     */
    fun getBookTypeById(id: String): SpecializedBookType? = specializedBookTypes[id]
    
    /**
     * Get book type from an item
     */
    fun getBookTypeFromItem(item: ItemStack?): SpecializedBookType? {
        if (item?.type != Material.BOOK && item?.type != Material.ENCHANTED_BOOK) return null

        val meta = item?.itemMeta ?: return null
        val bookTypeId = meta.persistentDataContainer.get(bookTypeKey, PersistentDataType.STRING)

        return bookTypeId?.let { getBookTypeById(it) }
    }
    
    /**
     * Create a specialized book item
     */
    fun createSpecializedBook(bookType: SpecializedBookType): ItemStack {
        val config = plugin.config
        val typeKey = bookType.id

        // Get item configuration from config, fallback to bookType properties
        val displayName = config.getString("specialized_books.items.$typeKey.display_name", bookType.displayName)
        val configLore = config.getStringList("specialized_books.items.$typeKey.lore")
        val customModelData = config.getInt("specialized_books.items.$typeKey.custom_model_data", bookType.customModelData ?: 0)
        val enchantmentGlintOverride = config.getBoolean("specialized_books.items.$typeKey.enchantment_glint_override", bookType.enchantment_glint_override ?: false)

        val book = ItemStack(if (enchantmentGlintOverride) Material.ENCHANTED_BOOK else Material.BOOK)
        val meta = book.itemMeta!!

        // Set display name with MiniMessage
        if (displayName != null) {
            meta.displayName(ChatUtils.parse(displayName))
        }

        // Set lore - use config lore if available, otherwise use default
        val loreComponents = if (configLore.isNotEmpty()) {
            configLore.map { ChatUtils.parse(it) }
        } else {
            buildList {
                addAll(bookType.description.map { ChatUtils.parse(it) })
                add(Component.text(""))
                add(messageManager.getMessage("gui.specialized_book.compatible_with_header"))
                bookType.compatibleItems.take(5).forEach { material ->
                    val materialName = material.name.lowercase().replace("_", " ")
                    add(messageManager.getMessage("gui.specialized_book.compatible_with_item", mapOf("material" to materialName)))
                }
                if (bookType.compatibleItems.size > 5) {
                    val remaining = bookType.compatibleItems.size - 5
                    add(messageManager.getMessage("gui.specialized_book.compatible_with_more", mapOf("count" to remaining)))
                }
                add(Component.text(""))
                add(messageManager.getMessage("gui.specialized_book.usage_line_1"))
                add(messageManager.getMessage("gui.specialized_book.usage_line_2"))
            }
        }
        meta.lore(loreComponents)

        // Set custom model data if specified
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData)
        }

        // Add NBT data
        meta.persistentDataContainer.set(bookTypeKey, PersistentDataType.STRING, bookType.id)
        
        if (meta is EnchantmentStorageMeta) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            meta.setEnchantmentGlintOverride(false)
        }

        book.itemMeta = meta
        return book
    }

    /**
     * Reload specialized book configurations
     */
    fun reloadConfigurations() {
        plugin.logger.info("Reloading specialized book configurations...")
        // Configuration is loaded dynamically from plugin.config, so no action needed
        plugin.logger.info("Specialized book configurations reloaded")
    }

    /**
     * Check if an item is a specialized book
     */
    fun isSpecializedBook(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(bookTypeKey, PersistentDataType.STRING)
    }
    
    /**
     * Get compatible book types for a specific item
     */
    fun getCompatibleBookTypes(item: Material): List<SpecializedBookType> {
        return specializedBookTypes.values.filter { bookType ->
            bookType.compatibleItems.contains(item)
        }
    }
    
    /**
     * Get the book type key for NBT storage
     */
    fun getBookTypeKey(): NamespacedKey = bookTypeKey
    
    /**
     * Get the enchantment ID key for NBT storage
     */
    fun getEnchantmentIdKey(): NamespacedKey = enchantmentIdKey

    /**
     * Get the enchantment glint override key for NBT storage
     */
    fun getGlintOverrideKey(): NamespacedKey = glintOverrideKey
}
