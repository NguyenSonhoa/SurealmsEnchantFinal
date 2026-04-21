package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.managers.RuneManager
import me.sanenuyan.surealmsEnchant.models.RuneItem
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import kotlin.random.Random

class RuneApplicationGUI( //dont use it, i have replace it in anvil like origin realms
    plugin: SurealmsEnchant,
    player: Player,
    private val runeManager: RuneManager
) : ConfigurableGUI(plugin, "rune_application", player) {

    private val itemSlot: Int get() = guiConfig.getItemConfig(guiType, "item_slot")?.slot ?: 20
    private val runeSlot: Int get() = guiConfig.getItemConfig(guiType, "rune_slot")?.slot ?: 24
    private val applyButtonSlot: Int get() = guiConfig.getItemConfig(guiType, "apply_button")?.slot ?: 22
    private val previewSlot: Int get() = guiConfig.getItemConfig(guiType, "preview_slot")?.slot ?: 31
    private val successIndicatorSlot: Int get() = guiConfig.getItemConfig(guiType, "success_indicator")?.slot ?: 29
    private val costIndicatorSlot: Int get() = guiConfig.getItemConfig(guiType, "cost_indicator")?.slot ?: 33

    override fun setupStaticItems() {

        setItem(guiConfig.getItemConfig(guiType, "help_button")?.slot ?: 49,
            createItem("help_button"))

        setItem(guiConfig.getItemConfig(guiType, "back_button")?.slot ?: 45,
            createItem("back_button"))
    }

    override fun setupDynamicContent() {
        updateApplicationInterface()
    }

    override fun shouldCancelClick(slot: Int, item: ItemStack?, player: Player): Boolean {

        return when (slot) {
            itemSlot, runeSlot -> false

            else -> true

        }
    }

    override fun updateAfterClick(slot: Int, player: Player) {

        if (slot == itemSlot || slot == runeSlot) {
            updateApplicationInterface()
        }
    }

    override fun shouldBlockShiftClick(item: ItemStack?, player: Player): Boolean {
        if (item == null) return true

        return when {
            plugin.runeManager.isRune(item) -> {

                val currentRune = inventory.getItem(runeSlot)
                currentRune != null && currentRune.type != Material.AIR
            }

            item.type.name.contains("SWORD") ||
                    item.type.name.contains("AXE") ||
                    item.type.name.contains("PICKAXE") ||
                    item.type.name.contains("SHOVEL") ||
                    item.type.name.contains("HOE") ||
                    item.type.name.contains("HELMET") ||
                    item.type.name.contains("CHESTPLATE") ||
                    item.type.name.contains("LEGGINGS") ||
                    item.type.name.contains("BOOTS") ||
                    item.type.name.contains("BOW") ||
                    item.type.name.contains("CROSSBOW") ||
                    item.type.name.contains("TRIDENT") -> {

                val currentItem = inventory.getItem(itemSlot)
                currentItem != null && currentItem.type != Material.AIR
            }
            else -> true

        }
    }

    private fun updateApplicationInterface() {
        val targetItem = inventory.getItem(itemSlot)
        val runeItem = inventory.getItem(runeSlot)

        if (targetItem != null && runeItem != null) {
            val rune = runeManager.getRuneFromItem(runeItem)
            if (rune != null) {
                updateWithValidCombination(targetItem, rune)
            } else {
                clearApplicationInterface()
            }
        } else {
            clearApplicationInterface()
        }
    }

    private fun updateWithValidCombination(targetItem: ItemStack, rune: RuneItem) {
        val successRate = calculateSuccessRate(targetItem, rune)
        val cost = calculateApplicationCost(targetItem, rune)

        val applyPlaceholders = mapOf(
            "success_rate" to successRate.toString(),
            "cost" to plugin.vaultIntegration.format(cost)
        )
        setItem(applyButtonSlot, createItem("apply_button", applyPlaceholders))

        val successPlaceholders = mapOf("success_rate" to successRate.toString())
        setItem(successIndicatorSlot, createItem("success_indicator", successPlaceholders))

        val costPlaceholders = mapOf("cost" to plugin.vaultIntegration.format(cost))
        setItem(costIndicatorSlot, createItem("cost_indicator", costPlaceholders))

        val previewItem = createPreviewItem(targetItem, rune)
        setItem(previewSlot, previewItem)
    }

    private fun clearApplicationInterface() {
        inventory.setItem(applyButtonSlot, null)
        inventory.setItem(previewSlot, null)
        inventory.setItem(successIndicatorSlot, null)
        inventory.setItem(costIndicatorSlot, null)
    }

    private fun calculateSuccessRate(targetItem: ItemStack, rune: RuneItem): Int {
        var baseRate = 75

        val meta = targetItem.itemMeta
        if (meta is Damageable) {
            val durabilityPercent = ((targetItem.type.maxDurability - meta.damage).toDouble() / targetItem.type.maxDurability) * 100
            baseRate += (durabilityPercent * 0.2).toInt()

        }

        when (rune.id) {
            "power_rune" -> baseRate -= 10

            "protection_rune" -> baseRate += 5

        }

        val playerLevel = player.level
        baseRate += (playerLevel / 10)

        return baseRate.coerceIn(10, 95)

    }

    private fun calculateApplicationCost(targetItem: ItemStack, rune: RuneItem): Double {
        var baseCost = 500.0

        when (rune.id) {
            "power_rune" -> baseCost = 1000.0
            "protection_rune" -> baseCost = 2000.0
        }

        when (targetItem.type.name) {
            "NETHERITE_SWORD", "NETHERITE_PICKAXE", "NETHERITE_AXE" -> baseCost *= 2.0
            "DIAMOND_SWORD", "DIAMOND_PICKAXE", "DIAMOND_AXE" -> baseCost *= 1.5
        }

        return baseCost
    }

    private fun createPreviewItem(targetItem: ItemStack, rune: RuneItem): ItemStack {
        val preview = targetItem.clone()
        val meta = preview.itemMeta!!

        val currentLore = meta.lore ?: mutableListOf()
        val newLore = currentLore.toMutableList()

        newLore.add("")
        newLore.add("§d§lRune Applied: §e${rune.displayName}")
        newLore.addAll(rune.description.map { "§7$it" })
        newLore.add("")
        newLore.add("§7This is a preview of the result")

        meta.lore = newLore
        preview.itemMeta = meta

        return preview
    }

    override fun handleClick(slot: Int, item: ItemStack?, player: Player) {
        plugin.logger.info("RuneApplicationGUI click: slot=$slot, item=${item?.type}, nbtId=${item?.let { getItemNBTId(it) }}")

        when (slot) {
            itemSlot -> {

                plugin.logger.info("Item slot clicked - allowing placement/removal")
                return
            }
            runeSlot -> {

                plugin.logger.info("Rune slot clicked - allowing placement/removal")
                return
            }
        }

        if (item == null) return

        val nbtId = getItemNBTId(item)

        when (nbtId) {
            "apply_button" -> {
                handleRuneApplication(player)
            }
            "help_button" -> {
                showRuneHelp(player)
            }
            "back_button" -> {
                close()
            }
            else -> {
                plugin.logger.info("Unhandled click: nbtId=$nbtId")
            }
        }
    }

    private fun handleRuneApplication(player: Player) {
        val targetItem = inventory.getItem(itemSlot)
        val runeItem = inventory.getItem(runeSlot)

        if (targetItem == null || runeItem == null) {
            player.sendMessage("§cPlease place both an item and a rune!")
            playSound("enchant_fail")
            return
        }

        val rune = runeManager.getRuneFromItem(runeItem)
        if (rune == null) {
            player.sendMessage("§cInvalid rune!")
            playSound("enchant_fail")
            return
        }

        val successRate = calculateSuccessRate(targetItem, rune)
        val cost = calculateApplicationCost(targetItem, rune)

        if (!plugin.vaultIntegration.hasEnough(player, cost)) {
            player.sendMessage("§cYou don't have enough money! Need: §6${plugin.vaultIntegration.format(cost)}")
            playSound("enchant_fail")
            return
        }

        plugin.vaultIntegration.withdrawPlayer(player, cost)

        val success = Random.nextInt(100) < successRate

        if (success) {

            val enhancedItem = runeManager.applyRuneToItem(targetItem, rune)
            inventory.setItem(itemSlot, enhancedItem)
            inventory.setItem(runeSlot, null)

            player.sendMessage("§a§lSUCCESS! §aRune applied successfully!")
            playSound("enchant_success")

            startGlowAnimation(itemSlot)
        } else {

            when (rune.id) {
                "power_rune" -> {

                    val meta = targetItem.itemMeta
                    if (meta is Damageable) {
                        meta.damage += (targetItem.type.maxDurability * 0.1).toInt()
                        targetItem.itemMeta = meta
                    }
                    player.sendMessage("§c§lFAILED! §cThe item was damaged in the process!")
                }
                "protection_rune" -> {

                    player.sendMessage("§c§lFAILED! §cThe rune crumbled to dust!")
                }
                else -> {
                    player.sendMessage("§c§lFAILED! §cThe rune application failed!")
                }
            }

            inventory.setItem(runeSlot, null)

            playSound("enchant_fail")
        }

        updateApplicationInterface()
    }
    private fun showRuneHelp(player: Player) {
        player.sendMessage("")
        player.sendMessage("§d§l=== RUNE APPLICATION GUIDE ===")
        player.sendMessage("§7Runes are powerful magical stones that can")
        player.sendMessage("§7enhance your items with special properties.")
        player.sendMessage("")
        player.sendMessage("§e§lHow to use:")
        player.sendMessage("§71. Place your item in the left slot")
        player.sendMessage("§72. Place a rune in the right slot")
        player.sendMessage("§73. Check the success rate and cost")
        player.sendMessage("§74. Click the anvil to apply!")
        player.sendMessage("")
        player.sendMessage("§c§lWarning:")
        player.sendMessage("§7- Failed applications consume the rune")
        player.sendMessage("§7- Some failures may damage your item")
        player.sendMessage("§7- Higher durability = better success rate")
        player.sendMessage("")

        playSound("click")
    }
}

