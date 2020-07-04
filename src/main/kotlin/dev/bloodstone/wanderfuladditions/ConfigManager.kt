/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import ch.jalu.configme.SettingsManagerBuilder
import ch.jalu.configme.exception.ConfigMeException
import ch.jalu.configme.properties.Property
import dev.bloodstone.mcutils.EnableableEntry
import dev.bloodstone.mcutils.PersistentNamespacedFlag
import dev.bloodstone.mcutils.recipes.WanderingTraderRecipe
import dev.bloodstone.wanderfuladditions.plugin_wrappers.ArmorStandEditorWrapper
import java.io.File
import org.bukkit.NamespacedKey
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.persistence.PersistentDataType

data class Wand(
    val isEnabled: Boolean,
    val item: ItemStack,
    val crafting: EnableableEntry<ShapedRecipe>,
    val trading: EnableableEntry<WanderingTraderRecipe>,
    val flag: PersistentNamespacedFlag<*>
)

enum class WandType {
    ITEM_FRAME,
    ARMOR_STAND
}

class ConfigManager(private val plugin: WanderfulAdditions) {
    lateinit var wands: Map<WandType, Wand>
    private val aseWrapper = ArmorStandEditorWrapper()
    private val pluginListener = PluginListener(plugin, listOf(aseWrapper))
    private val configFile = File(plugin.dataFolder, "config.yml")
    private val settingsManager = SettingsManagerBuilder
        .withYamlFile(configFile)
        .configurationData(
            WandConfigEntries::class.java
        ).useDefaultMigrationService()
        .create()

    fun init() {
        plugin.server.pluginManager.registerEvents(pluginListener, plugin)
    }
    fun saveConfig() {
        settingsManager.save()
    }
    fun reloadConfig(fromDisk: Boolean = true) {
        if (fromDisk) {
            settingsManager.reload()
            saveConfig()
        }
        loadConfig()
    }
    fun loadConfig() {
        try {
            wands = loadWandConfig()
        } catch (e: IllegalArgumentException) {
            throw InvalidConfigurationException(e.message, e)
        } catch (e: ConfigMeException) {
            throw InvalidConfigurationException(e.message, e)
        }
    }
    private data class WandInfo(
        val property: Property<WandConfig>,
        val flag: PersistentNamespacedFlag<*>,
        val craftingNamespacedKey: NamespacedKey
    )
    private fun loadWandConfig(): Map<WandType, Wand> {
        val wandInfo = mutableMapOf(
            WandType.ITEM_FRAME to WandInfo(
                WandConfigEntries.ITEM_FRAME,
                PersistentNamespacedFlag(
                    NamespacedKey(plugin, "item_frame_wand"),
                    PersistentDataType.BYTE,
                    1
                ),
                NamespacedKey(plugin, "item_frame_crafting")
            )
        )
        if (aseWrapper.isLoaded()) {
            wandInfo[WandType.ARMOR_STAND] = WandInfo(
                WandConfigEntries.ARMOR_STAND,
                aseWrapper.getFlag(),
                NamespacedKey(plugin, "armor_stand_crafting")
            )
        }
        val config = wandInfo.mapValues { (_, v) ->
            val config = settingsManager.getProperty(v.property)
            config.validate()
            loadSingleWandConfig(config, v.flag, v.craftingNamespacedKey)
        }
        val aseConfig = config[WandType.ARMOR_STAND]
        if (aseConfig != null) {
            aseWrapper.configure(aseConfig)
        }
        return config
    }

    private fun loadSingleWandConfig(config: WandConfig, itemFlag: PersistentNamespacedFlag<*>, craftingNamespacedKey: NamespacedKey): Wand {
        val wand = config.item.asItemStack()
        itemFlag.applyTo(wand)
        val craftingRecipe = config.crafting.asRecipe(wand, craftingNamespacedKey)
        val wanderingTraderRecipe = config.wandering_trader.asWanderingTraderRecipe(plugin, wand)
        return Wand(
            config.isEnabled,
            wand,
            EnableableEntry(config.crafting.isEnabled, craftingRecipe),
            EnableableEntry(config.wandering_trader.isEnabled, wanderingTraderRecipe),
            itemFlag
        )
    }
}
