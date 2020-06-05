/* Licensed under MIT */
package dev.bloodstone.invisiframes

import dev.bloodstone.mcutils.extensions.itemstack.addGlow
import dev.bloodstone.mcutils.extensions.itemstack.amount
import dev.bloodstone.mcutils.extensions.itemstack.lore
import dev.bloodstone.mcutils.extensions.itemstack.name
import dev.bloodstone.mcutils.extensions.itemstack.notNullMeta
import kotlin.random.Random
import kotlin.random.nextInt
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe

data class CraftingRecipe(val isEnabled: Boolean, val recipe: Recipe)
data class Ingredient(val item: ItemStack, val priceRange: IntRange) {
    constructor(type: Material, priceRange: IntRange) : this(ItemStack(type), priceRange)
    init {
        require(priceRange.first > 0) { "priceRange can't be 0 or less!" }
        require(priceRange.last <= 64) { "priceRange can't exceed 64!" }
    }
    fun getWithRandomCount(): ItemStack {
        val amount = Random.nextInt(priceRange)
        return this.item.clone().amount(amount)
    }
}
data class TradingRecipe(
    val isEnabled: Boolean,
    val result: ItemStack,
    val uses: IntRange,
    val ingredients: List<Ingredient>,
    val chance: Int
) {
    init {
        require(uses.first > 0) { "Trader recipe uses can't be 0 or less!" }
        require(chance > 0) { "Trader recipe chance can't be 0 or less!" }
        require(chance <= 100) { "Trader recipe chance can't be over 100!" }
    }
    fun getWithRandomIngredientAndUseCount(): MerchantRecipe {
        val recipe = MerchantRecipe(result, Random.nextInt(uses))
        recipe.ingredients = ingredients.map { it.getWithRandomCount() }
        return recipe
    }
}

class ConfigManager(private val plugin: InvisiFrames) {
    lateinit var wand: ItemStack
    lateinit var craftingRecipe: CraftingRecipe
    lateinit var wanderingTraderRecipe: TradingRecipe

    private val wandSection
        get() = plugin.config.getConfigurationSectionOrThrow("wand")
    private val recipeSection
        get() = plugin.config.getConfigurationSectionOrThrow("recipe")
    private val wanderingTraderSection
        get() = plugin.config.getConfigurationSectionOrThrow("wandering_trader")

    fun reloadConfig() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()
        // We first create and then overwrite values, in case they throw error
        val wand = loadWand()
        val craftingRecipe = loadCraftingRecipe(wand)
        val wanderingTraderRecipe = loadWanderingTraderRecipe(wand)
        this.wand = wand
        this.craftingRecipe = craftingRecipe
        this.wanderingTraderRecipe = wanderingTraderRecipe
    }

    private fun loadWand(): ItemStack {
        val section = wandSection
        val material = section.getMaterial("type")
        if (material.isAir) throw InvalidConfigurationException("Specified material '$material' is an air type, which is not allowed!")
        val name = section.getStringOrThrow("name")
        val lore = section.getStringList("lore")
        val glow = section.getBoolean("glow")
        return createWand(material, name, lore, glow)
    }
    private fun createWand(material: Material, name: String, lore: List<String>, glow: Boolean): ItemStack {
        val wand = ItemStack(material)
                .amount(1)
                .name(name)
                .lore(lore)
        if (glow) {
            wand.addGlow()
        }
        val wandMeta = wand.notNullMeta
        wandMeta.persistentDataContainer.set(plugin.wandNamespacedKey, plugin.wandPersistentDataType, plugin.wandPersistentDataValue)
        wand.notNullMeta = wandMeta
        return wand
    }

    private fun loadCraftingRecipe(wand: ItemStack = this.wand): CraftingRecipe {
        val section = recipeSection
        val enabled = section.getBoolean("enabled")
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
        val recipe = createCraftingRecipe(wand, amount, shape, ingredientMap)
        return CraftingRecipe(enabled, recipe)
    }
    private fun createCraftingRecipe(wand: ItemStack, amount: Int, shape: List<String>, ingredientMap: Map<Char, Material>): Recipe {
        val recipe = ShapedRecipe(plugin.recipeNamespacedKey, wand.clone().amount(amount))
        recipe.shape(*shape.toTypedArray())
        for ((char, material) in ingredientMap) {
            recipe.setIngredient(char, material)
        }
        return recipe
    }

    private fun loadWanderingTraderRecipe(wand: ItemStack = this.wand): TradingRecipe {
        val section = wanderingTraderSection
        val enabled = section.getBoolean("enabled")
        val chance = section.getInt("chance")
        val useSection = section.getConfigurationSectionOrThrow("uses")
        val uses = sectionToIntRange(useSection)
        if (uses.first < 1) throw InvalidConfigurationException("${useSection.currentPath}.min can't be less than 1")
        val ingredientsList = section.getMapList("ingredients")
        if (ingredientsList.size !in 1..2)
            throw InvalidConfigurationException("${section.currentPath}.ingredients needs to have 1 or 2 ingredients!")
        val ingredients = ingredientsList.mapIndexed { index, entry ->
            val sectionPath = "${section.currentPath}.ingredients[$index]"
            val type = entry["type"] as? String
                ?: throw InvalidConfigurationException("'$sectionPath.type' is not a string!")
            val material = Material.matchMaterial(type)
                ?: throw InvalidConfigurationException("'$type' in '$sectionPath.type' is not a valid material!")
            val countEntry = entry["count"] as? Map<*, *>
                ?: throw InvalidConfigurationException("$sectionPath.count is not a map!")
            val min = countEntry["min"] as? Int
                ?: throw InvalidConfigurationException("$sectionPath.count.min is not an integer!")
            val max = countEntry["max"] as? Int
                ?: throw InvalidConfigurationException("$sectionPath.count.max is not an integer!")
            val count = IntRange(min, max)
            Ingredient(material, count)
        }
        return TradingRecipe(enabled, wand, uses, ingredients, chance)
    }
    private fun sectionToIntRange(section: ConfigurationSection): IntRange {
        val min = section.getInt("min")
        val max = section.getInt("max")
        return IntRange(min, max)
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
