package me.kajias.thebridge.data;

import me.kajias.thebridge.configurations.BaseConfiguration;
import me.kajias.thebridge.enums.BlockColorType;
import me.kajias.thebridge.objects.GamePlayer;
import org.bukkit.Material;

import java.time.LocalDate;
import java.util.*;

public class PlayersData
{
   private static final BaseConfiguration playersData = new BaseConfiguration("players");

   public static void initialize() {
      playersData.load();
   }

   public static List<GamePlayer> loadedPlayers = new ArrayList<>();

   public static void savePlayerData(GamePlayer gamePlayer) {
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".name", gamePlayer.getPlayerName());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".games-played", gamePlayer.getGamesPlayed());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".games-won", gamePlayer.getGamesWon());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".goals", gamePlayer.getGoals());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".kills", gamePlayer.getKills());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".bought-bonuses", gamePlayer.getBoughtBonuses());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".bought-block-colors", gamePlayer.getBoughtBlockColors());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".bought-block-colors-black-list", gamePlayer.getBlockColorBlackList());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".block-color-type", gamePlayer.getBlockColorType().toString());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".pickaxe-type", gamePlayer.getPickaxeType().toString());
      playersData.getConfig().set("players." + gamePlayer.getUniqueId() + ".last-date", gamePlayer.getLastDate().toString());

      Optional<GamePlayer> optPlayer = loadedPlayers.stream().filter(x -> x.getUniqueId().equals(gamePlayer.getUniqueId())).findFirst();
      if (optPlayer.isPresent()) {
         loadedPlayers.set(loadedPlayers.indexOf(optPlayer.get()), gamePlayer);
      } else loadedPlayers.add(gamePlayer);
      playersData.save();
   }

   public static void loadAllPlayersData() {
      if (playersData.getConfig().getConfigurationSection("players") == null) return;
      for (String uuid : playersData.getConfig().getConfigurationSection("players").getKeys(false)) {
         GamePlayer gamePlayer = new GamePlayer(UUID.fromString(uuid));
         gamePlayer.setPlayerName(playersData.getConfig().getString("players." + uuid + ".name"));
         gamePlayer.setGamesPlayed(playersData.getConfig().getInt("players." + uuid + ".games-played"));
         gamePlayer.setGamesWon(playersData.getConfig().getInt("players." + uuid + ".games-won"));
         gamePlayer.setGoals(playersData.getConfig().getInt("players." + uuid + ".goals"));
         gamePlayer.setKills(playersData.getConfig().getInt("players." + uuid + ".kills"));
         for (Map.Entry<String, Object> entry : playersData.getConfig().getConfigurationSection("players." + uuid + ".bought-bonuses").getValues(false).entrySet())
            if (entry.getValue() instanceof Integer) gamePlayer.getBoughtBonuses().put(entry.getKey(), (Integer) entry.getValue());
         for (Map.Entry<String, Object> entry : playersData.getConfig().getConfigurationSection("players." + uuid + ".bought-block-colors").getValues(false).entrySet())
            if (entry.getValue() instanceof Integer) gamePlayer.getBoughtBlockColors().put(entry.getKey(), (Integer) entry.getValue());
         for (String blackListedColor : playersData.getConfig().getStringList("players." + uuid + ".bought-block-colors-black-list"))
            gamePlayer.getBlockColorBlackList().add(blackListedColor);
         gamePlayer.setBlockColorType(BlockColorType.valueOf(playersData.getConfig().getString("players." + uuid + ".block-color-type")));
         gamePlayer.setPickaxeType(Material.valueOf(playersData.getConfig().getString("players." + uuid + ".pickaxe-type")));
         if (playersData.getConfig().getString("players." + uuid + ".last-date") != null)
            gamePlayer.setLastDate(LocalDate.parse(playersData.getConfig().getString("players." + uuid + ".last-date")));
         gamePlayer.updateDateAndItems();
         loadedPlayers.add(gamePlayer);
      }
   }

   public static GamePlayer getPlayerData(UUID uuid) {
      return loadedPlayers.stream().filter(x -> x.getUniqueId().equals(uuid)).findFirst().orElse(new GamePlayer(uuid));
   }
}
