/* Licensed under MIT */
package dev.bloodstone.invisiframes

import ch.jalu.configme.SettingsManagerBuilder
import dev.bloodstone.mcutils.extensions.itemstack.addGlow
import dev.bloodstone.mcutils.extensions.itemstack.amount
import dev.bloodstone.mcutils.extensions.itemstack.lore
import dev.bloodstone.mcutils.extensions.itemstack.name
import dev.bloodstone.mcutils.extensions.itemstack.notNullMeta
import java.io.File
import kotlin.random.Random
import kotlin.random.nextInt
import org.bukkit.Material
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapedRecipe

data class CraftingRecipe(val isEnabled: Boolean, val recipe: Recipe)
data class Ingredient(val item: ItemStack, val priceRange: IntRange) {
    constructor(type: Material, priceRange: IntRange) : this(ItemStack(type), priceRange)
    init {
        require(priceRange.first > 0) { "Ingredient min price can't be 0 or less (got: ${priceRange.first})." }
        require(priceRange.last <= 64) { "Ingredient max price can't exceed 64 (got: ${priceRange.last})." }
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
        require(uses.first > 0) { "Trader recipe uses can't be 0 or less (got: ${uses.first})." }
        require(chance > 0) { "Trader recipe chance can't be 0 or less (got: $chance)." }
        require(chance <= 100) { "Trader recipe chance can't be over 100 (got: $chance)." }
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
    private val configFile = File(plugin.dataFolder, "config.yml")
    private val settingsManager = SettingsManagerBuilder
        .withYamlFile(configFile)
        .configurationData(
            WandEntry::class.java,
            RecipeEntry::class.java,
            WanderingTraderEntry::class.java
        ).useDefaultMigrationService()
        .create()

    fun saveConfig() {
        settingsManager.save()
    }
    fun reloadConfig() {
        settingsManager.reload()
        saveConfig()
        loadConfig()
    }
    fun loadConfig() {
        // We first create and then overwrite values, in case they throw error
        try {
            val wand = loadWand()
            val craftingRecipe = loadCraftingRecipe(wand)
            val wanderingTraderRecipe = loadWanderingTraderRecipe(wand)
            this.wand = wand
            this.craftingRecipe = craftingRecipe
            this.wanderingTraderRecipe = wanderingTraderRecipe
        } catch (e: IllegalArgumentException) {
            throw InvalidConfigurationException(e.message, e)
        }
    }

    private fun loadWand(): ItemStack {
        val material = Material.valueOf(settingsManager.getProperty(WandEntry.TYPE))
        if (material.isAir) throw InvalidConfigurationException("Specified material '${material.name}' is an air type, which is not allowed!")
        val name = settingsManager.getProperty(WandEntry.NAME)
        val lore = settingsManager.getProperty(WandEntry.LORE)
        val glow = settingsManager.getProperty(WandEntry.GLOW)
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
        val enabled = settingsManager.getProperty(RecipeEntry.ENABLED)
        val shape = settingsManager.getProperty(RecipeEntry.SHAPE)
        val ingredients = settingsManager.getProperty(RecipeEntry.INGREDIENTS)
        val amount = settingsManager.getProperty(RecipeEntry.AMOUNT)
        if (shape.size > 3 || shape.size <= 0) throw InvalidConfigurationException("Shape needs to be a list of 1 to 3 rows!")
        for (row in shape) {
            if (row.length > 3 || row.isEmpty()) throw InvalidConfigurationException("Rows can have up to 3 items!")
        }
        val ingredientsList = shape.joinToString(separator = "").toSet()
        val ingredientMap = HashMap<Char, Material>()
        for (ingredient in ingredientsList) {
            val ingredientName = ingredients[ingredient.toString()]
                ?: throw InvalidConfigurationException("No such ingredient mapping for key '$ingredient'")
            ingredientMap[ingredient] = Material.valueOf(ingredientName)
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
        val enabled = settingsManager.getProperty(WanderingTraderEntry.ENABLED)
        val chance = settingsManager.getProperty(WanderingTraderEntry.CHANCE)
        val minUses = settingsManager.getProperty(WanderingTraderEntry.MIN_USES)
        val maxUses = settingsManager.getProperty(WanderingTraderEntry.MAX_USES)
        val uses = IntRange(minUses, maxUses)
        if (uses.first < 1) throw InvalidConfigurationException("Wandering Trader recipe minimum uses can't be less than 1")
        val ingredientsList = settingsManager.getProperty(WanderingTraderEntry.INGREDIENTS)
        if (ingredientsList.size !in 1..2)
            throw InvalidConfigurationException("Wandering Trader needs to have 1 or 2 ingredients!")
        val ingredients = ingredientsList.map {
            val material = Material.valueOf(it.type)
            Ingredient(material, it.count.asRange())
        }
        return TradingRecipe(enabled, wand, uses, ingredients, chance)
    }
}
