package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.enchantments.Enchantment

/**
 * Manages the discovery and handling of enchantments added via datapacks.
 */
class DatapackEnchantmentManager(private val plugin: SurealmsEnchant) {

    private val discoveredEnchantments = mutableSetOf<Enchantment>()

    /**
     * Scans for and registers all enchantments not in the default Minecraft/Bukkit namespaces.
     * This should be called after all plugins and datapacks are loaded.
     */
    fun discoverEnchantments() {
        discoveredEnchantments.clear()
        Enchantment.values().forEach { enchantment ->
            if (enchantment.key.namespace.lowercase() !in listOf("minecraft", "bukkit")) {
                discoveredEnchantments.add(enchantment)
                plugin.logger.info("Discovered datapack enchantment: ${enchantment.key}")
            }
        }
        plugin.logger.info("Found ${discoveredEnchantments.size} datapack enchantments.")
    }

    /**
     * Returns a list of all discovered datapack enchantments.
     */
    fun getDiscoveredEnchantments(): List<Enchantment> {
        return discoveredEnchantments.toList()
    }

    /**
     * Checks if a given enchantment is a custom one discovered from a datapack.
     */
    fun isDatapackEnchantment(enchantment: Enchantment): Boolean {
        return discoveredEnchantments.contains(enchantment)
    }
}
