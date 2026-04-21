package me.sanenuyan.surealmsEnchant.models

import org.bukkit.Material

/**
 * Represents a category of items that a rune can be applied to.
 * @param displayName The name to be displayed in the rune's lore.
 * @param materials The set of materials belonging to this category.
 */
enum class ItemCategory(val displayName: String, private val materials: Set<Material>) {
    SWORDS("Swords", setOf(
        Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
        Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
    )),
    ARMOR("Armor", Material.values().filter {
        val name = it.name
        (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS"))
            && !name.startsWith("LEATHER_") // Exclude leather if you want
    }.toSet()),
    TOOLS("Tools", setOf(
        Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
        Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
        Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
        Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
        Material.SHEARS, Material.FISHING_ROD, Material.BOW, Material.CROSSBOW, Material.TRIDENT
    ));

    /**
     * Checks if a given material belongs to this category.
     */
    fun matches(material: Material): Boolean {
        return materials.contains(material)
    }

    companion object {
        /**
         * Finds the category for a given material.
         */
        fun fromMaterial(material: Material): ItemCategory? {
            return values().find { it.matches(material) }
        }
    }
}