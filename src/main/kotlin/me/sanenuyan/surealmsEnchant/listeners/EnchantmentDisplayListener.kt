package me.sanenuyan.surealmsEnchant.listeners

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import me.sanenuyan.surealmsEnchant.integrations.ExcellentEnchantsIntegration
import me.sanenuyan.surealmsEnchant.managers.DatapackEnchantmentManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.EnchantmentStorageMeta
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.concurrent.ThreadLocalRandom

class EnchantmentDisplayListener(
    private val plugin: SurealmsEnchant,
    private val excellentEnchantsIntegration: ExcellentEnchantsIntegration,
) : PacketListenerAbstract(), Listener {

    private val anvilFailureKey = NamespacedKey(plugin, "anvil_failure_chance")
    private val successKey = NamespacedKey(plugin, "success_chance")

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem ?: return
        val meta = item.itemMeta ?: return

        if (meta is EnchantmentStorageMeta) {
            val pdc = meta.persistentDataContainer
            if (!pdc.has(successKey, PersistentDataType.DOUBLE) && !pdc.has(anvilFailureKey, PersistentDataType.DOUBLE)) {
                val minSuccess = plugin.config.getDouble("default-enchant-book.success-chance.min", 0.0)
                val maxSuccess = plugin.config.getDouble("default-enchant-book.success-chance.max", 100.0)
                val successChance = ThreadLocalRandom.current().nextDouble(minSuccess, maxSuccess)
                val failureChance = 100.0 - successChance


                
                pdc.set(successKey, PersistentDataType.DOUBLE, successChance)
                pdc.set(anvilFailureKey, PersistentDataType.DOUBLE, failureChance)
                val lore = meta.lore()?.toMutableList() ?: mutableListOf()
                lore.removeIf { component ->
                    val plainText = PlainTextComponentSerializer.plainText().serialize(component)
                    plainText.contains("Tỷ lệ thất bại:") || plainText.contains("Tỷ lệ thành công:")
                }
                lore.add(
                    ChatUtils.parse("<#DC2625>Tỷ lệ thất bại: <white>${"%.2f".format(failureChance)}%")
                )
                lore.add(
                    ChatUtils.parse("<#85CC16>Tỷ lệ thành công: <white>${"%.2f".format(successChance)}%")
                )

                meta.lore(lore)
                item.itemMeta = meta
            }
        }
    }

    override fun onPacketSend(event: PacketSendEvent) {
        when (event.packetType) {
            PacketType.Play.Server.WINDOW_ITEMS -> {
                val wrapper = WrapperPlayServerWindowItems(event)
                val processedItems = wrapper.items.map { processItemStack(it) }
                wrapper.items = processedItems
            }
            PacketType.Play.Server.SET_SLOT -> {
                val wrapper = WrapperPlayServerSetSlot(event)
                wrapper.item = processItemStack(wrapper.item)
            }
            else -> {}
        }
    }

    private fun processItemStack(itemStack: ItemStack?): ItemStack? {
        if (itemStack == null || itemStack.isEmpty) {
            return itemStack
        }

        val bukkitStack = SpigotConversionUtil.toBukkitItemStack(itemStack)
        val itemMeta = bukkitStack.itemMeta ?: return itemStack

        if (itemMeta is EnchantmentStorageMeta) {
            val failureChance = itemMeta.persistentDataContainer.get(anvilFailureKey, PersistentDataType.DOUBLE)
            val successChance = itemMeta.persistentDataContainer.get(successKey, PersistentDataType.DOUBLE)

            if (failureChance != null && successChance != null) {
                val lore = itemMeta.lore()?.toMutableList() ?: mutableListOf()
                lore.removeIf { component ->
                    val plainText = PlainTextComponentSerializer.plainText().serialize(component)
                    plainText.contains("Tỷ lệ thất bại:") || plainText.contains("Tỷ lệ thành công:")
                }

                lore.add(
                    ChatUtils.parse("<#DC2625>Tỷ lệ thất bại: <white>${"%.2f".format(failureChance)}%")
                )
                lore.add(
                    ChatUtils.parse("<#85CC16>Tỷ lệ thành công: <white>${"%.2f".format(successChance)}%")
                )

                itemMeta.lore(lore)
            }

            val enchants = mutableMapOf<Enchantment, Int>()
            enchants.putAll(itemMeta.enchants)
            if (itemMeta is EnchantmentStorageMeta) {
                enchants.putAll(itemMeta.storedEnchants)
            }

            if (enchants.isEmpty()) {
                return itemStack
            }

            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            if (itemMeta is EnchantmentStorageMeta) {
                itemMeta.addItemFlags(ItemFlag.HIDE_STORED_ENCHANTS)
            }

            val newLore = mutableListOf<Component>()
            val enchantDisplayNames = mutableListOf<String>()

            for ((enchant, level) in enchants) {
                val customEnchant = if (excellentEnchantsIntegration.isEnabled()) {
                    EnchantRegistry.getByBukkit(enchant)
                } else {
                    null
                }
                val enchantName = customEnchant?.displayName
                    ?: enchant.key.key.replace('_', ' ').split(' ')
                        .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

                val romanLevel = toRoman(level)

                enchantDisplayNames.add("$enchantName $romanLevel")

            }

            if (itemMeta is EnchantmentStorageMeta) {
                val displayName = Component.text(enchantDisplayNames.joinToString(", "), NamedTextColor.AQUA)
                itemMeta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(displayName))
            }


        }
        bukkitStack.itemMeta = itemMeta
        return SpigotConversionUtil.fromBukkitItemStack(bukkitStack)
    }

    private fun toRoman(number: Int): String {
        if (number < 1 || number > 3999) return number.toString()
        val romanNumerals = listOf(
            1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
            100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
            10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
        )
        val result = StringBuilder()
        var num = number
        for ((value, symbol) in romanNumerals) {
            while (num >= value) {
                result.append(symbol)
                num -= value
            }
        }
        return result.toString()
    }
}
