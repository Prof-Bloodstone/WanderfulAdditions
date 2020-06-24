/* Licensed under MIT */
package dev.bloodstone.invisiframes

import ch.jalu.configme.Comment
import ch.jalu.configme.SettingsHolder
import ch.jalu.configme.configurationdata.CommentsConfiguration
import ch.jalu.configme.properties.Property
import ch.jalu.configme.properties.PropertyInitializer.newBeanProperty
import dev.bloodstone.mcutils.extensions.itemstack.addGlow
import dev.bloodstone.mcutils.extensions.itemstack.amount
import dev.bloodstone.mcutils.extensions.itemstack.lore
import dev.bloodstone.mcutils.extensions.itemstack.name
import dev.bloodstone.mcutils.recipes.RandomizedItemStack
import dev.bloodstone.mcutils.recipes.TradingRecipe
import dev.bloodstone.mcutils.recipes.WanderingTraderRecipe
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.plugin.Plugin

const val TYPE_URL = "https://papermc.io/javadocs/paper/1.15/org/bukkit/Material.html"
const val SUPPORTS_COLOR_CODES = "Supports color codes escaped by '&' sign."

open class CommentedSettingsHolder : SettingsHolder {
    protected open val basePath: String? = null
    protected open val sectionComment: Array<String>? = null
    override fun registerComments(conf: CommentsConfiguration?) {
        super.registerComments(conf)
        if (basePath == null || sectionComment == null) return
        conf?.setComment(basePath,
            *sectionComment!!
        )
    }
}

data class WandItemConfig(
    var type: String = "WOODEN_SWORD",
    var name: String = "&6Magic Wand",
    var lore: List<String> = emptyList(),
    var glow: Boolean = true
) {
    fun validate() {
        if (material.isAir)
            throw InvalidConfigurationException("Specified material '${material.name}' is an air type, which is not allowed!")
    }
    val material: Material
        get() {
            return Material.valueOf(type)
        }

    fun asItemStack(): ItemStack {
        return ItemStack(material)
            .amount(1)
            .name(name)
            .lore(lore)
            .also {
                if (glow) {
                    it.addGlow()
                }
            }
    }
}

data class CraftingConfig(
    var isEnabled: Boolean = false,
    var shape: List<String> = emptyList(),
    var ingredients: Map<String, String> = emptyMap(),
    var amount: Int = 1
) {
    val materializedIngredients: Map<Char, Material>
        get() {
            return ingredients.mapValues { (_, materialName) ->
                getValidatedMaterial(materialName)
            }.mapKeys { (key, _) ->
                require(key.length == 1)
                key[0]
            }
        }

    fun validate() {
        if (shape.size > 3 || shape.isEmpty()) throw InvalidConfigurationException("Shape needs to be a list of 1 to 3 rows!")
        for (row in shape) {
            if (row.length > 3 || row.isEmpty()) throw InvalidConfigurationException("Rows can have up to 3 items!")
        }
        ingredients.keys.forEach { ingredientKey ->
            if (ingredientKey.length != 1)
                throw InvalidConfigurationException("Expected 1 char long ingredient keys - got '$ingredientKey' instead.")
        }
        val ingredientsList = shape.joinToString(separator = "").toSet()
        for (ingredient in ingredientsList) {
            materializedIngredients[ingredient]
                ?: throw InvalidConfigurationException("No such ingredient mapping for key '$ingredient'")
        }
    }

    fun asRecipe(result: ItemStack, namespacedKey: NamespacedKey): ShapedRecipe {
        val recipe = ShapedRecipe(namespacedKey, result.clone().amount(amount))
        recipe.shape(*shape.toTypedArray())
        for ((char, material) in materializedIngredients) {
            recipe.setIngredient(char, material)
        }
        return recipe
    }
}

data class RangeField(var min: Int = 20, var max: Int = 40) {
    fun asRange(): IntRange {
        return IntRange(min, max)
    }
}

data class TradeIngredientField(var type: String = "EMERALD", var count: RangeField = RangeField()) {
    val material: Material
            get() = getValidatedMaterial(type)
    fun validate() {
        material
    }
}

data class WanderingTraderConfig(
    var isEnabled: Boolean = false,
    var chance: Int = 5,
    var uses: RangeField = RangeField(2, 5),
    var ingredients: List<TradeIngredientField> = listOf(
        TradeIngredientField()
    )
) {
    fun validate() {
        ingredients.forEach(TradeIngredientField::validate)
    }
    fun asTradingRecipe(result: ItemStack): TradingRecipe {
        val tradingIngredients = ingredients.map {
            RandomizedItemStack(it.material, it.count.asRange())
        }
        return TradingRecipe(result, uses.asRange(), tradingIngredients)
    }

    fun asWanderingTraderRecipe(plugin: Plugin, result: ItemStack): WanderingTraderRecipe {
        return WanderingTraderRecipe(plugin, chance, asTradingRecipe(result))
    }
}

data class WandConfig(
    var item: WandItemConfig = WandItemConfig(),
    var crafting: CraftingConfig = CraftingConfig(),
    var wandering_trader: WanderingTraderConfig = WanderingTraderConfig()
) {
    fun validate() {
        item.validate()
        crafting.validate()
        wandering_trader.validate()
    }
}

private fun getValidatedMaterial(name: String): Material {
    return Material.matchMaterial(name)
        ?: throw InvalidConfigurationException("Unable to find material '$name'")
}

object WandConfigEntries : CommentedSettingsHolder() {
    override val basePath = "wands"
    override val sectionComment = arrayOf(
        "Configure wands appearance and ways to get them."
    )

    @JvmField
    @Comment("Wand that let's you toggle between visible and invisible item frame.")
    var ITEM_FRAME: Property<WandConfig> = newBeanProperty(
        WandConfig::class.java,
        "$basePath.item_frame",
        WandConfig(
            item = WandItemConfig(
                type = "WOODEN_SWORD",
                name = "&6Item Frame Wand",
                lore = listOf(
                    "&2Now you see me,",
                    "&4now you don't"
                ),
                glow = true
            ),
            crafting = CraftingConfig(
                isEnabled = false,
                shape = listOf(
                    "E",
                    "E",
                    "S"
                ),
                ingredients = mapOf(
                    "S" to "STICK",
                    "E" to "EMERALD"
                ),
                amount = 1
            )
        )
    )

    /*
    @JvmField
    @Comment("Wand that let's you control armor stands - requires https://github.com/Prof-Bloodstone/ArmorStandEditor")
    var ARMOR_STAND = newBeanProperty(
        WandConfig::class.java,
        "$basePath.armor_stand",
        WandConfig(
            item = WandItemConfig(
                type = "WOODEN_HOE",
                name = "&6Armor Stand Wand",
                lore = listOf(
                    "Controlling the moves!"
                ),
                glow = true
            ),
            crafting = CraftingConfig(
                isEnabled = false,
                shape = listOf(
                    "E",
                    "S",
                    "S"
                ),
                ingredients = mapOf(
                    "S" to "STICK",
                    "E" to "EMERALD"
                ),
                amount = 1
            )
        )
    )
    */
}
