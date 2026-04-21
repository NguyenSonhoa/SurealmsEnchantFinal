package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.gui.holders.EnchantingTableGUIHolder
import me.sanenuyan.surealmsEnchant.listeners.EnchantingTableListenerSimple
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import me.sanenuyan.surealmsEnchant.models.CustomEnchantment
import me.sanenuyan.surealmsEnchant.utils.ChatUtils
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

class ConfigurableEnchantingTableGUI(
    plugin: SurealmsEnchant,
    player: Player,
    private val enchantmentSystem: EnchantmentSystem,
    private val specializedBookManager: SpecializedBookManager,
    private val customEnchantmentManager: CustomEnchantmentManager,
    private val listener: EnchantingTableListenerSimple
) : ConfigurableGUI(plugin, "enchanting_table", player) {

    private var lastInputKey: String? = null
    private var offeredEnchantments: Map<Int, CustomEnchantment> = emptyMap()

    private fun toRoman(number: Int): String {
        return when (number) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            5 -> "V"
            else -> number.toString()
        }
    }

    private val inputSlotNumber: Int
        get() = guiConfig.getItemConfig("enchanting_table", "input_slot")?.slot ?: 31
    private val tier1Slots: List<Int> get() = guiConfig.getSlots("enchanting_table", "tier_1_slots")
    private val tier2Slots: List<Int> get() = guiConfig.getSlots("enchanting_table", "tier_2_slots")
    private val tier3Slots: List<Int> get() = guiConfig.getSlots("enchanting_table", "tier_3_slots")

    private fun clearTierSlots() {
        (tier1Slots + tier2Slots + tier3Slots).forEach { slot ->
            inventory.setItem(slot, null)
        }
    }

    override fun handleClose() {
        onClose()
    }

    override fun onClose() {
        val bookItem = inventory.getItem(inputSlotNumber)
        if (bookItem != null && bookItem.type != Material.AIR) {
            player.inventory.addItem(bookItem)
            inventory.setItem(inputSlotNumber, null)
        }
        super.onClose()
    }

    override fun initialize() {
        val holder = EnchantingTableGUIHolder(plugin, player)
        val title = getUpdatedTitle()
        val size = guiConfig.getSize(guiType)
        val customInventory = Bukkit.createInventory(holder, size, title)
        holder.setInventory(customInventory)
        val inventoryField = this.javaClass.superclass.getDeclaredField("inventory")
        inventoryField.isAccessible = true
        inventoryField.set(this, customInventory)
        setupItems()
    }
    private fun serializeOffers(offers: Map<Int, CustomEnchantment>): String {
        return offers.entries.joinToString(";") { "${it.key}:${it.value.id}" }
    }

    private fun rerollOfferedEnchantments() {
        val inputItem = inventory.getItem(inputSlotNumber)
        val newOffers = mutableMapOf<Int, CustomEnchantment>()
        if (inputItem != null && inputItem.type != Material.AIR) {
            val bookType = specializedBookManager.getBookTypeFromItem(inputItem)
            if (bookType == null) {

                val existingOffers = listener.playerOfferedEnchantments[player.uniqueId]
                if (existingOffers != null) {
                    offeredEnchantments = existingOffers
                    return
                } else {
                    for (tier in 1..3) {
                        val applicableEnchants = enchantmentSystem.getApplicableEnchantments(inputItem, tier, null)
                        if (applicableEnchants.isNotEmpty()) {
                            newOffers[tier] = applicableEnchants.random()
                        }
                    }
                    listener.playerOfferedEnchantments[player.uniqueId] = newOffers
                }
            } else {

                for (tier in 1..3) {
                    val applicableEnchants = enchantmentSystem.getApplicableEnchantments(inputItem, tier, bookType)
                    if (applicableEnchants.isNotEmpty()) {
                        newOffers[tier] = applicableEnchants.random()
                    }
                }
            }
        }
        this.offeredEnchantments = newOffers
    }

    fun safeRefresh() {
        val inputItem = inventory.getItem(inputSlotNumber)?.clone()
        val inputKey = generateInputKey(inputItem)

        if (inputItem == null || inputItem.type == Material.AIR) {
            offeredEnchantments = emptyMap()
            lastInputKey = null
            clearTierSlots()
            updateTitle()
            return
        }

        val meta = inputItem.itemMeta
        if (meta is org.bukkit.inventory.meta.EnchantmentStorageMeta && meta.hasStoredEnchants()) {
            offeredEnchantments = emptyMap()
            lastInputKey = null
            clearTierSlots()
            updateTitle()
            return
        }

        val bookType = specializedBookManager.getBookTypeFromItem(inputItem)
        if (bookType == null) {

            val existingOffers = listener.playerOfferedEnchantments[player.uniqueId]
            if (existingOffers != null) {
                offeredEnchantments = existingOffers
            } else {
                rerollOfferedEnchantments()
            }
        } else {

            if (bookType != null) {

                val oldOffers = readOfferedEnchantmentsFromNBT(inputItem)
                if (oldOffers != null) {
                    offeredEnchantments = oldOffers
                } else {
                    rerollOfferedEnchantments()

                    val meta = inputItem.itemMeta
                    meta?.persistentDataContainer?.set(
                        NamespacedKey(plugin, "offered_enchants"), PersistentDataType.STRING, serializeOffers(offeredEnchantments)
                    )
                    inputItem.itemMeta = meta
                }
            }
        }
        refresh()
        inventory.setItem(inputSlotNumber, inputItem)
        updateTierDisplay()
        updateTitle()
    }
    private fun readOfferedEnchantmentsFromNBT(item: ItemStack): Map<Int, CustomEnchantment>? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val offersNBT = container.get(NamespacedKey(plugin, "offered_enchants"), PersistentDataType.STRING)
        return if (offersNBT != null) deserializeOffers(offersNBT, customEnchantmentManager) else null
    }
    private fun getUpdatedTitle(): Component {
        var titleComponent = ChatUtils.parse(guiConfig.getTitle(guiType, player))

        for (tier in 1..3) {
            val enchantment = offeredEnchantments[tier]
            var canAffordAndHasEnchant = false

            if (enchantment != null) {
                val cost = enchantmentSystem.calculateEnchantCost(enchantment, enchantment.level)
                val requiredLevel = enchantmentSystem.getRequiredLevel(enchantment.tier)
                if (plugin.vaultIntegration.hasEnough(player, cost) && player.level >= requiredLevel) {
                    canAffordAndHasEnchant = true
                }
            }

            val configPath = "gui_title_placeholders.tier$tier.${if (canAffordAndHasEnchant) "has_enchants" else "no_enchants"}"
            val defaultText = if (canAffordAndHasEnchant) "Yes" else "No"
            val replacement = plugin.config.getString(configPath, defaultText) ?: defaultText
            val replacementComponent = ChatUtils.parse(replacement)

            titleComponent = titleComponent.replaceText { builder ->
                builder.matchLiteral("{tier$tier}").replacement(replacementComponent)
            }
        }
        return titleComponent
    }

    private fun updateTitle() {
        if (inventory.viewers.isEmpty()) return

        val newTitle = getUpdatedTitle()
        val holder = inventory.holder as? EnchantingTableGUIHolder ?: return
        val oldInventory = inventory

        val inputItem = oldInventory.getItem(inputSlotNumber)
        oldInventory.setItem(inputSlotNumber, null)

        val newInventory = Bukkit.createInventory(holder, oldInventory.size, newTitle)
        newInventory.contents = oldInventory.contents
        newInventory.setItem(inputSlotNumber, inputItem)

        holder.setInventory(newInventory)
        try {
            val inventoryField = this.javaClass.superclass.getDeclaredField("inventory")
            inventoryField.isAccessible = true
            inventoryField.set(this, newInventory)
        } catch (e: Exception) {
            plugin.logger.severe("Failed to update inventory title via reflection: " + e.message)
            oldInventory.setItem(inputSlotNumber, inputItem)
            player.sendMessage(ChatUtils.parse("<red>An error occurred, please try again.</red>"))
            return
        }
        player.openInventory(newInventory)
    }

    private fun updateTierDisplay() {
        val inputItem = inventory.getItem(inputSlotNumber)
        if (inputItem == null || inputItem.type == Material.AIR) {
            clearTierSlots()
            return
        }

        val meta = inputItem.itemMeta
        if (meta != null && meta.hasEnchants() && meta !is org.bukkit.inventory.meta.EnchantmentStorageMeta) {
            clearTierSlots()
            return
        }

        clearTierSlots()
        showTierEnchantments(tier1Slots, 1)
        showTierEnchantments(tier2Slots, 2)
        showTierEnchantments(tier3Slots, 3)
    }
    fun deserializeOffers(data: String, customEnchantmentManager: CustomEnchantmentManager): Map<Int, CustomEnchantment> {
        return data.split(';').mapNotNull{ entry ->
            val parts = entry.split(':')
            if (parts.size == 2) {
                val slot = parts[0].toIntOrNull()
                val enchantment = customEnchantmentManager.getEnchantmentById(parts[1])
                if (slot != null && enchantment != null) {
                    slot to enchantment
                } else null
            } else null
        }.toMap()
    }
    private fun createEnchantmentDisplayItem(enchant: CustomEnchantment, tier: Int): ItemStack {
        val inputItem = inventory.getItem(inputSlotNumber) ?: return createEmptySlotItem()

        val item = ItemStack(Material.STICK)
        val meta = item.itemMeta ?: return item

        meta.displayName(ChatUtils.parse("<!i>${enchant.displayName} ???"))
        meta.setCustomModelData(1)

        val enchantKey = NamespacedKey(plugin, "enchantment_id")
        meta.persistentDataContainer.set(enchantKey, PersistentDataType.STRING, enchant.id)

        val newLore = mutableListOf<Component>()
        newLore.addAll(enchant.description.map { ChatUtils.parse(it) })

        val bookType = specializedBookManager.getBookTypeFromItem(inputItem)
        if (bookType != null) {
            val bookKey = NamespacedKey(plugin, "book_type")
            meta.persistentDataContainer.set(bookKey, PersistentDataType.STRING, bookType.id)
        }

        val cost = enchantmentSystem.calculateEnchantCost(enchant, enchant.level)
        val requiredLevel = enchantmentSystem.getRequiredLevel(tier)

        newLore.add(Component.empty())
        newLore.add(ChatUtils.parse("<#FFFFFF><shadow:black> ꓄ $cost Bạc"))
        newLore.add(ChatUtils.parse("<#FFFFFF><shadow:black> ꕿ $requiredLevel Levels"))
        newLore.add(Component.empty())
        newLore.add(ChatUtils.parse("<gray>→ Nhấn để phù phép</gray>"))

        meta.lore(newLore)

        item.itemMeta = meta
        return item
    }

    private fun createEmptySlotItem(): ItemStack {
        val item = ItemStack(Material.AIR)
        val meta: ItemMeta? = item.itemMeta
        meta?.displayName(ChatUtils.parse("<gray>No enchantments available</gray>"))
        item.itemMeta = meta
        return item
    }

    private fun createUnavailableItem(cost: Double, requiredLevel: Int): ItemStack {
        val section = plugin.config.getConfigurationSection("enchantment_option_unavailable")
        if (section == null) {
            return ItemStack(Material.BARRIER).apply {
                itemMeta = itemMeta?.apply {
                    displayName(ChatUtils.parse("<red>Unavailable</red>"))
                }
            }
        }

        val materialName = section.getString("material") ?: "BARRIER"
        val material = Material.getMaterial(materialName) ?: Material.BARRIER
        val displayName = ChatUtils.parse(section.getString("display_name") ?: "<red>Unavailable</red>")
        val lore = section.getStringList("lore").map { line ->
            ChatUtils.parse(
                line.replace("%cost%", cost.toString())
                    .replace("%exp%", requiredLevel.toString())
            )
        }
        val customModelData = section.getInt("custom_model_data", 0)

        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.displayName(displayName)
        meta?.lore(lore)
        if (customModelData > 0) {
            meta?.setCustomModelData(customModelData)
        }
        item.itemMeta = meta
        return item
    }

    private fun generateInputKey(item: ItemStack?): String? {
        if (item == null || item.type == Material.AIR) return null
        val bookType = specializedBookManager.getBookTypeFromItem(item)
        return if (bookType != null) {
            "${item.type}:${item.enchantments.hashCode()}:${bookType.id}"
        } else {
            "${item.type}:${item.enchantments.hashCode()}"
        }
    }

    private fun showTierEnchantments(slots: List<Int>, tier: Int) {
        val enchantment = offeredEnchantments[tier]
        val inputItem = inventory.getItem(inputSlotNumber)

        val isSpecializedBook = inputItem != null && inputItem.type != Material.AIR &&
                inputItem.itemMeta?.persistentDataContainer?.has(
                    NamespacedKey(plugin, "book_type"), PersistentDataType.STRING
                ) == true

        if (enchantment != null) {
            val cost = enchantmentSystem.calculateEnchantCost(enchantment, enchantment.level)
            val requiredLevel = enchantmentSystem.getRequiredLevel(tier)
            val canAfford = plugin.vaultIntegration.hasEnough(player, cost) && player.level >= requiredLevel

            if (!isSpecializedBook && inputItem != null && inputItem.type == Material.BOOK) {

                if (canAfford) {
                    slots.forEach { slot ->
                        val item = createEnchantmentDisplayItem(enchantment, tier)
                        inventory.setItem(slot, item)
                    }
                } else {
                    val unavailableItem = createUnavailableItem(cost, requiredLevel)
                    slots.forEach { slot ->
                        inventory.setItem(slot, unavailableItem.clone())
                    }
                }
                return
            }

            if (canAfford) {
                slots.forEach { slot ->
                    val item = createEnchantmentDisplayItem(enchantment, tier)
                    inventory.setItem(slot, item)
                }
            } else {
                val unavailableItem = createUnavailableItem(cost, requiredLevel)
                slots.forEach { slot ->
                    inventory.setItem(slot, unavailableItem.clone())
                }
            }
        } else {
            val emptyItem = createEmptySlotItem()
            slots.forEach { slot ->
                inventory.setItem(slot, emptyItem.clone())
            }
        }
    }

    override fun handleClick(slot: Int, item: ItemStack?, player: Player) {
        if (item == null) return

        val unavailableSection = plugin.config.getConfigurationSection("enchantment_option_unavailable")
        if (unavailableSection != null) {
            val materialName = unavailableSection.getString("material", "BARRIER")
            val customModelData = unavailableSection.getInt("custom_model_data", 0)
            if (item.type.name == materialName && item.itemMeta?.customModelData == customModelData) {
                return
            }
        }

        val allTierSlots = tier1Slots + tier2Slots + tier3Slots
        if (slot in allTierSlots) {
            handleEnchantmentClick(slot, item, player)
        }
    }

    private fun handleEnchantmentClick(slot: Int, item: ItemStack, player: Player) {
        val meta = item.itemMeta ?: return
        val container = meta.persistentDataContainer
        val enchantKey = NamespacedKey(plugin, "enchantment_id")
        val enchantId = container.get(enchantKey, PersistentDataType.STRING) ?: return

        val bookItem = inventory.getItem(inputSlotNumber) ?: return

        val tier = when (slot) {
            in tier1Slots -> 1
            in tier2Slots -> 2
            in tier3Slots -> 3
            else -> return
        }

        val enchantment = offeredEnchantments[tier] ?: return
        if (enchantment.id != enchantId) return

        val cost = plugin.enchantmentSystem.calculateEnchantCost(enchantment, enchantment.level)
        if (!plugin.vaultIntegration.hasEnough(player, cost)) {
            player.sendMessage(ChatUtils.parse("<red>You don't have enough money!</red>"))
            return
        }

        val requiredLevel = enchantmentSystem.getRequiredLevel(tier)
        if (player.level < requiredLevel) {
            player.sendMessage(ChatUtils.parse("<red>You don't have enough experience levels!</red>"))
            return
        }

        plugin.vaultIntegration.withdrawPlayer(player, cost)
        player.level -= requiredLevel

        val enchantedBook = enchantmentSystem.createEnchantedBook(
            bookItem,
            enchantment,
            enchantment.level,
            specializedBookManager.getBookTypeFromItem(bookItem)
        )

        inventory.setItem(inputSlotNumber, null)
        offeredEnchantments = emptyMap()
        lastInputKey = null
        if (specializedBookManager.getBookTypeFromItem(bookItem) == null) {
            listener.playerOfferedEnchantments.remove(player.uniqueId)
        }
        safeRefresh()
        player.inventory.addItem(enchantedBook)

        player.sendMessage(ChatUtils.parse("<green>Enchantment applied successfully!</green>"))
        playSound("enchant_success")
    }

    fun hasItemInInputSlot(): Boolean {
        val inputItem = inventory.getItem(inputSlotNumber)
        return inputItem != null && inputItem.type != org.bukkit.Material.AIR
    }

    fun getOfferedEnchantments(): Map<Int, CustomEnchantment> {
        return offeredEnchantments
    }
}

