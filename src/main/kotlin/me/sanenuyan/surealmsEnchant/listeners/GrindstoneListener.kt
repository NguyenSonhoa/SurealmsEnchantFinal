package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.managers.MessageManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.GrindstoneInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.EnchantmentStorageMeta

class GrindstoneListener(
    private val plugin: SurealmsEnchant,
    private val specializedBookManager: SpecializedBookManager,
    private val messageManager: MessageManager
) : Listener {

    @EventHandler
    fun onGrindstoneClick(event: InventoryClickEvent) {
        val inventory = event.inventory
        if (inventory.type != InventoryType.GRINDSTONE) return

        val grindstoneInventory = inventory as GrindstoneInventory
        val player = event.whoClicked as? Player ?: return

        if (event.slot != 2) return

        val firstItem = grindstoneInventory.getItem(0)
        val secondItem = grindstoneInventory.getItem(1)
        val resultItem = grindstoneInventory.getItem(2)

        if (firstItem != null && isOurEnchantedBook(firstItem) && secondItem == null) {
            handleBookDisenchanting(event, player, firstItem, resultItem)
        } else if (secondItem != null && isOurEnchantedBook(secondItem) && firstItem == null) {
            handleBookDisenchanting(event, player, secondItem, resultItem)
        }
    }

    private fun handleBookDisenchanting(
        event: InventoryClickEvent,
        player: Player,
        enchantedBook: ItemStack,
        resultItem: ItemStack?
    ) {

        val bookMeta = enchantedBook.itemMeta as? EnchantmentStorageMeta ?: return
        val enchantmentId = bookMeta.persistentDataContainer.get(
            specializedBookManager.getEnchantmentIdKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        )

        if (enchantmentId == null) {
            return

        }

        val bookTypeId = bookMeta.persistentDataContainer.get(
            specializedBookManager.getBookTypeKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        )

        val disenchantedBook = if (bookTypeId != null) {

            val bookType = specializedBookManager.getBookTypeById(bookTypeId)
            bookType?.let { specializedBookManager.createSpecializedBook(it) } ?: ItemStack(Material.BOOK)
        } else {

            ItemStack(Material.BOOK)
        }

        event.isCancelled = true

        val grindstoneInventory = event.inventory as GrindstoneInventory

        grindstoneInventory.setItem(2, disenchantedBook)

        val xpToGive = calculateDisenchantXP(enchantedBook)
        if (xpToGive > 0) {
            player.giveExp(xpToGive)
        }

        if (grindstoneInventory.getItem(0) == enchantedBook) {
            grindstoneInventory.setItem(0, null)
        } else {
            grindstoneInventory.setItem(1, null)
        }

        val resultToGive = disenchantedBook.clone()
        if (player.inventory.firstEmpty() != -1) {
            player.inventory.addItem(resultToGive)
        } else {
            player.world.dropItemNaturally(player.location, resultToGive)
        }

        grindstoneInventory.setItem(2, null)

        player.playSound(player.location, Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.0f)

        if (bookTypeId != null) {
            val bookType = specializedBookManager.getBookTypeById(bookTypeId)
            val specializedMessage = messageManager.getMessage("grindstone.disenchant_specialized") ?: ""
            val message = specializedMessage.replace("{book_type}", bookType?.displayName ?: "specialized book")
        } else {
            val regularMessage = messageManager.getMessage("grindstone.disenchant_regular") ?: ""
        }

        if (xpToGive > 0) {
            val xpMessage = messageManager.getMessage("grindstone.xp_gain") ?: ""
            val message = xpMessage.replace("{xp}", xpToGive.toString())
        }
    }

    private fun calculateDisenchantXP(enchantedBook: ItemStack): Int {
        val meta = enchantedBook.itemMeta as? EnchantmentStorageMeta ?: return 0
        val storedEnchants = meta.storedEnchants

        var totalXP = 0
        for ((enchantment, level) in storedEnchants) {

            val baseXP = when {
                enchantment.key.key.contains("protection") -> 5
                enchantment.key.key.contains("sharpness") -> 4
                enchantment.key.key.contains("efficiency") -> 3
                enchantment.key.key.contains("unbreaking") -> 6
                enchantment.key.key.contains("fortune") -> 8
                enchantment.key.key.contains("silk_touch") -> 15
                enchantment.key.key.contains("mending") -> 20
                enchantment.key.key.contains("infinity") -> 15
                else -> 2
            }

            totalXP += baseXP * level
        }

        val multiplier = plugin.config.getDouble("grindstone.xp-multiplier", 0.5)
        return (totalXP * multiplier).toInt()
    }

    private fun isOurEnchantedBook(item: ItemStack): Boolean {
        if (item.type != Material.ENCHANTED_BOOK) return false

        val meta = item.itemMeta as? EnchantmentStorageMeta ?: return false

        return meta.persistentDataContainer.has(
            specializedBookManager.getEnchantmentIdKey(),
            org.bukkit.persistence.PersistentDataType.STRING
        )
    }
}

private fun Any.replace(string: String, string2: String) {}

