package me.sanenuyan.surealmsEnchant.managers

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.collections.iterator

class MessageManager(private val plugin: SurealmsEnchant) {

    private lateinit var messagesConfig: FileConfiguration
    private val messages = mutableMapOf<String, String>()

    init {
        loadMessages()
    }

    fun loadMessages() {
        val messagesFile = File(plugin.dataFolder, "messages.yml")
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false)
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile)
        messages.clear()
        for (key in messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages[key] = messagesConfig.getString(key)!!
            }
        }
    }

    fun reloadMessages() {
        loadMessages()
    }

    fun getMessage(key: String, vararg placeholders: TagResolver): Component {
        val message = messages[key] ?: return Component.text("Message not found: $key")
        return MiniMessage.miniMessage().deserialize(message, *placeholders)
    }

    fun getMessage(key: String, placeholders: Map<String, Any>): Component {
        val message = messages[key] ?: return Component.text("Message not found: $key")
        val resolver = TagResolver.builder()
        for ((k, v) in placeholders) {
            resolver.resolver(Placeholder.component(k, Component.text(v.toString())))
        }
        return MiniMessage.miniMessage().deserialize(message, resolver.build())
    }
}