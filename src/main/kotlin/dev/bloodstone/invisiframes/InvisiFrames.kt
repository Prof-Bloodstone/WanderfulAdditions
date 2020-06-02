/* Licensed under MIT */
package dev.bloodstone.invisiframes

import org.bukkit.NamespacedKey
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

public class InvisiFrames() : JavaPlugin() {
    val wandNamespacedKey = NamespacedKey(this, "wand")
    val wandPersistentDataType = PersistentDataType.BYTE!!
    val wandPersistentDataValue: Byte = 1

    val recipeNamespacedKey = NamespacedKey(this, "wandRecipe")

    val configManager = ConfigManager(this)

    val recipeEnabled: Boolean
        get() = configManager.recipeEnabled

    val wand: ItemStack
        get() = configManager.wand

    val recipe: Recipe
        get() = configManager.recipe

    override fun onEnable() {
        super.onEnable()
        try {
            configManager.loadConfig()
        } catch (e: InvalidConfigurationException) {
            logger.severe("ERROR: $e")
            logger.severe("Will disable now")
            pluginLoader.disablePlugin(this)
            return
        }
        val commandManager = CommandManager(this)
        commandManager.registerCommands()
        server.pluginManager.registerEvents(InvisiFramesListener(this), this)
        registerRecipe()
    }

    fun reload() {
        // Reload configuration
        unregisterRecipe()
        configManager.loadConfig()
        registerRecipe()
    }

    override fun onDisable() {
        super.onDisable()
        unregisterRecipe()
        // Listeners and commands are disabled automatically
    }

    private fun registerRecipe() {
        if (recipeEnabled) server.addRecipe(recipe)
    }

    private fun unregisterRecipe() {
        if (recipeEnabled) server.removeRecipe(recipeNamespacedKey)
    }
}
