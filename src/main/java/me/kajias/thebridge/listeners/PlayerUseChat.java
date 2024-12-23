package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class PlayerUseChat implements Listener
{
    private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

    public PlayerUseChat(TheBridge plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCommandExecution(PlayerCommandPreprocessEvent e) {
        if (!e.getPlayer().isOp()) {
            if (Arena.getPlayerArenaMap().get(e.getPlayer()) != null && !e.getMessage().equals("/leave")) {
                Utils.sendMessage(e.getPlayer(), config.getString("messages.no-commands-in-game"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String format = config.getString("messages.game-chat-format.default");
        Arena arena = Arena.getPlayerArenaMap().get(player);
        if (arena != null && (arena.getState() == ArenaState.STARTED || arena.getState() == ArenaState.ENDING)) {
            format = config.getString("messages.game-chat-format.team");
            if (!e.getMessage().isEmpty() && e.getMessage().toCharArray()[0] == '!') {
                e.setMessage(e.getMessage().replace("!", ""));
                format = config.getString("messages.game-chat-format.global");
            } else {
                e.getRecipients().removeIf(recipient -> arena.getTeamColor(recipient) != arena.getTeamColor(e.getPlayer()));
            }
            format = format.replace("%team_color%", Utils.getColorCode(arena.getTeamColor(player)));
        }
        e.getRecipients().removeIf(recipient -> recipient.getWorld() != e.getPlayer().getWorld());
        e.setFormat(Utils.colorize(format));
    }
}
