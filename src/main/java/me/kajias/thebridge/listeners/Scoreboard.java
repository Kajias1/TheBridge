package me.kajias.thebridge.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.ScoreboardConfiguration;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.utils.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class Scoreboard implements Listener 
{
    private static final FileConfiguration scoreboardConfig = ScoreboardConfiguration.baseConfig.getConfig();

    public static Map<UUID, FastBoard> boards = new HashMap<>();

    public Scoreboard(TheBridge plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);

        TheBridge.INSTANCE.getServer().getScheduler().scheduleSyncRepeatingTask(TheBridge.INSTANCE, () -> {
            for (FastBoard board : boards.values()) {
                Player player = board.getPlayer();
                Arena arena = Arena.getPlayerArenaMap().get(player);

                List<String> lines = scoreboardConfig.getStringList("boards.lobby");
                if (arena != null) {
                    lines = new ArrayList<>();
                    for (String line : scoreboardConfig.getStringList("boards." + arena.getState().toString().toLowerCase())) {
                        if (!PlaceholderAPI.setPlaceholders(player, line).contains("none team")) lines.add(line);
                    }
                }
                updateBoard(board, PlaceholderAPI.setPlaceholders(player, lines).toArray(new String[0]));
            }
        }, 0L, 2L);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        FastBoard board = new FastBoard(player);
        board.updateTitle(Utils.colorize(scoreboardConfig.getString("title")));
        boards.put(player.getUniqueId(), board);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) board.delete();
    }

    private void updateBoard(FastBoard board, String ... lines) {
        for (int a = 0; a < lines.length; ++a)
            lines[a] = Utils.colorize(lines[a]);
        board.updateLines(lines);
    }
}
