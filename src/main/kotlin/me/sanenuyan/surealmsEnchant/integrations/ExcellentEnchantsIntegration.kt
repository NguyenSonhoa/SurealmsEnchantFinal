package me.sanenuyan.surealmsEnchant.integrations

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method
import me.sanenuyan.surealmsEnchant.models.CustomEnchantment
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment as EEEnchantment

class ExcellentEnchantsIntegration(private val plugin: SurealmsEnchant) {

    private var excellentEnchantsPlugin: Plugin? = null
    private var isEnabled = false

    private var enchantmentManagerField: Any? = null
    private var getEnchantsMethod: Method? = null
    private var getEnchantByIdMethod: Method? = null
    private var addEnchantMethod: Method? = null
    private var removeEnchantMethod: Method? = null
    private var getLevelMethod: Method? = null
    private var canEnchantMethod: Method? = null

    fun initialize(): Boolean {
        return try {
            val excellentPlugin = Bukkit.getPluginManager().getPlugin("ExcellentEnchants")

            if (excellentPlugin == null) {
                plugin.logger.info("ExcellentEnchants not found - integration disabled")
                return false
            }

            if (!excellentPlugin.isEnabled) {
                plugin.logger.info("ExcellentEnchants is disabled - integration disabled")
                return false
            }

            excellentEnchantsPlugin = excellentPlugin

            initializeReflection()

            isEnabled = true

            plugin.logger.info("§aExcellentEnchants integration enabled successfully")
            plugin.logger.info("§7Available custom enchantments: ${getAvailableEnchantments().size}")

            true

        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize ExcellentEnchants integration: ${e.message}")
            false
        }
    }

    private fun initializeReflection() {
        try {
            val pluginClass = excellentEnchantsPlugin!!.javaClass
            plugin.logger.info("ExcellentEnchants plugin class: ${pluginClass.name}")

            val possibleFieldNames = listOf(
                "enchantmentManager",
                "enchantManager",
                "manager",
                "enchantmentRegistry",
                "registry"
            )

            var managerField: java.lang.reflect.Field? = null
            for (fieldName in possibleFieldNames) {
                try {
                    managerField = pluginClass.getDeclaredField(fieldName)
                    plugin.logger.info("Found field: $fieldName")
                    break
                } catch (e: NoSuchFieldException) {
                    plugin.logger.info("Field not found: $fieldName")
                    continue
                }
            }

            if (managerField == null) {

                val allFields = pluginClass.declaredFields
                plugin.logger.info("Available fields in ExcellentEnchants:")
                allFields.forEach { field ->
                    plugin.logger.info("  - ${field.name}: ${field.type.simpleName}")
                }

                managerField = allFields.find { field ->
                    val fieldType = field.type.simpleName.lowercase()
                    fieldType.contains("enchant") || fieldType.contains("manager") || fieldType.contains("registry")
                }
            }

            if (managerField != null) {
                managerField.isAccessible = true
                this.enchantmentManagerField = managerField.get(excellentEnchantsPlugin)
                plugin.logger.info("Successfully got manager field: ${managerField.name}")

                if (this.enchantmentManagerField != null) {
                    val managerClass = this.enchantmentManagerField!!.javaClass
                    plugin.logger.info("Manager class: ${managerClass.name}")

                    val allMethods = managerClass.declaredMethods
                    plugin.logger.info("Available methods in manager:")
                    allMethods.forEach { method ->
                        plugin.logger.info("  - ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
                    }

                    val possibleGetAllMethods = listOf("getEnchantments", "getAllEnchantments", "getAll", "values")
                    for (methodName in possibleGetAllMethods) {
                        try {
                            getEnchantsMethod = managerClass.getMethod(methodName)
                            plugin.logger.info("Found get all method: $methodName")
                            break
                        } catch (e: NoSuchMethodException) {
                            continue
                        }
                    }

                    val possibleGetByIdMethods = listOf("getEnchantmentById", "getById", "get")
                    for (methodName in possibleGetByIdMethods) {
                        try {
                            getEnchantByIdMethod = managerClass.getMethod(methodName, String::class.java)
                            plugin.logger.info("Found get by id method: $methodName")
                            break
                        } catch (e: NoSuchMethodException) {
                            continue
                        }
                    }
                }

                plugin.logger.info("ExcellentEnchants reflection initialized successfully")
            } else {
                throw Exception("Could not find enchantment manager field")
            }

        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize ExcellentEnchants reflection: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    fun isEnabled(): Boolean = isEnabled

    fun getPlugin(): Plugin? = excellentEnchantsPlugin

    fun getAvailableEnchantments(): Collection<Any> {
        return try {
            if (!isEnabled) {
                return emptyList()
            }

            val enchantRegistryClass = Class.forName("su.nightexpress.excellentenchants.enchantment.EnchantRegistry")
            val getRegisteredMethod = enchantRegistryClass.getMethod("getRegistered")
            val result = getRegisteredMethod.invoke(null)

            when (result) {
                is Collection<*> -> {
                    val enchantments = result.filterNotNull()
                    @Suppress("UNCHECKED_CAST")
                    enchantments as Collection<Any>
                }
                else -> {
                    plugin.logger.warning("Unexpected result type from EnchantRegistry: ${result?.javaClass?.name}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get available enchantments from EnchantRegistry: ${e.message}")
            e.printStackTrace()

            getAvailableEnchantmentsFallback()
        }
    }
    fun getAsCustomEnchantments(): List<CustomEnchantment> {
        if (!isEnabled) return emptyList()

        return try {
            val excellentEnchants = EnchantRegistry.getRegistered()

            excellentEnchants.mapNotNull { eeEnchant ->
                try {
                    val definition = eeEnchant.definition
                    val distribution = eeEnchant.distribution

                    val tier = when {
                        distribution.isTreasure -> 3
                        definition.maxLevel > 5 -> 3
                        definition.maxLevel > 3 -> 2
                        else -> 1
                    }

                    val description = getEnchantDescription(eeEnchant, 1)

                    CustomEnchantment(
                        id = eeEnchant.id,
                        displayName = eeEnchant.displayName,
                        description = description,
                        bukkitEnchantment = eeEnchant.bukkitEnchantment,
                        level = 1,
                        maxLevel = definition.maxLevel,
                        tier = tier,
                        compatibleItems = emptyList(),
                        conflictsWith = emptyList(),
                        costMultiplier = 1.0,
                        rarity = me.sanenuyan.surealmsEnchant.models.EnchantmentRarity.COMMON,
                        isExcellentEnchant = true,
                        excellentEnchantData = eeEnchant
                    )
                } catch (e: Exception) {
                    plugin.logger.warning("Failed to process enchant ${eeEnchant.id}: ${e.message}")
                    null
                }
            }
        } catch (e: NoClassDefFoundError) {
            plugin.logger.warning("Failed to get enchantments from ExcellentEnchants. Is it installed?")
            emptyList()
        } catch (e: Exception) {
            plugin.logger.warning("An error occurred while getting enchantments from ExcellentEnchants: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    fun getEnchantDescription(eeEnchant: Any, level: Int): List<String> {
        try {

            try {
                val method = eeEnchant.javaClass.getMethod("getDescription", Int::class.java)
                val result = method.invoke(eeEnchant, level)
                val parsed = parseDescriptionResult(result)
                if (parsed.isNotEmpty()) {
                    return parsed
                }
            } catch (e: Exception) {

            }

            try {
                val method = eeEnchant.javaClass.getMethod("getDescription")
                val result = method.invoke(eeEnchant)
                val parsed = parseDescriptionResult(result)
                if (parsed.isNotEmpty()) {
                    return parsed
                }
            } catch (e: Exception) {

            }

            try {
                val definitionMethod = eeEnchant.javaClass.getMethod("getDefinition")
                val definition = definitionMethod.invoke(eeEnchant)

                if (definition != null) {

                    try {
                        val descMethod = definition.javaClass.getMethod("getDescription")
                        val result = descMethod.invoke(definition)
                        val parsed = parseDescriptionResult(result)
                        if (parsed.isNotEmpty()) {
                            return parsed
                        }
                    } catch (e: Exception) {

                    }

                    val methods = definition.javaClass.methods
                    for (method in methods) {
                        if (method.name.lowercase().contains("desc") && method.parameterCount == 0) {
                            try {
                                val result = method.invoke(definition)
                                val parsed = parseDescriptionResult(result)
                                if (parsed.isNotEmpty()) {
                                    return parsed
                                }
                            } catch (e: Exception) {

                            }
                        }
                    }
                }
            } catch (e: Exception) {

            }

            val methods = eeEnchant.javaClass.methods
            for (method in methods) {
                if (method.name.lowercase().contains("desc") && method.parameterCount <= 1) {
                    try {
                        val result = if (method.parameterCount == 0) {
                            method.invoke(eeEnchant)
                        } else {
                            method.invoke(eeEnchant, level)

                        }
                        val parsed = parseDescriptionResult(result)
                        if (parsed.isNotEmpty()) {
                            return parsed
                        }
                    } catch (e: Exception) {

                    }
                }
            }

            for (method in methods) {
                if (method.name.lowercase().contains("lore") && method.parameterCount <= 1) {
                    try {
                        val result = if (method.parameterCount == 0) {
                            method.invoke(eeEnchant)
                        } else {
                            method.invoke(eeEnchant, level)
                        }
                        val parsed = parseDescriptionResult(result)
                        if (parsed.isNotEmpty()) {
                            return parsed
                        }
                    } catch (e: Exception) {

                    }
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Error getting description: ${e.message}")
        }

        return listOf("ExcellentEnchants enchantment", "Description not available")
    }
    private fun parseDescriptionResult(result: Any?): List<String> {
        return when (result) {
            null -> emptyList()
            is String -> {
                if (result.isBlank()) {
                    emptyList()
                } else if (result.contains("")) {
                    result.split("").map { it.trim() }.filter { it.isNotEmpty() }
                } else {
                    listOf(result.trim())
                }
            }
            is List<*> -> {
                result.filterIsInstance<String>().filter { it.isNotBlank() }
            }
            is Array<*> -> {
                result.filterIsInstance<String>().filter { it.isNotBlank() }
            }
            else -> {
                val str = result.toString()
                if (str.isNotBlank() && str != "null") {
                    listOf(str)
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun getAvailableEnchantmentsFallback(): Collection<Any> {
        return try {
            if (enchantmentManagerField == null || getEnchantsMethod == null) {
                plugin.logger.warning("Manager reflection not available for fallback")
                return emptyList()
            }

            plugin.logger.info("Using fallback method to get enchantments...")
            val result = getEnchantsMethod!!.invoke(enchantmentManagerField)

            when (result) {
                is Collection<*> -> {
                    val enchantments = result.filterNotNull()
                    plugin.logger.info("Found ${enchantments.size} enchantments from fallback")
                    @Suppress("UNCHECKED_CAST")
                    enchantments as Collection<Any>
                }
                else -> {
                    plugin.logger.warning("Unknown result type in fallback: ${result?.javaClass?.name}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Fallback method also failed: ${e.message}")
            emptyList()
        }
    }

    fun getEnchantmentById(id: String): Any? {
        return try {
            if (!isEnabled || enchantmentManagerField == null || getEnchantByIdMethod == null) {
                return null
            }

            getEnchantByIdMethod!!.invoke(enchantmentManagerField, id)
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get enchantment by ID $id: ${e.message}")
            null
        }
    }

    fun getEnchantmentByName(name: String): Any? {
        return try {
            getAvailableEnchantments().find { enchant ->
                val displayName = getEnchantmentProperty(enchant, "displayName") as? String
                val id = getEnchantmentProperty(enchant, "id") as? String

                displayName?.equals(name, ignoreCase = true) == true ||
                        id?.equals(name, ignoreCase = true) == true
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to get enchantment by name $name: ${e.message}")
            null
        }
    }

    fun getEnchantmentProperty(enchant: Any, propertyName: String): Any? {
        return try {
            val field = enchant.javaClass.getDeclaredField(propertyName)
            field.isAccessible = true
            field.get(enchant)
        } catch (e: Exception) {
            try {
                val method = enchant.javaClass.getMethod("get${propertyName.replaceFirstChar { it.uppercase() }}")
                method.invoke(enchant)
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun hasCustomEnchantments(item: ItemStack): Boolean {
        if (!isEnabled) return false

        return getAvailableEnchantments().any { enchant ->
            getEnchantmentLevel(item, enchant) > 0
        }
    }

    fun getEnchantmentLevel(item: ItemStack, enchant: Any): Int {
        if (!isEnabled) return 0

        return try {
            val getLevelMethod = enchant.javaClass.getMethod("getLevel", ItemStack::class.java)
            getLevelMethod.invoke(enchant, item) as Int
        } catch (e: Exception) {
            0
        }
    }

    fun addEnchantment(item: ItemStack, enchant: Any, level: Int): ItemStack {
        if (!isEnabled) return item

        return try {
            val addMethod = enchant.javaClass.getMethod("add", ItemStack::class.java, Int::class.java, Boolean::class.java)
            addMethod.invoke(enchant, item, level, true)
            item
        } catch (e: Exception) {
            val enchantId = getEnchantmentProperty(enchant, "id") as? String ?: "unknown"
            plugin.logger.warning("Failed to add ExcellentEnchant $enchantId: ${e.message}")
            item
        }
    }

    fun removeEnchantment(item: ItemStack, enchant: Any): ItemStack {
        if (!isEnabled) return item

        return try {
            val removeMethod = enchant.javaClass.getMethod("remove", ItemStack::class.java)
            removeMethod.invoke(enchant, item)
            item
        } catch (e: Exception) {
            val enchantId = getEnchantmentProperty(enchant, "id") as? String ?: "unknown"
            plugin.logger.warning("Failed to remove ExcellentEnchant $enchantId: ${e.message}")
            item
        }
    }

    fun getEnchantments(item: ItemStack): Map<Any, Int> {
        if (!isEnabled) return emptyMap()

        val result = mutableMapOf<Any, Int>()

        getAvailableEnchantments().forEach { enchant ->
            val level = getEnchantmentLevel(item, enchant)
            if (level > 0) {
                result[enchant] = level
            }
        }

        return result
    }

    fun getEnchantmentsByCategory(category: String): List<Any> {
        if (!isEnabled) return emptyList()

        val lowerCategory = category.lowercase()

        return getAvailableEnchantments().filter { enchant ->
            val id = getEnchantmentProperty(enchant, "id") as? String ?: ""
            val lowerIdId = id.lowercase()

            when (lowerCategory) {
                "weapon" -> lowerIdId.contains("sword") || lowerIdId.contains("weapon") || lowerIdId.contains("combat")
                "tool" -> lowerIdId.contains("tool") || lowerIdId.contains("mining") || lowerIdId.contains("dig")
                "armor" -> lowerIdId.contains("armor") || lowerIdId.contains("protection") || lowerIdId.contains("defense")
                "bow" -> lowerIdId.contains("bow") || lowerIdId.contains("arrow") || lowerIdId.contains("projectile")
                else -> false
            }
        }
    }

    fun searchEnchantments(query: String): List<Any> {
        if (!isEnabled) return emptyList()

        val lowerQuery = query.lowercase()

        return getAvailableEnchantments().filter { enchant ->
            val id = getEnchantmentProperty(enchant, "id") as? String ?: ""
            val displayName = getEnchantmentProperty(enchant, "displayName") as? String ?: ""

            id.lowercase().contains(lowerQuery) ||
                    displayName.lowercase().contains(lowerQuery)
        }
    }

    fun getRandomEnchantments(item: ItemStack, count: Int = 3): List<Any> {
        if (!isEnabled) return emptyList()

        val compatibleEnchants = getAvailableEnchantments().filter { enchant ->
            isCompatible(item, enchant)
        }

        return compatibleEnchants.shuffled().take(count)
    }

    fun isCompatible(item: ItemStack, enchant: Any): Boolean {
        if (!isEnabled) return false

        return try {
            val canEnchantMethod = enchant.javaClass.getMethod("canEnchantItem", ItemStack::class.java)
            canEnchantMethod.invoke(enchant, item) as Boolean
        } catch (e: Exception) {
            false
        }
    }

    fun getEnchantmentInfo(enchant: Any): Map<String, Any> {
        return mapOf(
            "id" to (getEnchantmentProperty(enchant, "id") ?: "unknown"),
            "name" to (getEnchantmentProperty(enchant, "displayName") ?: "Unknown"),
            "description" to (getEnchantDescription(enchant, 1)),
            "maxLevel" to (getEnchantmentProperty(enchant, "maxLevel") ?: 1),
            "isTreasure" to (getEnchantmentProperty(enchant, "isTreasure") ?: false),
            "isCursed" to (getEnchantmentProperty(enchant, "isCursed") ?: false)
        )
    }

    fun disable() {
        isEnabled = false
        excellentEnchantsPlugin = null
        plugin.logger.info("ExcellentEnchants integration disabled")
    }
}

