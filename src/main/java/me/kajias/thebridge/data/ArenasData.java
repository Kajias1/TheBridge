package me.kajias.thebridge.data;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.objects.Region;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.BaseConfiguration;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.ArenaType;
import me.kajias.thebridge.enums.TeamColor;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ArenasData
{
   private static final BaseConfiguration arenasData = new BaseConfiguration("arenas");
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public static void initialize() {
      arenasData.load();
   }

   public static List<Arena> loadedArenas = new ArrayList<>();

   public static void saveArenaData(Arena arena) {
      if (arena.haveSetupProperly()) {
         arenasData.getConfig().set("arenas." + arena.getName() + ".name", arena.getName());
         arenasData.getConfig().set("arenas." + arena.getName() + ".world-name", arena.getWorldName());
         arenasData.getConfig().set("arenas." + arena.getName() + ".type", arena.getType().toString());
         arenasData.getConfig().set("arenas." + arena.getName() + ".game-type", arena.getGameType().toString());
         arenasData.getConfig().set("arenas." + arena.getName() + ".waiting-spawn-location", arena.getWaitingPlayersSpawn().serialize());
         if (!arena.getTeamSpawnLocationMap().isEmpty()) {
            for (Map.Entry<TeamColor, Location> entry : arena.getTeamSpawnLocationMap().entrySet()) {
               arenasData.getConfig().set("arenas." + arena.getName() + ".team-spawn-locations." + entry.getKey().toString(), entry.getValue().serialize());
            }
         } else arenasData.getConfig().set("arenas." + arena.getName() + ".team-spawn-locations", null);
         if (!arena.getTeamRespawnLocationMap().isEmpty()) {
            for (Map.Entry<TeamColor, Location> entry : arena.getTeamRespawnLocationMap().entrySet()) {
               arenasData.getConfig().set("arenas." + arena.getName() + ".team-respawn-locations." + entry.getKey().toString(), entry.getValue().serialize());
            }
         } else arenasData.getConfig().set("arenas." + arena.getName() + ".team-respawn-locations", null);
         if (!arena.getRegions().isEmpty()) {
            for (Region region : arena.getRegions()) {
               arenasData.getConfig().set("arenas." + arena.getName() + ".regions." + region.getUniqueId() + ".position1", region.getPosition1().serialize());
               arenasData.getConfig().set("arenas." + arena.getName() + ".regions." + region.getUniqueId() + ".position2", region.getPosition2().serialize());
            }
         } else arenasData.getConfig().set("arenas." + arena.getName() + ".team-respawn-locations", null);

         Optional<Arena> optArena = loadedArenas.stream().filter(x -> x.getName().equals(arena.getName())).findFirst();
         if (optArena.isPresent()) {
            loadedArenas.set(loadedArenas.indexOf(optArena.get()), arena);
         } else loadedArenas.add(arena);
         arenasData.save();
      }
   }

   public static void loadAllArenasData() {
      if (arenasData.getConfig().getConfigurationSection("arenas") == null) return;
      for (String arenaName : arenasData.getConfig().getConfigurationSection("arenas").getKeys(false)) {
         Arena arena = new Arena(arenasData.getConfig().getString("arenas." + arenaName + ".name"), arenasData.getConfig().getString("arenas." + arenaName + ".world-name"));
         arena.setType(ArenaType.valueOf(arenasData.getConfig().getString("arenas." + arenaName + ".type")));
         arena.setGameType(GameType.valueOf(arenasData.getConfig().getString("arenas." + arenaName + ".game-type")));
         arena.setWaitingPlayersSpawn(Location.deserialize(arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".waiting-spawn-location").getValues(false)));
         if (arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-spawn-locations") != null) {
            for (String teamColorString : arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-spawn-locations").getKeys(false)) {
               arena.getTeamSpawnLocationMap().put(
                       TeamColor.valueOf(teamColorString),
                       Location.deserialize(arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-spawn-locations." + teamColorString).getValues(false))
               );
            }
         }
         if (arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-respawn-locations") != null) {
            for (String teamColorString : arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-respawn-locations").getKeys(false)) {
               arena.getTeamRespawnLocationMap().put(
                       TeamColor.valueOf(teamColorString),
                       Location.deserialize(arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".team-respawn-locations." + teamColorString).getValues(false))
               );
            }
         }
         if (arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".regions") != null) {
            for (String regionUUID : arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".regions").getKeys(false)) {
               arena.getRegions().add(new Region(
                       UUID.fromString(regionUUID),
                       Location.deserialize(arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".regions." + regionUUID + ".position1").getValues(false)),
                       Location.deserialize(arenasData.getConfig().getConfigurationSection("arenas." + arenaName + ".regions." + regionUUID + ".position2").getValues(false))
               ));
            }
         }
         if (arena.haveSetupProperly()) {
            arena.setState(ArenaState.WAITING);
            loadedArenas.add(arena);
            TheBridge.printLog(Utils.colorize(config.getString("messages.log.arena-loaded-successfully").replace("%arena%", arenaName)));
         } else {
            TheBridge.printLog(Utils.colorize(config.getString("messages.log.arena-load-failed").replace("%arena%", arenaName)));
            TheBridge.printLog(config.getString("messages.log.arena-was-not-setup-properly").replace("%arena%", arenaName));
         }
      }
   }

   public static void removeArenaData(Arena arena) {
      loadedArenas.remove(arena);
      try {
         arenasData.getConfig().set("arenas." + arena.getName(), null);
      } catch (NullPointerException ignored) {}
      arenasData.save();
   }
}
