package me.sanenuyan.surealmsEnchant.integrations

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUI
import me.sanenuyan.surealmsEnchant.gui.holders.EnchantingTableGUIHolder
import org.bukkit.entity.Player

/**
 * PlaceholderAPI integration for SurealmsEnchant
 * Provides placeholders for enchantment GUI states and current enchantments
 */
class PlaceholderAPIIntegration(private val plugin: SurealmsEnchant) : PlaceholderExpansion() {
    
    // store player gui states
    private val playerGUIStates = mutableMapOf<Player, GUIState>()
    
    data class GUIState(
        val tier1Available: Boolean = false,
        val tier2Available: Boolean = false,
        val tier3Available: Boolean = false,
        val tier1Enchant: String = "",
        val tier2Enchant: String = "",
        val tier3Enchant: String = "",
        val bookInSlot: Boolean = false,
        val playerLevel: Int = 0,
        val playerMoney: Double = 0.0
    )
    
    override fun getIdentifier(): String = "surealmsenchant"
    
    override fun getAuthor(): String = "SanenuYan"
    
    override fun getVersion(): String = plugin.description.version
    
    override fun persist(): Boolean = true
    
    override fun canRegister(): Boolean = true
    
    /**
     * Handle placeholder requests
     */
    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) return null
        
        val state = getPlayerGUIState(player)
        
        return when (params.lowercase()) {
            // Tier availability placeholders
            "tier1_available" -> state.tier1Available.toString()
            "tier2_available" -> state.tier2Available.toString()
            "tier3_available" -> state.tier3Available.toString()
            
            // Current enchantment placeholders
            "tier1_enchant" -> state.tier1Enchant
            "tier2_enchant" -> state.tier2Enchant
            "tier3_enchant" -> state.tier3Enchant
            
            // GUI state placeholders
            "book_in_slot" -> state.bookInSlot.toString()
            "player_level" -> state.playerLevel.toString()
            "player_money" -> plugin.vaultIntegration.format(state.playerMoney)
            
            // Tier availability with colors
            "tier1_status" -> if (state.tier1Available) "§aAvailable" else "§cUnavailable"
            "tier2_status" -> if (state.tier2Available) "§bAvailable" else "§cUnavailable"
            "tier3_status" -> if (state.tier3Available) "§dAvailable" else "§cUnavailable"
            
            // Enchantment names with colors
            "tier1_enchant_colored" -> if (state.tier1Enchant.isNotEmpty()) "§a${state.tier1Enchant}" else "§7No enchantment"
            "tier2_enchant_colored" -> if (state.tier2Enchant.isNotEmpty()) "§b${state.tier2Enchant}" else "§7No enchantment"
            "tier3_enchant_colored" -> if (state.tier3Enchant.isNotEmpty()) "§d${state.tier3Enchant}" else "§7No enchantment"
            
            // Combined status
            "gui_status" -> if (state.bookInSlot) "§aReady" else "§ePlace a book"
            
            // Tier counts
            "available_tiers" -> listOf(state.tier1Available, state.tier2Available, state.tier3Available).count { it }.toString()
            "total_enchants" -> listOf(state.tier1Enchant, state.tier2Enchant, state.tier3Enchant).count { it.isNotEmpty() }.toString()

            // Total enchantments in system
            "total" -> getTotalEnchantmentsCount().toString()
            "total_available" -> getTotalAvailableEnchantments().toString()
            "total_builtin" -> plugin.customEnchantmentManager.getAllEnchantments().size.toString()
            "total_excellent" -> plugin.excellentEnchantsIntegration.getAvailableEnchantments().size.toString()

            else -> null
        }
    }
    
    /**
     * Get current GUI state for player
     */
    private fun getPlayerGUIState(player: Player): GUIState {
        val gui = plugin.enchantingTableGUIManager.getGUI(player)

        if (gui == null) {
            // Player doesn't have GUI open, return cached state or default
            return playerGUIStates[player] ?: GUIState(
                playerLevel = player.level,
                playerMoney = plugin.vaultIntegration.getBalance(player)
            )
        }

        val bookInSlot = gui.hasItemInInputSlot()
        val offeredEnchantments = gui.getOfferedEnchantments()

        val tier1Enchant = offeredEnchantments[1]?.name ?: ""
        val tier2Enchant = offeredEnchantments[2]?.name ?: ""
        val tier3Enchant = offeredEnchantments[3]?.name ?: ""

        val state = GUIState(
            tier1Available = tier1Enchant.isNotEmpty(),
            tier2Available = tier2Enchant.isNotEmpty(),
            tier3Available = tier3Enchant.isNotEmpty(),
            tier1Enchant = tier1Enchant,
            tier2Enchant = tier2Enchant,
            tier3Enchant = tier3Enchant,
            bookInSlot = bookInSlot,
            playerLevel = player.level,
            playerMoney = plugin.vaultIntegration.getBalance(player)
        )

        // cache state
        playerGUIStates[player] = state

        return state
    }
    
    /**
     * Extract enchantment name from GUI slot
     */
    private fun getEnchantmentNameFromSlot(inventory: org.bukkit.inventory.Inventory, slot: Int): String {
        val item = inventory.getItem(slot) ?: return ""
        val meta = item.itemMeta ?: return ""
        val displayName = meta.displayName ?: return ""
        
        // Remove color codes and extract clean name
        return displayName.replace("§[0-9a-fk-or]".toRegex(), "")
    }
    
    /**
     * Update player GUI state (called from GUI when state changes)
     */
    fun updatePlayerState(player: Player) {
        // Force refresh of player state
        getPlayerGUIState(player)
    }
    
    /**
     * Clear player state when GUI closes
     */
    fun clearPlayerState(player: Player) {
        playerGUIStates.remove(player)
    }
    
    /**
     * Set specific enchantment for tier (called when enchantment is selected)
     */
    fun setTierEnchantment(player: Player, tier: Int, enchantmentName: String) {
        val currentState = playerGUIStates[player] ?: getPlayerGUIState(player)
        
        val newState = when (tier) {
            1 -> currentState.copy(tier1Enchant = enchantmentName, tier1Available = enchantmentName.isNotEmpty())
            2 -> currentState.copy(tier2Enchant = enchantmentName, tier2Available = enchantmentName.isNotEmpty())
            3 -> currentState.copy(tier3Enchant = enchantmentName, tier3Available = enchantmentName.isNotEmpty())
            else -> currentState
        }
        
        playerGUIStates[player] = newState
    }
    
    /**
     * Parse placeholders in text
     */
    fun parsePlaceholders(player: Player, text: String): String {
        if (!isEnabled()) return text
        
        var result = text
        
        // replace placholder in gui
        val state = getPlayerGUIState(player)
        
        result = result.replace("%surealmsenchant_tier1_available%", state.tier1Available.toString())
        result = result.replace("%surealmsenchant_tier2_available%", state.tier2Available.toString())
        result = result.replace("%surealmsenchant_tier3_available%", state.tier3Available.toString())
        result = result.replace("%surealmsenchant_tier1_enchant%", state.tier1Enchant)
        result = result.replace("%surealmsenchant_tier2_enchant%", state.tier2Enchant)
        result = result.replace("%surealmsenchant_tier3_enchant%", state.tier3Enchant)
        result = result.replace("%surealmsenchant_book_in_slot%", state.bookInSlot.toString())
        result = result.replace("%surealmsenchant_player_level%", state.playerLevel.toString())
        result = result.replace("%surealmsenchant_player_money%", plugin.vaultIntegration.format(state.playerMoney))
        
        // Use PlaceholderAPI for other placeholders
        return try {
            me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, result)
        } catch (e: Exception) {
            result
        }
    }
    
    /**
     * Check if PlaceholderAPI is enabled
     */
    fun isEnabled(): Boolean {
        return try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Get total number of enchantments in the system
     */
    private fun getTotalEnchantmentsCount(): Int {
        return try {
            val builtInCount = plugin.customEnchantmentManager.getAllEnchantments().size
            val excellentCount = plugin.excellentEnchantsIntegration.getAvailableEnchantments().size
            plugin.logger.info("Total enchants: builtin=$builtInCount, excellent=$excellentCount")
            builtInCount + excellentCount
        } catch (e: Exception) {
            plugin.logger.warning("Error calculating total enchantments: ${e.message}")
            0
        }
    }

    /**
     * Get total number of available enchantments (non-duplicate)
     */
    private fun getTotalAvailableEnchantments(): Int {
        return try {
            val allEnchants = mutableSetOf<String>()

            // add built-in enchantments
            plugin.customEnchantmentManager.getAllEnchantments().forEach { enchant ->
                allEnchants.add(enchant.id)
            }

            // add ExcellentEnchants
            plugin.excellentEnchantsIntegration.getAvailableEnchantments().forEach { enchant ->
                // get enchant ID from ExcellentEnchant object
                try {
                    val idMethod = enchant.javaClass.getMethod("getId")
                    val id = idMethod.invoke(enchant) as String
                    allEnchants.add(id)
                } catch (e: Exception) {
                    //
                    allEnchants.add(enchant.toString())
                }
            }

            plugin.logger.info("Total unique enchants: ${allEnchants.size}")
            allEnchants.size
        } catch (e: Exception) {
            plugin.logger.warning("Error calculating available enchantments: ${e.message}")
            0
        }
    }
}
