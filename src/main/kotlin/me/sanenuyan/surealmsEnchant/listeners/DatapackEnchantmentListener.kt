package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.Particle
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.inventory.ItemStack
import me.sanenuyan.surealmsEnchant.managers.DatapackEnchantmentManager
import org.bukkit.event.entity.EntityDamageByEntityEvent

class DatapackEnchantmentListener(
    private val plugin: SurealmsEnchant,
    private val datapackEnchantmentManager: DatapackEnchantmentManager
) : Listener {

    private fun getEnchantmentByKey(key: NamespacedKey): Enchantment? {
        return try {
            Enchantment.getByKey(key)
        } catch (e: Exception) {
            null
        }
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

    private fun handleGenericDatapackEnchant(
        event: Any,
        player: Player,
        item: ItemStack? = null,
        enchant: Enchantment,
        level: Int
    ) {
        val key = enchant.key.key
        val configPath = "datapack-enchantments.$key"

        plugin.logger.info("Datapack enchantment effect triggered: $key at level $level by ${player.name}")

    }
}