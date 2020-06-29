/* Licensed under MIT */
package dev.bloodstone.invisiframes

import org.bstats.bukkit.Metrics
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.plugin.java.JavaPlugin

public class InvisiFrames() : JavaPlugin() {

    private val issuesURL = "https://github.com/Prof-Bloodstone/InvisiFrames/issues"
    private val issuesLog = arrayOf(
        "This is probably an error - please report to $issuesURL.",
        "Include all relevant logs and the configuration file."
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
        try {
            configManager.reloadConfig()
        } catch (e: InvalidConfigurationException) {
            logger.severe("ERROR: $e")
            logger.severe("Will disable now")
            pluginLoader.disablePlugin(this)
            return
        }
        getArmorStandEditorWandFlag()
        registerBstats()
        val commandManager = CommandManager(this)
        commandManager.registerCommands()
        server.pluginManager.registerEvents(WandListener(this), this)
        registerAll()
        isFullyEnabled = true
    }

    fun reload() {
        // Reload configuration
        unregisterAll()
        try {
            configManager.reloadConfig()
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
            if (wand != null) {
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
