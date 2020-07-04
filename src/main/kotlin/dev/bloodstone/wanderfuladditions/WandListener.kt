/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot

class WandListener(private val plugin: WanderfulAdditions) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val itemFrame = event.rightClicked as? ItemFrame ?: return
        if (event.hand != EquipmentSlot.HAND) return
        val item = event.player.equipment!!.itemInMainHand // Player always has equipment
        val container = item.itemMeta?.persistentDataContainer ?: return
        val wand = plugin.wands[WandType.ITEM_FRAME]
        if (wand == null) {
            plugin.logIssue("Unable to find item-frame configuration!")
            return
        }
        if (!wand.flag.isIn(container)) return
        itemFrame.isVisible = !itemFrame.isVisible
        event.isCancelled = true
    }
}
