package me.kajias.thebridge;

import me.kajias.thebridge.commands.LeaveCommand;
import me.kajias.thebridge.listeners.*;
import me.kajias.thebridge.managers.ArenaManager;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.commands.AdminCommand;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.configurations.ScoreboardConfiguration;
import me.kajias.thebridge.data.ArenasData;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.gui.GUIListener;
import me.kajias.thebridge.gui.GUIManager;
import me.kajias.thebridge.hooks.PAPIExpansion;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class TheBridge extends JavaPlugin
{
   public static Plugin INSTANCE;
   public static Location lobbyLocation = null;
   public static GUIManager guiManager = null;
   public static GUIListener guiListener = null;
   public static PlayerPoints playerPoints = null;
   public static Economy economy = null;

   @Override
   public void onEnable() {
      if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
         printLog("&cДля работы плагина требуется PlaceholderAPI");
         Bukkit.getPluginManager().disablePlugin(this);
         return;
      }
      if (Bukkit.getPluginManager().getPlugin("PlayerPoints") == null) {
         printLog("&cДля работы плагина требуется PlayerPoints");
         Bukkit.getPluginManager().disablePlugin(this);
         return;
      } else {
         playerPoints = (PlayerPoints) this.getServer().getPluginManager().getPlugin("PlayerPoints");
      }
      if (!setupEconomy() ) {
         printLog("&cДля работы плагина требуется Vault!");
         getServer().getPluginManager().disablePlugin(this);
         return;
      }

      INSTANCE = this;

      getConfig().options().copyDefaults(true);
      saveConfig();

      ItemConfiguration.initialize();
      MenuConfiguration.initialize();
      ScoreboardConfiguration.initialize();

      if (getConfig().getConfigurationSection("lobby-spawn-point") != null) {
         lobbyLocation = Location.deserialize(getConfig().getConfigurationSection("lobby-spawn-point").getValues(false));
      } else printLog(Utils.colorize(getConfig().getString("messages.log.no-lobby")));

      PlayersData.initialize();
      PlayersData.loadAllPlayersData();
      ArenasData.initialize();
      ArenasData.loadAllArenasData();

      new DisabledEvents(this);
      new PlayerBreakPlaceBlock(this);
      new PlayerDamage(this);
      new PlayerInteractItem(this);
      new PlayerMove(this);
      new PlayerQuitJoin(this);
      new PlayerUseChat(this);
      new Scoreboard(this);

      getCommand("thebridge").setExecutor(new AdminCommand());
      getCommand("tb").setExecutor(new AdminCommand());
      getCommand("leave").setExecutor(new LeaveCommand());

      guiManager = new GUIManager();
      guiListener = new GUIListener(guiManager, this);

      PAPIExpansion.registerHook();
   }

   @Override
   public void onDisable() {
      PlayersData.loadedPlayers.forEach(PlayersData::savePlayerData);
      ArenasData.loadedArenas.forEach(Arena::stop);
      ArenasData.loadedArenas.forEach(ArenasData::saveArenaData);
   }

   public static void printLog(String message) {
      Bukkit.getConsoleSender().sendMessage(Utils.colorize(INSTANCE.getConfig().getString("messages.log.prefix") + message));
   }

   private boolean setupEconomy() {
      if (getServer().getPluginManager().getPlugin("Vault") == null) {
         return false;
      }
      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp == null) {
         return false;
      }
      economy = rsp.getProvider();
      return economy != null;
   }

   public static PlayerPoints getPlayerPoints() {
      return playerPoints;
   }

   public static Economy getEconomy() {
      return economy;
   }
}
