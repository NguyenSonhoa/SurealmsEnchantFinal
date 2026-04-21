package me.sanenuyan.surealmsEnchant.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage

object ChatUtils {
    
    private val miniMessage = MiniMessage.builder().build()

    /**
     * Parses a string with MiniMessage format into a Component.
     * It also removes italics from the component.
     * @param text The string to parse.
     * @return The parsed Component.
     */
    fun parse(text: String?): Component {
        if (text.isNullOrEmpty()) {
            return Component.empty()
        }
        val sanitized = text.replace("§[0-9a-fk-or]".toRegex(), "")
        return miniMessage.deserialize(sanitized).decoration(TextDecoration.ITALIC, false)

    }
//    fun parseColor(text: String): String {
//        return if (text.contains("<") && text.contains(">")) text else text.replace('&', '§')
//    }
}
