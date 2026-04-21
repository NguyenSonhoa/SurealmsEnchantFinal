package me.sanenuyan.surealmsEnchant.api

import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap

/**
 * Integrated EnchantmentsAPI for SurealmsEnchant
 * Based on de.freesoccerhdx.enchantmentsapi.EnchantmentsAPI
 */
object EnchantmentsAPI {
    
    private val registeredEnchantments = ConcurrentHashMap<NamespacedKey, EnchantmentHolder>()
    private lateinit var plugin: JavaPlugin
    
    /**
     * Initialize the API with plugin instance
     */
    fun initialize(pluginInstance: JavaPlugin) {
        plugin = pluginInstance
    }
    
    /**
     * Create a new enchantment using EnchantmentBuilder
     * Note: This creates a data holder, not a real Bukkit enchantment
     * Real enchantment registration requires complex implementation for 1.21.5+
     */
    fun createNewEnchantment(builder: EnchantmentBuilder): EnchantmentHolder {
        val enchantmentData = builder.build()
        val holder = EnchantmentHolder(enchantmentData, builder)
        registeredEnchantments[builder.key] = holder

        plugin.logger.info("Registered custom enchantment data: ${builder.key}")

        return holder
    }
    
    /**
     * Get all registered enchantments
     */
    fun getRegisteredEnchantments(): Map<NamespacedKey, EnchantmentHolder> {
        return registeredEnchantments.toMap()
    }
    
    /**
     * Get enchantment holder by key
     */
    fun getEnchantmentHolder(key: NamespacedKey): EnchantmentHolder? {
        return registeredEnchantments[key]
    }
    
    /**
     * Check if enchantment is registered
     */
    fun isRegistered(key: NamespacedKey): Boolean {
        return registeredEnchantments.containsKey(key)
    }
    
    /**
     * Holder class for enchantments
     */
    class EnchantmentHolder(
        private val enchantmentData: EnchantmentBuilder.CustomEnchantmentData,
        private val builder: EnchantmentBuilder
    ) {

        fun getEnchantmentData(): EnchantmentBuilder.CustomEnchantmentData = enchantmentData
        fun getBuilder(): EnchantmentBuilder = builder
        fun getKey(): NamespacedKey = builder.key
        fun getName(): String = builder.name

        // For compatibility with existing code that expects Enchantment
        fun getEnchantment(): Enchantment? {
            // Try to find existing Bukkit enchantment by key
            return Enchantment.getByKey(builder.key)
        }
    }
}
