/* Licensed under MIT */
package dev.bloodstone.invisiframes

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.PaperCommandManager
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Dependency
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Flags
import co.aikar.commands.annotation.HelpCommand
import co.aikar.commands.annotation.Subcommand
import org.bukkit.command.CommandSender
import org.bukkit.configuration.InvalidConfigurationException
import org.bukkit.entity.Player

class CommandManager(private val plugin: InvisiFrames) : PaperCommandManager(plugin) {
    fun registerCommands() {
        // enable unstable api to use help
        @Suppress("DEPRECATION")
        enableUnstableAPI("help")

        registerCommand(CommandIF())
    }
}

@CommandAlias("invisiframes|if")
private class CommandIF() : BaseCommand() {
    @Dependency
    private lateinit var plugin: InvisiFrames

    private fun giveWand(player: Player) {
        player.inventory.addItem(plugin.wand)
    }

    @Subcommand("wand")
    @Description("Give yourself (or others) the magic wand")
    @CommandPermission("invisiframes.wand")
    @CommandCompletion("*")
    fun onWand(@Suppress("UNUSED_PARAMETER") sender: CommandSender, @Flags("defaultself") player: Player) {
        giveWand(player)
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
