/* Licensed under MIT */
package dev.bloodstone.wanderfuladditions

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Flags
import co.aikar.commands.annotation.HelpCommand
import co.aikar.commands.annotation.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class CommandManager(private val plug: WanderfulAdditions) : PaperCommandManager(plug) {
    fun registerCommands() {
        // enable unstable api to use help
        @Suppress("DEPRECATION")
        enableUnstableAPI("help")

        registerCompletions()
        registerConditions()
        registerCommand(CommandWA())
    }

    private fun registerCompletions() {
        commandCompletions.registerCompletion("enabledwands") { _ ->
            plug.wands.filter { it.value.isEnabled }.keys.map { it.toString() }
        }
    }

    private fun registerConditions() {
        commandConditions.addCondition(String::class.java, "enabledwands") { _, _, value ->
            if (value != null) {
                try {
                    WandType.valueOf(value.toUpperCase())
                } catch (_: IllegalArgumentException) {
                    val enabledWands = plug.wands.filter { it.value.isEnabled }.keys
                    throw ConditionFailedException("Unknown wand type. Valid values are ${enabledWands.joinToString(", ")}")
                }
            }
        }
    }
}

@CommandAlias("wanderfuladditions|wa")
private class CommandWA() : BaseCommand() {
    @Dependency
    private lateinit var plugin: WanderfulAdditions

    private fun giveWand(player: Player, wand: ItemStack) {
        player.inventory.addItem(wand)
    }

    private fun handleWandGive(sender: CommandSender, wandString: String, player: Player) {
        val wandType =
            try {
                WandType.valueOf(wandString.toUpperCase())
            } catch (e: IllegalArgumentException) {
                val wandTypes = WandType.values().filter {
                    val wand = plugin.wands[it]
                    wand != null && wand.isEnabled
                }.joinToString(", ")
                sender.sendMessage("Invalid wand name. Must be one of: $wandTypes")
                return
            }
        val wand = plugin.wands[wandType] ?: run {
            plugin.logIssue("Unable to retrieve $wandType from wand list!")
            sender.sendMessage("And error occurred. Check logs.")
            return
        }
        giveWand(player, wand.item)
    }

    @Subcommand("give")
    @Description("Give yourself (or others) the magic wand")
    @CommandPermission("wanderfuladditions.give")
    @CommandCompletion("@enabledwands @players")
    fun onWandGive(sender: CommandSender, @Conditions("enabledwands") wand: String, @Flags("defaultself") player: Player) {
        handleWandGive(sender, wand, player)
    }

    @Subcommand("reload")
    @Description("Reload plugin configuration")
    @CommandPermission("wanderfuladditions.reload")
    fun onReload(sender: CommandSender) {
        try {
            plugin.reload()
            sender.sendMessage("Configuration successfully reloaded!")
        } catch (e: InvalidConfigurationException) {
            plugin.logger.severe("ERROR: $e")
            plugin.logger.severe("Will not update configuration")
            sender.sendMessage(arrayOf(
                    "There was an error in configuration:",
                    "$e",
                    "Will not use new configuration"
            ))
        }
    }

    @Default
    @HelpCommand
    fun help(@Suppress("UNUSED_PARAMETER") sender: CommandSender, help: CommandHelp) {
        help.showHelp()
    }
}
