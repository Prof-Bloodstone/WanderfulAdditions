package dev.bloodstone.invisiframes

import dev.bloodstone.mcutils.PersistentNamespacedFlag
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataType
import java.lang.reflect.Field


class 

fun getArmorStandEditorWandFlag() : PersistentNamespacedFlag<*> {
    val plugin = Bukkit.getServer().pluginManager.getPlugin("ArmorStandEditor") ?: throw PluginNotFoundException("Unable to find ArmorStandEditor.")
    val nskField: Field
    try {
        nskField = plugin.javaClass.getField("editToolKey")
    } catch (e: NoSuchFieldException) {
        throw UnsupportedPluginException("ArmorStandEditor is missing required field", e)
    }
    val nsk = nskField.get(plugin) as? NamespacedKey
        ?: throw UnsupportedPluginException("ArmorStandEditor field type is incorrect (got ${nskField.type}")
    return PersistentNamespacedFlag(nsk, PersistentDataType.BYTE, 1)
}

