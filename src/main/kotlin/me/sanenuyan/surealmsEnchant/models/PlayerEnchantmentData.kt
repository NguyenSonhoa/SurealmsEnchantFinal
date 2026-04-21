package me.sanenuyan.surealmsEnchant.models

import org.bukkit.entity.Player

data class PlayerEnchantmentData(
    val player: Player,
    val offeredEnchantments: List<CustomEnchantment>
)
