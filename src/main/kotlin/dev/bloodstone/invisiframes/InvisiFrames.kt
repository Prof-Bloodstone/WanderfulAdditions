/* Licensed under MIT */
package dev.bloodstone.invisiframes

import org.bukkit.NamespacedKey
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

public class InvisiFrames() : JavaPlugin() {
    val wandNamespacedKey = NamespacedKey(this, "wand")
    val wandPersistentDataType = PersistentDataType.BYTE!!
    val wandPersistentDataValue: Byte = 1

    val recipeNamespacedKey = NamespacedKey(this, "wandRecipe")

    private val configManager = ConfigManager(this)
    private val wanderingTraderListener = WanderingTraderListener(this)

    val wand: ItemStack
        get() = configManager.wand

    val craftingRecipe: CraftingRecipe
        get() = configManager.craftingRecipe

    val wanderingTraderRecipe: TradingRecipe
        get() = configManager.wanderingTraderRecipe

    override fun onEnable() {
        super.onEnable()
        try {
            configManager.reloadConfig()
        } catch (e: InvalidConfigurationException) {
            logger.severe("ERROR: $e")
            logger.severe("Will disable now")
            pluginLoader.disablePlugin(this)
            return
        }
        val commandManager = CommandManager(this)
        commandManager.registerCommands()
        server.pluginManager.registerEvents(WandListener(this), this)
        registerWanderingTrader()
        registerRecipe()
    }

    fun reload() {
        // Reload configuration
        unregisterRecipe()
        unregisterWanderingTrader()
        try {
            configManager.reloadConfig()
        } finally {
            registerRecipe()
            registerWanderingTrader()
        }
    }

    override fun onDisable() {
        super.onDisable()
        unregisterRecipe()
        // Listeners and commands are disabled automatically
    }

    private fun registerWanderingTrader() {
        if (wanderingTraderRecipe.isEnabled) server.pluginManager.registerEvents(wanderingTraderListener, this)
    }

    private fun unregisterWanderingTrader() {
        if (wanderingTraderRecipe.isEnabled)
            CreatureSpawnEvent.getHandlerList().unregister(wanderingTraderListener)
    }

    private fun registerRecipe() {
        if (craftingRecipe.isEnabled) server.addRecipe(craftingRecipe.recipe)
    }

    private fun unregisterRecipe() {
        if (craftingRecipe.isEnabled) server.removeRecipe(recipeNamespacedKey)
    }
}
