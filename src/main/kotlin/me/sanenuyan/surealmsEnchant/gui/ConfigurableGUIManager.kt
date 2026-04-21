package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.gui.holders.EnchantingTableGUIHolder
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

class ConfigurableGUIManager(
    private val plugin: SurealmsEnchant
) : Listener {

    private val activeGUIs = mutableMapOf<Player, ConfigurableGUI>()

    fun registerGUI(player: Player, gui: ConfigurableGUI) {
        activeGUIs[player] = gui
    }

    fun unregisterGUI(player: Player) {
        activeGUIs.remove(player)
    }

    fun getActiveGUI(player: Player): ConfigurableGUI? {
        return activeGUIs[player]
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOW)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return
        val slot = event.slot
        val clickedItem = event.currentItem

        val gui = activeGUIs[player] ?: return

        if (EnchantingTableGUIHolder.isEnchantingTableGUI(inventory)) {
            return
        }

        if (inventory == gui.getGUIInventory()) {

            val shouldCancel = gui.shouldCancelClick(slot, clickedItem, player)

            event.isCancelled = shouldCancel

            gui.handleClick(slot, clickedItem, player)

            if (!shouldCancel) {
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    gui.updateAfterClick(slot, player)
                }, 1L)
            }
        } else if (event.view.topInventory == gui.getGUIInventory()) {
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val inventory = event.inventory

        val gui = activeGUIs[player] ?: return

        if (inventory != gui.getGUIInventory()) return

        unregisterGUI(player)

        if (gui is me.sanenuyan.surealmsEnchant.gui.ConfigurableEnchantingTableGUI) {
            gui.onClose()
        }

        gui.onClose()
    }
}

