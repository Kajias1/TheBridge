package me.kajias.thebridge.objects;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.managers.ArenaManager;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.ArenaType;
import me.kajias.thebridge.enums.TeamColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.IllegalPluginAccessException;

import java.io.File;
import java.util.*;

public class Arena
{
   private static final FileConfiguration itemsConfig = ItemConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
   private static final HashMap<Player, Arena> playerArenaMap = new HashMap<>();

   private final String name;
   private final String worldName;
   private World world;
   private ArenaType type;
   private GameType gameType;
   private int allowedPlayerAmount;
   private ArenaState state;
   private Location waitingPlayersSpawn;
   private final HashMap<TeamColor, Location> teamSpawnLocationMap;
   private final HashMap<TeamColor, Location> teamRespawnLocationMap;
   private HashMap<TeamColor, List<String>> teamMap;
   private final List<Player> players;
   private Game game;
   private final List<Block> placedBlocks;
   private final List<Region> regions;

   public Arena(String name, String worldName) {
      this.name = name;
      this.worldName = worldName;
      teamSpawnLocationMap = new HashMap<>();
      teamRespawnLocationMap = new HashMap<>();
      teamMap = new HashMap<>();
      setType(ArenaType.SOLO);
      gameType = GameType.SCORES;
      allowedPlayerAmount = 2;
      state = ArenaState.DISABLED;
      placedBlocks = new ArrayList<>();
      game = null;
      players = new ArrayList<>();
      regions = new ArrayList<>();

      WorldCreator worldCreator = new WorldCreator(worldName);
      world = Bukkit.getServer().createWorld(worldCreator);
      world.setAutoSave(false);
      world.setDifficulty(Difficulty.EASY);
      world.setGameRuleValue("doMobSpawning", "false");
      world.setGameRuleValue("doWeatherCycle", "false");
      world.setGameRuleValue("doDaylightCycle", "false");
      world.setGameRuleValue("doFireTick", "false");
      world.setGameRuleValue("doTileDrops", "false");
      world.setGameRuleValue("doMobLoot", "false");
      world.setGameRuleValue("keepInventory", "true");
      world.setGameRuleValue("announceAdvancements", "false");
      world.setGameRuleValue("commandBlockOutput", "false");
   }

   public void setType(ArenaType type) {
      teamMap = new HashMap<>();
      this.type = type;
      switch (type) {
         case SOLO:
            allowedPlayerAmount = 2;
            break;
         case DUO:
         case ONE_X_FOUR:
            allowedPlayerAmount = 4;
            break;
         case TRIO:
            allowedPlayerAmount = 6;
            break;
         case QUAD:
         case TWO_X_FOUR:
            allowedPlayerAmount = 8;
            break;
      }
      createTeams();
   }

   public void setGameType(GameType type) {
      this.gameType = type;
   }

   public void createTeams() {
      switch (this.type) {
         case SOLO:
         case QUAD:
         case TRIO:
         case DUO:
            teamMap.put(TeamColor.RED, new ArrayList<>());
            teamMap.put(TeamColor.BLUE, new ArrayList<>());
            break;
         case ONE_X_FOUR:
         case TWO_X_FOUR:
            teamMap.put(TeamColor.RED, new ArrayList<>());
            teamMap.put(TeamColor.BLUE, new ArrayList<>());
            teamMap.put(TeamColor.GREEN, new ArrayList<>());
            teamMap.put(TeamColor.YELLOW, new ArrayList<>());
            break;
      }
   }

   public void addPlayer(Player player) {
      if (state == ArenaState.WAITING || state == ArenaState.STARTING) {
         if (players.size() < allowedPlayerAmount) {
            playerArenaMap.putIfAbsent(player, this);
            players.add(player);
            Utils.resetPlayerAttributes(player);
            player.teleport(waitingPlayersSpawn, PlayerTeleportEvent.TeleportCause.PLUGIN);

            ItemStack teamSelectItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.team-select.material")));
            ItemMeta teamSelectItemMeta = teamSelectItem.getItemMeta();
            teamSelectItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.team-select.name")));
            teamSelectItem.setItemMeta(teamSelectItemMeta);
            player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.team-select.slot") - 1, teamSelectItem);

            ItemStack leaveGameItem = new ItemStack(Material.getMaterial(itemsConfig.getString("hot-bar-items.leave-game.material")));
            ItemMeta leaveGameItemMeta = leaveGameItem.getItemMeta();
            leaveGameItemMeta.setDisplayName(Utils.colorize(itemsConfig.getString("hot-bar-items.leave-game.name")));
            leaveGameItem.setItemMeta(leaveGameItemMeta);
            player.getInventory().setItem(itemsConfig.getInt("hot-bar-items.leave-game.slot") - 1, leaveGameItem);

            for (Player p : players)
               Utils.sendMessage(p, config.getString("messages.player-joined")
                       .replace("%player%", player.getDisplayName())
                       .replace("%current%", String.valueOf(players.size()))
                       .replace("%max%", String.valueOf(allowedPlayerAmount)));

            if (type == ArenaType.DUO || type == ArenaType.TRIO || type == ArenaType.QUAD || type == ArenaType.TWO_X_FOUR || type == ArenaType.ONE_X_FOUR) {
               if (players.size() >= allowedPlayerAmount - 1) {
                  if (game == null) {
                     game = new Game(this);
                     game.startCountDownTimer(config.getInt("game-config.long-start-countdown"));
                  } else if (players.size() == allowedPlayerAmount) game.shortenCountDownTimer(config.getInt("game-config.start-countdown"));
               }
            } else {
               if (players.size() == allowedPlayerAmount) {
                  game = new Game(this);
                  game.startCountDownTimer(config.getInt("game-config.start-countdown"));
               }
            }
         } else Utils.sendMessage(player, config.getString("messages.arena-is-full"));
      } else Utils.sendMessage(player, config.getString("messages.arena-has-been-started"));
   }

   public void removePlayer(Player player) {
      players.remove(player);
      playerArenaMap.remove(player);
      removeFromTeam(player);
      Bukkit.getOnlinePlayers().forEach(x -> x.showPlayer(player));
      Utils.teleportToLobby(player);

      for (Player p : players) {
         if (state != ArenaState.STARTED)
            Utils.sendMessage(p, config.getString("messages.player-left")
                    .replace("%player%", player.getDisplayName())
                    .replace("%current%", String.valueOf(players.size()))
                    .replace("%max%", String.valueOf(allowedPlayerAmount)));
         else Utils.sendMessage(p, config.getString("messages.player-left-in-game").replace("%player%", player.getName()));
      }

      if (game != null && state == ArenaState.STARTING && players.size() < allowedPlayerAmount) {
         if (type == ArenaType.DUO || type == ArenaType.TRIO || type == ArenaType.QUAD || type == ArenaType.TWO_X_FOUR || type == ArenaType.ONE_X_FOUR) {
            if (players.size() < allowedPlayerAmount - 1) {
               game.stopCountDownTimer();
               game = null;
            } else if (game.getCountDown() > config.getInt("game-config.long-start-countdown") / 2) game.startCountDownTimer(config.getInt("game-config.long-start-countdown") / 2);
         } else {
            game.stopCountDownTimer();
            game = null;
         }
      }

      if (state == ArenaState.STARTED) {
         if (type == ArenaType.SOLO && players.size() <= 1
         || (type == ArenaType.DUO || type == ArenaType.TRIO || type == ArenaType.QUAD) && (teamMap.get(TeamColor.RED).isEmpty() || teamMap.get(TeamColor.BLUE).isEmpty())
         || (type == ArenaType.ONE_X_FOUR || type == ArenaType.TWO_X_FOUR) && (teamMap.get(TeamColor.RED).isEmpty() || teamMap.get(TeamColor.BLUE).isEmpty() || teamMap.get(TeamColor.GREEN).isEmpty() || teamMap.get(TeamColor.YELLOW).isEmpty())
         ) {
            players.forEach(p -> {
               Utils.sendMessage(p, config.getString("messages.no-players-left"));
            });
            restart();
         }
      }
   }

   public void stop() {
      if (!world.getPlayers().isEmpty()) {
         for (Player p : world.getPlayers()) {
            removePlayer(p);
         }
      }
      for (Block block : placedBlocks) {
         if (block.getType() != Material.AIR) block.setType(Material.AIR);
      }
      placedBlocks.clear();

      if (game != null) {
         game.destroy();
         game = null;
      }
   }

   public void restart() {
      stop();

      try {
         Bukkit.getScheduler().runTaskLater(TheBridge.INSTANCE, () -> {
            setState(ArenaState.WAITING);
         }, 3 * 20L);
      } catch (IllegalPluginAccessException ignored) {}
   }

   public void removeFromTeam(Player player) {
      teamMap.values().forEach(x -> x.remove(player.getName()));
      player.setPlayerListName(player.getName());
   }

   public boolean addToTeam(TeamColor teamColor, Player player) {
      if (teamMap.containsKey(teamColor) && !teamMap.get(teamColor).contains(player.getName())) {
         if (teamMap.get(teamColor).size() < allowedPlayerAmount / teamMap.size()) {
            removeFromTeam(player);
            teamMap.get(teamColor).add(player.getName());
            Utils.sendMessage(player, config.getString("messages.joined-team." + teamColor.toString().toLowerCase()));
            Sounds.ORB_PICKUP.play(player);
            player.setPlayerListName(Utils.colorize(Utils.getColorCode(teamColor) + player.getName()));
            return true;
         } else {
            Utils.sendMessage(player, config.getString("messages.team-is-full"));
            Sounds.NOTE_BASS.play(player);
         }
      }
      return false;
   }

   public TeamColor getTeamColor(Player player) {
      for (Map.Entry<TeamColor, List<String>> entry : teamMap.entrySet()) {
         if (entry.getValue().contains(player.getName())) return entry.getKey();
      }
      return null;
   }

   public TeamColor getTeamColor(String playerName) {
      for (Map.Entry<TeamColor, List<String>> entry : teamMap.entrySet()) {
         if (entry.getValue().contains(playerName)) return entry.getKey();
      }
      return null;
   }

   public TeamColor getTeamColor(Location location) {
      return Utils.getKeyByValue(teamSpawnLocationMap, Utils.getClosestTo(location, teamSpawnLocationMap.values()));
   }

   public String getTypeString() {
      switch (type) {
         case SOLO: return "1v1";
         case DUO: return "2v2";
         case ONE_X_FOUR: return "1v1v1v1";
         case TWO_X_FOUR: return "2v2v2v2";
      }
      return "none";
   }

   public String getGameTypeString() {
      if (this.gameType == null) return config.getString("messages.game-type.none");
      return config.getString("messages.game-type." + this.gameType.toString().toLowerCase());
   }

   public String getName() {
      return name;
   }

   public String getWorldName() {
      return worldName;
   }

   public World getWorld() {
      return world;
   }

   public ArenaType getType() {
      return type;
   }

   public GameType getGameType() {
      return gameType;
   }

   public Location getWaitingPlayersSpawn() {
      return waitingPlayersSpawn;
   }

   public void setWaitingPlayersSpawn(Location waitingPlayersSpawn) {
      this.waitingPlayersSpawn = waitingPlayersSpawn;
   }

   public HashMap<TeamColor, Location> getTeamSpawnLocationMap() {
      return teamSpawnLocationMap;
   }

   public HashMap<TeamColor, Location> getTeamRespawnLocationMap() {
      return teamRespawnLocationMap;
   }

   public HashMap<TeamColor, List<String>> getTeamMap() {
      return teamMap;
   }

   public List<Player> getPlayers() {
      return players;
   }

   public int getAllowedPlayerAmount() {
      return allowedPlayerAmount;
   }

   public static HashMap<Player, Arena> getPlayerArenaMap() {
      return playerArenaMap;
   }

   public void setState(ArenaState state) {
      this.state = state;
   }

   public boolean haveSetupProperly() {
      return this.waitingPlayersSpawn != null && !this.getTeamSpawnLocationMap().isEmpty() && !this.getTeamRespawnLocationMap().isEmpty() && type != null;
   }

   public ArenaState getState() {
      return this.state;
   }

   public List<Block> getPlacedBlocks() {
      return placedBlocks;
   }

   public Game getGame() {
      return game;
   }

   public List<Region> getRegions() {
      return regions;
   }
}
