package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.util.Vector
import kotlin.math.cos

class CustomEnchantmentEffects(private val plugin: SurealmsEnchant) : Listener {
// it was beta but we closed this, since Quinx leave project.
    private val smeltingMap = mapOf(
        Material.IRON_ORE to Material.IRON_INGOT,
        Material.DEEPSLATE_IRON_ORE to Material.IRON_INGOT,
        Material.GOLD_ORE to Material.GOLD_INGOT,
        Material.DEEPSLATE_GOLD_ORE to Material.GOLD_INGOT,
        Material.COPPER_ORE to Material.COPPER_INGOT,
        Material.DEEPSLATE_COPPER_ORE to Material.COPPER_INGOT,
        Material.ANCIENT_DEBRIS to Material.NETHERITE_SCRAP,
        Material.COBBLESTONE to Material.STONE,
        Material.COBBLED_DEEPSLATE to Material.DEEPSLATE,
        Material.SAND to Material.GLASS,
        Material.RED_SAND to Material.GLASS,
        Material.CLAY to Material.TERRACOTTA,
        Material.NETHERRACK to Material.NETHER_BRICK,
        Material.CACTUS to Material.GREEN_DYE,
        Material.RAW_IRON to Material.IRON_INGOT,
        Material.RAW_GOLD to Material.GOLD_INGOT,
        Material.RAW_COPPER to Material.COPPER_INGOT
    )

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        val tool = player.inventory.itemInMainHand

        if (hasCustomEnchantment(tool, "auto_smelting")) {
            handleAutoSmelting(event, player, block.type, tool)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        val victim = event.entity as? LivingEntity ?: return
        val weapon = attacker.inventory.itemInMainHand

        val backstabLevel = getCustomEnchantmentLevel(weapon, "backstab")
        if (backstabLevel > 0) {
            handleBackstab(event, attacker, victim, backstabLevel)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onExpChange(event: PlayerExpChangeEvent) {
        val player = event.player
        val armor = player.inventory.armorContents

        var totalLuckyOrbLevel = 0
        for (armorPiece in armor) {
            if (armorPiece != null) {
                totalLuckyOrbLevel += getCustomEnchantmentLevel(armorPiece, "lucky_orb")
            }
        }

        if (totalLuckyOrbLevel > 0) {
            handleLuckyOrb(event, totalLuckyOrbLevel)
        }
    }

    private fun handleAutoSmelting(event: BlockBreakEvent, player: Player, blockType: Material, tool: ItemStack) {
        val smeltedMaterial = smeltingMap[blockType] ?: return

        if (!plugin.config.getBoolean("custom-enchantments.auto-smelting.enabled", true)) return

        val probability = plugin.config.getDouble("custom-enchantments.auto-smelting.probability", 100.0)
        if (Math.random() * 100 > probability) return

        event.isDropItems = false

        val fortuneLevel = tool.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.FORTUNE)
        val dropCount = calculateFortuneDrops(1, fortuneLevel)

        val smeltedItem = ItemStack(smeltedMaterial, dropCount)
        event.block.world.dropItemNaturally(event.block.location, smeltedItem)

        player.playSound(event.block.location, Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, 1.0f)

        val expAmount = getSmeltingExperience(blockType)
        if (expAmount > 0) {
            val expOrb = player.world.spawn(event.block.location, ExperienceOrb::class.java)
            expOrb.experience = expAmount
        }

        if (plugin.config.getBoolean("custom-enchantments.auto-smelting.show-message", true)) {
            player.sendMessage("§6§lAuto Smelting! §7Smelted ${dropCount}x ${smeltedMaterial.name.lowercase().replace("_", " ")}")
        }
    }

    private fun handleBackstab(event: EntityDamageByEntityEvent, attacker: Player, victim: LivingEntity, level: Int) {

        if (!isAttackingFromBehind(attacker, victim)) return

        val damageMultiplier = 1.0 + (0.2 + (level * 0.1))

        val originalDamage = event.damage
        val newDamage = originalDamage * damageMultiplier

        event.damage = newDamage

        victim.world.spawnParticle(
            org.bukkit.Particle.CRIT,
            victim.location.add(0.0, victim.height / 2, 0.0),
            10, 0.3, 0.3, 0.3, 0.1
        )

        attacker.playSound(attacker.location, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f)

        if (plugin.config.getBoolean("custom-enchantments.backstab.show-message", true)) {
            attacker.sendMessage("§c§lBackstab! §7+${String.format("%.1f", (damageMultiplier - 1) * 100)}% damage")
        }
    }

    private fun handleLuckyOrb(event: PlayerExpChangeEvent, totalLevel: Int) {
        val originalExp = event.amount
        val multiplier = 1.0 + (0.5 + (totalLevel * 0.25))

        val newExp = (originalExp * multiplier).toInt()

        event.amount = newExp

        val player = event.player

        player.world.spawnParticle(
            org.bukkit.Particle.HAPPY_VILLAGER,
            player.location.add(0.0, 1.0, 0.0),
            5, 0.3, 0.3, 0.3, 0.1
        )

        if (plugin.config.getBoolean("custom-enchantments.lucky-orb.show-message", true) && newExp > originalExp) {
            player.sendMessage("§a§lLucky Orb! §7+${newExp - originalExp} bonus EXP")
        }
    }

    private fun hasCustomEnchantment(item: ItemStack, enchantmentId: String): Boolean {
        val meta = item.itemMeta ?: return false
        val lore = meta.lore ?: return false

        return lore.any { line ->
            line.contains(enchantmentId.replace("_", " "), ignoreCase = true) ||
                    line.contains(enchantmentId.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } })
        }
    }

    private fun getCustomEnchantmentLevel(item: ItemStack, enchantmentId: String): Int {
        val meta = item.itemMeta ?: return 0
        val lore = meta.lore ?: return 0

        for (line in lore) {
            if (line.contains(enchantmentId.replace("_", " "), ignoreCase = true)) {

                when {
                    line.contains(" I") && !line.contains(" II") && !line.contains(" III") -> return 1
                    line.contains(" II") && !line.contains(" III") -> return 2
                    line.contains(" III") -> return 3
                    line.contains(" 1") -> return 1
                    line.contains(" 2") -> return 2
                    line.contains(" 3") -> return 3
                }
            }
        }

        return 0
    }

    private fun isAttackingFromBehind(attacker: Player, victim: LivingEntity): Boolean {
        val attackerLocation = attacker.location
        val victimLocation = victim.location

        val victimDirection = victimLocation.direction

        val toAttacker = attackerLocation.toVector().subtract(victimLocation.toVector()).normalize()

        val dotProduct = victimDirection.dot(toAttacker)

        return dotProduct < -0.5

    }

    private fun calculateFortuneDrops(baseDrops: Int, fortuneLevel: Int): Int {
        if (fortuneLevel <= 0) return baseDrops

        val random = Math.random()
        val bonusChance = fortuneLevel / (fortuneLevel + 2.0)

        return if (random < bonusChance) {
            baseDrops + (1..fortuneLevel).random()
        } else {
            baseDrops
        }
    }

    private fun getSmeltingExperience(material: Material): Int {
        return when (material) {
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE -> (0.7 * 10).toInt()
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE -> (1.0 * 10).toInt()
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE -> (0.7 * 10).toInt()
            Material.ANCIENT_DEBRIS -> (2.0 * 10).toInt()
            else -> (0.1 * 10).toInt()
        }
    }
}

