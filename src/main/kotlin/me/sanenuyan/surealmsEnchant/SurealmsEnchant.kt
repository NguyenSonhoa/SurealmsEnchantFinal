package me.sanenuyan.surealmsEnchant

import com.github.retrooper.packetevents.PacketEvents
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder
import me.sanenuyan.surealmsEnchant.api.EnchantmentsAPI
import me.sanenuyan.surealmsEnchant.commands.EnchantCommand
import me.sanenuyan.surealmsEnchant.config.GUIConfig
import me.sanenuyan.surealmsEnchant.core.EnchantmentSystem
import me.sanenuyan.surealmsEnchant.economy.VaultIntegration
import me.sanenuyan.surealmsEnchant.gui.AshesMenuListener
import me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUI
import me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUIManager
import me.sanenuyan.surealmsEnchant.integrations.ExcellentEnchantsIntegration
import me.sanenuyan.surealmsEnchant.integrations.MythicMobsIntegration
import me.sanenuyan.surealmsEnchant.listeners.AnvilFailureListener
import me.sanenuyan.surealmsEnchant.listeners.AnvilListener
import me.sanenuyan.surealmsEnchant.listeners.BrokenItemListener
import me.sanenuyan.surealmsEnchant.listeners.EnchantingTableListenerSimple
import me.sanenuyan.surealmsEnchant.listeners.EnchantingTableParticleListener
import me.sanenuyan.surealmsEnchant.listeners.EnchantmentDisplayListener
import me.sanenuyan.surealmsEnchant.listeners.GrindstoneListener
import me.sanenuyan.surealmsEnchant.listeners.RuneListener
import me.sanenuyan.surealmsEnchant.managers.MessageManager
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentEffects
import me.sanenuyan.surealmsEnchant.managers.CustomEnchantmentManager
import me.sanenuyan.surealmsEnchant.managers.RuneManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import me.sanenuyan.surealmsEnchant.managers.AshesItemManager

import me.sanenuyan.surealmsEnchant.nms.NMSEnchantmentRegistry
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneItemConfig
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.event.Listener
import java.io.File
import java.util.logging.Level
import org.bukkit.event.EventHandler
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.Material

class SurealmsEnchant : JavaPlugin() {

    lateinit var enchantmentSystem: EnchantmentSystem
        private set
    lateinit var vaultIntegration: VaultIntegration
        private set
    lateinit var excellentEnchantsIntegration: ExcellentEnchantsIntegration
        private set
    lateinit var guiConfig: GUIConfig
        private set
    var placeholderAPIIntegration: me.sanenuyan.surealmsEnchant.integrations.PlaceholderAPIIntegration? = null
        private set
    lateinit var specializedBookManager: SpecializedBookManager
        private set
    lateinit var customEnchantmentManager: CustomEnchantmentManager
        private set
    lateinit var runeManager: RuneManager
        private set
    lateinit var ashesItemManager: AshesItemManager

        private set
    lateinit var customEnchantmentEffects: CustomEnchantmentEffects
        private set
    lateinit var messageManager: MessageManager
        private set
    lateinit var runeConfig: RuneConfig

        private set

    lateinit var nmsEnchantmentRegistry: NMSEnchantmentRegistry
        private set

    lateinit var enchantingTableGUI: EnchantingTableGUI
    lateinit var enchantingTableGUIManager: EnchantingTableGUIManager
    lateinit var enchantIndexGUI: EnchantingTableGUI
    lateinit var configurableGUIManager: me.sanenuyan.surealmsEnchant.gui.ConfigurableGUIManager
        private set

    internal lateinit var enchantingTableListener: EnchantingTableListenerSimple
    private lateinit var grindstoneListener: GrindstoneListener
    private lateinit var runeListener: RuneListener
    private lateinit var enchantmentDisplayListener: EnchantmentDisplayListener
    private lateinit var enchantingTableParticleListener: EnchantingTableParticleListener

    private var mythicMobsIntegration: MythicMobsIntegration? = null

    private lateinit var ashesMenuListener: AshesMenuListener

    private lateinit var enchantCommand: EnchantCommand

    override fun onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this))
        PacketEvents.getAPI().load()
    }

    override fun onEnable() {
        try {

            displayStartupBanner()

            saveDefaultConfig()

            loadRuneConfig()

            PacketEvents.getAPI().init()

            EnchantmentsAPI.initialize(this)

            excellentEnchantsIntegration = ExcellentEnchantsIntegration(this)

            initializeCoreComponents()

            initializeGUIComponents()

            if (server.pluginManager.isPluginEnabled("MythicMobs")) {
                mythicMobsIntegration = MythicMobsIntegration(this)
                mythicMobsIntegration?.initialize()
            }

            registerEventListeners()

            registerCommands()

            vaultIntegration = VaultIntegration(this)
            guiConfig = GUIConfig(this)
            placeholderAPIIntegration = me.sanenuyan.surealmsEnchant.integrations.PlaceholderAPIIntegration(this)
            enchantingTableGUIManager = EnchantingTableGUIManager(this, enchantmentSystem, specializedBookManager, customEnchantmentManager, enchantingTableListener)

            nmsEnchantmentRegistry = NMSEnchantmentRegistry(this)

            server.scheduler.runTaskLater(this, Runnable {
                setupIntegrations()
            }, 20L)

            displaySuccessMessage()

        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to enable SurealmsEnchant plugin", e)
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        try {

            displayShutdownBanner()

            server.onlinePlayers.forEach { player ->
                if (enchantingTableGUI.isEnchantingTableGUI(player.openInventory.topInventory)) {
                    player.closeInventory()
                }
            }

            if (::nmsEnchantmentRegistry.isInitialized) {
                nmsEnchantmentRegistry.unregisterEnchantments()
            }

            if (::enchantmentDisplayListener.isInitialized) {
                PacketEvents.getAPI().eventManager.unregisterListener(enchantmentDisplayListener)
            }

            PacketEvents.getAPI().terminate()

            displayGoodbyeMessage()

        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error during plugin disable", e)
        }
    }

    private fun initializeCoreComponents() {
        logger.info("Initializing core components...")

        messageManager = MessageManager(this)
        customEnchantmentManager = CustomEnchantmentManager(this)
        specializedBookManager = SpecializedBookManager(this, messageManager)
        enchantmentSystem = EnchantmentSystem(this, customEnchantmentManager, specializedBookManager, runeConfig)
        runeManager = RuneManager(this, enchantmentSystem, runeConfig)
        ashesItemManager = AshesItemManager(this)

        customEnchantmentEffects = CustomEnchantmentEffects(this)

        logger.info("Core components initialized successfully")
    }

    private fun initializeGUIComponents() {
        logger.info("Initializing GUI components...")

        enchantingTableListener = EnchantingTableListenerSimple(this)
        enchantingTableGUI = me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUI(this, enchantmentSystem, specializedBookManager, customEnchantmentManager, enchantingTableListener)
        enchantingTableListener.setGUI(enchantingTableGUI)

        configurableGUIManager = me.sanenuyan.surealmsEnchant.gui.ConfigurableGUIManager(this)

        logger.info("GUI components initialized successfully")
    }

    private fun registerEventListeners() {
        logger.info("Registering event listeners...")

        grindstoneListener = GrindstoneListener(this, specializedBookManager, messageManager)
        runeListener = RuneListener(this, runeManager, runeConfig, enchantmentSystem)

        enchantmentDisplayListener = EnchantmentDisplayListener(this, excellentEnchantsIntegration)
        PacketEvents.getAPI().eventManager.registerListener(enchantmentDisplayListener)

        enchantingTableParticleListener = EnchantingTableParticleListener(this)
        server.pluginManager.registerEvents(enchantingTableParticleListener, this)
        enchantingTableParticleListener.startParticleTask()

        server.pluginManager.registerEvents(enchantingTableListener, this)
        server.pluginManager.registerEvents(grindstoneListener, this)
        server.pluginManager.registerEvents(runeListener, this)
        server.pluginManager.registerEvents(configurableGUIManager, this)
        server.pluginManager.registerEvents(customEnchantmentEffects, this)
        server.pluginManager.registerEvents(AnvilFailureListener(this, enchantmentSystem, runeConfig), this)
        server.pluginManager.registerEvents(BrokenItemListener(this), this)
        server.pluginManager.registerEvents(AnvilListener(this, ashesItemManager, enchantmentSystem, runeConfig), this)
        ashesMenuListener = AshesMenuListener(this)
        server.pluginManager.registerEvents(ashesMenuListener, this)
        logger.info("Event listeners registered successfully")
    }

    private fun registerCommands() {
        logger.info("Registering commands...")

        enchantCommand = EnchantCommand(this, enchantingTableGUI, specializedBookManager, runeManager, ashesItemManager, ashesMenuListener, runeConfig, messageManager)

        try {
            val commandMap = server.commandMap
            val enchantCmd = object : org.bukkit.command.Command(
                "surealmsenchant",
                "Access the advanced enchanting system",
                "/surealmsenchant [index|book <type>|rune <type>|custom <enchant> <item>|vanilla <enchant> <item>|datapack <action>|help|reload|ashes|ashesmenu]",

                listOf("se", "senchant", "enchant")
            ) {
                override fun execute(sender: org.bukkit.command.CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                    return enchantCommand.onCommand(sender, this, commandLabel, args)
                }

                override fun tabComplete(sender: org.bukkit.command.CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                    return enchantCommand.onTabComplete(sender, this, alias, args)?.toMutableList() ?: mutableListOf()
                }
            }

            commandMap.register("surealms", enchantCmd)

            logger.info("Commands registered successfully")
        } catch (e: Exception) {
            logger.severe("Failed to register commands: ${e.message}")
            throw e
        }
    }

    private fun setupIntegrations() {
        logger.info("Setting up integrations...")

        if (server.pluginManager.getPlugin("MythicMobs") != null) {
            logger.info("Found MythicMobs. Initializing integration...")
            try {
                mythicMobsIntegration = MythicMobsIntegration(this)
                mythicMobsIntegration?.initialize()
                logger.info("§aMythicMobs integration enabled. Custom drop type 'SUREALMSENCHANT' will be registered on drop load.")
            } catch (e: NoClassDefFoundError) {
                logger.warning("§eMythicMobs integration failed. You may be using an incompatible version of MythicMobs.")
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "An unexpected error occurred while initializing MythicMobs integration", e)
            }
        } else {
            logger.info("§7MythicMobs not found - custom drop type disabled.")
        }

        guiConfig.load()

        if (vaultIntegration.setupEconomy()) {
            logger.info("§aVault economy integration enabled")
        } else {
            logger.warning("§eVault economy not found - money costs disabled")
        }

        if (excellentEnchantsIntegration.initialize()) {
            logger.info("§aExcellentEnchants integration enabled")
            logger.info("§7Available ExcellentEnchants: ${excellentEnchantsIntegration.getAvailableEnchantments().size}")
        } else {
            logger.info("§7ExcellentEnchants not found - using built-in custom enchantments")

            if (nmsEnchantmentRegistry.registerEnchantments()) {
                logger.info("§aBuilt-in custom enchantments registered successfully")
            } else {
                logger.warning("§eFailed to register built-in custom enchantments")
            }
        }

        try {
            if (placeholderAPIIntegration?.isEnabled() == true) {
                placeholderAPIIntegration?.register()
                logger.info("§aPlaceholderAPI integration enabled")
            } else {
                logger.info("§7PlaceholderAPI not found - placeholders disabled")
            }
        } catch (e: Exception) {
            logger.warning("§eFailed to setup PlaceholderAPI integration: ${e.message}")
        }

        logger.info("Integrations setup completed")
    }

    private fun loadRuneConfig() {
        logger.info("Loading Rune configuration...")

        val runesSection = config.getConfigurationSection("runes")
        val enableRunes = runesSection?.getBoolean("enabled", false) ?: false
        val allowRuneStacking = runesSection?.getBoolean("allow-stacking", false) ?: false
        val shatterMessage = runesSection?.getString("shatter-message", "§c§lSHATTERED - Apply another Protection Rune to protect again") ?: "§c§lSHATTERED - Apply another Protection Rune to protect again"
        val protectionMessage = runesSection?.getString("protection-message", "<!italic><gold>Đã được bảo vệ khỏi phù phép thất bại</gold>") ?: "<!italic><gold>Đã được bảo vệ khỏi phù phép thất bại</gold>"

        val powerRuneSection = runesSection?.getConfigurationSection("power-rune")
        val powerRuneEnabled = powerRuneSection?.getBoolean("enabled", false) ?: false
        val powerRuneCost = powerRuneSection?.getDouble("cost", 1000.0) ?: 1000.0
        val powerRuneItemConfig = RuneItemConfig(
            displayName = powerRuneSection?.getString("display_name", "§6§lPower Rune") ?: "§6§lPower Rune",
            lore = powerRuneSection?.getStringList("lore") ?: listOf(
                "§7Upgrades all enchantments on a tool",
                "§7by 1 level (if not at maximum)",
                "",
                "§eApply at an anvil like an enchanted book"
            ),
            material = Material.valueOf(powerRuneSection?.getString("material", "NETHER_STAR") ?: "NETHER_STAR"),
            customModelData = powerRuneSection?.getInt("custom_model_data", 2001) ?: 2001
        )

        val protectionRuneSection = runesSection?.getConfigurationSection("protection-rune")
        val protectionRuneEnabled = protectionRuneSection?.getBoolean("enabled", false) ?: false
        val protectionRuneCost = protectionRuneSection?.getDouble("cost", 2000.0) ?: 2000.0
        val maxProtectionRunes = protectionRuneSection?.getInt("max-applications", 1) ?: 1
        val protectionRuneItemConfig = RuneItemConfig(
            displayName = protectionRuneSection?.getString("display_name", "§b§lProtection Rune") ?: "§b§lProtection Rune",
            lore = protectionRuneSection?.getStringList("lore") ?: listOf(
                "§7Prevents a tool from being destroyed",
                "§7when it reaches zero durability",
                "§7Instead, the tool will shatter",
                "",
                "§eApply at an anvil like an enchanted book"
            ),
            material = Material.valueOf(protectionRuneSection?.getString("material", "TOTEM_OF_UNDYING") ?: "TOTEM_OF_UNDYING"),
            customModelData = protectionRuneSection?.getInt("custom_model_data", 2002) ?: 2002
        )

        this.runeConfig = RuneConfig(
            enableRunes = enableRunes,
            powerRuneEnabled = powerRuneEnabled,
            protectionRuneEnabled = protectionRuneEnabled,
            powerRuneCost = powerRuneCost,
            protectionRuneCost = protectionRuneCost,
            allowRuneStacking = allowRuneStacking,
            maxProtectionRunes = maxProtectionRunes,
            shatterMessage = shatterMessage,
            protectionMessage = protectionMessage,
            powerRuneItemConfig = powerRuneItemConfig,
            protectionRuneItemConfig = protectionRuneItemConfig
        )
        logger.info("Rune configuration loaded successfully.")
    }

    init {
        instance = this
    }

    private fun displayStartupBanner() {
        val lines = listOf(
            "",
            "§6╔══════════════════════════════════════════════════════════════╗",
            "§6║                                                              ║",
            "§6║    §5§l███████╗██╗   ██╗██████╗ ███████╗ █████╗ ██╗     ███╗   ███╗███████╗    §6║",
            "§6║    §5§l██╔════╝██║   ██║██╔══██╗██╔════╝██╔══██╗██║     ████╗ ████║██╔════╝    §6║",
            "§6║    §5§l███████╗██║   ██║██████╔╝█████╗  ███████║██║     ██╔████╔██║███████╗    §6║",
            "§6║    §5§l╚════██║██║   ██║██╔══██╗██╔══╝  ██╔══██║██║     ██║╚██╔╝██║╚════██║    §6║",
            "§6║    §5§l███████║╚██████╔╝██║  ██║███████╗██║  ██║███████╗██║ ╚═╝ ██║███████║    §6║",
            "§6║    §5§l╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚══════╝╚═╝     ╚═╝╚══════╝    §6║",
            "§6║                                                              ║",
            "§6║                    §b§lENCHANT §3§lSYSTEM                           §6║",
            "§6║                                                              ║",
            "§6║    §7Version: §e${description.version}                                    §6║",
            "§6║    §7Author: §e${description.authors.joinToString(", ")}                              §6║",
            "§6║    §7Server: §e${server.name} ${server.version}                §6║",
            "§6║                                                              ║",
            "§6╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun displaySuccessMessage() {
        val lines = listOf(
            "",
            "§a╔══════════════════════════════════════════════════════════════╗",
            "§a║                    §2§l✓ PLUGIN ENABLED ✓                        §a║",
            "§a╠══════════════════════════════════════════════════════════════╣",
            "§a║                                                              ║",
            "§a║  §2§l🎯 FEATURES LOADED:                                        §a║",
            "§a║    §7• §eAdvanced Enchanting System                           §a║",
            "§a║    §7• §eSpecialized Books & Runes                           §a║",
            "§a║    §7• §eDatapack Generation                                 §a║",
            "§a║    §7• §eVault Integration ${if (::vaultIntegration.isInitialized && vaultIntegration.isEnabled) "§a✓" else "§c✗"}                            §a║",
            "§a║                                                              ║",
            "§a║  §2§l🎮 COMMANDS:                                              §a║",
            "§a║    §7• §b/surealmsenchant §7- Open enchanting table           §a║",
            "§a║    §7• §b/surealmsenchant index §7- Browse enchantments       §a║",
            "§a║    §7• §b/surealmsenchant datapack generate §7- Create datapack §a║",
            "§a║                                                              ║",
            "§a║  §2§l⚡ PLEASE CREDIT IF YOU HAVE OUR PLUGIN!                 §a║",
            "§a║                                                              ║",
            "§a╚══════════════════════════════════════════════════════════════╝",
            ""
        )
        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun displayShutdownBanner() {
        val lines = listOf(
            "",
            "§c╔══════════════════════════════════════════════════════════════╗",
            "§c║                                                              ║",
            "§c║                    §4§l⚠ SHUTTING DOWN ⚠                       §c║",
            "§c║                                                              ║",
            "§c║    §7Closing open GUIs...                                    §c║",
            "§c║    §7Saving data...                                          §c║",
            "§c║    §7Cleaning up resources...                               §c║",
            "§c║                                                              ║",
            "§c╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun displayGoodbyeMessage() {
        val lines = listOf(
            "",
            "§e╔══════════════════════════════════════════════════════════════╗",
            "§e║                                                              ║",
            "§e║                    §6§l✓ PLUGIN DISABLED ✓                     §e║",
            "§e║                                                              ║",
            "§e║    §7Thank you for using SurealmsEnchant!                    §e║",
            "§e║    §7All enchantments and data have been saved safely.      §e║",
            "§e║                                                              ║",
            "§e║    §7Plugin Version: §6${description.version}              §e║",
            "§e║    §7Uptime: §6${getFormattedUptime()}                     §e║",
            "§e║                                                              ║",
            "§e║    §7good bye :)                                           §e║",
            "§e║                                                              ║",
            "§e╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun getFormattedUptime(): String {
        val uptimeMillis = System.currentTimeMillis() - startTime
        val seconds = (uptimeMillis / 1000) % 60
        val minutes = (uptimeMillis / (1000 * 60)) % 60
        val hours = (uptimeMillis / (1000 * 60 * 60)) % 24
        val days = uptimeMillis / (1000 * 60 * 60 * 24)

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    fun reloadPlugin() {
        try {
            displayReloadBanner()

            org.bukkit.event.HandlerList.unregisterAll(this)
            if (::enchantmentDisplayListener.isInitialized) {
                PacketEvents.getAPI().eventManager.unregisterListener(enchantmentDisplayListener)
            }
            logger.info("All event listeners have been unregistered.")

            reloadConfig()
            loadRuneConfig()

            messageManager.reloadMessages()
            if (!File(dataFolder, "gui.yml").exists()) {
                saveResource("gui.yml", true)
            }
            logger.info("Configuration files have been reloaded from disk.")

            initializeCoreComponents()
            initializeGUIComponents()
            logger.info("Core and GUI components have been re-initialized.")

            registerEventListeners()
            logger.info("Event listeners have been re-registered with updated components.")

            enchantCommand = EnchantCommand(this, enchantingTableGUI, specializedBookManager, runeManager, ashesItemManager, ashesMenuListener, runeConfig, messageManager)
            logger.info("Command handler has been updated with new component instances.")

            guiConfig.load()
            specializedBookManager.reloadConfigurations()
            logger.info("Additional configurations and managers have been reloaded.")

            displayReloadSuccess()

        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Failed to reload SurealmsEnchant plugin", e)
            displayReloadError(e)
        }
    }

    private fun displayReloadBanner() {
        val lines = listOf(
            "",
            "§b╔══════════════════════════════════════════════════════════════╗",
            "§b║                                                              ║",
            "§b║                    §3§l🔄 RELOADING PLUGIN 🔄                   §b║",
            "§b║                                                              ║",
            "§b║    §7Reloading configuration...                              §b║",
            "§b║    §7Reinitializing systems...                              §b║",
            "§b║                                                              ║",
            "§b╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun displayReloadSuccess() {
        val lines = listOf(
            "",
            "§a╔══════════════════════════════════════════════════════════════╗",
            "§a║                                                              ║",
            "§a║                    §2§l✓ RELOAD COMPLETE ✓                     §a║",
            "§a║                                                              ║",
            "§a║    §7Configuration reloaded successfully!                    §a║",
            "§a║    §7All systems are operational.                           §a║",
            "§a║                                                              ║",
            "§a╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    private fun displayReloadError(error: Exception) {
        val lines = listOf(
            "",
            "§c╔══════════════════════════════════════════════════════════════╗",
            "§c║                                                              ║",
            "§c║                    §4§l✗ RELOAD FAILED ✗                       §c║",
            "§c║                                                              ║",
            "§c║    §7Error: §c${error.message?.take(40) ?: "Unknown error"}                    §c║",
            "§c║    §7Check console for details.                             §c║",
            "§c║                                                              ║",
            "§c╚══════════════════════════════════════════════════════════════╝",
            ""
        )

        lines.forEach { line ->
            logger.info(line)
        }
    }

    companion object {
        @JvmStatic
        lateinit var instance: SurealmsEnchant
            private set

        private val startTime = System.currentTimeMillis()
    }
}

