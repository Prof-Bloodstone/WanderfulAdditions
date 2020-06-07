/* Licensed under MIT */
package dev.bloodstone.invisiframes

import ch.jalu.configme.Comment
import ch.jalu.configme.SettingsHolder
import ch.jalu.configme.configurationdata.CommentsConfiguration
import ch.jalu.configme.properties.MapProperty
import ch.jalu.configme.properties.PropertyInitializer.listProperty
import ch.jalu.configme.properties.PropertyInitializer.newProperty
import ch.jalu.configme.properties.StringListProperty
import ch.jalu.configme.properties.types.BeanPropertyType
import ch.jalu.configme.properties.types.PrimitivePropertyType

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

object WandEntry : CommentedSettingsHolder() {
    override val basePath = "wand"
    override val sectionComment = arrayOf(
        "Configure the wand that can be used to toggle item frame visibility states.",
        "It will be the result of crafting/trading."
    )

    @JvmField
    @Comment("The item type that'll be used for the wand.", "For possible values, see: $TYPE_URL")
    var TYPE = newProperty("$basePath.type", "WOODEN_SWORD")

    @JvmField
    @Comment(SUPPORTS_COLOR_CODES)
    var NAME = newProperty("$basePath.name", "&6Item Frame Wand")

    @JvmField
    @Comment("Lore is shown under the item name.", SUPPORTS_COLOR_CODES)
    var LORE = StringListProperty("$basePath.lore", arrayListOf(
        "&2Now you see me,",
        "&4now you don't"
    ))

    @JvmField
    @Comment("Whether the wand should have a glow or not.")
    var GLOW = newProperty("$basePath.glow", true)
}

object RecipeEntry : CommentedSettingsHolder() {
    override val basePath = "recipe"
    override val sectionComment = arrayOf("Configure crafting recipe for the wand.")

    @JvmField
    var ENABLED = newProperty("$basePath.enabled", true)

    @JvmField
    @Comment(
        "The shape player needs to put the ingredients in to craft the wand.",
        "Needs to be a list of 1 to 3 rows, each with 1 to 3 ingredient placeholders."
    )
    var SHAPE = StringListProperty("$basePath.shape", arrayListOf(
        "E",
        "E",
        "S"
    ))

    @JvmField
    @Comment(
        "Mapping between ingredient placeholder (used in shape), and ingredient type.",
        "For possible values, see: $TYPE_URL"
    )
    var INGREDIENTS = MapProperty("$basePath.ingredients", mapOf(
        "E" to "EMERALD",
        "S" to "STICK"
    ), PrimitivePropertyType.STRING)

    @JvmField
    @Comment("How many wands should player be given.")
    var AMOUNT = newProperty("$basePath.amount", 1)
}

data class RangeField(var min: Int = 20, var max: Int = 40) {
    fun asRange(): IntRange {
        return IntRange(min, max)
    }
}

data class TradeIngredientField(var type: String = "EMERALD", var count: RangeField = RangeField())

object WanderingTraderEntry : CommentedSettingsHolder() {
    override val basePath = "wandering_trader"
    override val sectionComment = arrayOf("Configure Wandering Trader selling the wand.")

    private val usesPath = "$basePath.uses"

    override fun registerComments(conf: CommentsConfiguration?) {
        super.registerComments(conf)
        conf?.setComment(usesPath, "How many wands will be available for trade.")
    }

    @JvmField
    var ENABLED = newProperty("$basePath.enabled", true)

    @JvmField
    @Comment("What are the chances of a wandering trader having the recipe - from 1 to 100")
    var CHANCE = newProperty("$basePath.chance", 10)

    @JvmField
    var MIN_USES = newProperty("$usesPath.min", 2)

    @JvmField
    var MAX_USES = newProperty("$usesPath.max", 5)

    @JvmField
    @Comment(
        "List of 1 or 2 ingredients required to buy the wand.",
        "For available types, see: $TYPE_URL",
        "A random amount of items will be chosen between `count.min` and `count.max`."
    )
    var INGREDIENTS = listProperty(BeanPropertyType.of(TradeIngredientField::class.java))
        .path("$basePath.ingredients")
        .defaultValue(arrayListOf(
            TradeIngredientField()
        ))
        .build()
}
