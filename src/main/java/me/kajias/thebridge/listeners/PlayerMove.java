package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.objects.Game;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class PlayerMove implements Listener
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   private final List<UUID> playerDisableFlyList = new ArrayList<>();

   public PlayerMove(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onPlayerMove(PlayerMoveEvent e) {
      Player player = e.getPlayer();
      Arena arena = Arena.getPlayerArenaMap().get(player);
      if (arena != null) {
         Game game = arena.getGame();
         if (game != null && game.isStarted() && !game.getDeadPlayers().contains(player)) {
            if (player.getLocation().getY() <= 0) {
               game.respawnPlayer(player, EntityDamageEvent.DamageCause.VOID);
               return;
            }

            Location reservedLocation = game.getPlayerLockedPositionMap().get(player);
            if (reservedLocation != null) {
               if(Math.abs(e.getTo().getX()) != Math.abs(reservedLocation.getX()) || Math.abs(e.getTo().getZ()) != Math.abs(reservedLocation.getZ())) {
                  e.getPlayer().teleport(e.getFrom());
                  return;
               }
            }

            Block block = player.getLocation().clone().add(0, -0.5, 0).getBlock();
            if (block != null && block.getType() == Material.ENDER_PORTAL) {
               if (arena.getTeamColor(player.getLocation()) != arena.getTeamColor(player)) {
                  if (arena.getState() == ArenaState.STARTED && !game.getDeadPlayers().contains(player)) game.handlePlayerScore(player);
               } else {
                  player.teleport(arena.getTeamRespawnLocationMap().get(arena.getTeamColor(player)), PlayerTeleportEvent.TeleportCause.PLUGIN);
                  Utils.sendMessage(player, config.getString("messages.cant-jump-to-own-portal"));
               }
            }
         }
      }
   }

   @EventHandler
   public void setVelocity(PlayerToggleFlightEvent e) {
      Player player = e.getPlayer();
      Arena arena = Arena.getPlayerArenaMap().get(player);

      if (arena != null && arena.getState() == ArenaState.STARTED && player.getGameMode() != GameMode.CREATIVE) {
         GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
         Game game = arena.getGame();

         if (game != null && !game.getDeadPlayers().contains(player)) {
            e.setCancelled(true);
            if (!playerDisableFlyList.contains(player.getUniqueId()) && gamePlayer.getBoughtBonuses()
                    .containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items.double-jump.name")))) {
               playerDisableFlyList.add(player.getUniqueId());
               player.setAllowFlight(false);
               player.setVelocity(e.getPlayer().getLocation().getDirection().normalize().setY(config.getDouble("game-config.double-jump-multiplier")));
               Sounds.ENDERDRAGON_WINGS.play(player);

               final int coolDownMax = config.getInt("game-config.double-jump-cool-down-ticks");
               player.setExp(1.0f);
               new BukkitRunnable()
               {
                  int coolDown = coolDownMax;

                  @Override
                  public void run() {
                     player.setExp((float) coolDown / coolDownMax);
                     player.setFallDistance(0.0f);
                     coolDown--;
                     if (coolDown <= 0) {
                        playerDisableFlyList.remove(player.getUniqueId());
                        player.setExp(0.0f);
                        this.cancel();
                        player.setAllowFlight(true);
                     }
                  }
               }.runTaskTimer(TheBridge.INSTANCE, 0L, 1L);
            }
         }
      }
   }
}
