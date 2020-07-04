/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions.plugin_wrappers

import dev.bloodstone.mcutils.PersistentNamespacedFlag
import dev.bloodstone.wanderfuladditions.UnsupportedPluginException
import dev.bloodstone.wanderfuladditions.Wand
import java.lang.reflect.Field
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType

class ArmorStandEditorWrapper : PluginWrapper("ArmorStandEditor") {

    override fun getFlag(): PersistentNamespacedFlag<Byte> {
        val plugin = getPluginOrThrow()
        val nskField: Field
        try {
            nskField = plugin.javaClass.getField("editToolKey")
        } catch (e: NoSuchFieldException) {
            throw UnsupportedPluginException("$name is missing required field", e)
        }
        val nsk = nskField.get(plugin) as? NamespacedKey
            ?: throw UnsupportedPluginException("$name field type is incorrect (got ${nskField.type}")
        return PersistentNamespacedFlag(nsk, PersistentDataType.BYTE, 1)
    }

    override fun configure(config: Wand) {
        val plugin = getPluginOrThrow()
        val fieldMapping = mapOf(
            "editTool" to config.item.type,
            "requireToolData" to false,
            "requireToolLore" to false,
            "requireToolKey" to true
        )
        for ((k, v) in fieldMapping) {
            val field: Field
            try {
                field = plugin.javaClass.getDeclaredField(k)
            } catch (e: NoSuchFieldException) {
                throw UnsupportedPluginException("Encountered issue getting $name.$k", e)
            }
            val accessible = field.isAccessible
            field.isAccessible = true
            field.set(plugin, v)
            field.isAccessible = accessible
        }
    }
}
