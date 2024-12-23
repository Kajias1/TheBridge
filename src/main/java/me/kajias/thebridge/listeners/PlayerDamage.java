package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.objects.Game;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Random;

public class PlayerDamage implements Listener
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public static final HashMap<Player,Player> lastToDamage = new HashMap<>();

   public PlayerDamage(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler(priority = EventPriority.NORMAL)
   public void onDamage(EntityDamageEvent e) {
      if (e.getEntity() instanceof Player) {
         Player player = (Player) e.getEntity();
         Arena arena = Arena.getPlayerArenaMap().get(player);

         if (arena == null) {
            e.setCancelled(true);
            return;
         }

         if (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING || arena.getState() == ArenaState.ENDING) {
            e.setCancelled(true);
         }

         if (player.getHealth() - e.getFinalDamage() <= 0 && e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            Game game = arena.getGame();
            if (game != null) game.respawnPlayer(player, EntityDamageEvent.DamageCause.FALL);
         }
      }
   }

   @EventHandler(priority = EventPriority.LOW)
   public void onPlayerDamageByPlayer(EntityDamageByEntityEvent e) {
      Player victim;
      Player damageDealer;

      if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
         victim = (Player) e.getEntity();
         damageDealer = (Player) e.getDamager();
      } else if (e.getEntity() instanceof Player && e.getDamager() instanceof Projectile) {
         victim = (Player) e.getEntity();
         damageDealer = (Player) ((Projectile) e.getDamager()).getShooter();
      } else {
         victim = null;
         damageDealer = null;
      }

      if (victim != null && damageDealer != null) {
         Arena arena = Arena.getPlayerArenaMap().get(victim);
         if (arena != null) {
            Game game = arena.getGame();
            if (game != null) {
               if (game.getDeadPlayers().contains(victim) || game.getDeadPlayers().contains(damageDealer) || arena.getTeamColor(victim) == arena.getTeamColor(damageDealer)) {
                  e.setCancelled(true);
                  return;
               }

               lastToDamage.put(victim, damageDealer);
               if (victim.getHealth() - e.getFinalDamage() <= 0) {
                  game.respawnPlayer(victim, EntityDamageEvent.DamageCause.ENTITY_ATTACK);
               }

               if (damageDealer.getItemInHand() != null) {
                  ItemStack item = damageDealer.getItemInHand();
                  if (ItemConfiguration.isHotBarItem(item)) {
                     PlayerInteractItem.doAction(item, damageDealer, new PlayerInteractEvent(damageDealer, Action.LEFT_CLICK_AIR, item, null, null));
                     e.setCancelled(true);
                     return;
                  }
               }
            }
         }
      }
   }
}
