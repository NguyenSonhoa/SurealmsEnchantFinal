package me.sanenuyan.surealmsEnchant.economy

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.entity.Player

class VaultIntegration(private val plugin: SurealmsEnchant) {

    private var economy: Any? = null
    var isEnabled = false
    private var economyClass: Class<*>? = null

    fun setupEconomy(): Boolean {
        try {

            val vaultPlugin = plugin.server.pluginManager.getPlugin("Vault")
            if (vaultPlugin == null || !vaultPlugin.isEnabled) {

                plugin.logger.info("Debug - Available plugins:")
                plugin.server.pluginManager.plugins.forEach { p ->
                    plugin.logger.info("  - ${p.name} v${p.description.version} (${if (p.isEnabled) "enabled" else "disabled"})")
                }
                plugin.logger.info("Vault plugin not found or not enabled - economy features disabled")
                return false
            }

            plugin.logger.info("Found Vault plugin v${vaultPlugin.description.version}")

            val essentialsPlugin = plugin.server.pluginManager.getPlugin("Essentials")
                ?: plugin.server.pluginManager.getPlugin("EssentialsX")
            if (essentialsPlugin != null && essentialsPlugin.isEnabled) {
                plugin.logger.info("Found ${essentialsPlugin.name} v${essentialsPlugin.description.version}")
            } else {
                plugin.logger.warning("EssentialsX not found - economy may not work")
            }

            plugin.logger.info("Debug - Registered services:")
            plugin.server.servicesManager.knownServices.forEach { serviceClass ->
                val registrations = plugin.server.servicesManager.getRegistrations(serviceClass)
                registrations.forEach { registration ->
                    plugin.logger.info("  - ${serviceClass.simpleName}: ${registration.provider.javaClass.name} (priority: ${registration.priority})")
                }
            }

            val economyRegistrations = plugin.server.servicesManager.knownServices
                .filter { it.simpleName == "Economy" }

            if (economyRegistrations.isEmpty()) {
                plugin.logger.warning("No Economy service class found in registered services!")
                plugin.logger.warning("This usually means Vault Economy API is not available")
                plugin.logger.info("Economy features disabled")
                return false
            }

            economyClass = economyRegistrations.first()
            plugin.logger.info("Found Economy service class: ${economyClass!!.name}")

            val rsp = plugin.server.servicesManager.getRegistration(economyClass!!)
            if (rsp == null) {
                plugin.logger.warning("No economy provider registered!")
                plugin.logger.warning("Available economy plugins should register with Vault")
                plugin.logger.warning("Make sure EssentialsX has economy enabled in config.yml")
                plugin.logger.info("Economy features disabled")
                return false
            }

            economy = rsp.provider
            isEnabled = true

            val nameMethod = economyClass!!.getMethod("getName")
            val economyName = nameMethod.invoke(economy) as String

            plugin.logger.info("Economy integration enabled with $economyName")
            return true

        } catch (e: Exception) {
            plugin.logger.warning("Failed to setup economy integration: ${e.message}")
            plugin.logger.warning("Exception type: ${e.javaClass.simpleName}")
            if (e is ClassNotFoundException) {
                plugin.logger.info("This usually means Vault Economy API is not available")
            }
            plugin.logger.info("Economy features disabled")
            return false
        }
    }

    fun isEconomyEnabled(): Boolean = isEnabled && economy != null

    fun getBalance(player: Player): Double {
        if (!isEconomyEnabled()) return 0.0

        return try {

            val getBalanceMethod = try {
                economyClass!!.getMethod("getBalance", org.bukkit.OfflinePlayer::class.java)
            } catch (e: NoSuchMethodException) {

                economyClass!!.getMethod("getBalance", Player::class.java)
            }
            getBalanceMethod.invoke(economy, player) as Double
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get player balance: ${e.message}")
            plugin.logger.warning("Available methods in Economy class:")
            economyClass!!.methods.filter { it.name == "getBalance" }.forEach { method ->
                plugin.logger.warning("  - ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
            }
            0.0
        }
    }

    fun hasEnough(player: Player, amount: Double): Boolean {
        if (!isEconomyEnabled()) return true
        return getBalance(player) >= amount
    }

    fun withdrawPlayer(player: Player, amount: Double): Boolean {
        if (!isEconomyEnabled()) return true

        return try {

            val withdrawMethod = try {
                economyClass!!.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer::class.java, Double::class.javaPrimitiveType)
            } catch (e: NoSuchMethodException) {

                economyClass!!.getMethod("withdrawPlayer", Player::class.java, Double::class.javaPrimitiveType)
            }
            val response = withdrawMethod.invoke(economy, player, amount)

            val transactionSuccessMethod = response.javaClass.getMethod("transactionSuccess")
            transactionSuccessMethod.invoke(response) as Boolean
        } catch (e: Exception) {
            plugin.logger.warning("Failed to withdraw money from player: ${e.message}")
            false
        }
    }

    fun depositPlayer(player: Player, amount: Double): Boolean {
        if (!isEconomyEnabled()) return true

        return try {
            val depositMethod = economyClass!!.getMethod("depositPlayer", Player::class.java, Double::class.javaPrimitiveType)
            val response = depositMethod.invoke(economy, player, amount)

            val transactionSuccessMethod = response.javaClass.getMethod("transactionSuccess")
            transactionSuccessMethod.invoke(response) as Boolean
        } catch (e: Exception) {
            plugin.logger.warning("Failed to deposit money to player: ${e.message}")
            false
        }
    }

    fun format(amount: Double): String {
        if (!isEconomyEnabled()) return "$%.2f".format(amount)

        return try {
            val formatMethod = economyClass!!.getMethod("format", Double::class.javaPrimitiveType)
            formatMethod.invoke(economy, amount) as String
        } catch (e: Exception) {
            "$%.2f".format(amount)
        }
    }

    fun getCurrencyName(): String {
        if (!isEconomyEnabled()) return "coins"

        return try {
            val currencyMethod = economyClass!!.getMethod("currencyNamePlural")
            currencyMethod.invoke(economy) as String
        } catch (e: Exception) {
            "coins"
        }
    }

    fun getCurrencySymbol(): String {
        if (!isEconomyEnabled()) return "$"

        return try {
            val formatMethod = economyClass!!.getMethod("format", Double::class.javaPrimitiveType)
            val formatted = formatMethod.invoke(economy, 0.0) as String
            formatted.replace("0", "").replace(".", "").replace(",", "").trim().ifEmpty { "$" }
        } catch (e: Exception) {
            "$"
        }
    }
}

