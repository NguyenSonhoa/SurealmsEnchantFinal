package me.sanenuyan.surealmsEnchant.gui

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.gui.holders.EnchantIndexGUIHolder
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.models.CustomEnchantment
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ConfigurableEnchantIndexGUI(
    plugin: SurealmsEnchant,
    player: Player,
    private val customEnchantmentManager: CustomEnchantmentManager
) : ConfigurableGUI(plugin, "enchant_index", player) {

    companion object {
        const val GUI_TYPE_ID = "ENCHANT_INDEX"
    }

    private var currentPage = 0
    private var currentFilter = FilterType.ALL
    private var searchQuery = ""
    private var filteredEnchantments = listOf<CustomEnchantment>()

    enum class FilterType {
        ALL, TIER_1, TIER_2, TIER_3, EXCELLENT_ONLY
    }

    override fun setupStaticItems() {

        val filterAllItem = createItem("filter_all")
        val filterAllSlot = guiConfig.getItemConfig(guiType, "filter_all")?.slot ?: 9
        setItem(filterAllSlot, filterAllItem)

        val filterTier1Item = createItem("filter_tier_1")
        val filterTier1Slot = guiConfig.getItemConfig(guiType, "filter_tier_1")?.slot ?: 10
        setItem(filterTier1Slot, filterTier1Item)

        val filterTier2Item = createItem("filter_tier_2")
        val filterTier2Slot = guiConfig.getItemConfig(guiType, "filter_tier_2")?.slot ?: 11
        setItem(filterTier2Slot, filterTier2Item)

        val filterTier3Item = createItem("filter_tier_3")
        val filterTier3Slot = guiConfig.getItemConfig(guiType, "filter_tier_3")?.slot ?: 12
        setItem(filterTier3Slot, filterTier3Item)

        val filterExcellentItem = createItem("filter_excellent")
        val filterExcellentSlot = guiConfig.getItemConfig(guiType, "filter_excellent")?.slot ?: 13
        setItem(filterExcellentSlot, filterExcellentItem)

        val searchItem = createItem("search_button")
        val searchSlot = guiConfig.getItemConfig(guiType, "search_button")?.slot ?: 14
        setItem(searchSlot, searchItem)

        val backItem = createItem("back_button")
        val backSlot = guiConfig.getItemConfig(guiType, "back_button")?.slot ?: 45
        setItem(backSlot, backItem)

        val closeButtonConfig = guiConfig.getItemConfig(guiType, "close_button")
        if (closeButtonConfig != null) {
            val closeItem = createItem("close_button")
            setItem(closeButtonConfig.slot, closeItem)
        }

        updatePaginationItems()
    }

    override fun setupDynamicContent() {

        updateFilteredEnchantments()

        val contentSlots = getSlots("content_slots")
        contentSlots.forEach { slot ->
            inventory.setItem(slot, null)
        }

        displayEnchantments()
    }

    fun refreshGUI() {

        updateFilteredEnchantments()

        refresh()
    }

    override fun getGUITypeId(): String = GUI_TYPE_ID

    override fun initialize() {

        val holder = EnchantIndexGUIHolder(plugin, player)
        val title = guiConfig.getTitle(guiType, player)
        val size = guiConfig.getSize(guiType)
        val customInventory = Bukkit.createInventory(holder, size, title)
        holder.setInventory(customInventory)

        val inventoryField = this.javaClass.superclass.getDeclaredField("inventory")
        inventoryField.isAccessible = true
        inventoryField.set(this, customInventory)

        setupItems()
    }

    override fun shouldCancelClick(slot: Int, item: ItemStack?, player: Player): Boolean {

        return true
    }

    private fun updateFilteredEnchantments() {
        val allEnchantments = getAllAvailableEnchantments()
        val groupedByName = allEnchantments.groupBy { it.name }
        val representativeEnchants = mutableListOf<CustomEnchantment>()

        for ((_, enchantLevels) in groupedByName) {
            val level1Enchant = enchantLevels.find { it.level == 1 }

            if (level1Enchant != null) {
                val matchesFilter = when (currentFilter) {
                    FilterType.ALL -> true
                    FilterType.TIER_1 -> level1Enchant.tier == 1
                    FilterType.TIER_2 -> level1Enchant.tier == 2
                    FilterType.TIER_3 -> level1Enchant.tier == 3
                    FilterType.EXCELLENT_ONLY -> level1Enchant.isExcellentEnchant
                }

                if (matchesFilter) {
                    representativeEnchants.add(level1Enchant)
                }
            }
        }

        filteredEnchantments = representativeEnchants.filter { enchant ->
            if (searchQuery.isEmpty()) {
                true
            } else {
                enchant.name.contains(searchQuery, ignoreCase = true) ||
                        enchant.id.contains(searchQuery, ignoreCase = true) ||
                        enchant.description.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }.sortedWith(compareBy({ it.tier }, { it.name }))
    }

    private fun getAllAvailableEnchantments(): List<CustomEnchantment> {
        val enchantments = mutableListOf<CustomEnchantment>()

        val builtInEnchants = customEnchantmentManager.getAllEnchantments()
        enchantments.addAll(builtInEnchants)

        if (plugin.excellentEnchantsIntegration.isEnabled()) {
            val excellentEnchants = plugin.excellentEnchantsIntegration.getAsCustomEnchantments()
            enchantments.addAll(excellentEnchants)
        }

        return enchantments
    }

    private fun displayEnchantments() {
        val contentSlots = getSlots("content_slots")
        val enchantsPerPage = guiConfig.getEnchantsPerPage()
        val startIndex = currentPage * enchantsPerPage
        val endIndex = minOf(startIndex + enchantsPerPage, filteredEnchantments.size)

        for (i in startIndex until endIndex) {
            val slotIndex = i - startIndex
            if (slotIndex < contentSlots.size) {
                val slot = contentSlots[slotIndex]
                val enchantment = filteredEnchantments[i]

                val enchantItem = createEnchantmentItem(enchantment, emptyList(), enchantment.level)
                setItem(slot, enchantItem)
            }
        }
    }

    private fun updatePaginationItems() {
        val enchantsPerPage = guiConfig.getEnchantsPerPage()
        val totalPages = if (filteredEnchantments.isEmpty()) 1 else
            (filteredEnchantments.size + enchantsPerPage - 1) / enchantsPerPage

        val prevSlot = guiConfig.getItemConfig(guiType, "previous_page")?.slot ?: 48
        if (currentPage > 0) {
            setItem(prevSlot, createItem("previous_page"))
        } else {
            inventory.setItem(prevSlot, null)
        }

        val pageInfoSlot = guiConfig.getItemConfig(guiType, "page_info")?.slot ?: 49
        val placeholders = mapOf(
            "current" to (currentPage + 1).toString(),
            "total" to totalPages.toString(),
            "count" to filteredEnchantments.size.toString()
        )
        setItem(pageInfoSlot, createItem("page_info", placeholders))

        val nextSlot = guiConfig.getItemConfig(guiType, "next_page")?.slot ?: 50
        if (currentPage < totalPages - 1) {
            setItem(nextSlot, createItem("next_page"))
        } else {
            inventory.setItem(nextSlot, null)
        }
    }

    override fun handleClick(slot: Int, item: ItemStack?, player: Player) {
        if (item == null) return

        val nbtId = getItemNBTId(item)

        when (nbtId) {
            "filter_all" -> {
                currentFilter = FilterType.ALL
                currentPage = 0
                refreshGUI()
                playSound("click")
            }
            "filter_tier_1" -> {
                currentFilter = FilterType.TIER_1
                currentPage = 0
                refreshGUI()
                playSound("click")
            }
            "filter_tier_2" -> {
                currentFilter = FilterType.TIER_2
                currentPage = 0
                refreshGUI()
                playSound("click")
            }
            "filter_tier_3" -> {
                currentFilter = FilterType.TIER_3
                currentPage = 0
                refreshGUI()
                playSound("click")
            }
            "filter_excellent" -> {
                currentFilter = FilterType.EXCELLENT_ONLY
                currentPage = 0
                refreshGUI()
                playSound("click")
            }
            "search_button" -> {
                player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.search_coming_soon"))
                playSound("click")
            }
            "back_button" -> {

                close()
                playSound("click")
            }
            "close_button" -> {
                close()
                playSound("click")
            }
            "previous_page" -> {
                if (currentPage > 0) {
                    currentPage--
                    refresh()
                    playSound("page_turn")
                }
            }
            "next_page" -> {
                val enchantsPerPage = guiConfig.getEnchantsPerPage()
                val totalPages = (filteredEnchantments.size + enchantsPerPage - 1) / enchantsPerPage
                if (currentPage < totalPages - 1) {
                    currentPage++
                    refresh()
                    playSound("page_turn")
                }
            }
            "enchantment_item" -> {

                val enchantId = getEnchantmentId(item)
                if (enchantId != null) {
                    val enchantment = filteredEnchantments.find { it.id == enchantId }
                    if (enchantment != null) {
                        showEnchantmentDetails(player, enchantment)
                    }
                }
                playSound("click")
            }
        }
    }

    private fun showEnchantmentDetails(player: Player, enchantment: CustomEnchantment) {
        val placeholders = mapOf(
            "name" to enchantment.displayName,
            "id" to enchantment.id,
            "tier" to enchantment.tier.toString(),
            "max_level" to enchantment.maxLevel.toString(),
            "source" to if (enchantment.isExcellentEnchant) "" else "",
            "rarity" to enchantment.rarity.displayName
        )

        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.header", placeholders))
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.id", placeholders))
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.tier", placeholders))
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.max_level", placeholders))
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.source", placeholders))
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.rarity", placeholders))

        if (enchantment.isTreasure) {
            player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.treasure_indicator"))
        }
        if (enchantment.isCursed) {
            player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.cursed_indicator"))
        }

        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.description_header"))
        enchantment.description.forEach { line ->
            player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.description_line", mapOf("line" to line)))
        }
        player.sendMessage(plugin.messageManager.getMessage("gui.enchant_index.details.footer"))
    }

    fun setSearchQuery(query: String) {
        searchQuery = query
        currentPage = 0
        refresh()
    }
}

