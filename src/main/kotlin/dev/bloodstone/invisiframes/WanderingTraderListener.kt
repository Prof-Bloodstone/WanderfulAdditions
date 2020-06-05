/* Licensed under MIT */
package dev.bloodstone.invisiframes

import kotlin.random.Random
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

class WanderingTraderListener(private val plugin: InvisiFrames) : Listener {
    private fun shouldAddRecipe(): Boolean {
        val r = Random.nextInt(0, 101) // Until is exclusive
        return plugin.wanderingTraderRecipe.chance > r
    }

    @EventHandler(ignoreCancelled = true)
    fun onCreatureSpawnEvent(event: CreatureSpawnEvent) {
        val trader = event.entity as? WanderingTrader ?: return
        if (!shouldAddRecipe()) return
        val recipe = plugin.wanderingTraderRecipe
        assert(recipe.isEnabled)
        val recipes = trader.recipes.toMutableList()
        recipes.add(recipe.getWithRandomIngredientAndUseCount())
        trader.recipes = recipes
    }
}
