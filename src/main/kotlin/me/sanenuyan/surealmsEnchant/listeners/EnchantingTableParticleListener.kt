package me.sanenuyan.surealmsEnchant.listeners

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitRunnable

class EnchantingTableParticleListener(private val plugin: SurealmsEnchant) : Listener {

    fun startParticleTask() {
        object : BukkitRunnable() {
            override fun run() {
                for (player in plugin.server.onlinePlayers) {
                    val location = player.location
                    val radius = 3

                    for (x in -radius..radius) {
                        for (y in -radius..radius) {
                            for (z in -radius..radius) {
                                val block = location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).block
                                if (block.type == Material.ENCHANTING_TABLE) {

                                    block.world.spawnParticle(
                                        Particle.ENCHANT,
                                        block.location.add(0.5, 1.0, 0.5),

                                        10,

                                        0.5, 0.5, 0.5,

                                        0.1

                                    )
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L)

    }
}

