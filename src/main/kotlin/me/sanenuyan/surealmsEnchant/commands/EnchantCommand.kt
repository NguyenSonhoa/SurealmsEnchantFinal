package me.sanenuyan.surealmsEnchant.commands

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import me.sanenuyan.surealmsEnchant.gui.EnchantingTableGUI
import me.sanenuyan.surealmsEnchant.gui.AshesMenuListener
import me.sanenuyan.surealmsEnchant.managers.MessageManager
import me.sanenuyan.surealmsEnchant.managers.AshesItemManager
import me.sanenuyan.surealmsEnchant.managers.RuneManager
import me.sanenuyan.surealmsEnchant.managers.SpecializedBookManager
import me.sanenuyan.surealmsEnchant.models.RuneConfig
import me.sanenuyan.surealmsEnchant.models.RuneType
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EnchantCommand(
    private val plugin: SurealmsEnchant,
    private val enchantingTableGUI: EnchantingTableGUI,
    private val specializedBookManager: SpecializedBookManager,
    private val runeManager: RuneManager,
    private val ashesItemManager: AshesItemManager,

    private val ashesMenuListener: AshesMenuListener,
    private val runeConfig: RuneConfig,
    private val messageManager: MessageManager
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        when {
            args.isEmpty() -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                if (!sender.hasPermission("surealms.enchant.use")) {
                    sender.sendMessage(messageManager.getMessage("commands.no_permission"))
                    return true
                }

                enchantingTableGUI.openGUI(sender)
                sender.sendMessage(messageManager.getMessage("commands.open.enchanting_table"))
            }

            args.size == 1 && args[0].equals("index", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                if (!sender.hasPermission("surealms.enchant.index")) {
                    sender.sendMessage(messageManager.getMessage("commands.no_permission"))
                    return true
                }

                val configurableIndexGUI = me.sanenuyan.surealmsEnchant.gui.ConfigurableEnchantIndexGUI(
                    plugin,
                    sender,
                    plugin.customEnchantmentManager
                )
                configurableIndexGUI.initialize()
                configurableIndexGUI.open()
                sender.sendMessage(messageManager.getMessage("commands.open.enchant_index"))
            }

            args.size >= 1 && args[0].equals("ashesmenu", ignoreCase = true) -> {
                handleAshesMenuCommand(sender, args)
            }

            args.size == 1 && args[0].equals("runes", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                if (!sender.hasPermission("surealms.enchant.runes")) {
                    sender.sendMessage(messageManager.getMessage("commands.no_permission"))
                    return true
                }

                val runeGUI = me.sanenuyan.surealmsEnchant.gui.RuneApplicationGUI(plugin, sender, plugin.runeManager)
                runeGUI.initialize()
                runeGUI.open()
                sender.sendMessage(messageManager.getMessage("commands.open.rune_gui"))
            }

            args.size == 1 && args[0].equals("help", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                showHelp(sender)
            }

            args.size >= 2 && args[0].equals("book", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                handleBookCommand(sender, args)
            }

            args.size >= 1 && args[0].equals("tome", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                handleTomeCommand(sender, args)
            }

            args.size >= 1 && args[0].equals("ashes", ignoreCase = true) -> {

                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                handleAshesCommand(sender, args)
            }

            args.size >= 2 && args[0].equals("rune", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                handleRuneCommand(sender, args)
            }

            args.size >= 2 && args[0].equals("custom", ignoreCase = true) -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }

                handleCustomEnchantCommand(sender, args)
            }

            args.size == 1 && args[0].equals("reload", ignoreCase = true) -> {

                if (!sender.hasPermission("surealms.enchant.admin")) {
                    sender.sendMessage(messageManager.getMessage("commands.no_permission"))
                    return true
                }

                plugin.reloadPlugin()
                sender.sendMessage(messageManager.getMessage("plugin.reload"))
            }

            else -> {
                if (sender !is Player) {
                    sender.sendMessage(messageManager.getMessage("commands.player_only"))
                    return true
                }
                sender.sendMessage(messageManager.getMessage("commands.invalid_arguments"))
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (sender !is Player) return emptyList()

        return when (args.size) {
            1 -> {
                val completions = mutableListOf("help", "index", "tome", "ashes", "ashesmenu")

                if (sender.hasPermission("surealms.enchant.book")) {
                    completions.add("book")
                }

                if (sender.hasPermission("surealms.enchant.rune")) {
                    completions.add("rune")
                }

                if (sender.hasPermission("surealms.enchant.custom")) {
                    completions.add("custom")
                }

                if (sender.hasPermission("surealms.enchant.vanilla")) {
                    completions.add("vanilla")
                }

                if (sender.hasPermission("surealms.enchant.datapack")) {
                    completions.add("datapack")
                }

                if (sender.hasPermission("surealms.enchant.admin")) {
                    completions.add("reload")
                }

                completions.filter { it.startsWith(args[0], ignoreCase = true) }
            }

            2 -> {
                when {
                    args[0].equals("book", ignoreCase = true) && sender.hasPermission("surealms.enchant.book") -> {
                        val bookTypes = specializedBookManager.getAllBookTypes().map { it.id }
                        bookTypes.filter { it.startsWith(args[1], ignoreCase = true) }
                    }

                    args[0].equals("rune", ignoreCase = true) && sender.hasPermission("surealms.enchant.rune") -> {
                        val runeTypes = runeManager.getAllRuneTypes().map { it.id }
                        runeTypes.filter { it.startsWith(args[1], ignoreCase = true) }
                    }

                    args[0].equals("tome", ignoreCase = true) && sender.hasPermission("surealms.enchant.admin") -> {
                        getPlayerAndAmountCompletions(args[1], null)
                    }

                    args[0].equals("ashes", ignoreCase = true) && sender.hasPermission("surealms.enchant.admin") -> {

                        getPlayerAndAmountCompletions(args[1], null)
                    }

                    args[0].equals("ashesmenu", ignoreCase = true) && sender.hasPermission("surealms.enchant.admin") -> {
                        Bukkit.getOnlinePlayers().map { it.name }.filter { it.startsWith(args[1], ignoreCase = true) }
                    }

                    else -> emptyList()
                }
            }

            3 -> {
                when {
                    (args[0].equals("book", ignoreCase = true) && sender.hasPermission("surealms.enchant.book")) ||
                            (args[0].equals("rune", ignoreCase = true) && sender.hasPermission("surealms.enchant.rune")) -> {
                        getPlayerAndAmountCompletions(args[2], args[1])
                    }
                    (args[0].equals("tome", ignoreCase = true) && sender.hasPermission("surealms.enchant.admin")) ||
                            (args[0].equals("ashes", ignoreCase = true) && sender.hasPermission("surealms.enchant.admin")) -> {

                        getPlayerAndAmountCompletions(args[2], args[1])
                    }

                    else -> emptyList()
                }
            }
            4 -> {
                when {
                    (args[0].equals("book", ignoreCase = true) && sender.hasPermission("surealms.enchant.book")) ||
                            (args[0].equals("rune", ignoreCase = true) && sender.hasPermission("surealms.enchant.rune")) -> {
                        getPlayerAndAmountCompletions(args[3], args[2])
                    }
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }

    private fun handleAshesMenuCommand(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("surealms.enchant.ashesmenu")) {
            sender.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        val target: Player?
        if (args.size > 1) {
            if (!sender.hasPermission("surealms.enchant.admin")) {
                sender.sendMessage(messageManager.getMessage("commands.no_permission"))
                return
            }
            val targetName = args[1]
            target = Bukkit.getPlayer(targetName)
            if (target == null) {
                sender.sendMessage(messageManager.getMessage("commands.player_not_found", Placeholder.unparsed("player", targetName)))
                return
            }
        } else {
            if (sender !is Player) {
                sender.sendMessage("Usage: /se ashesmenu <player>")
                return
            }
            target = sender
        }

        ashesMenuListener.openAshesMenu(target)

        if (target != sender) {
            sender.sendMessage(messageManager.getMessage("commands.ashesmenu.opened_for_other", Placeholder.unparsed("player", target.name)))
        }
    }

    private fun parsePlayerAndAmount(sender: Player, args: Array<out String>, startIndex: Int): Pair<Player, Int>? {
        var target: Player = sender
        var amount: Int = 1

        if (args.size > startIndex) {
            val arg = args[startIndex]
            val argAsInt = arg.toIntOrNull()

            if (argAsInt != null) {

                amount = argAsInt
                if (args.size > startIndex + 1) {

                    val playerArg = args[startIndex + 1]
                    val playerByName = Bukkit.getPlayer(playerArg)
                    if (playerByName != null) {
                        target = playerByName
                    } else {
                        sender.sendMessage(messageManager.getMessage("commands.player_not_found", Placeholder.unparsed("player", playerArg)))
                        return null
                    }
                }
            } else {

                val playerByName = Bukkit.getPlayer(arg)
                if (playerByName != null) {
                    target = playerByName
                    if (args.size > startIndex + 1) {

                        amount = args[startIndex + 1].toIntOrNull() ?: 1
                    }
                } else {
                    sender.sendMessage(messageManager.getMessage("commands.player_not_found", Placeholder.unparsed("player", arg)))
                    return null
                }
            }
        }

        if (amount <= 0) {
            sender.sendMessage(messageManager.getMessage("commands.invalid_amount"))
            return null
        }

        return Pair(target, amount)
    }

    private fun getPlayerAndAmountCompletions(currentArg: String, previousArg: String?): List<String> {
        val completions = mutableListOf<String>()
        val onlinePlayers = Bukkit.getOnlinePlayers().map { it.name }
        val commonAmounts = listOf("1", "16", "64")

        val previousArgIsPlayer = previousArg != null && Bukkit.getPlayer(previousArg) != null
        val previousArgIsAmount = previousArg != null && previousArg.toIntOrNull() != null

        if (previousArg == null) {

            completions.addAll(onlinePlayers)
            completions.addAll(commonAmounts)
        } else if (previousArgIsPlayer) {

            completions.addAll(commonAmounts)
        } else if (previousArgIsAmount) {

            completions.addAll(onlinePlayers)
        }

        return completions.filter { it.startsWith(currentArg, ignoreCase = true) }
    }

    private fun handleTomeCommand(sender: Player, args: Array<out String>) {
        if (!sender.hasPermission("surealms.enchant.admin")) {
            sender.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        val (target, amount) = parsePlayerAndAmount(sender, args, 1) ?: return

        val tomeOfRenewal = plugin.enchantingTableListener.createTomeOfRenewal(amount)

        target.inventory.addItem(tomeOfRenewal)
        sender.sendMessage(
            messageManager.getMessage(
                "commands.tome.received_sender",
                mapOf("player" to target.name, "amount" to amount.toString())
            )
        )
        if (target != sender) {
            target.sendMessage(
                messageManager.getMessage(
                    "commands.tome.received_target",
                    mapOf("amount" to amount.toString())
                )
            )
        }
    }

    private fun handleAshesCommand(sender: Player, args: Array<out String>) {
        if (!sender.hasPermission("surealms.enchant.admin")) {
            sender.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        val (target, amount) = parsePlayerAndAmount(sender, args, 1) ?: return

        val ashesItem = ashesItemManager.createAshesItem(amount)

        target.inventory.addItem(ashesItem)
        sender.sendMessage(
            messageManager.getMessage(
                "commands.ashes.received_sender",
                mapOf("player" to target.name, "amount" to amount.toString())
            )
        )
        if (target != sender) {
            target.sendMessage(
                messageManager.getMessage(
                    "commands.ashes.received_target",
                    mapOf("amount" to amount.toString())
                )
            )
        }
    }

    private fun handleBookCommand(sender: Player, args: Array<out String>) {
        if (!sender.hasPermission("surealms.enchant.book")) {
            sender.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(messageManager.getMessage("commands.book.usage"))
            return
        }

        if (args[1].equals("list", ignoreCase = true)) {

            sender.sendMessage(messageManager.getMessage("commands.book.list_header"))
            specializedBookManager.getAllBookTypes().forEach { bookType ->
                sender.sendMessage(
                    messageManager.getMessage(
                        "commands.book.list_item",
                        mapOf("id" to bookType.id, "name" to bookType.displayName)
                    )
                )
            }
            sender.sendMessage(messageManager.getMessage("commands.book.list_footer"))
            return
        }

        val bookTypeId = args[1].lowercase()
        val bookType = specializedBookManager.getBookTypeById(bookTypeId)

        if (bookType == null) {
            sender.sendMessage(
                messageManager.getMessage(
                    "commands.book.not_found",
                    Placeholder.unparsed("type", bookTypeId)
                )
            )
            sender.sendMessage(messageManager.getMessage("commands.book.list_suggestion"))
            return
        }

        val (target, amount) = parsePlayerAndAmount(sender, args, 2) ?: return

        val book = specializedBookManager.createSpecializedBook(bookType)
        book.amount = amount

        target.inventory.addItem(book)
        sender.sendMessage(
            messageManager.getMessage(
                "commands.book.received_sender",
                mapOf("player" to target.name, "amount" to amount.toString(), "book_name" to bookType.displayName)
            )
        )
        if (target != sender) {
            target.sendMessage(
                messageManager.getMessage(
                    "commands.book.received_target",
                    mapOf("amount" to amount.toString(), "book_name" to bookType.displayName)
                )
            )
        }
    }

    private fun handleRuneCommand(sender: Player, args: Array<out String>) {
        if (!sender.hasPermission("surealms.enchant.rune")) {
            sender.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        if (args.size < 2) {
            sender.sendMessage(messageManager.getMessage("commands.rune.usage"))
            return
        }

        if (args[1].equals("list", ignoreCase = true)) {

            sender.sendMessage(messageManager.getMessage("commands.rune.list_header"))
            runeManager.getAllRuneTypes().forEach { runeType ->
                val config = when (runeType) {
                    RuneType.POWER_RUNE -> runeConfig.powerRuneItemConfig
                    RuneType.PROTECTION_RUNE -> runeConfig.protectionRuneItemConfig
                }
                sender.sendMessage(
                    messageManager.getMessage(
                        "commands.rune.list_item",
                        mapOf("id" to runeType.id, "name" to config.displayName)
                    )
                )
                sender.sendMessage(
                    messageManager.getMessage(
                        "commands.rune.list_item_description",
                        mapOf("description" to config.lore.joinToString(" "))
                    )
                )
            }
            sender.sendMessage(messageManager.getMessage("commands.rune.list_footer"))
            return
        }

        val runeTypeId = args[1].lowercase()
        val runeType = RuneType.values().find { it.id == runeTypeId }

        if (runeType == null) {
            sender.sendMessage(
                messageManager.getMessage(
                    "commands.rune.not_found",
                    Placeholder.unparsed("type", runeTypeId)
                )
            )
            sender.sendMessage(messageManager.getMessage("commands.rune.list_suggestion"))
            return
        }

        val (target, amount) = parsePlayerAndAmount(sender, args, 2) ?: return

        val runeConfigForType = when (runeType) {
            RuneType.POWER_RUNE -> runeConfig.powerRuneItemConfig
            RuneType.PROTECTION_RUNE -> runeConfig.protectionRuneItemConfig
        }

        val rune = runeManager.createRune(runeType)
        rune.amount = amount

        target.inventory.addItem(rune)
        sender.sendMessage(
            messageManager.getMessage(
                "commands.rune.received_sender",
                mapOf("player" to target.name, "amount" to amount.toString(), "rune_name" to runeConfigForType.displayName)
            )
        )
        if (target != sender) {
            target.sendMessage(
                messageManager.getMessage(
                    "commands.rune.received_target",
                    mapOf("amount" to amount.toString(), "rune_name" to runeConfigForType.displayName)
                )
            )
        }
    }

    private fun handleCustomEnchantCommand(player: Player, args: Array<out String>) {
        if (!player.hasPermission("surealms.enchant.custom")) {
            player.sendMessage(messageManager.getMessage("commands.no_permission"))
            return
        }

        when {
            args.size == 2 && args[1].equals("list", ignoreCase = true) -> {
                player.sendMessage(messageManager.getMessage("commands.custom.list_header"))
                player.sendMessage(
                    messageManager.getMessage(
                        "commands.custom.list_item",
                        mapOf("enchant" to "auto_smelting", "description" to "Auto Smelting for pickaxes")
                    )
                )
                player.sendMessage(
                    messageManager.getMessage(
                        "commands.custom.list_item",
                        mapOf("enchant" to "backstab", "description" to "Backstab for swords/axes")
                    )
                )
                player.sendMessage(
                    messageManager.getMessage(
                        "commands.custom.list_item",
                        mapOf("enchant" to "lucky_orb", "description" to "Lucky Orb for armor")
                    )
                )
                player.sendMessage(messageManager.getMessage("commands.custom.list_footer"))
            }
        }
    }

    private fun showHelp(player: Player) {
        player.sendMessage(messageManager.getMessage("commands.help"))
    }
}

