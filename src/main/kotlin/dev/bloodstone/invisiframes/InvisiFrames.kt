/* Licensed under MIT */
package dev.bloodstone.invisiframes

import org.bstats.bukkit.Metrics
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

    val bstatsPluginId = 7788

    private val configManager = ConfigManager(this)
    private val wanderingTraderListener = WanderingTraderListener(this)
    private var isFullyEnabled = false

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
        registerBstats()
        val commandManager = CommandManager(this)
        commandManager.registerCommands()
        server.pluginManager.registerEvents(WandListener(this), this)
        registerWanderingTrader()
        registerRecipe()
        isFullyEnabled = true
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
        if (isFullyEnabled) unregisterRecipe()
        // Listeners and commands are disabled automatically
        isFullyEnabled = false
    }

    private fun registerBstats() {
        val metrics = Metrics(this, bstatsPluginId)

        val obtainMethods = mutableListOf<String>()
        if (craftingRecipe.isEnabled) obtainMethods.add("Crafting")
        if (wanderingTraderRecipe.isEnabled) obtainMethods.add("WanderingTrader")
        val obtainMethodString = if (obtainMethods.isNotEmpty()) obtainMethods.joinToString(separator = " + ") else "None"
        metrics.addCustomChart(Metrics.SimplePie("wand_obtaining_method") { -> obtainMethodString })
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
