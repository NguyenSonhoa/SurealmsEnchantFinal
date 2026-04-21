package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.managers.AshesItemManager
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import org.bukkit.persistence.PersistentDataType

class AnvilListener(
    private val plugin: SurealmsEnchant,
    private val ashesItemManager: AshesItemManager,
    private val enchantmentSystem: EnchantmentSystem,
    private val runeConfig: RuneConfig
) : Listener {

    private val isBrokenKey = NamespacedKey(plugin, "is_broken")

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val anvilInventory = event.inventory
        val firstItem = anvilInventory.firstItem
        val secondItem = anvilInventory.secondItem

        if (firstItem == null) {
            return
        }

        val firstItemMeta = firstItem.itemMeta
        if (firstItemMeta != null && firstItemMeta.persistentDataContainer.has(isBrokenKey, PersistentDataType.BYTE)) {
            event.result = null

            return
        }

        if (secondItem == null) {
            return
        }

        val secondItemMeta = secondItem.itemMeta
        if (secondItemMeta != null) {
            val runeType = secondItemMeta.persistentDataContainer.get(enchantmentSystem.runeTypeKey, PersistentDataType.STRING)
            if (runeType == RuneType.PROTECTION_RUNE.id) {
                val resultItem = firstItem.clone()
                val resultMeta = resultItem.itemMeta

                if (resultMeta != null && !resultMeta.persistentDataContainer.has(enchantmentSystem.protectedKey)) {

                    val lore = resultMeta.lore() ?: mutableListOf()
                    lore.add(
                        MiniMessage.miniMessage().deserialize(runeConfig.protectionMessage)
                            .decoration(TextDecoration.ITALIC, false)
                    )
                    resultMeta.lore(lore)

                    resultMeta.persistentDataContainer.set(enchantmentSystem.protectedKey, PersistentDataType.BYTE, 1.toByte())

                    resultItem.itemMeta = resultMeta
                }

                event.result = resultItem
                plugin.server.scheduler.runTask(plugin, Runnable {
                    anvilInventory.repairCost = 30
                })
                return
            }
        }

        if (firstItem.type == Material.ENCHANTED_BOOK && ashesItemManager.isAshesItem(secondItem)) {
            val bookMeta = firstItem.itemMeta as? EnchantmentStorageMeta ?: return

            val baseChanceModifier = plugin.config.getDouble("ashes.chance-modifier", 5.0)
            val resultLoreLines = plugin.config.getStringList("enchanting.result_lore.success.lines")

            var successChance: Double? = null
            var anvilFailureChance: Double? = null

            bookMeta.lore()?.forEach { componentLine ->
                val plainText = MiniMessage.miniMessage().stripTags(MiniMessage.miniMessage().serialize(componentLine))

                if (plainText.startsWith("Tỷ lệ thành công: ")) {
                    successChance = plainText.substringAfter(": ").removeSuffix("%").toDoubleOrNull()
                } else if (plainText.startsWith("Tỷ lệ thất bại: ")) {
                    anvilFailureChance = plainText.substringAfter(": ").removeSuffix("%").toDoubleOrNull()
                }
            }
            if (successChance != null && anvilFailureChance != null) {

                val scaledChanceModifier = baseChanceModifier * secondItem.amount

                val newSuccessChance = (successChance!! + scaledChanceModifier).coerceAtMost(100.0)
                val newAnvilFailureChance = (anvilFailureChance!! - scaledChanceModifier).coerceAtLeast(0.0)

                val resultBook = firstItem.clone()
                val resultMeta = resultBook.itemMeta as EnchantmentStorageMeta

                val updatedLoreComponents = resultLoreLines.map {
                    val formattedLine = it.replace("{chance}", String.format("%.2f", newSuccessChance))
                        .replace("{anvil_chance}", String.format("%.2f", newAnvilFailureChance))
                    MiniMessage.miniMessage().deserialize(formattedLine)
                }
                val anvilFailureKey = NamespacedKey(plugin, "anvil_failure_chance")
                resultMeta.persistentDataContainer.set(anvilFailureKey, PersistentDataType.DOUBLE, newAnvilFailureChance)

                val successKey = NamespacedKey(plugin, "success_chance")
                resultMeta.persistentDataContainer.set(successKey, PersistentDataType.DOUBLE, newSuccessChance)
                resultMeta.lore(updatedLoreComponents)

                resultBook.itemMeta = resultMeta

                event.result = resultBook

                plugin.server.scheduler.runTask(plugin, Runnable {
                    anvilInventory.repairCost = 10
                })
            } else {
                println("Failed to parse successChance or anvilFailureChance. Success: $successChance, AnvilFailure: $anvilFailureChance")

            }
        }
    }
}

