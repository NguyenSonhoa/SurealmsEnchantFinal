package me.sanenuyan.surealmsEnchant.integrations

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import io.lumine.mythic.api.adapters.AbstractItemStack
import io.lumine.mythic.api.config.MythicLineConfig
import io.lumine.mythic.api.drops.DropMetadata
import io.lumine.mythic.api.drops.IItemDrop
import io.lumine.mythic.bukkit.MythicBukkit
import io.lumine.mythic.core.drops.Drop
import io.lumine.mythic.bukkit.adapters.BukkitItemStack
import io.lumine.mythic.bukkit.events.MythicDropLoadEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

class MythicMobsIntegration(private val plugin: SurealmsEnchant) : Listener {

    fun initialize() {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onMythicDropLoad(event: MythicDropLoadEvent) {
        if (event.dropName.equals("SUREALMSENCHANT", ignoreCase = true)) {
            event.register(SurealmsEnchantDrop(event.config, plugin))
        }
    }
}

class SurealmsEnchantDrop(config: MythicLineConfig, private val plugin: SurealmsEnchant) : Drop(config.line, config), IItemDrop {

    private val bookId: String = config.getString(arrayOf("type", "t"), "tome_of_renewal") // Default to a known book ID if 'type' is not specified
    private val itemAmount: Int = config.getInteger(arrayOf("amount", "a"), 1)

    override fun getDrop(meta: DropMetadata, p1: Double): AbstractItemStack? {
        val bookType = plugin.specializedBookManager.getBookTypeById(bookId)
        val item: ItemStack? = bookType?.let {
            plugin.specializedBookManager.createSpecializedBook(it)
        }

        if (item != null) {
            item.amount = itemAmount
            return BukkitItemStack(item)
        }

        // log a warning if the book ID is not found, which can help in debugging
        plugin.logger.warning("MythicMobsIntegration: Could not find SurealmsEnchant book with ID: '$bookId'. Please ensure this ID is registered in SurealmsEnchant.")
        return null
    }
}
