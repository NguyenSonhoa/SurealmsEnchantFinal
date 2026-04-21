package me.sanenuyan.surealmsEnchant.datapack

import me.sanenuyan.surealmsEnchant.SurealmsEnchant
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonArray
import java.io.File
import java.io.FileWriter
import java.util.UUID

/**
 * Generates datapack-style custom enchantments
 */
class DatapackEnchantmentGenerator(private val plugin: SurealmsEnchant) { //if you are good at datapack, recode whole logic :skull:
    
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * Generate all custom enchantments as datapack files
     */
    fun generateCustomEnchantments() {
        val datapackFolder = File(plugin.dataFolder, "generated_datapack")
        
        try {
            // Create datapack structure
            createDatapackStructure(datapackFolder)
            
            // Generate pack.mcmeta
            generatePackMcmeta(datapackFolder)
            
            // Generate custom enchantments
            generateAutoSmeltingEnchantment(datapackFolder)
            generateBackstabEnchantment(datapackFolder)
            generateLuckyOrbEnchantment(datapackFolder)
            
            plugin.logger.info("Generated datapack with custom enchantments at: ${datapackFolder.absolutePath}")
            plugin.logger.info("Copy this folder to your world's datapacks folder to use vanilla-style enchantments")
            
        } catch (e: Exception) {
            plugin.logger.severe("Failed to generate datapack: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Create datapack folder structure
     */
    private fun createDatapackStructure(datapackFolder: File) {
        // Create main folders
        datapackFolder.mkdirs()
        File(datapackFolder, "data").mkdirs()
        File(datapackFolder, "data/surealms").mkdirs()
        File(datapackFolder, "data/surealms/enchantment").mkdirs()
        File(datapackFolder, "data/surealms/tags").mkdirs()
        File(datapackFolder, "data/surealms/tags/enchantment").mkdirs()
    }
    
    /**
     * Generate pack.mcmeta file
     */
    private fun generatePackMcmeta(datapackFolder: File) {
        val packMcmeta = JsonObject().apply {
            val pack = JsonObject().apply {
                addProperty("pack_format", 57) // 1.21.5 format
                addProperty("description", "SurealmsEnchant Custom Enchantments")
            }
            add("pack", pack)
        }
        
        val file = File(datapackFolder, "pack.mcmeta")
        FileWriter(file).use { writer ->
            gson.toJson(packMcmeta, writer)
        }
    }
    
    /**
     * Generate Auto Smelting enchantment
     */
    private fun generateAutoSmeltingEnchantment(datapackFolder: File) {
        val enchantment = JsonObject().apply {
            addProperty("description", "Auto Smelting")
            addProperty("supported_items", "#minecraft:enchantable/mining")
            addProperty("weight", 1) // Very rare
            addProperty("max_level", 1)
            
            // Cost configuration
            add("min_cost", JsonObject().apply {
                addProperty("base", 20)
                addProperty("per_level_above_first", 0)
            })
            add("max_cost", JsonObject().apply {
                addProperty("base", 50)
                addProperty("per_level_above_first", 0)
            })
            
            addProperty("anvil_cost", 8)
            
            // Equipment slots
            add("slots", JsonArray().apply {
                add("mainhand")
            })
            
            // Effects - Custom component for auto smelting
            add("effects", JsonObject().apply {
                add("surealms:auto_smelting", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("id", "auto_smelting_effect")
                        add("smelting_recipes", JsonObject().apply {
                            addProperty("iron_ore", "iron_ingot")
                            addProperty("deepslate_iron_ore", "iron_ingot")
                            addProperty("gold_ore", "gold_ingot")
                            addProperty("deepslate_gold_ore", "gold_ingot")
                            addProperty("copper_ore", "copper_ingot")
                            addProperty("deepslate_copper_ore", "copper_ingot")
                            addProperty("ancient_debris", "netherite_scrap")
                            addProperty("raw_iron", "iron_ingot")
                            addProperty("raw_gold", "gold_ingot")
                            addProperty("raw_copper", "copper_ingot")
                        })
                        addProperty("fortune_compatible", true)
                        addProperty("experience_multiplier", 1.0)
                    })
                })
            })
        }
        
        val file = File(datapackFolder, "data/surealms/enchantment/auto_smelting.json")
        file.parentFile.mkdirs() // Create directories if they don't exist
        FileWriter(file).use { writer ->
            gson.toJson(enchantment, writer)
        }
    }
    
    /**
     * Generate Backstab enchantment
     */
    private fun generateBackstabEnchantment(datapackFolder: File) {
        val enchantment = JsonObject().apply {
            addProperty("description", "Backstab")
            addProperty("supported_items", "#minecraft:enchantable/weapon")
            addProperty("weight", 2) // Rare
            addProperty("max_level", 3)
            
            // Cost configuration
            add("min_cost", JsonObject().apply {
                addProperty("base", 10)
                addProperty("per_level_above_first", 8)
            })
            add("max_cost", JsonObject().apply {
                addProperty("base", 40)
                addProperty("per_level_above_first", 15)
            })
            
            addProperty("anvil_cost", 4)
            
            // Equipment slots
            add("slots", JsonArray().apply {
                add("mainhand")
            })
            
            // Effects - Custom damage modifier
            add("effects", JsonObject().apply {
                add("surealms:backstab", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("id", "backstab_effect")
                        add("damage_modifier", JsonObject().apply {
                            addProperty("type", "minecraft:linear")
                            addProperty("base", 0.2) // 20% base bonus
                            addProperty("per_level_above_first", 0.1) // 10% per level
                        })
                        addProperty("angle_threshold", -0.5) // 120 degrees
                        addProperty("show_particles", true)
                        addProperty("play_sound", true)
                    })
                })
            })
        }
        
        val file = File(datapackFolder, "data/surealms/enchantment/backstab.json")
        file.parentFile.mkdirs() // Create directories if they don't exist
        FileWriter(file).use { writer ->
            gson.toJson(enchantment, writer)
        }
    }
    
    /**
     * Generate Lucky Orb enchantment
     */
    private fun generateLuckyOrbEnchantment(datapackFolder: File) {
        val enchantment = JsonObject().apply {
            addProperty("description", "Lucky Orb")
            addProperty("supported_items", "#minecraft:enchantable/armor")
            addProperty("weight", 5) // Uncommon
            addProperty("max_level", 3)
            
            // Cost configuration
            add("min_cost", JsonObject().apply {
                addProperty("base", 5)
                addProperty("per_level_above_first", 5)
            })
            add("max_cost", JsonObject().apply {
                addProperty("base", 25)
                addProperty("per_level_above_first", 10)
            })
            
            addProperty("anvil_cost", 2)
            
            // Equipment slots
            add("slots", JsonArray().apply {
                add("armor")
            })
            
            // Effects - Experience modifier
            add("effects", JsonObject().apply {
                add("surealms:lucky_orb", JsonArray().apply {
                    add(JsonObject().apply {
                        addProperty("id", "lucky_orb_effect")
                        add("experience_modifier", JsonObject().apply {
                            addProperty("type", "minecraft:linear")
                            addProperty("base", 0.5) // 50% base bonus
                            addProperty("per_level_above_first", 0.25) // 25% per level
                        })
                        addProperty("stacks_across_armor", true)
                        addProperty("show_particles", true)
                    })
                })
            })
        }
        
        val file = File(datapackFolder, "data/surealms/enchantment/lucky_orb.json")
        file.parentFile.mkdirs()
        FileWriter(file).use { writer ->
            gson.toJson(enchantment, writer)
        }
    }
    
    /**
     * Generate enchantment tags for organization
     */
    private fun generateEnchantmentTags(datapackFolder: File) {
        // Custom enchantments tag
        val customEnchantmentsTag = JsonObject().apply {
            addProperty("replace", false)
            add("values", JsonArray().apply {
                add("surealms:auto_smelting")
                add("surealms:backstab")
                add("surealms:lucky_orb")
            })
        }
        
        val tagFile = File(datapackFolder, "data/surealms/tags/enchantment/custom_enchantments.json")
        tagFile.parentFile.mkdirs() // Create directories if they don't exist
        FileWriter(tagFile).use { writer ->
            gson.toJson(customEnchantmentsTag, writer)
        }
        
        // Legendary enchantments tag
        val legendaryTag = JsonObject().apply {
            addProperty("replace", false)
            add("values", JsonArray().apply {
                add("surealms:auto_smelting")
            })
        }
        
        val legendaryFile = File(datapackFolder, "data/surealms/tags/enchantment/legendary.json")
        FileWriter(legendaryFile).use { writer ->
            gson.toJson(legendaryTag, writer)
        }
    }
    
    /**
     * Generate advancement for obtaining custom enchantments
     */
    private fun generateAdvancements(datapackFolder: File) {
        val advancementFolder = File(datapackFolder, "data/surealms/advancement")
        advancementFolder.mkdirs()
        
        val advancement = JsonObject().apply {
            add("display", JsonObject().apply {
                add("icon", JsonObject().apply {
                    addProperty("item", "minecraft:enchanted_book")
                })
                addProperty("title", "Master Enchanter")
                addProperty("description", "Obtain all SurealmsEnchant custom enchantments")
                addProperty("frame", "challenge")
                addProperty("show_toast", true)
                addProperty("announce_to_chat", true)
            })
            
            add("criteria", JsonObject().apply {
                add("auto_smelting", JsonObject().apply {
                    addProperty("trigger", "minecraft:enchanted_item")
                    add("conditions", JsonObject().apply {
                        add("item", JsonObject().apply {
                            add("enchantments", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("enchantment", "surealms:auto_smelting")
                                })
                            })
                        })
                    })
                })
                
                add("backstab", JsonObject().apply {
                    addProperty("trigger", "minecraft:enchanted_item")
                    add("conditions", JsonObject().apply {
                        add("item", JsonObject().apply {
                            add("enchantments", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("enchantment", "surealms:backstab")
                                })
                            })
                        })
                    })
                })
                
                add("lucky_orb", JsonObject().apply {
                    addProperty("trigger", "minecraft:enchanted_item")
                    add("conditions", JsonObject().apply {
                        add("item", JsonObject().apply {
                            add("enchantments", JsonArray().apply {
                                add(JsonObject().apply {
                                    addProperty("enchantment", "surealms:lucky_orb")
                                })
                            })
                        })
                    })
                })
            })
            
            add("requirements", JsonArray().apply {
                add(JsonArray().apply {
                    add("auto_smelting")
                    add("backstab")
                    add("lucky_orb")
                })
            })
        }
        
        val file = File(advancementFolder, "master_enchanter.json")
        FileWriter(file).use { writer ->
            gson.toJson(advancement, writer)
        }
    }
    
    /**
     * Generate complete datapack with all files
     */
    fun generateCompleteDatapack() {
        val datapackFolder = File(plugin.dataFolder, "surealms_enchantments_datapack")
        
        // Generate main structure
        generateCustomEnchantments()
        
        // Generate additional files
        generateEnchantmentTags(datapackFolder)
        generateAdvancements(datapackFolder)
        
        // Create installation instructions
        generateInstallationInstructions(datapackFolder)
    }
    
    /**
     * Generate installation instructions
     */
    private fun generateInstallationInstructions(datapackFolder: File) {
        val instructions = """
# SurealmsEnchant Datapack Installation

## Installation Steps:
1. Copy the 'surealms_enchantments_datapack' folder to your world's datapacks folder
2. Restart your server or run `/reload`
3. Custom enchantments will be available in enchanting tables and commands

## Custom Enchantments:
- **surealms:auto_smelting** - Automatically smelts ores when mining (Legendary)
- **surealms:backstab** - Deal more damage when attacking from behind (Rare)
- **surealms:lucky_orb** - Gain more EXP when collecting orbs (Uncommon)

## Commands:
- `/enchant @s surealms:auto_smelting 1`
- `/enchant @s surealms:backstab 3`
- `/enchant @s surealms:lucky_orb 3`

## Give Commands:
- `/give @s diamond_pickaxe{Enchantments:[{id:"surealms:auto_smelting",lvl:1}]}`
- `/give @s diamond_sword{Enchantments:[{id:"surealms:backstab",lvl:3}]}`
- `/give @s diamond_helmet{Enchantments:[{id:"surealms:lucky_orb",lvl:3}]}`

## Notes:
- These enchantments work like vanilla enchantments
- They appear in enchanting tables with configured weights
- Compatible with anvils, grindstones, and all vanilla systems
- Effects are handled by the SurealmsEnchant plugin
        """.trimIndent()
        
        val file = File(datapackFolder, "INSTALLATION.md")
        file.writeText(instructions)
    }
}
