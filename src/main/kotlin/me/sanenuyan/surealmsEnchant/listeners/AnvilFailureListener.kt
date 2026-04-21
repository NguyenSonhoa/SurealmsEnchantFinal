package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ThreadLocalRandom
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.format.TextDecoration

class AnvilFailureListener(private val plugin: SurealmsEnchant, private val enchantmentSystem: EnchantmentSystem, private val runeConfig: RuneConfig) : Listener {

    private val oldModelKey = NamespacedKey(plugin, "old_item_model")
    private val isBrokenKey = NamespacedKey(plugin, "is_broken")
    private val anvilFailureKey = NamespacedKey(plugin, "anvil_failure_chance")

    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.inventory !is AnvilInventory) return
        if (event.slot != 2) return

        val player = event.whoClicked as? Player ?: return
        val anvil = event.inventory as AnvilInventory
        val resultItem = event.currentItem ?: return

        val firstItem = anvil.getItem(0)
        if (firstItem != null) {
            val firstItemMeta = firstItem.itemMeta
            if (firstItemMeta != null && firstItemMeta.persistentDataContainer.has(isBrokenKey, PersistentDataType.BYTE)) {
                event.isCancelled = true
                player.sendMessage(ChatUtils.parse("<red>This item was broken, can't use it.</red>"))
                return
            }
        }

        val book = anvil.getItem(1) ?: return
        val bookMeta = book.itemMeta ?: return

        if (!bookMeta.persistentDataContainer.has(anvilFailureKey, PersistentDataType.DOUBLE)) {
            return
        }

        val failureChance = bookMeta.persistentDataContainer.get(anvilFailureKey, PersistentDataType.DOUBLE)!!
        val roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0)

        if (roll < failureChance) {

//

            event.isCancelled = true

            val itemToBreak = anvil.getItem(0)?.clone() ?: ItemStack(Material.AIR)
            val itemToBreakMeta = itemToBreak.itemMeta

            anvil.setItem(1, null)

            if (itemToBreakMeta != null && itemToBreakMeta.persistentDataContainer.has(enchantmentSystem.protectedKey, PersistentDataType.BYTE)) {

                itemToBreakMeta.persistentDataContainer.remove(enchantmentSystem.protectedKey)

                val lore = itemToBreakMeta.lore()?.toMutableList() ?: mutableListOf()
                val protectedLoreComponent = MiniMessage.miniMessage().deserialize(runeConfig.protectionMessage)
                    .decoration(TextDecoration.ITALIC, false)
                lore.remove(protectedLoreComponent)
                itemToBreakMeta.lore(lore)

                itemToBreak.itemMeta = itemToBreakMeta

                val leftover = player.inventory.addItem(itemToBreak)
                if (leftover.isNotEmpty()) {
                    leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                }

                player.sendMessage(ChatUtils.parse(runeConfig.shatterMessage))
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)

                player.world.spawnParticle(Particle.ENCHANT, player.location, 25)

                anvil.setItem(0, null)

            } else {

                anvil.setItem(0, null)

                val brokenItem = breakItem(itemToBreak)

                val leftover = player.inventory.addItem(brokenItem)
                if (leftover.isNotEmpty()) {
                    leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
                }

                val newItemModelKey = plugin.config.getString("anvil-failure.new-item-model", "minecraft:cobblestone")?.let { NamespacedKey.fromString(it) }
                playCustomTotemAnimation(player, newItemModelKey)

                player.sendMessage(ChatUtils.parse("<#DC2625>Failed to enchant, your item was broken, fix it and try again.</white>"))
            }

        } else {

//

        }
    }

    private fun breakItem(item: ItemStack): ItemStack {
        val config = plugin.config
        if (!config.getBoolean("anvil-failure.enabled", false)) return item

        val meta = item.itemMeta ?: return item

        val oldModel = meta.itemModel
        val oldModelString = oldModel?.toString() ?: item.type.key.toString()
        meta.persistentDataContainer.set(oldModelKey, PersistentDataType.STRING, oldModelString)

        config.getString("anvil-failure.new-item-model", "minecraft:cobblestone")?.let {
            NamespacedKey.fromString(it)?.let { modelKey ->
                meta.itemModel = modelKey
            }
        }

        if (config.getBoolean("anvil-failure.prevent-usage", false)) {
            meta.persistentDataContainer.set(isBrokenKey, PersistentDataType.BYTE, 1)
        }

        meta.removeEnchantments()

        item.itemMeta = meta
        return item
    }

    private fun playCustomTotemAnimation(player: Player, itemModelKey: NamespacedKey?) {
        if (itemModelKey == null) return

        val totem = ItemStack(Material.TOTEM_OF_UNDYING)
        val meta = totem.itemMeta ?: return
        meta.itemModel = itemModelKey
        totem.itemMeta = meta
        val hand = player.inventory.itemInMainHand
        player.inventory.setItemInMainHand(totem)
        player.playEffect(EntityEffect.TOTEM_RESURRECT)
        player.inventory.setItemInMainHand(hand)
        player.world.spawnParticle(Particle.END_ROD, player.location, 25)
        player.playSound(player.location, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f)
    }
}