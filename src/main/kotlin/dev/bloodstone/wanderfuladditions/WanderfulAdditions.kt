/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import org.bstats.bukkit.Metrics
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.plugin.java.JavaPlugin

public class WanderfulAdditions() : JavaPlugin() {

    private val issuesURL = "https://github.com/Prof-Bloodstone/WanderfulAdditions/issues"
    private val issuesLog = arrayOf(
        "This is probably an error - please report to $issuesURL.",
        "Include all relevant logs and the configuration file.",
        "It's advised to restart the server since plugin might be in a partial state. Sorry :("
    ).joinToString(separator = "\n")
    private val bstatsPluginId = 7788

    private val configManager = ConfigManager(this)
    private var isFullyEnabled = false
    val wands
        get() = configManager.wands

    val enabledWands
        get() = wands.values.filter { wand -> wand.isEnabled }

    override fun onEnable() {
        super.onEnable()
        configManager.init()
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
        server.pluginManager.registerEvents(ItemFrameListener(this), this)
        registerAll()
        isFullyEnabled = true
    }

    fun reload(fromDisk: Boolean = true) {
        // Reload configuration
        unregisterAll()
        try {
            configManager.reloadConfig(fromDisk)
        } finally {
            registerAll()
        }
    }

    fun logIssue(msg: String) {
        logger.severe("$msg\n$issuesLog")
    }

    override fun onDisable() {
        super.onDisable()
        if (isFullyEnabled) unregisterAll()
        // Listeners and commands are disabled automatically
        isFullyEnabled = false
    }

    private fun registerBstats() {
        val metrics = Metrics(this, bstatsPluginId)

        for (wand_type in WandType.values()) {
            val obtainMethods = mutableListOf<String>()
            val wand = wands[wand_type]
            if (wand != null && wand.isEnabled) {
                if (wand.crafting.isEnabled) obtainMethods.add("Crafting")
                if (wand.trading.isEnabled) obtainMethods.add("WanderingTrader")
            }
            val obtainMethodString =
                if (obtainMethods.isNotEmpty()) obtainMethods.joinToString(separator = " + ") else "None"
            metrics.addCustomChart(Metrics.SimplePie("${wand_type.toString().toLowerCase()}_wand_obtaining_method") { -> obtainMethodString })
        }
    }

    private fun registerAll() {
        for (wand in wands.values) {
            if (wand.isEnabled) {
                if (wand.crafting.isEnabled) server.addRecipe(wand.crafting.entry)
                if (wand.trading.isEnabled) wand.trading.entry.register()
            }
        }
    }

    private fun unregisterAll() {
        for (wand in wands.values) {
            if (wand.isEnabled) {
                if (wand.crafting.isEnabled) server.removeRecipe(wand.crafting.entry.key)
                if (wand.trading.isEnabled) wand.trading.entry.unregister()
            }
        }
    }
}
