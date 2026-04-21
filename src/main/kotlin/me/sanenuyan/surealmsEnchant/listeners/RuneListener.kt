package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.managers.RuneManager
import me.sanenuyan.surealmsEnchant.models.RuneApplicationResult
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneType
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack

class RuneListener(
    private val plugin: SurealmsEnchant,
    private val runeManager: RuneManager,
    private val runeConfig: RuneConfig,
    private val enchantmentSystem: EnchantmentSystem
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onAnvilClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        if (inventory.type != InventoryType.ANVIL) return

        val anvilInventory = inventory as AnvilInventory
        val player = event.whoClicked as? Player ?: return

        if (event.slot != 2) return

        val firstItem = anvilInventory.getItem(0)

        val secondItem = anvilInventory.getItem(1)

        val resultItem = anvilInventory.getItem(2)

        if (firstItem != null && secondItem != null && resultItem != null) {
            if (runeManager.isRune(secondItem)) {
                val runeType = runeManager.getRuneType(secondItem)

                if (runeType == RuneType.PROTECTION_RUNE) {
                    val targetMeta = firstItem.itemMeta

                    if (targetMeta != null && !targetMeta.persistentDataContainer.has(enchantmentSystem.protectedKey)) {

                        val config = runeConfig.protectionRuneItemConfig
                        val runeTypeName = config.displayName.replace("§[0-9a-fk-or]".toRegex(), "")
                        player.sendMessage(ChatUtils.parse("<white>ꕩ $runeTypeName <#85CC16>đã nhận được tín hiệu từ trang bị và đã phù phép lên nó."))
                        player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f)
                    }
                    return

                }

                handleRuneApplication(event, player, firstItem, secondItem, resultItem)
            }
        }
    }

    private fun handleRuneApplication(
        event: InventoryClickEvent,
        player: Player,
        targetItem: ItemStack,
        runeItem: ItemStack,
        resultItem: ItemStack
    ) {
        val runeType = runeManager.getRuneType(runeItem)
        if (runeType == null) {
            event.isCancelled = true
            player.sendMessage("§cInvalid rune!")
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return
        }

        val result = runeManager.applyRune(targetItem.clone(), runeItem)

        when (result) {
            is RuneApplicationResult.Success -> {

                val config = when (runeType) {
                    RuneType.POWER_RUNE -> runeConfig.powerRuneItemConfig
                    RuneType.PROTECTION_RUNE -> runeConfig.protectionRuneItemConfig
                }
                val runeTypeName = config.displayName.replace("§[0-9a-fk-or]".toRegex(), "")
                player.sendMessage(ChatUtils.parse("<white>ꕩ $runeTypeName <#85CC16>đã nhận được tín hiệu từ trang bị và đã phù phép lên nó."))
                player.playSound(player.location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f)

                runeManager.applyRune(resultItem, runeItem)
            }

            is RuneApplicationResult.IncompatibleItem -> {
                event.isCancelled = true
                player.sendMessage(ChatUtils.parse("<white>ꕫ <#DC2625>Cổ ngữ không thể sử dụng trên vật phẩm này"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.AlreadyProtected -> {
                event.isCancelled = true
                player.sendMessage(ChatUtils.parse("<white>ꕫ <#DC2625>Trang bị này đã có cổ ngữ Bảo vệ"))
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.NoEnchantmentsToUpgrade -> {
                event.isCancelled = true
                player.sendMessage("§cThis item has no enchantments to upgrade!")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.AllEnchantmentsMaxLevel -> {
                event.isCancelled = true
                player.sendMessage("§cAll enchantments on this item are already at maximum level!")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.InvalidRune -> {
                event.isCancelled = true
                player.sendMessage("§cInvalid rune!")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.InvalidItem -> {
                event.isCancelled = true
                player.sendMessage("§cInvalid item for rune application!")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }

            is RuneApplicationResult.Failure -> {
                event.isCancelled = true
                player.sendMessage("§cRune application failed: ${result.reason}")
                player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            }
        }
    }
}

