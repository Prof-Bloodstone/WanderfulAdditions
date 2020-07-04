/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions.plugin_wrappers

import dev.bloodstone.mcutils.PersistentNamespacedFlag
import dev.bloodstone.wanderfuladditions.PluginNotFoundException
import dev.bloodstone.wanderfuladditions.Wand
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

abstract class PluginWrapper(val name: String) {

    fun getPlugin(): JavaPlugin? = Bukkit.getServer().pluginManager.getPlugin(name) as? JavaPlugin
    fun isLoaded(): Boolean = getPlugin() != null
    fun getPluginOrThrow(): JavaPlugin {
        return getPlugin()
            ?: throw PluginNotFoundException("Unable to find $name.")
    }
    abstract fun getFlag(): PersistentNamespacedFlag<Byte>
    abstract fun configure(config: Wand)
}
