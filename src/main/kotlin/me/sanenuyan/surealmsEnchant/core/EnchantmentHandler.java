package me.sanenuyan.surealmsEnchant.core;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import java.util.logging.Logger;

public class EnchantmentHandler {

    private static final Logger logger = Logger.getLogger(EnchantmentHandler.class.getName());

    // 确保从插件API动态获取附魔类型
    Enchantment getEnchantmentDynamically(String enchantName) {
        // 动态获取附魔类型
        Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchantName));
        if (ench == null) {
            // 优雅降级，使用默认值
            ench = Enchantment.PROTECTION;
        }
        return ench;
    }


    // 修正附魔层级的判断逻辑
    void applyEnchantment(ItemStack item, String enchantName, int level) {
        Enchantment ench = getEnchantmentDynamically(enchantName);
        if (ench != null) {
            // 确保正确应用附魔层级
            item.addEnchantment(ench, level);
        } else {
            // 日志记录无效附魔选项
            logger.warning("Invalid enchantment option: " + enchantName);
        }
    }
}