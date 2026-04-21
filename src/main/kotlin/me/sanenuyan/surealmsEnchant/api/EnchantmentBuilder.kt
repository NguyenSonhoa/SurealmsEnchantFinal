package me.sanenuyan.surealmsEnchant.api

import org.bukkit.NamespacedKey
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.enchantments.Enchantment
import org.bukkit.enchantments.EnchantmentTarget
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemStack

/**
 * Builder for creating custom enchantments
 * Based on de.freesoccerhdx.enchantmentsapi.EnchantmentBuilder
 */
class EnchantmentBuilder(
    val key: NamespacedKey,
    val name: String
) {
    
    private var weight: Int = 1
    private var maxLevel: Int = 1
    private var anvilCost: Int = 1
    private var minCost: Cost? = null
    private var maxCost: Cost? = null
    private var supportedItems: MutableSet<Material> = mutableSetOf()
    private var supportedTags: MutableSet<Tag<Material>> = mutableSetOf()
    private var equipmentSlotGroup: EquipmentSlotGroup = EquipmentSlotGroup.MAINHAND
    private var description: String = ""
    private var exclusiveWith: MutableSet<Enchantment> = mutableSetOf()
    
    /**
     * Set the weight (rarity) of the enchantment
     * Higher weight = more common
     */
    fun weight(weight: Int): EnchantmentBuilder {
        this.weight = weight
        return this
    }
    
    /**
     * Set the maximum level of the enchantment
     */
    fun maxLevel(maxLevel: Int): EnchantmentBuilder {
        this.maxLevel = maxLevel
        return this
    }
    
    /**
     * Set the anvil cost multiplier
     */
    fun anvilCost(anvilCost: Int): EnchantmentBuilder {
        this.anvilCost = anvilCost
        return this
    }
    
    /**
     * Set the minimum cost for enchanting table
     */
    fun minCost(cost: Cost): EnchantmentBuilder {
        this.minCost = cost
        return this
    }
    
    /**
     * Set the maximum cost for enchanting table
     */
    fun maxCost(cost: Cost): EnchantmentBuilder {
        this.maxCost = cost
        return this
    }
    
    /**
     * Add supported material
     */
    fun supportedItem(material: Material): EnchantmentBuilder {
        this.supportedItems.add(material)
        return this
    }
    
    /**
     * Add supported tag
     */
    fun supportedItem(tag: Tag<Material>): EnchantmentBuilder {
        this.supportedTags.add(tag)
        return this
    }
    
    /**
     * Set equipment slot group
     */
    fun equipmentSlotGroup(slotGroup: EquipmentSlotGroup): EnchantmentBuilder {
        this.equipmentSlotGroup = slotGroup
        return this
    }
    
    /**
     * Set description
     */
    fun description(description: String): EnchantmentBuilder {
        this.description = description
        return this
    }
    
    /**
     * Add exclusive enchantment
     */
    fun exclusiveWith(enchantment: Enchantment): EnchantmentBuilder {
        this.exclusiveWith.add(enchantment)
        return this
    }
    
    /**
     * Build the enchantment data
     */
    fun build(): CustomEnchantmentData {
        return CustomEnchantmentData(
            key = key,
            name = name,
            weight = weight,
            maxLevel = maxLevel,
            anvilCost = anvilCost,
            minCost = minCost ?: Cost(1, 0),
            maxCost = maxCost ?: Cost(30, 0),
            supportedItems = supportedItems,
            supportedTags = supportedTags,
            equipmentSlotGroup = equipmentSlotGroup,
            description = description,
            exclusiveWith = exclusiveWith
        )
    }
    
    /**
     * Cost class for enchanting table costs
     */
    class Cost(
        val base: Int,
        val perLevel: Int
    ) {
        fun getCost(level: Int): Int {
            return base + (perLevel * level)
        }
    }
    
    /**
     * Custom enchantment data holder
     * Stores enchantment properties without extending Enchantment class
     */
    data class CustomEnchantmentData(
        val key: NamespacedKey,
        val name: String,
        val weight: Int,
        val maxLevel: Int,
        val anvilCost: Int,
        val minCost: Cost,
        val maxCost: Cost,
        val supportedItems: Set<Material>,
        val supportedTags: Set<Tag<Material>>,
        val equipmentSlotGroup: EquipmentSlotGroup,
        val description: String,
        val exclusiveWith: Set<Enchantment>
    ) {

        fun getMinCost(level: Int): Int = minCost.getCost(level)
        fun getMaxCost(level: Int): Int = maxCost.getCost(level)

        fun canEnchantItem(item: ItemStack): Boolean {
            val material = item.type

            // Check direct material support
            if (supportedItems.contains(material)) {
                return true
            }

            // Check tag support
            for (tag in supportedTags) {
                if (tag.isTagged(material)) {
                    return true
                }
            }

            return false
        }

        fun isTreasure(): Boolean = weight <= 1

        fun translationKey(): String {
            return "enchantment.${key.namespace}.${key.key}"
        }
    }
}
