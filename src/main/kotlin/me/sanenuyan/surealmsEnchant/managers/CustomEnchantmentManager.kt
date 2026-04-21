package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.models.CustomEnchantment
import me.sanenuyan.surealmsEnchant.models.EnchantmentRarity
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment

/**
 * Manages all custom and vanilla enchantments in the system
 */
class CustomEnchantmentManager(private val plugin: SurealmsEnchant) {

    private val enchantments = mutableMapOf<String, CustomEnchantment>()
    init {
        initializeEnchantments()
    }
    /**
     * Initialize all enchantments (vanilla + custom)
     */

    private fun getVanillaEnchantmentConfig(key: String): Pair<String, String> {
        val section = plugin.config.getConfigurationSection("vanilla-enchantments.$key")
        val name = section?.getString("name") ?: key.replace('_', ' ').capitalize()
        val description = section?.getString("description") ?: ""
        return name to description
    }
    private fun initializeEnchantments() {
        // Universal Enchantments (can be applied to many items)
        initializeUniversalEnchantments()

        // Weapon Enchantments
        initializeWeaponEnchantments()

        // Tool Enchantments
        initializeToolEnchantments()

        // Armor Enchantments
        initializeArmorEnchantments()

        // Bow Enchantments
        initializeBowEnchantments()

        // Crossbow Enchantments
        initializeCrossbowEnchantments()

        // Trident Enchantments
        initializeTridentEnchantments()

        // Fishing Rod Enchantments
        initializeFishingRodEnchantments()

        // Custom Enchantments (now handled by CustomEnchantmentRegistry)
        // initializeCustomEnchantments()
    }

    /**
     * Initializes enchantments that can be applied to a wide variety of items.
     */
    private fun initializeUniversalEnchantments() {
        val (unbreakingName, unbreakingDesc) = getVanillaEnchantmentConfig("unbreaking")
        for (level in 1..3) {
            val tier = level
            registerEnchantment(CustomEnchantment("unbreaking_$level", unbreakingName, listOf(unbreakingDesc), Enchantment.UNBREAKING, level, 3, tier, getAllDurableItems(), emptyList(), 1.5, EnchantmentRarity.UNCOMMON))
        }
        val (mendingName, mendingDesc) = getVanillaEnchantmentConfig("mending")
        registerEnchantment(CustomEnchantment("mending_1", mendingName, listOf(mendingDesc), Enchantment.MENDING, 1, 1, 3, getAllDurableItems(), listOf(Enchantment.INFINITY), 1.0, EnchantmentRarity.MYTHIC))
        val (vanishingCurseName, vanishingCurseDesc) = getVanillaEnchantmentConfig("vanishing_curse")
        registerEnchantment(CustomEnchantment("vanishing_curse_1", vanishingCurseName, listOf(vanishingCurseDesc), Enchantment.VANISHING_CURSE, 1, 1, 3, getAllDurableItems(), emptyList(), 5.0, EnchantmentRarity.EPIC))
    }

    /**
     * Initialize weapon enchantments (Swords and Axes)
     */
    private fun initializeWeaponEnchantments() {
        val weaponMaterials = getSwordMaterials() + getAxeMaterials()
        val (sharpnessName, sharpnessDesc) = getVanillaEnchantmentConfig("sharpness")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("sharpness_$level", sharpnessName, listOf(sharpnessDesc), Enchantment.SHARPNESS, level, 5, tier, weaponMaterials, listOf(Enchantment.SMITE, Enchantment.BANE_OF_ARTHROPODS), 1.0, EnchantmentRarity.COMMON))
        }
        val (smiteName, smiteDesc) = getVanillaEnchantmentConfig("smite")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("smite_$level", smiteName, listOf(smiteDesc), Enchantment.SMITE, level, 5, tier, weaponMaterials, listOf(Enchantment.SHARPNESS, Enchantment.BANE_OF_ARTHROPODS), 1.0, EnchantmentRarity.COMMON))
        }
        val (baneName, baneDesc) = getVanillaEnchantmentConfig("bane_of_arthropods")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("bane_of_arthropods_$level", baneName, listOf(baneDesc), Enchantment.BANE_OF_ARTHROPODS, level, 5, tier, weaponMaterials, listOf(Enchantment.SHARPNESS, Enchantment.SMITE), 1.0, EnchantmentRarity.COMMON))
        }
        val (knockbackName, knockbackDesc) = getVanillaEnchantmentConfig("knockback")
        for (level in 1..2) {
            val tier = level
            registerEnchantment(CustomEnchantment("knockback_$level", knockbackName, listOf(knockbackDesc), Enchantment.KNOCKBACK, level, 2, tier, getSwordMaterials(), emptyList(), 1.2, EnchantmentRarity.UNCOMMON))
        }
        val (fireAspectName, fireAspectDesc) = getVanillaEnchantmentConfig("fire_aspect")
        for (level in 1..2) {
            val tier = level
            registerEnchantment(CustomEnchantment("fire_aspect_$level", fireAspectName, listOf(fireAspectDesc), Enchantment.FIRE_ASPECT, level, 2, tier, getSwordMaterials(), emptyList(), 1.8, EnchantmentRarity.RARE))
        }
        val (lootingName, lootingDesc) = getVanillaEnchantmentConfig("looting")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("looting_$level", lootingName, listOf(lootingDesc), Enchantment.LOOTING, level, 3, level, getSwordMaterials(), emptyList(), 2.0, if (level == 3) EnchantmentRarity.EPIC else EnchantmentRarity.RARE))
        }
        val (sweepingEdgeName, sweepingEdgeDesc) = getVanillaEnchantmentConfig("sweeping_edge")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("sweeping_edge_$level", sweepingEdgeName, listOf(sweepingEdgeDesc), Enchantment.SWEEPING_EDGE, level, 3, level, getSwordMaterials(), emptyList(), 2.5, when (level) { 1,2 -> EnchantmentRarity.EPIC; 3 -> EnchantmentRarity.MYTHIC else -> EnchantmentRarity.EPIC }))
        }
    }

    /**
     * Initialize tool enchantments
     */
    private fun initializeToolEnchantments() {
        val miningTools = getPickaxeMaterials() + getAxeMaterials() + getShovelMaterials()
        val (efficiencyName, efficiencyDesc) = getVanillaEnchantmentConfig("efficiency")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("efficiency_$level", efficiencyName, listOf(efficiencyDesc), Enchantment.EFFICIENCY, level, 5, tier, getToolMaterials(), emptyList(), 1.0, EnchantmentRarity.COMMON))
        }
        val (fortuneName, fortuneDesc) = getVanillaEnchantmentConfig("fortune")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("fortune_$level", fortuneName, listOf(fortuneDesc), Enchantment.FORTUNE, level, 3, level, miningTools, listOf(Enchantment.SILK_TOUCH), 2.0, EnchantmentRarity.RARE))
        }
        val (silkTouchName, silkTouchDesc) = getVanillaEnchantmentConfig("silk_touch")
        registerEnchantment(CustomEnchantment("silk_touch_1", silkTouchName, listOf(silkTouchDesc), Enchantment.SILK_TOUCH, 1, 1, 3, miningTools, listOf(Enchantment.FORTUNE), 3.0, EnchantmentRarity.LEGENDARY))
    }

    /**
     * Initialize armor enchantments
     */
    private fun initializeArmorEnchantments() {
        val armor = getArmorMaterials()
        val boots = getBootMaterials()
        val (protectionName, protectionDesc) = getVanillaEnchantmentConfig("protection")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            val rarity = if (level == 4) EnchantmentRarity.EPIC else EnchantmentRarity.COMMON
            val cost = if (level == 4) 3.0 else 1.0
            registerEnchantment(CustomEnchantment("protection_$level", protectionName, listOf(protectionDesc), Enchantment.PROTECTION, level, 4, tier, armor, listOf(Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION), cost, rarity))
        }
        val (fireProtectionName, fireProtectionDesc) = getVanillaEnchantmentConfig("fire_protection")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            val rarity = if (level == 4) EnchantmentRarity.EPIC else EnchantmentRarity.UNCOMMON
            val cost = if (level == 4) 3.0 else 1.2
            registerEnchantment(CustomEnchantment("fire_protection_$level", fireProtectionName, listOf(fireProtectionDesc), Enchantment.FIRE_PROTECTION, level, 4, tier, armor, listOf(Enchantment.PROTECTION, Enchantment.BLAST_PROTECTION, Enchantment.PROJECTILE_PROTECTION), cost, rarity))
        }
        val (blastProtectionName, blastProtectionDesc) = getVanillaEnchantmentConfig("blast_protection")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            val rarity = if (level == 4) EnchantmentRarity.EPIC else EnchantmentRarity.UNCOMMON
            val cost = if (level == 4) 3.0 else 1.5
            registerEnchantment(CustomEnchantment("blast_protection_$level", blastProtectionName, listOf(blastProtectionDesc), Enchantment.BLAST_PROTECTION, level, 4, tier, armor, listOf(Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.PROJECTILE_PROTECTION), cost, rarity))
        }
        val (projectileProtectionName, projectileProtectionDesc) = getVanillaEnchantmentConfig("projectile_protection")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            val rarity = if (level == 4) EnchantmentRarity.EPIC else EnchantmentRarity.UNCOMMON
            val cost = if (level == 4) 3.0 else 1.2
            registerEnchantment(CustomEnchantment("projectile_protection_$level", projectileProtectionName, listOf(projectileProtectionDesc), Enchantment.PROJECTILE_PROTECTION, level, 4, tier, armor, listOf(Enchantment.PROTECTION, Enchantment.FIRE_PROTECTION, Enchantment.BLAST_PROTECTION), cost, rarity))
        }
        val (featherFallingName, featherFallingDesc) = getVanillaEnchantmentConfig("feather_falling")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            val rarity = if (level == 4) EnchantmentRarity.EPIC else EnchantmentRarity.UNCOMMON
            val cost = if (level == 4) 2.5 else 1.2
            registerEnchantment(CustomEnchantment("feather_falling_$level", featherFallingName, listOf(featherFallingDesc), Enchantment.FEATHER_FALLING, level, 4, tier, boots, emptyList(), cost, rarity))
        }
        val (thornsName, thornsDesc) = getVanillaEnchantmentConfig("thorns")
        for (level in 1..3) {
            val rarity = if (level == 3) EnchantmentRarity.EPIC else EnchantmentRarity.RARE
            val cost = if (level == 3) 3.0 else 2.0
            registerEnchantment(CustomEnchantment("thorns_$level", thornsName, listOf(thornsDesc), Enchantment.THORNS, level, 3, level, getChestplateMaterials(), emptyList(), cost, rarity))
        }
        val (respirationName, respirationDesc) = getVanillaEnchantmentConfig("respiration")
        for (level in 1..3) {
            val rarity = if (level == 3) EnchantmentRarity.EPIC else EnchantmentRarity.RARE
            val cost = if (level == 3) 2.5 else 1.8
            registerEnchantment(CustomEnchantment("respiration_$level", respirationName, listOf(respirationDesc), Enchantment.RESPIRATION, level, 3, level, getHelmetMaterials(), emptyList(), cost, rarity))
        }
        val (depthStriderName, depthStriderDesc) = getVanillaEnchantmentConfig("depth_strider")
        for (level in 1..3) {
            val rarity = if (level == 3) EnchantmentRarity.EPIC else EnchantmentRarity.RARE
            val cost = if (level == 3) 2.5 else 1.8
            registerEnchantment(CustomEnchantment("depth_strider_$level", depthStriderName, listOf(depthStriderDesc), Enchantment.DEPTH_STRIDER, level, 3, level, boots, listOf(Enchantment.FROST_WALKER), cost, rarity))
        }
        val (aquaAffinityName, aquaAffinityDesc) = getVanillaEnchantmentConfig("aqua_affinity")
        registerEnchantment(CustomEnchantment("aqua_affinity_1", aquaAffinityName, listOf(aquaAffinityDesc), Enchantment.AQUA_AFFINITY, 1, 1, 2, getHelmetMaterials(), emptyList(), 1.5, EnchantmentRarity.RARE))
        val (frostWalkerName, frostWalkerDesc) = getVanillaEnchantmentConfig("frost_walker")
        for (level in 1..2) {
            val rarity = if (level == 2) EnchantmentRarity.MYTHIC else EnchantmentRarity.EPIC
            val cost = if (level == 2) 3.0 else 2.5
            registerEnchantment(CustomEnchantment("frost_walker_$level", frostWalkerName, listOf(frostWalkerDesc), Enchantment.FROST_WALKER, level, 2, level + 1, boots, listOf(Enchantment.DEPTH_STRIDER), cost, rarity))
        }
        val (soulSpeedName, soulSpeedDesc) = getVanillaEnchantmentConfig("soul_speed")
        for (level in 1..3) {
            val rarity = if (level == 3) EnchantmentRarity.MYTHIC else EnchantmentRarity.EPIC
            val cost = if (level == 3) 3.5 else 3.0
            registerEnchantment(CustomEnchantment("soul_speed_$level", soulSpeedName, listOf(soulSpeedDesc), Enchantment.SOUL_SPEED, level, 3, level, boots, emptyList(), cost, rarity))
        }
        val (bindingCurseName, bindingCurseDesc) = getVanillaEnchantmentConfig("binding_curse")
        registerEnchantment(CustomEnchantment("binding_curse_1", bindingCurseName, listOf(bindingCurseDesc), Enchantment.BINDING_CURSE, 1, 1, 3, armor, emptyList(), 5.0, EnchantmentRarity.EPIC))
    }

    /**
     * Initialize bow enchantments
     */

    private fun initializeBowEnchantments() {
        val bow = listOf(Material.BOW)
        val (powerName, powerDesc) = getVanillaEnchantmentConfig("power")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            val rarity = if (level == 5) EnchantmentRarity.EPIC else EnchantmentRarity.COMMON
            val cost = if (level == 5) 3.0 else 1.0
            registerEnchantment(CustomEnchantment("power_$level", powerName, listOf(powerDesc), Enchantment.POWER, level, 5, tier, bow, emptyList(), cost, rarity))
        }
        val (punchName, punchDesc) = getVanillaEnchantmentConfig("punch")
        for (level in 1..2) {
            val rarity = if (level == 2) EnchantmentRarity.RARE else EnchantmentRarity.UNCOMMON
            val cost = if (level == 2) 2.0 else 1.5
            registerEnchantment(CustomEnchantment("punch_$level", punchName, listOf(punchDesc), Enchantment.PUNCH, level, 2, level, bow, emptyList(), cost, rarity))
        }
        val (flameName, flameDesc) = getVanillaEnchantmentConfig("flame")
        registerEnchantment(CustomEnchantment("flame_1", flameName, listOf(flameDesc), Enchantment.FLAME, 1, 1, 2, bow, emptyList(), 2.0, EnchantmentRarity.RARE))
        val (infinityName, infinityDesc) = getVanillaEnchantmentConfig("infinity")
        registerEnchantment(CustomEnchantment("infinity_1", infinityName, listOf(infinityDesc), Enchantment.INFINITY, 1, 1, 3, bow, listOf(Enchantment.MENDING), 4.0, EnchantmentRarity.LEGENDARY))
    }

    /**
     * Initialize crossbow enchantments
     */
    private fun initializeCrossbowEnchantments() {
        val crossbow = listOf(Material.CROSSBOW)
        val (quickChargeName, quickChargeDesc) = getVanillaEnchantmentConfig("quick_charge")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("quick_charge_$level", quickChargeName, listOf(quickChargeDesc), Enchantment.QUICK_CHARGE, level, 3, level, crossbow, emptyList(), 1.2, EnchantmentRarity.UNCOMMON))
        }
        val (multishotName, multishotDesc) = getVanillaEnchantmentConfig("multishot")
        registerEnchantment(CustomEnchantment("multishot_1", multishotName, listOf(multishotDesc), Enchantment.MULTISHOT, 1, 1, 2, crossbow, listOf(Enchantment.PIERCING), 2.0, EnchantmentRarity.RARE))
        val (piercingName, piercingDesc) = getVanillaEnchantmentConfig("piercing")
        for (level in 1..4) {
            val tier = when (level) {
                1, 2 -> 1
                3 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("piercing_$level", piercingName, listOf(piercingDesc), Enchantment.PIERCING, level, 4, tier, crossbow, listOf(Enchantment.MULTISHOT), 2.0, EnchantmentRarity.RARE))
        }
    }
    /**
     * Initialize trident enchantments
     */
    private fun initializeTridentEnchantments() {
        val trident = listOf(Material.TRIDENT)
        val (impalingName, impalingDesc) = getVanillaEnchantmentConfig("impaling")
        for (level in 1..5) {
            val tier = when (level) {
                1, 2 -> 1
                3, 4 -> 2
                else -> 3
            }
            registerEnchantment(CustomEnchantment("impaling_$level", impalingName, listOf(impalingDesc), Enchantment.IMPALING, level, 5, tier, trident, emptyList(), 1.5, EnchantmentRarity.UNCOMMON))
        }
        val (loyaltyName, loyaltyDesc) = getVanillaEnchantmentConfig("loyalty")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("loyalty_$level", loyaltyName, listOf(loyaltyDesc), Enchantment.LOYALTY, level, 3, level, trident, listOf(Enchantment.RIPTIDE), 2.0, EnchantmentRarity.RARE))
        }
        val (channelingName, channelingDesc) = getVanillaEnchantmentConfig("channeling")
        registerEnchantment(CustomEnchantment("channeling_1", channelingName, listOf(channelingDesc), Enchantment.CHANNELING, 1, 1, 3, trident, listOf(Enchantment.RIPTIDE), 3.0, EnchantmentRarity.EPIC))
        val (riptideName, riptideDesc) = getVanillaEnchantmentConfig("riptide")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("riptide_$level", riptideName, listOf(riptideDesc), Enchantment.RIPTIDE, level, 3, level, trident, listOf(Enchantment.LOYALTY, Enchantment.CHANNELING), 2.5, EnchantmentRarity.EPIC))
        }
    }

    /**
     * Initialize fishing rod enchantments
     */
    private fun initializeFishingRodEnchantments() {
        val fishingRod = listOf(Material.FISHING_ROD)
        val (lureName, lureDesc) = getVanillaEnchantmentConfig("lure")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("lure_$level", lureName, listOf(lureDesc), Enchantment.LURE, level, 3, level, fishingRod, emptyList(), 1.5, EnchantmentRarity.UNCOMMON))
        }
        val (luckOfTheSeaName, luckOfTheSeaDesc) = getVanillaEnchantmentConfig("luck_of_the_sea")
        for (level in 1..3) {
            registerEnchantment(CustomEnchantment("luck_of_the_sea_$level", luckOfTheSeaName, listOf(luckOfTheSeaDesc), Enchantment.LUCK_OF_THE_SEA, level, 3, level, fishingRod, emptyList(), 2.0, EnchantmentRarity.RARE))
        }
    }

    /**
     * Register a new enchantment
     */
    fun registerEnchantment(enchantment: CustomEnchantment) {
        enchantments[enchantment.id] = enchantment
    }

    /**
     * Get all enchantments
     */
    fun getAllEnchantments(): Collection<CustomEnchantment> = enchantments.values

    /**
     * Get enchantment by ID
     */
    fun getEnchantmentById(id: String): CustomEnchantment? = enchantments[id]

    /**
     * Get enchantments by tier
     */
    fun getEnchantmentsByTier(tier: Int): List<CustomEnchantment> {
        return when (tier) {
            3 -> enchantments.values.filter { it.tier == 3 || it.tier == 2 }
            else -> enchantments.values.filter { it.tier == tier }
        }
    }

    /**
     * Get enchantments compatible with a specific material
     */
    fun getCompatibleEnchantments(material: Material): List<CustomEnchantment> {
        return enchantments.values.filter { it.compatibleItems.contains(material) }
    }

    /**
     * Get all vanilla enchantments as CustomEnchantment objects
     */
    fun getAllVanillaEnchantments(): List<CustomEnchantment> {
        return enchantments.values.filter { !it.isCustom }
    }

    // Helper methods for material lists
    private fun getSwordMaterials() = listOf(Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD)
    private fun getPickaxeMaterials() = listOf(Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE)
    private fun getAxeMaterials() = listOf(Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE)
    private fun getShovelMaterials() = listOf(Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL)
    private fun getHoeMaterials() = listOf(Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE)
    private fun getToolMaterials() = getPickaxeMaterials() + getAxeMaterials() + getShovelMaterials() + getHoeMaterials()

    private fun getHelmetMaterials() = listOf(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET)
    private fun getChestplateMaterials() = listOf(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE)
    private fun getLeggingMaterials() = listOf(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS)
    private fun getBootMaterials() = listOf(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS)
    private fun getArmorMaterials() = getHelmetMaterials() + getChestplateMaterials() + getLeggingMaterials() + getBootMaterials()

    private fun getAllDurableItems() = getSwordMaterials() + getToolMaterials() + getArmorMaterials() + listOf(
        Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHIELD, Material.FISHING_ROD,
        Material.FLINT_AND_STEEL, Material.ELYTRA, Material.SHEARS,
        Material.CARROT_ON_A_STICK, Material.WARPED_FUNGUS_ON_A_STICK
    )
}
