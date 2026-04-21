package me.sanenuyan.surealmsEnchant.gui.holders

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

class EnchantingTableGUIHolder(
    val plugin: SurealmsEnchant,
    val player: Player,
    val guiType: String = "ENCHANTING_TABLE"
) : InventoryHolder {

    private var inventory: Inventory? = null

    override fun getInventory(): Inventory {
        return inventory ?: throw IllegalStateException("Inventory not set")
    }

    fun setInventory(inv: Inventory) {
        this.inventory = inv
    }

    fun isMyInventory(inv: Inventory): Boolean {
        return inv.holder == this
    }

    companion object {

        fun isEnchantingTableGUI(inventory: Inventory): Boolean {
            return inventory.holder is EnchantingTableGUIHolder
        }

        fun getHolder(inventory: Inventory): EnchantingTableGUIHolder? {
            return inventory.holder as? EnchantingTableGUIHolder
        }
    }
}

