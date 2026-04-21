package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.listeners.EnchantingTableListenerSimple
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * Manager for EnchantingTable GUIs to maintain compatibility
 */
class EnchantingTableGUIManager(
    private val plugin: SurealmsEnchant,
    private val enchantmentSystem: EnchantmentSystem,
    private val specializedBookManager: SpecializedBookManager,
    private val customEnchantmentManager: CustomEnchantmentManager,
    private val listener: EnchantingTableListenerSimple
) {
    
    // Store active GUIs per player
    private val activeGUIs = mutableMapOf<Player, ConfigurableEnchantingTableGUI>()
    
    /**
     * Open enchanting table GUI for player
     */
    fun openGUI(player: Player) {
        val gui = ConfigurableEnchantingTableGUI(plugin, player, enchantmentSystem, specializedBookManager, customEnchantmentManager, listener)
        gui.initialize()
        gui.open()
        activeGUIs[player] = gui
    }
    
    /**
     * Check if inventory is an enchanting table GUI
     */
    fun isEnchantingTableGUI(inventory: Inventory): Boolean {
        val title = plugin.guiConfig.getTitle("enchanting_table")
        return inventory.holder == null && inventory.size == plugin.guiConfig.getSize("enchanting_table") && 
               inventory.viewers.any { viewer -> 
                   viewer is Player && activeGUIs.containsKey(viewer)
               }
    }
    
    /**
     * Get GUI for player
     */
    fun getGUI(player: Player): ConfigurableEnchantingTableGUI? {
        return activeGUIs[player]
    }
    
    /**
     * Remove GUI for player
     */
    fun removeGUI(player: Player) {
        activeGUIs.remove(player)
    }
    
    /**
     * Handle click in enchanting table GUI
     */
    fun handleClick(player: Player, slot: Int, item: org.bukkit.inventory.ItemStack?) {
        val gui = activeGUIs[player]
        gui?.handleClick(slot, item, player)
    }
}
