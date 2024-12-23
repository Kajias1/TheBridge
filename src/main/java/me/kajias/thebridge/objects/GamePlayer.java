package me.kajias.thebridge.objects;

import me.kajias.thebridge.enums.BlockColorType;
import me.kajias.thebridge.hooks.PAPIExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class GamePlayer
{
   private final UUID uuid;
   private String playerName;
   private HashMap<String, Integer> boughtBonuses;
   private HashMap<String, Integer> boughtBlockColors;
   private String selectedBlockColor;
   private Material pickaxeType;
   private int gamesPlayed;
   private int gamesWon;
   private int goals;
   private int kills;

   private BlockColorType blockColorType;
   private List<String> blockColorBlackList;
   private LocalDate lastDate;

   public GamePlayer(UUID uuid) {
      this.uuid = uuid;
      try {
         playerName = Bukkit.getPlayer(uuid).getName();
      } catch (NullPointerException e) {
         playerName = null;
      }
      gamesPlayed = 0;
      gamesWon = 0;
      goals = 0;
      kills = 0;
      boughtBonuses = new HashMap<>();
      boughtBlockColors = new HashMap<>();
      selectedBlockColor = null;
      blockColorType = BlockColorType.SINGLE;
      blockColorBlackList = new ArrayList<>();
      pickaxeType = Material.DIAMOND_PICKAXE;
      lastDate = LocalDate.now();
   }

   public UUID getUniqueId() {
      return uuid;
   }

   public String getPlayerName() {
      return playerName;
   }

   public int getGamesPlayed() {
      return gamesPlayed;
   }

   public int getGamesWon() {
      return gamesWon;
   }

   public LocalDate getLastDate() {
      return lastDate;
   }

   public void setGamesPlayed(int gamesPlayed) {
      this.gamesPlayed = gamesPlayed;
   }

   public void setGamesWon(int gamesWon) {
      this.gamesWon = gamesWon;
   }

   public int getGoals() {
      return goals;
   }

   public void setGoals(int goals) {
      this.goals = goals;
   }

   public int getKills() {
      return kills;
   }

   public void setKills(int kills) {
      this.kills = kills;
   }

   public void setPlayerName(String playerName) {
      this.playerName = playerName;
   }

   public void setLastDate(LocalDate lastDate) {
      this.lastDate = lastDate;
   }

   public void updateDateAndItems() {
      if (this.lastDate.isBefore(LocalDate.now())) {
         if (!this.boughtBonuses.isEmpty()) {
            HashMap<String, Integer> updatedBoughtBonuses = new HashMap<>();
            for (Map.Entry<String, Integer> entry : this.boughtBonuses.entrySet()) {
               if (entry.getValue() - (int) ChronoUnit.DAYS.between(this.lastDate, LocalDate.now()) > 1)
                  updatedBoughtBonuses.put(entry.getKey(), entry.getValue() - (int) ChronoUnit.DAYS.between(this.lastDate, LocalDate.now()));
            }
            this.boughtBonuses.clear();
            this.boughtBonuses = updatedBoughtBonuses;
         }
         if (!this.boughtBlockColors.isEmpty()) {
            HashMap<String, Integer> updatedBoughtBlockColors = new HashMap<>();
            for (Map.Entry<String, Integer> entry : this.boughtBlockColors.entrySet()) {
               if (entry.getValue() - (int) ChronoUnit.DAYS.between(this.lastDate, LocalDate.now()) > 1)
                  updatedBoughtBlockColors.put(entry.getKey(), entry.getValue() - (int) ChronoUnit.DAYS.between(this.lastDate, LocalDate.now()));
            }
            this.boughtBlockColors.clear();
            this.boughtBlockColors = updatedBoughtBlockColors;
         }
      }
      this.lastDate = LocalDate.now();
   }

   public List<String> getAvailableBlockColors() {
      List<String> filtered = new ArrayList<>();
      for (String boughtColor : boughtBlockColors.keySet()) {
         if (!blockColorBlackList.contains(boughtColor)) filtered.add(boughtColor);
      }
      return filtered;
   }

   public HashMap<String, Integer> getBoughtBonuses() {
      return boughtBonuses;
   }

   public void setBoughtBonuses(HashMap<String, Integer> boughtBonuses) {
      this.boughtBonuses = boughtBonuses;
   }

   public HashMap<String, Integer> getBoughtBlockColors() {
      return boughtBlockColors;
   }

   public void setBoughtBlockColors(HashMap<String, Integer> boughtBlockColors) {
      this.boughtBlockColors = boughtBlockColors;
   }

   public String getSelectedBlockColor() {
      return selectedBlockColor;
   }

   public void setSelectedBlockColor(String selectedBlockColor) {
      this.selectedBlockColor = selectedBlockColor;
   }

   public List<String> getBlockColorBlackList() {
      return blockColorBlackList;
   }

   public void setBlockColorBlackList(List<String> blockColorBlackList) {
      this.blockColorBlackList = blockColorBlackList;
   }

   public BlockColorType getBlockColorType() {
      return blockColorType;
   }

   public void setBlockColorType(BlockColorType blockColorType) {
      this.blockColorType = blockColorType;
   }

   public Material getPickaxeType() {
      return pickaxeType;
   }

   public void setPickaxeType(Material pickaxeType) {
      this.pickaxeType = pickaxeType;
   }
}
