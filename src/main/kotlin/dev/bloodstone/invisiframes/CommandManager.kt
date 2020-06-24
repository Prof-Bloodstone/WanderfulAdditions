/* Licensed under MIT */
package dev.bloodstone.invisiframes

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

class CommandManager(private val plug: InvisiFrames) : PaperCommandManager(plug) {
    fun registerCommands() {
        // enable unstable api to use help
        @Suppress("DEPRECATION")
        enableUnstableAPI("help")

        registerCommand(CommandIF())
    }

    private fun registerCompletions() {
        commandCompletions.registerCompletion("enabledWands") { _ ->
            plug.enabledWands.map { it.toString() }
        }
    }

    private fun registerConditions() {
        commandConditions.addCondition(String::class.java,"enabledWand") { _, _, value ->
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

@CommandAlias("invisiframes|if")
private class CommandIF() : BaseCommand() {
    @Dependency
    private lateinit var plugin: InvisiFrames

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

    @Subcommand("wand")
    @Description("Give yourself (or others) the magic wand")
    @CommandPermission("invisiframes.wand")
    @CommandCompletion("@enabledWands *")
    fun onWandGive(sender: CommandSender, @Conditions("enabledWand") wand: String, @Flags("defaultself") player: Player) {
        handleWandGive(sender, wand, player)
    }

    @Subcommand("reload")
    @Description("Reload plugin configuration")
    @CommandPermission("invisiframes.reload")
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
