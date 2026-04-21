package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class AshesMenu(private val plugin: SurealmsEnchant, private val config: FileConfiguration) {

    val titleComponent: Component = MiniMessage.miniMessage().deserialize(config.getString("ashes-menu.title", "<dark_purple>Ashes Conversion")!!)
    val inputSlots: List<Int> = config.getIntegerList("ashes-menu.input-slots").takeIf { it.isNotEmpty() } ?: (0..8).toList()
    val outputSlot: Int = config.getInt("ashes-menu.output-slot", 23)

    // Configurable convert button
    val convertSlot: Int = config.getInt("ashes-menu.convert-button.slot", 21)
    private val convertMaterial: Material = Material.getMaterial(config.getString("ashes-menu.convert-button.material", "ANVIL")!!.uppercase()) ?: Material.ANVIL
    private val convertName: String = config.getString("ashes-menu.convert-button.name", "<green>Convert")!!
    private val convertLore: List<Component> = config.getStringList("ashes-menu.convert-button.lore").map { MiniMessage.miniMessage().deserialize(it) }
    private val convertCustomModelData: Int = config.getInt("ashes-menu.convert-button.custom-model-data", 0)

    // Configurable sound
    val soundName: String = config.getString("ashes-menu.sound.name", "BLOCK_ANVIL_USE")!!
    val soundVolume: Float = config.getDouble("ashes-menu.sound.volume", 1.0).toFloat()
    val soundPitch: Float = config.getDouble("ashes-menu.sound.pitch", 1.0).toFloat()

    private val fillMaterial: Material = Material.getMaterial(config.getString("ashes-menu.fill-item.material", "GRAY_STAINED_GLASS_PANE")!!.uppercase()) ?: Material.GRAY_STAINED_GLASS_PANE
    private val fillCustomModelData: Int = config.getInt("ashes-menu.fill-item.custom-model-data", 0)

    fun openMenu(player: Player) {
        val inventory = Bukkit.createInventory(null, 36, titleComponent)

        val background = ItemStack(fillMaterial)
        val backgroundMeta = background.itemMeta
        backgroundMeta.isHideTooltip = true
        backgroundMeta.displayName(Component.text(" "))
        if (fillCustomModelData != 0) {
            backgroundMeta.setCustomModelData(fillCustomModelData)
        }
        background.itemMeta = backgroundMeta

        val functionalSlots = inputSlots + outputSlot + convertSlot
        for (i in 0..35) {
            if (i !in functionalSlots) {
                inventory.setItem(i, background)
            }
        }

        val convertButton = ItemStack(convertMaterial)
        val convertMeta = convertButton.itemMeta
        convertMeta.displayName(ChatUtils.parse(convertName))
        convertMeta.lore(convertLore)
        if (convertCustomModelData != 0) {
            convertMeta.setCustomModelData(convertCustomModelData)
        }
        convertButton.itemMeta = convertMeta
        inventory.setItem(convertSlot, convertButton)

        player.openInventory(inventory)
    }
}