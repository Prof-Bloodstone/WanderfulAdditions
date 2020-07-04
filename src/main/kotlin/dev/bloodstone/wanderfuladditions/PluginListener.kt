/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import dev.bloodstone.wanderfuladditions.plugin_wrappers.PluginWrapper
import java.io.PrintWriter
import java.io.StringWriter
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.event.server.PluginEnableEvent
import org.bukkit.plugin.Plugin

class PluginListener(private val plugin: WanderfulAdditions, pluginWrappers: List<PluginWrapper>) : Listener {
    private val pluginNames = pluginWrappers.map { it.name }.toSet()

    private fun configNeedsReload(plugin: Plugin): Boolean {
        return pluginNames.contains(plugin.name)
    }

    private fun handlePluginStateChange(plugin: Plugin) {
        if (configNeedsReload(plugin)) {
            try {
                this.plugin.reload(false)
            } catch (e: Exception) {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                this.plugin.logIssue("$sw\nError reloading configuration on '${plugin.name}' plugin state change.")
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPluginEnable(e: PluginEnableEvent) {
        handlePluginStateChange(e.plugin)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPluginDisable(e: PluginDisableEvent) {
        handlePluginStateChange(e.plugin)
    }
}
