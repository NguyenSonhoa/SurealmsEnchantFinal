package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

class AshesItemManager(private val plugin: SurealmsEnchant) {

    private val ashesKey = NamespacedKey(plugin, "is_ashes")

    fun createAshesItem(amount: Int): ItemStack {
        val config = plugin.config
        val materialName = config.getString("ashes.material", "COAL")
        val customModelData = config.getInt("ashes.custom-model-data", 0)
        val displayName: Component = MiniMessage.miniMessage().deserialize(config.getString("ashes.display_name", "<gold>Ashes")!!)
        val lore: List<Component> = config.getStringList("ashes.lore").map { MiniMessage.miniMessage().deserialize(it) }

        val material = Material.matchMaterial(materialName!!) ?: Material.COAL
        val itemStack = ItemStack(material, amount)
        val meta = itemStack.itemMeta

        meta?.displayName(displayName)

        meta?.lore(lore)
        if (customModelData != 0) {
            meta?.setCustomModelData(customModelData)
        }

        meta?.persistentDataContainer?.set(ashesKey, PersistentDataType.BOOLEAN, true)

        itemStack.itemMeta = meta
        return itemStack
    }

    fun isAshesItem(itemStack: ItemStack?): Boolean {
        if (itemStack == null || itemStack.type == Material.AIR) return false
        val meta = itemStack.itemMeta ?: return false
        return meta.persistentDataContainer.has(ashesKey, PersistentDataType.BOOLEAN)
    }
}

