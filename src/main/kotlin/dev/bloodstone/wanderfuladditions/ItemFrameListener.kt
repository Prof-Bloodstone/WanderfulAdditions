/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import org.bukkit.Material
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot

class ItemFrameListener(private val plugin: WanderfulAdditions) : Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamageEvent(event: EntityDamageEvent) {
        val itemFrame = event.entity as? ItemFrame ?: return
        itemFrame.isVisible = true
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerInteractEntityEvent(event: PlayerInteractEntityEvent) {
        val itemFrame = event.rightClicked as? ItemFrame ?: return
        if (itemFrame.item.type == Material.AIR) return // It's empty
        if (event.hand != EquipmentSlot.HAND) return // We only care about main hand
        val item = event.player.equipment!!.itemInMainHand // Player always has equipment
        val container = item.itemMeta?.persistentDataContainer ?: return
        val wand = plugin.wands[WandType.ITEM_FRAME] ?: run {
            plugin.logIssue("Unable to find item-frame configuration!")
            return
        }
        if (!wand.flag.isIn(container)) return
        itemFrame.isVisible = !itemFrame.isVisible
        event.isCancelled = true
    }
}
