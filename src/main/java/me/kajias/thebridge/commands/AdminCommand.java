package me.kajias.thebridge.commands;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.objects.Region;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.data.ArenasData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.managers.ArenaManager;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdminCommand implements CommandExecutor
{
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();

   public static final Map<Player, Arena> setupMap = new HashMap<>();

   @Override
   public boolean onCommand(CommandSender sender, Command command, String str, String[] args) {
      if (sender instanceof Player) {
         Player player = ((Player) sender).getPlayer();

         if (player.hasPermission("rw.admin")) {
            if (args.length == 0) {
               showHelp(player);
            } else {
               switch (args[0].toLowerCase()) {
                  case "adminhelp":
                     showHelp(player);
                     break;
                  case "setlobby":
                     config.set("lobby-spawn-point", player.getLocation().serialize());
                     TheBridge.INSTANCE.saveConfig();
                     TheBridge.lobbyLocation = player.getLocation();
                     Utils.sendMessage(player, config.getString("messages.lobby-was-set"));
                     break;
                  case "create":
                     Arena createdArena;
                     if (args.length == 3) {
                        if (ArenaManager.getArenaByName(args[1]) == null) {
                           if (new File(args[2]).listFiles() != null) {
                              createdArena = new Arena(args[1], args[2]);
                              createdArena.setState(ArenaState.SETUP);
                              Utils.sendMessage(player, config.getString("messages.arena-created").replace("%arena_name%", args[1]));
                              player.teleport(new Location(createdArena.getWorld(), 0, 60, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                              player.setGameMode(GameMode.CREATIVE);
                              player.getInventory().clear();
                              setupMap.put(player, createdArena);

                              ItemStack arenaConfigurationItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.arena-configuration.material")));
                              ItemMeta arenaConfigurationMeta = arenaConfigurationItem.getItemMeta();
                              arenaConfigurationMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.arena-configuration.name")));
                              arenaConfigurationItem.setItemMeta(arenaConfigurationMeta);
                              player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.arena-configuration.slot") - 1, arenaConfigurationItem);

                              ItemStack regionSelectionWand = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.region-selection-wand.material")));
                              ItemMeta regionSelectionWandMeta = regionSelectionWand.getItemMeta();
                              regionSelectionWandMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.region-selection-wand.name")));
                              regionSelectionWand.setItemMeta(regionSelectionWandMeta);
                              player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.region-selection-wand.slot") - 1, regionSelectionWand);
                           } else Utils.sendMessage(player, config.getString("messages.template-world-not-found").replace("%world_name%", args[2]));
                        } else Utils.sendMessage(player, config.getString("messages.arena-exists").replace("%arena_name%", args[1]));
                     } else showHelp(player);
                     break;
                  case "disable":
                     if (args.length == 2) {
                        if (ArenaManager.getArenaByName(args[1]) != null) {
                           Arena arena = ArenaManager.getArenaByName(args[1]);
                           if (arena.getState() != ArenaState.DISABLED) {
                              ArenaManager.disableArena(arena);
                              Utils.sendMessage(player, config.getString("messages.arena-has-been-disabled").replace("%arena_name%", args[1]));
                           } else
                              Utils.sendMessage(player, config.getString("messages.failed-to-disable-arena").replace("%arena_name%", args[1]));
                        } else
                           Utils.sendMessage(player, config.getString("messages.arena-does-not-exist").replace("%arena_name%%", args[1]));
                     } else Utils.sendMessage(player, config.getString("messages.arena-name-is-not-specified"));
                     break;
                  case "enable":
                     if (args.length == 2) {
                        if (ArenaManager.getArenaByName(args[1]) != null) {
                           Arena arena = ArenaManager.getArenaByName(args[1]);
                           if (arena.getState() == ArenaState.DISABLED) {
                              ArenaManager.enableArena(arena);
                              Utils.sendMessage(player, config.getString("messages.arena-has-been-enabled").replace("%arena_name%", args[1]));
                           } else
                              Utils.sendMessage(player, config.getString("messages.arena-is-enabled").replace("%arena_name%", args[1]));
                        } else
                           Utils.sendMessage(player, config.getString("messages.arena-does-not-exist").replace("%arena_name%", args[1]));
                     } else Utils.sendMessage(player, config.getString("messages.arena-name-is-not-specified"));
                     break;
                  case "setup":
                     if (args.length == 2) {
                        if (ArenaManager.getArenaByName(args[1]) != null) {
                           Arena modifiableArena = ArenaManager.getArenaByName(args[1]);
                           ArenaManager.disableArena(modifiableArena);
                           if (!setupMap.containsKey(player)) {
                              Bukkit.getScheduler().runTaskLater(TheBridge.INSTANCE, () -> {
                                 setupMap.put(player, modifiableArena);
                                 modifiableArena.getPlacedBlocks().clear();
                                 Utils.sendMessage(player, config.getString("messages.entering-setup"));
                                 modifiableArena.setState(ArenaState.SETUP);
                                 player.teleport(new Location(modifiableArena.getWorld(), 0, 60, 0), PlayerTeleportEvent.TeleportCause.PLUGIN);
                                 player.getInventory().clear();
                                 player.getInventory().setArmorContents(null);
                                 player.setGameMode(GameMode.CREATIVE);

                                 ItemStack arenaConfigurationItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.arena-configuration.material")));
                                 ItemMeta arenaConfigurationMeta = arenaConfigurationItem.getItemMeta();
                                 arenaConfigurationMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.arena-configuration.name")));
                                 arenaConfigurationItem.setItemMeta(arenaConfigurationMeta);
                                 player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.arena-configuration.slot") - 1, arenaConfigurationItem);

                                 ItemStack regionSelectionWand = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.region-selection-wand.material")));
                                 ItemMeta regionSelectionWandMeta = regionSelectionWand.getItemMeta();
                                 regionSelectionWandMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.region-selection-wand.name")));
                                 regionSelectionWand.setItemMeta(regionSelectionWandMeta);
                                 player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.region-selection-wand.slot") - 1, regionSelectionWand);
                              }, 20L);
                           } else Utils.sendMessage(player, config.getString("Strings.arena-is-already-in-setup"));
                        } else Utils.sendMessage(player, config.getString("messages.arena-does-not-exist").replace("%arena_name%", args[1]));
                     } else Utils.sendMessage(player, config.getString("messages.arena-name-is-not-specified"));
                     break;
                  case "addreg":
                     if (setupMap.containsKey(player)) {
                        if (Region.playerLocationMap1.containsKey(player) && Region.playerLocationMap2.containsKey(player)) {
                           Arena modifiableArena = setupMap.get(player);
                           modifiableArena.getRegions().add(new Region(Region.playerLocationMap1.get(player), Region.playerLocationMap2.get(player)));
                           Utils.sendMessage(player, config.getString("messages.region-added"));
                        } else Utils.sendMessage(player, config.getString("messages.no-selected-region"));
                     } else Utils.sendMessage(player, config.getString("messages.not-in-arena-world"));
                     break;
                  case "clearreg":
                     if (setupMap.containsKey(player)) {
                        Arena modifiableArena = setupMap.get(player);
                        modifiableArena.getRegions().clear();
                        Utils.sendMessage(player, config.getString("messages.regions-cleared"));
                     } else Utils.sendMessage(player, config.getString("messages.not-in-arena-world"));
                     break;
                  case "remove":
                     if (args.length == 2) {
                        if (ArenaManager.getArenaByName(args[1]) != null) {
                           Arena modifiableArena = ArenaManager.getArenaByName(args[1]);
                           if (!modifiableArena.getWorld().getPlayers().contains(player)) {
                              ArenaManager.disableArena(modifiableArena);
                              ArenaManager.removeArena(modifiableArena);
                              Utils.sendMessage(player, config.getString("messages.arena-removed-successfully"));
                           } else Utils.sendMessage(player, config.getString("messages.must-exit-from-arena-to-remove"));
                        } else Utils.sendMessage(player, config.getString("messages.arena-does-not-exist").replace("%arena_name%", args[1]));
                     } else Utils.sendMessage(player, config.getString("messages.arena-name-is-not-specified"));
                     break;
                  case "list":
                     listArenas(player);
                     break;
               }
            }
            return true;
         } else Utils.sendMessage(player, config.getString("messages.no-permission"));
         return false;
      }
      Bukkit.getLogger().warning(config.getString("messages.log.must-be-player-to-execute-command"));
      return false;
   }

   private void showHelp(Player player) {
      for (String s : config.getStringList("messages.admin-help")) {
         player.sendMessage(Utils.colorize(s));
      }
   }

   private void listArenas(Player player) {
      if (ArenasData.loadedArenas.isEmpty()) {
         Utils.sendMessage(player, config.getString("messages.arena-list-format.arena-list-empty"));
         return;
      }
      for (Arena arena : ArenasData.loadedArenas) {
         player.sendMessage(Utils.colorize(
                 config.getString("messages.arena-list-format.arena")
                         .replace("%name%", arena.getName())
                         .replace("%world%", arena.getWorldName())
                         .replace("%state%", config.getString("messages.arena-list-format.arena-states." + arena.getState().toString().toLowerCase()))));
      }
   }
}
