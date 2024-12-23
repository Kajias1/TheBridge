package me.kajias.thebridge.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.commands.AdminCommand;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.enums.TeamColor;
import me.kajias.thebridge.objects.Arena;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

public class Utils
{
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
   private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + '&' + "[0-9A-FK-OR]");
   private static final List<Integer> allowedColors = Arrays.asList(1, 2, 3, 4, 5, 6, 9, 10, 11, 13, 14);

   public static String colorize(String str) {
      return ChatColor.translateAlternateColorCodes('&', str);
   }

   public static List<String> colorize(List<String> strList) {
      List<String> result = new ArrayList<>();
      strList.forEach(str -> result.add(colorize(str)));
      return result;
   }

   public static String stripColor(String input) {
      return ChatColor.stripColor(input == null ? null : STRIP_COLOR_PATTERN.matcher(input).replaceAll(""));
   }

   public static void sendMessage(Player player, String message) {
      player.sendMessage(colorize(config.getString("messages.prefix") + message));
   }

   public static void removePotionEffects(Player p) {
      for (PotionEffect effect : p.getActivePotionEffects()) {
         p.removePotionEffect(effect.getType());
      }
   }

   public static void resetPlayerAttributes(Player player) {
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      removePotionEffects(player);
      player.setExp(0.0f);
      player.setLevel(0);
      player.setMaxHealth(20.0);
      player.setHealth(player.getMaxHealth());
      player.setFoodLevel(20);
      player.setGameMode(GameMode.ADVENTURE);
      player.setAllowFlight(false);
      player.setFlying(false);
   }

   public static void teleportToLobby(Player player) {
      resetPlayerAttributes(player);

      player.setFallDistance(0.0f);
      if (TheBridge.lobbyLocation != null) {
         player.teleport(TheBridge.lobbyLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
      } else {
         sendMessage(player, config.getString("messages.lobby-was-not-set"));
         player.teleport(new Location(Bukkit.getWorld("world"), 0, Bukkit.getWorld("world").getHighestBlockYAt(0, 0), 0));
      }

      ItemStack selectArenaItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.arena-select.material")));
      ItemMeta selectArenaItemMeta = selectArenaItem.getItemMeta();
      selectArenaItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.arena-select.name")));
      selectArenaItem.setItemMeta(selectArenaItemMeta);
      player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.arena-select.slot") - 1, selectArenaItem);
      new BukkitRunnable()
      {
         @Override
         public void run() {
            if (Arena.getPlayerArenaMap().containsKey(player)) cancel();
            else if (AdminCommand.setupMap.containsKey(player)) cancel();
            else if (!player.isOnline()) cancel();
            else {
               player.getInventory().getItem(itemsConfig.getInt("hot-bar-items.arena-select.slot") - 1).setDurability(
                       Short.parseShort(String.valueOf(allowedColors.get(new Random().nextInt(allowedColors.size())))));
            }
         }
      }.runTaskTimer(TheBridge.INSTANCE, 0L, 5L);

      ItemStack fastJoinAnyArenaItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.fast-join-any-arena.material")));
      ItemMeta fastJoinAnyArenaItemMeta = fastJoinAnyArenaItem.getItemMeta();
      fastJoinAnyArenaItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.fast-join-any-arena.name")));
      fastJoinAnyArenaItem.setItemMeta(fastJoinAnyArenaItemMeta);
      player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.fast-join-any-arena.slot") - 1, fastJoinAnyArenaItem);

      ItemStack bonusShopItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.bonus-shop.material")));
      ItemMeta bonusShopItemMeta = bonusShopItem.getItemMeta();
      bonusShopItemMeta.setDisplayName(colorize(itemsConfig.getString("hot-bar-items.bonus-shop.name")));
      bonusShopItem.setItemMeta(bonusShopItemMeta);
      player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.bonus-shop.slot") - 1, bonusShopItem);
   }

   public static String convertToTime(int n) {
      int minutes = (n % 3600) / 60;
      int seconds = n % 60;
      return (minutes < 10 ? "0" : "") + minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
   }

   public static Location getClosestTo(Location start, Collection<Location> locations) {
      Location closestLocation = locations.stream().findFirst().orElse(start);
      for (Location location : locations) {
         if (start.distance(location) < start.distance(closestLocation)) {
            closestLocation = location;
         }
      }
      return closestLocation;
   }

   public static <T, E> T getKeyByValue(HashMap<T, E> map, E value) {
      for (Map.Entry<T, E> entry : map.entrySet()) {
         if (Objects.equals(value, entry.getValue())) {
            return entry.getKey();
         }
      }
      return null;
   }

   public static String getColorCode(TeamColor teamColor) {
      if (teamColor == null) return config.getString("messages.team-scores-format.team-colors.none");
      return config.getString("messages.team-scores-format.team-colors." + teamColor.toString().toLowerCase());
   }

   public static ItemStack getCustomTextureHead(String value) {
      ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      GameProfile profile = new GameProfile(UUID.randomUUID(), "");
      profile.getProperties().put("textures", new Property("textures", value));
      Field profileField = null;
      try {
         profileField = meta.getClass().getDeclaredField("profile");
         profileField.setAccessible(true);
         profileField.set(meta, profile);
      } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
         e.printStackTrace();
      }
      head.setItemMeta(meta);
      return head;
   }

   public static Set<Block> sphereAround(Location location, int radius) {
      Set<Block> sphere = new HashSet<Block>();
      Block center = location.getBlock();
      for(int x = -radius; x <= radius; x++) {
         for(int y = -radius; y <= radius; y++) {
            for(int z = -radius; z <= radius; z++) {
               Block b = center.getRelative(x, y, z);
               if(center.getLocation().distance(b.getLocation()) <= radius) {
                  sphere.add(b);
               }
            }
         }
      }
      return sphere;
   }
}
