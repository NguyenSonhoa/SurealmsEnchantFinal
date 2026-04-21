
package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class BrokenItemListener(private val plugin: SurealmsEnchant) : Listener {

    private val isBrokenKey = NamespacedKey(plugin, "is_broken")

    private fun isBroken(item: ItemStack?): Boolean {
        if (item == null || item.itemMeta == null) return false
        return item.itemMeta.persistentDataContainer.has(isBrokenKey, PersistentDataType.BYTE)
    }

    private fun denyUsage(player: Player) {
        plugin.config.getString("anvil-failure.usage-denied-message", "<red>Vật phẩm này đã hỏng và không thể sử dụng!")?.let { message ->
            player.sendActionBar(MiniMessage.miniMessage().deserialize(message))
        }
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!isBroken(event.item)) return

        denyUsage(event.player)
        event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager !is Player) return

        if (!isBroken(damager.inventory.itemInMainHand)) return

        denyUsage(damager)
        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isBroken(event.player.inventory.itemInMainHand)) return

        denyUsage(event.player)
        event.isCancelled = true
    }

    @EventHandler
    fun onArmorEquip(event: InventoryClickEvent) {
        if (event.slotType != InventoryType.SlotType.ARMOR) return
        if (!isBroken(event.cursor)) return

        val player = event.whoClicked as Player
        denyUsage(player)
        event.isCancelled = true
    }
}
