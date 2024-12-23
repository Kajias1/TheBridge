package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.commands.AdminCommand;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.gui.menus.arena_select.ArenaSelectMenu;
import me.kajias.thebridge.gui.menus.player_utilities.BonusShopMenu;
import me.kajias.thebridge.objects.Region;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.gui.menus.arena_setup.ArenaSetupMenu;
import me.kajias.thebridge.gui.menus.team_select.TeamSelectMenu;
import me.kajias.thebridge.managers.ArenaManager;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Random;

public class PlayerInteractItem implements Listener
{
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();

   public PlayerInteractItem(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onInventoryItemInteract(InventoryClickEvent e) {
      if (!(e.getWhoClicked() instanceof Player)) return;
      if (e.getCurrentItem() == null) return;

      Player player = ((Player) e.getWhoClicked()).getPlayer();
      ItemStack clickedItem = e.getCurrentItem();
      if (e.getHotbarButton() != -1) {
         ItemStack item = player.getInventory().getItem(e.getHotbarButton());
         try {
            if (ItemConfiguration.isHotBarItem(item)) {
               e.setCancelled(true);
               return;
            }
         } catch (NullPointerException ignored) {
         }
      }

      if (ItemConfiguration.isHotBarItem(e.getCurrentItem())) {
         doAction(e.getCurrentItem(), player, null);
         e.setCancelled(true);
         return;
      }

      if (isLeatherArmorItem(e.getCurrentItem())) {
         e.setCancelled(true);
      }
   }

   @EventHandler
   public void onHotBarItemInteract(PlayerInteractEvent e) {
      Player player = e.getPlayer();
      ItemStack item = e.getItem();

      if (item == null) return;

      if (ItemConfiguration.isHotBarItem(item)) {
         doAction(e.getItem(), player, e);
         e.setCancelled(true);
         return;
      }

      if (isLeatherArmorItem(item)) {
         e.setCancelled(true);
      }
   }

   private boolean isLeatherArmorItem(ItemStack itemStack) {
      return itemStack.getType() == Material.LEATHER_BOOTS || itemStack.getType() == Material.LEATHER_LEGGINGS ||
              itemStack.getType() == Material.LEATHER_CHESTPLATE || itemStack.getType() == Material.LEATHER_HELMET;
   }

   public static void doAction(ItemStack item, Player player, PlayerInteractEvent e) {
      String displayName = item.getItemMeta().getDisplayName();

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.arena-configuration.name")))) {
         TheBridge.guiManager.openGUI(new ArenaSetupMenu(), player);
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.region-selection-wand.name")))) {
         if (AdminCommand.setupMap.containsKey(player)) {
            if (e != null && e.getClickedBlock() != null) {
               Location clickedLoc = e.getClickedBlock().getLocation();
               if (e.getAction() == Action.LEFT_CLICK_BLOCK) {
                  Region.playerLocationMap1.put(player, clickedLoc);
                  Utils.sendMessage(player, config.getString("messages.selection-1-message")
                          .replace("%x%", String.valueOf(clickedLoc.getX()))
                          .replace("%y%", String.valueOf(clickedLoc.getY()))
                          .replace("%z%", String.valueOf(clickedLoc.getZ()))
                  );
                  Sounds.ORB_PICKUP.play(player);
               } else if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                  Region.playerLocationMap2.put(player, clickedLoc);
                  Utils.sendMessage(player, config.getString("messages.selection-2-message")
                          .replace("%x%", String.valueOf(clickedLoc.getX()))
                          .replace("%y%", String.valueOf(clickedLoc.getY()))
                          .replace("%z%", String.valueOf(clickedLoc.getZ()))
                  );
                  Sounds.ORB_PICKUP.play(player);
               }
            }
         } else Utils.sendMessage(player, config.getString("messages.not-in-arena-world"));
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.fast-join-any-arena.name"))) ||
              displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.new-game.name")))) {
         Arena arena = ArenaManager.findBestArena();
         if (arena != null) {
            arena.addPlayer(player);
         } else Utils.sendMessage(player, config.getString("messages.no-available-arenas"));
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.team-select.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null) {
            TheBridge.guiManager.openGUI(new TeamSelectMenu(arena), player);
         } else Utils.sendMessage(player, config.getString("messages.not-in-game"));
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.leave-game.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null) {
            arena.removePlayer(player);
         } else Utils.sendMessage(player, config.getString("messages.not-in-game"));
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.bonus-shop.name")))) {
         TheBridge.guiManager.openGUI(new BonusShopMenu(), player);
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.arena-select.name")))) {
         TheBridge.guiManager.openGUI(new ArenaSelectMenu(), player);
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.fast-start.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null && arena.getState() == ArenaState.STARTING) {
            arena.getGame().shortenCountDownTimer(3);
            for (Player p : arena.getPlayers()) {
               p.getInventory().setItem(itemsConfig.getInt("hot-bar-items.fast-start.slot") - 1, null);
               Utils.sendMessage(p, config.getString("messages.fast-start-used").replace("%player_name%", player.getName()));
            }
         }
         return;
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.lightning-stick.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null) {
            Map.Entry<Player, Player> victimDamageDealerPair = PlayerDamage.lastToDamage
                    .entrySet()
                    .stream()
                    .filter(entry -> player == entry.getValue()).findAny().orElse(null);
            if (victimDamageDealerPair != null && victimDamageDealerPair.getKey() != null) {
               Player victim = victimDamageDealerPair.getKey();
               victim.setNoDamageTicks(0);
               victim.damage(4.0f + new Random().nextInt(2));
               arena.getWorld().strikeLightningEffect(victim.getLocation());
               victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1, true));
               victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 2, true));
               arena.getPlayers().forEach(p -> {
                  Utils.sendMessage(p, config.getString("messages.lightning-stick-strike-message")
                          .replace("%player_name%", Utils.getColorCode(arena.getTeamColor(player)) + player.getName())
                          .replace("%victim_name%", Utils.getColorCode(arena.getTeamColor(victim)) + victim.getName())
                  );
               });
               player.getInventory().remove(player.getItemInHand());
            }
         }
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.time-back.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null) {
            if (player.getLocation().distance(Utils.getClosestTo(player.getLocation(), arena.getTeamSpawnLocationMap()
                    .values())) <= config.getInt("game-config.time-back-item-block-radius")) {
               if (arena.getTeamColor(Utils.getClosestTo(player.getLocation(), arena.getTeamSpawnLocationMap().values())) != arena.getTeamColor(player)) {
                  Utils.sendMessage(player, config.getString("messages.cant-use-time-back-on-enemy-base"));
                  return;
               }
            }
            new BukkitRunnable()
            {
               int time = 3;
               final Location location = player.getLocation();

               @Override
               public void run() {
                  player.sendTitle(Utils.colorize(config.getString("messages.time-back").replace("%time%", String.valueOf(time))), "");
                  player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 100, 1));

                  if (!Arena.getPlayerArenaMap().containsKey(player)) cancel();
                  if (time <= 0) {
                     player.setFallDistance(0.0f);
                     player.teleport(location);
                     player.setHealth(player.getMaxHealth());
                     Sounds.ENDERMAN_TELEPORT.play(player);
                     cancel();
                  }

                  time--;
               }
            }.runTaskTimer(TheBridge.INSTANCE, 0L, 20L);
            player.getInventory().remove(player.getItemInHand());
            return;
         }
      }

      if (displayName.equalsIgnoreCase(Utils.colorize(itemsConfig.getString("hot-bar-items.teleport-to-base.name")))) {
         Arena arena = Arena.getPlayerArenaMap().get(player);
         if (arena != null) {
            player.setFallDistance(0.0f);
            player.teleport(arena.getTeamRespawnLocationMap().get(arena.getTeamColor(player)), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.setHealth(player.getMaxHealth());
            Sounds.ENDERMAN_TELEPORT.play(player);
            player.getInventory().remove(player.getItemInHand());
         }
         return;
      }
   }
}
