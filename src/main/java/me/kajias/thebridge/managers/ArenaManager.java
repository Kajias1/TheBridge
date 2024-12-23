package me.kajias.thebridge.managers;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.data.ArenasData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.ArenaType;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class ArenaManager
{
   public static void enableArena(Arena arena) {
      arena.setState(ArenaState.WAITING);
   }

   public static void disableArena(Arena arena) {
      arena.stop();
      arena.setState(ArenaState.DISABLED);
   }

   public static void removeArena(Arena arena) {
      arena.stop();
      ArenasData.removeArenaData(arena);
   }

   public static Arena getArenaByName(String name) {
      Optional<Arena> result = ArenasData.loadedArenas.stream().filter(x -> x.getName().equalsIgnoreCase(name)).findFirst();
      return result.orElse(null);
   }

   public static Arena findBestArena() {
      return ArenasData.loadedArenas.stream().filter(x -> (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING) &&
              !x.getPlayers().isEmpty()).findAny().orElse(ArenasData.loadedArenas.stream()
              .filter(x -> (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING)).findAny().orElse(null));
   }

   public static Arena findBestArenaByType(ArenaType arenaType) {
      return ArenasData.loadedArenas.stream().filter(x -> (x.getType() == arenaType && (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING)) &&
              !x.getPlayers().isEmpty()).findAny().orElse(ArenasData.loadedArenas.stream()
              .filter(x -> (x.getType() == arenaType && (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING))).findAny().orElse(null));
   }

   public static Arena findBestArenaByGameType(GameType gameType) {
      return ArenasData.loadedArenas.stream().filter(x -> (x.getGameType() == gameType && (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING)) &&
              !x.getPlayers().isEmpty()).findAny().orElse(ArenasData.loadedArenas.stream()
              .filter(x -> (x.getGameType() == gameType && (x.getState() == ArenaState.WAITING || x.getState() == ArenaState.STARTING))).findAny().orElse(null));
   }
}
