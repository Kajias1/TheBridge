package me.kajias.thebridge.commands;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor
{
    private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                if (Arena.getPlayerArenaMap().containsKey(player)) {
                    Arena.getPlayerArenaMap().get(player).removePlayer(player);
                } else Utils.sendMessage(player, config.getString("messages.not-in-game"));
            }
        }

        return true;
    }
}
