package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.gui.ConfigurableEnchantIndexGUI
import me.sanenuyan.surealmsEnchant.gui.ConfigurableEnchantingTableGUI
import me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUI
import me.sanenuyan.surealmsEnchant.gui.holders.EnchantingTableGUIHolder
import me.sanenuyan.surealmsEnchant.models.*
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.logging.Level

class EnchantingTableListenerSimple(
    private val plugin: SurealmsEnchant
) : Listener {

    lateinit var enchantingTableGUI: EnchantingTableGUI
        private set

    fun setGUI(gui: EnchantingTableGUI) {
        this.enchantingTableGUI = gui
    }

    val playerOfferedEnchantments = mutableMapOf<UUID, Map<Int, CustomEnchantment>>()

    private fun isTomeOfRenewal(item: ItemStack): Boolean {
        val config = plugin.config.getConfigurationSection("tome-of-renewal") ?: return false
        if (item.type.name != config.getString("material", "BOOK")) {
            return false
        }
        val meta = item.itemMeta ?: return false
        if (!meta.hasCustomModelData()) {
            return false
        }
        if (meta.customModelData != config.getInt("custom-model-data", 0)) {
            return false
        }
        return true
    }
    fun createTomeOfRenewal(amount: Int): ItemStack {
        val config = plugin.config.getConfigurationSection("tome-of-renewal")
        val materialName = config?.getString("material", "BOOK") ?: "BOOK"
        val material = Material.getMaterial(materialName) ?: Material.BOOK
        val name = ChatUtils.parse(config?.getString("name", "<#ff00ff>Tome of Renewal") ?: "")
        val lore = config?.getStringList("lore")?.map { ChatUtils.parse(it) } ?: emptyList()
        val customModelData = config?.getInt("custom-model-data", 0) ?: 0

        val item = ItemStack(material, amount)
        val meta = item.itemMeta
        meta?.displayName(name)
        meta?.lore(lore)
        if (customModelData > 0) {
            meta?.setCustomModelData(customModelData)
        }
        item.itemMeta = meta
        return item
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action == Action.RIGHT_CLICK_BLOCK && event.clickedBlock?.type == Material.ENCHANTING_TABLE) {
            val player = event.player
            if (!player.hasPermission("surealms.enchant.table")) {
                event.isCancelled = true
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.no_permission_use"))
                return
            }
            event.isCancelled = true
            val gui = ConfigurableEnchantingTableGUI(
                plugin,
                player,
                plugin.enchantmentSystem,
                plugin.specializedBookManager,
                plugin.customEnchantmentManager,
                this
            )
            gui.initialize()
            gui.open()
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.clickedInventory ?: return
        val slot = event.slot

        if (inventory.holder is Player) {
            if (EnchantingTableGUIHolder.isEnchantingTableGUI(event.view.topInventory)) {
                handleShiftClickFromPlayerInventory(event, player)
            }
            return
        }

        if (EnchantingTableGUIHolder.isEnchantingTableGUI(inventory)) {
            val inputSlot = enchantingTableGUI.getInputSlot()
            val enchantIndexSlot = plugin.guiConfig.getItemConfig("enchanting_table", "enchant_index_button")?.slot ?: 40
            val closeButtonSlot = plugin.guiConfig.getItemConfig("enchanting_table", "close_button")?.slot ?: 44

            val tier1Slots = plugin.guiConfig.getSlots("enchanting_table", "tier_1_slots")
            val tier2Slots = plugin.guiConfig.getSlots("enchanting_table", "tier_2_slots")
            val tier3Slots = plugin.guiConfig.getSlots("enchanting_table", "tier_3_slots")

            when (slot) {
                inputSlot -> {
                    handleBookSlotClick(event, player)
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        val activeGUI = plugin.configurableGUIManager.getActiveGUI(player)
                        if (activeGUI is ConfigurableEnchantingTableGUI) {
                            activeGUI.safeRefresh()
                        }
                    }, 1L)
                }
                enchantIndexSlot -> {
                    event.isCancelled = true
                    handleEnchantIndexClick(player)
                }
                closeButtonSlot -> {
                    event.isCancelled = true
                    player.closeInventory()
                }
                in tier1Slots, in tier2Slots, in tier3Slots -> {
                    event.isCancelled = true
                    handleEnchantmentOptionClick(event, player, slot)
                }
                else -> {
                    event.isCancelled = true
                }
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val inventory = event.inventory

        if (!EnchantingTableGUIHolder.isEnchantingTableGUI(inventory)) return

        val inputSlot = enchantingTableGUI.getInputSlot()
        val bookItem = inventory.getItem(inputSlot)

        if (bookItem != null && bookItem.type != Material.AIR) {
            val leftover = player.inventory.addItem(bookItem)
            if (leftover.isNotEmpty()) {
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            }
        }
        inventory.setItem(inputSlot, null)
    }

    private fun handleBookSlotClick(event: InventoryClickEvent, player: Player) {
        val cursorItem = event.cursor
        val currentItem = event.currentItem

        if (cursorItem != null && isTomeOfRenewal(cursorItem)) {
            event.isCancelled = true
            if (currentItem == null || currentItem.type == Material.AIR) {
                playerOfferedEnchantments.remove(player.uniqueId)
                player.sendMessage(ChatUtils.parse(plugin.config.getString("tome-of-renewal.success-message", "<green>Your enchantments have been renewed!</green>")))
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f)
                cursorItem.amount--
            }
            return
        }

        val isPlacingBook = cursorItem != null && cursorItem.type != Material.AIR

        if (isPlacingBook) {
            if (cursorItem!!.amount > 1) {
                event.isCancelled = true
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.too_many_items"))
                return
            }
            val isSpecialized = plugin.specializedBookManager.getBookTypeFromItem(cursorItem) != null
            val isVanillaBook = cursorItem.type == Material.BOOK

            if (!isSpecialized && !isVanillaBook) {
                event.isCancelled = true
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.invalid_item"))
                return
            }
        }
        event.isCancelled = false
    }

    private fun handleShiftClickFromPlayerInventory(event: InventoryClickEvent, player: Player) {
        if (event.action != org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY) return

        val clickedItem = event.currentItem ?: return
        val inputSlot = enchantingTableGUI.getInputSlot()
        val topInventory = event.view.topInventory
        val currentInputItem = topInventory.getItem(inputSlot)

        event.isCancelled = true

        if (isTomeOfRenewal(clickedItem)) {
            if (currentInputItem == null || currentInputItem.type == Material.AIR) {
                playerOfferedEnchantments.remove(player.uniqueId)
                player.sendMessage(ChatUtils.parse(plugin.config.getString("tome-of-renewal.success-message", "<green>Your enchantments have been renewed!</green>")))
                player.playSound(player.location, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f)
                clickedItem.amount--
                val activeGUI = plugin.configurableGUIManager.getActiveGUI(player)
                if (activeGUI is ConfigurableEnchantingTableGUI) {
                    activeGUI.safeRefresh()
                }
            } else {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.input_slot_occupied"))
            }
            return
        }

        val isSpecialized = plugin.specializedBookManager.getBookTypeFromItem(clickedItem) != null
        val isVanillaBook = clickedItem.type == Material.BOOK

        if (isSpecialized || isVanillaBook) {
            if (currentInputItem == null || currentInputItem.type == Material.AIR) {
                if (clickedItem.amount == 1) {
                    topInventory.setItem(inputSlot, clickedItem.clone())
                    event.currentItem = null
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        val activeGUI = plugin.configurableGUIManager.getActiveGUI(player)
                        if (activeGUI is ConfigurableEnchantingTableGUI) {
                            activeGUI.safeRefresh()
                        }
                    }, 1L)
                } else {
                    player.sendMessage(plugin.messageManager.getMessage("enchanting.error.too_many_items"))
                }
            } else {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.input_slot_occupied"))
            }
        } else {
            player.sendMessage(plugin.messageManager.getMessage("enchanting.error.only_books_allowed"))
        }
    }

    private fun handleEnchantIndexClick(player: Player) {
        player.closeInventory()
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            try {
                val enchantIndexGUI = ConfigurableEnchantIndexGUI(plugin, player, plugin.customEnchantmentManager)
                enchantIndexGUI.initialize()
                enchantIndexGUI.open()
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error opening enchant index GUI", e)
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.gui_open_fail"))
            }
        }, 2L)
    }

    private fun handleEnchantmentOptionClick(event: InventoryClickEvent, player: Player, slot: Int) {
        val inputSlot = enchantingTableGUI.getInputSlot()
        val bookItem = event.inventory.getItem(inputSlot)

        if (bookItem == null || bookItem.type == Material.AIR) {
            player.sendMessage(plugin.messageManager.getMessage("enchanting.error.no_book_in_slot"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return
        }

        if (!isValidBookForEnchanting(bookItem)) {
            player.sendMessage(plugin.messageManager.getMessage("enchanting.error.invalid_item"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return
        }

        val optionItem = event.currentItem
        if (optionItem == null || optionItem.type == Material.AIR) {
            return
        }

        val result = processEnchantment(player, bookItem, optionItem, slot)
        handleEnchantingResult(result, player, event.inventory)
    }

    private fun processEnchantment(player: Player, bookItem: ItemStack, optionItem: ItemStack, slot: Int): EnchantingResult {
        val enchantmentInfo = parseEnchantmentFromOption(optionItem, player)
            ?: return EnchantingResult.Failure("enchanting.error.option_parse_fail")

        val (enchantment, tier, cost) = enchantmentInfo

        if (player.level < tier.requiredLevel) {
            return EnchantingResult.InsufficientLevel(tier.requiredLevel, player.level)
        }

        if (!plugin.vaultIntegration.hasEnough(player, cost)) {
            return EnchantingResult.InsufficientFunds(cost, plugin.vaultIntegration.getBalance(player))
        }

        if (plugin.enchantmentSystem.isBookEnchanted(bookItem)) {
            return EnchantingResult.BookAlreadyEnchanted
        }

        val successMin = plugin.config.getDouble("enchanting.success_rate_range.min", 75.0)
        val successMax = plugin.config.getDouble("enchanting.success_rate_range.max", 100.0)
        val successChance = ThreadLocalRandom.current().nextDouble(successMin, successMax)

        if (!plugin.vaultIntegration.withdrawPlayer(player, cost)) {
            return EnchantingResult.Failure("enchanting.error.withdraw_fail")
        }
        player.level -= tier.requiredLevel

        val finalLevel = if (enchantment.level >= enchantment.maxLevel) {
            enchantment.level
        } else {
            (enchantment.level..enchantment.maxLevel).random()
        }

        val finalEnchantment = enchantment.copy(level = finalLevel, tier = finalLevel)

        val anvilFailureChance = 100.0 - successChance

        return EnchantingResult.Success(finalEnchantment, successChance, anvilFailureChance)
    }

    private fun parseEnchantmentFromOption(optionItem: ItemStack, player: Player): Triple<CustomEnchantment, EnchantTier, Double>? {
        val meta = optionItem.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val key = NamespacedKey(plugin, "enchantment_id")
        val enchantId = container.get(key, PersistentDataType.STRING) ?: return null

        val enchantment = plugin.customEnchantmentManager.getEnchantmentById(enchantId)
            ?: plugin.enchantmentSystem.getVanillaAsCustom(enchantId)
            ?: return null

        val tier = plugin.enchantmentSystem.getAvailableTiers(player).find { it.tier == enchantment.tier } ?: return null
        val cost = plugin.enchantmentSystem.calculateEnchantCost(enchantment, tier.tier)

        return Triple(enchantment, tier, cost)
    }

    private fun handleEnchantingResult(result: EnchantingResult, player: Player, inventory: org.bukkit.inventory.Inventory) {
        val inputSlot = enchantingTableGUI.getInputSlot()
        val bookItem = inventory.getItem(inputSlot)

        when (result) {
            is EnchantingResult.Success -> {
                val enchantedBook = plugin.enchantmentSystem.createEnchantedBook(bookItem!!.clone(), result.enchantment, result.enchantment.level)
                val meta = enchantedBook.itemMeta

                if (meta != null) {

                    val anvilFailureKey = NamespacedKey(plugin, "anvil_failure_chance")
                    meta.persistentDataContainer.set(anvilFailureKey, PersistentDataType.DOUBLE, result.anvilFailureChance)

                    val successKey = NamespacedKey(plugin, "success_chance")
                    meta.persistentDataContainer.set(successKey, PersistentDataType.DOUBLE, result.chance)

                    val loreConfig = plugin.config.getConfigurationSection("enchanting.result_lore.success")
                    if (loreConfig?.getBoolean("enabled", true) == true) {
                        val lore = loreConfig.getStringList("lines").map {
                            ChatUtils.parse(it
                                .replace("{chance}", "%.2f".format(result.chance))
                                .replace("{anvil_chance}", "%.2f".format(result.anvilFailureChance))
                            )
                        }
                        val newLore = meta.lore() ?: mutableListOf()
                        newLore.addAll(lore)
                        meta.lore(newLore)
                    }
                }

                enchantedBook.itemMeta = meta

                if (bookItem.amount > 1) {
                    bookItem.amount -= 1
                    inventory.setItem(inputSlot, bookItem)
                    val leftover = player.inventory.addItem(enchantedBook)
                    if (leftover.isNotEmpty()) {
                        leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                    }
                } else {
                    inventory.setItem(inputSlot, enchantedBook)
                }

                playerOfferedEnchantments.remove(player.uniqueId)
                player.sendMessage(plugin.messageManager.getMessage("enchanting.success", mapOf("enchantment" to result.enchantment.displayName, "tier" to result.enchantment.tier.toString())))
                player.playSound(player.location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f)
            }
            is EnchantingResult.Destruction -> {
                inventory.setItem(inputSlot, null)
                val loreConfig = plugin.config.getConfigurationSection("enchanting.result_lore.destruction")
                if (loreConfig?.getBoolean("enabled", true) == true) {
                    val message = loreConfig.getStringList("lines").joinToString("")
                    player.sendMessage(ChatUtils.parse(message))
                }
                player.playSound(player.location, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f)
            }
            is EnchantingResult.InsufficientFunds -> {
                val required = plugin.vaultIntegration.format(result.required)
                val available = plugin.vaultIntegration.format(result.available)
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.insufficient_funds", mapOf("required" to required, "available" to available)))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            is EnchantingResult.InsufficientLevel -> {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.insufficient_levels", mapOf("required" to result.required.toString(), "current" to result.current.toString())))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            is EnchantingResult.BookAlreadyEnchanted -> {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.already_enchanted"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            is EnchantingResult.Failure -> {
                player.sendMessage(plugin.messageManager.getMessage(result.reason))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            is EnchantingResult.InvalidBook -> {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.invalid_item"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
            is EnchantingResult.NoEnchantmentSelected -> {
                player.sendMessage(plugin.messageManager.getMessage("enchanting.error.option_parse_fail"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
        }

        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val activeGUI = plugin.configurableGUIManager.getActiveGUI(player)
            if (activeGUI is ConfigurableEnchantingTableGUI) {
                activeGUI.safeRefresh()
            }
        }, 1L)
    }

    private fun isValidBookForEnchanting(item: ItemStack): Boolean {
        return plugin.specializedBookManager.getBookTypeFromItem(item) != null || item.type == Material.BOOK
    }
}

