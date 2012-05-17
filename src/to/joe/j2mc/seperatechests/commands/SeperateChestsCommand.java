package to.joe.j2mc.seperatechests.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import to.joe.j2mc.core.command.MasterCommand;
import to.joe.j2mc.seperatechests.J2MC_seperatechests;

public class SeperateChestsCommand extends MasterCommand {

    J2MC_seperatechests plugin;

    public SeperateChestsCommand(J2MC_seperatechests plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void exec(CommandSender sender, String commandName, String[] args, Player player, boolean isPlayer) {
        if (isPlayer) {
            if (args.length == 1 && args[0].equalsIgnoreCase("delete")) {
                if (!plugin.pendingDeletion.contains(player.getName())) {
                    if (plugin.pendingSmackers.contains(player.getName())) {
                        player.sendMessage(ChatColor.RED + "nope.avi You're already in alter mode you can't delete as well");
                        return;
                    }
                    player.sendMessage(ChatColor.RED + "You will delete the next seperate chests block you open! CAUTION! Disconnect and reonnect to get rid of this");
                    plugin.getLogger().info(player.getName() + " went into seperate chests delete mode!");
                    this.plugin.pendingDeletion.add(player.getName());
                } else {
                    player.sendMessage(ChatColor.RED + "Already in delete mode you dolt");
                    return;
                }
                return;
            }
            if (!plugin.pendingSmackers.contains(player.getName())) {
                this.plugin.pendingSmackers.add(player.getName());
                player.sendMessage(ChatColor.RED + "The next chest block you open, you can alter it");
                plugin.getLogger().info(player.getName() + " going into seperate chests mode.");
            } else {
                player.sendMessage(ChatColor.RED + "Already in alter mode you dolt");
            }
        }
    }

}
