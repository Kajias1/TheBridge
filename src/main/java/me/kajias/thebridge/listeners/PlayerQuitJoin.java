package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.objects.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitJoin implements Listener
{
   public PlayerQuitJoin(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent e) {
      Utils.teleportToLobby(e.getPlayer());

      GamePlayer gamePlayer = PlayersData.getPlayerData(e.getPlayer().getUniqueId());
      gamePlayer.setPlayerName(e.getPlayer().getName());
      if (PlayersData.loadedPlayers.stream().noneMatch(x -> x.getUniqueId().equals(e.getPlayer().getUniqueId()))) {
         PlayersData.loadedPlayers.add(gamePlayer);
         PlayersData.savePlayerData(gamePlayer);
      }

      e.setJoinMessage("");
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent e) {
      Arena arena = Arena.getPlayerArenaMap().get(e.getPlayer());
      if (arena != null) {
         arena.removePlayer(e.getPlayer());
      }

      e.setQuitMessage("");
   }
}
