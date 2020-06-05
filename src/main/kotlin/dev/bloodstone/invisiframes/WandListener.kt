/* Licensed under MIT */
package dev.bloodstone.invisiframes

import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot

// TODO: Remove
var frame_visible: Boolean = true
var ItemFrame.visible: Boolean
    get() = frame_visible
    set(value) { frame_visible = value }

class WandListener(private val plugin: InvisiFrames) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val itemFrame = event.rightClicked as? ItemFrame ?: return
        if (event.hand != EquipmentSlot.HAND) return
        val item = event.player.equipment!!.itemInMainHand // Player always has equipment
        val container = item.itemMeta?.persistentDataContainer ?: return
        if (!container.has(plugin.wandNamespacedKey, plugin.wandPersistentDataType)) return
        if (container.get(plugin.wandNamespacedKey, plugin.wandPersistentDataType) != plugin.wandPersistentDataValue) return
        // TODO: Update to 1.16
        itemFrame.visible = !itemFrame.visible
        val state = if (itemFrame.visible) "visible" else "invisible"
        event.player.sendMessage("You just made item frame $state!")
        event.isCancelled = true
    }
}
