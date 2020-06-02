/* Licensed under MIT */
package dev.bloodstone.invisiframes

import dev.bloodstone.invisiframes.utils.amount
import dev.bloodstone.invisiframes.utils.lore
import dev.bloodstone.invisiframes.utils.name
import dev.bloodstone.invisiframes.utils.safeMeta
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe

class ConfigManager(private val plugin: InvisiFrames) {
    lateinit var wand: ItemStack
    lateinit var recipe: Recipe
    var recipeEnabled = false

    fun loadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        // We first create and then overwrite values, in case they throw error
        val wand = loadWand()
        val recipe = loadRecipe(wand)
        this.wand = wand
        this.recipe = recipe
    }
    fun loadWand(): ItemStack {
        val section = plugin.config.getConfigurationSectionOrThrow("wand")
        val material = section.getMaterial("type")
        if (material.isAir) throw InvalidConfigurationException("Specified material '$material' is an air type, which is not allowed!")
        val name = section.getStringOrThrow("name")
        val lore = section.getStringList("lore")
        return createWand(material, name, lore)
    }
    private fun createWand(material: Material, name: String, lore: List<String>): ItemStack {
        val wand = ItemStack(material)
                .amount(1)
                .name(name)
                .lore(lore)
        val wandMeta = wand.safeMeta
        wandMeta.persistentDataContainer.set(plugin.wandNamespacedKey, plugin.wandPersistentDataType, plugin.wandPersistentDataValue)
        wand.safeMeta = wandMeta
        return wand
    }
    fun loadRecipe(wand: ItemStack = this.wand): Recipe {
        val section = plugin.config.getConfigurationSectionOrThrow("recipe")
        recipeEnabled = section.getBoolean("enabled")
        val amount = section.getInt("amount")
        val shape = section.getStringList("shape")
        if (shape.size > 3 || shape.size <= 0) throw InvalidConfigurationException("Shape needs to be a list of 1 to 3 rows!")
        for (row in shape) {
            if (row.length > 3 || row.isEmpty()) throw InvalidConfigurationException("Rows can have up to 3 items!")
        }
        val ingredientsList = shape.joinToString(separator = "").toSet()
        val ingredients = section.getConfigurationSectionOrThrow("ingredients")
        val ingredientMap = HashMap<Char, Material>()
        for (ingredient in ingredientsList) {
            ingredientMap[ingredient] = ingredients.getMaterial(ingredient.toString())
        }
        return createRecipe(wand, amount, shape, ingredientMap)
    }
    private fun createRecipe(wand: ItemStack, amount: Int, shape: List<String>, ingredientMap: Map<Char, Material>): Recipe {
        val recipe = ShapedRecipe(plugin.recipeNamespacedKey, wand.clone().amount(amount))
        recipe.shape(*shape.toTypedArray())
        for ((char, material) in ingredientMap) {
            recipe.setIngredient(char, material)
        }
        return recipe
    }
}

fun FileConfiguration.getConfigurationSectionOrThrow(path: String): ConfigurationSection {
    return getConfigurationSection(path) ?: throw InvalidConfigurationException("Missing $path section!")
}

fun ConfigurationSection.getConfigurationSectionOrThrow(path: String): ConfigurationSection {
    return getConfigurationSection(path) ?: throw InvalidConfigurationException("Missing $currentPath.$path section!")
}

fun ConfigurationSection.getStringOrThrow(path: String): String {
    return getString(path) ?: throw InvalidConfigurationException("$currentPath.$path does not contain a string!")
}

fun ConfigurationSection.getMaterial(path: String): Material {
    val type = getStringOrThrow(path)
    return Material.matchMaterial(type) ?: throw InvalidConfigurationException("'$type' in '$currentPath.$path' is not a valid material!")
}
