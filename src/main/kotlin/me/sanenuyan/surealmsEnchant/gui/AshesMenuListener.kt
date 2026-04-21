package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.managers.AshesItemManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryAction

import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.Sound

class AshesMenuListener(private val plugin: SurealmsEnchant) : Listener {

    private val ashesMenu = AshesMenu(plugin, plugin.config)
    private val ashesItemManager = AshesItemManager(plugin)

    fun openAshesMenu(player: Player) {
        ashesMenu.openMenu(player)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val eventTitleComponent: Component = event.view.title()
        val menuTitleComponent: Component = ashesMenu.titleComponent

        if (!eventTitleComponent.equals(menuTitleComponent)) return

        val clickedInventory = event.clickedInventory
        val playerInventory = event.whoClicked.inventory
        val topInventory = event.view.topInventory

        if (clickedInventory == topInventory) {
            event.isCancelled = true

            when (event.rawSlot) {
                in ashesMenu.inputSlots -> {

                    event.isCancelled = false
                }
                ashesMenu.outputSlot -> {

                    if (event.cursor != null && event.cursor.type != Material.AIR) {
                        event.isCancelled = true
                    } else if (event.currentItem != null && event.currentItem?.type != Material.AIR) {

                        event.isCancelled = false
                    } else {

                        event.isCancelled = true
                    }
                }
                ashesMenu.convertSlot -> {

                    plugin.server.scheduler.runTask(plugin, Runnable {
                        val inventory = event.view.topInventory
                        var totalAshesAmount = 0
                        val enchantedBooksToClear = mutableListOf<Int>()

                        for (slot in ashesMenu.inputSlots) {
                            val item = inventory.getItem(slot)

                            if (item != null && item.type == Material.ENCHANTED_BOOK) {
                                val meta = item.itemMeta as? EnchantmentStorageMeta

                                val enchantments = meta?.storedEnchants ?: emptyMap()
                                if (enchantments.isNotEmpty()) {

                                    totalAshesAmount += enchantments.size * item.amount
                                    enchantedBooksToClear.add(slot)

                                }
                            }
                        }

                        if (totalAshesAmount > 0) {
                            val outputItem = inventory.getItem(ashesMenu.outputSlot)

                            if (outputItem == null || ashesItemManager.isAshesItem(outputItem)) {

                                for (slot in enchantedBooksToClear) {
                                    inventory.setItem(slot, null)
                                }

                                val existingAmount = outputItem?.amount ?: 0
                                val newTotalAmount = existingAmount + totalAshesAmount

                                val ashes = ashesItemManager.createAshesItem(newTotalAmount)
                                inventory.setItem(ashesMenu.outputSlot, ashes)

                                val player = event.whoClicked as? Player
                                if (player != null) {
                                    try {
                                        val sound = Sound.valueOf(ashesMenu.soundName.uppercase())
                                        player.playSound(player.location, sound, ashesMenu.soundVolume, ashesMenu.soundPitch)
                                    } catch (e: IllegalArgumentException) {
                                        plugin.logger.warning("Invalid sound name configured: ${ashesMenu.soundName}")
                                    }
                                }
                            }
                        }
                    })

// Allow the convert button click

                }

            }
        }

        else if (clickedInventory == playerInventory) {

            if (event.isShiftClick) {

                event.isCancelled = true

                if (event.currentItem != null && event.currentItem?.type == Material.ENCHANTED_BOOK) {
                    for (slot in ashesMenu.inputSlots) {
                        if (topInventory.getItem(slot) == null || topInventory.getItem(slot)!!.type == Material.AIR) {
                            event.isCancelled = false

                            break

                        }
                    }
                }
            }

        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val eventTitleComponent: Component = event.view.title()
        val menuTitleComponent: Component = ashesMenu.titleComponent

        if (!eventTitleComponent.equals(menuTitleComponent)) return

        val player = event.player as Player
        val inventory = event.inventory

        val slotsToReturn = ashesMenu.inputSlots + ashesMenu.outputSlot

        for (slot in slotsToReturn) {
            val item = inventory.getItem(slot)
            if (item != null) {

                val leftover = player.inventory.addItem(item)

                leftover.values.forEach {
                    player.world.dropItem(player.location, it)
                }

                inventory.setItem(slot, null)
            }
        }
    }
}

