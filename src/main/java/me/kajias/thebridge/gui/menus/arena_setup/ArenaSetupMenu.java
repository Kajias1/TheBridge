package me.kajias.thebridge.gui.menus.arena_setup;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.commands.AdminCommand;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.ArenasData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.TeamColor;
import me.kajias.thebridge.gui.InventoryButton;
import me.kajias.thebridge.gui.InventoryGUI;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ArenaSetupMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public ArenaSetupMenu() {
      super(false);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.arena-configuration.size"), Utils.colorize(menusConfig.getString("menus.arena-configuration.title")));
   }

   @Override
   public void decorate(Player player) {
      Arena arena = AdminCommand.setupMap.get(player);

      ItemStack setWaitingLocationItem = createItem(
              Material.valueOf(menusConfig.getString("menus.arena-configuration.items.set-waiting-spawn-location.material")),
              menusConfig.getString("menus.arena-configuration.items.set-waiting-spawn-location.name"),
              menusConfig.getStringList("menus.arena-configuration.items.set-waiting-spawn-location.lore"),
              (short) menusConfig.getInt("menus.arena-configuration.items.set-waiting-spawn-location.data"),
              arena.getWaitingPlayersSpawn() != null);
      InventoryButton setWaitingLocationButton = new InventoryButton()
              .creator(player1 -> setWaitingLocationItem)
              .consumer(event -> {
                 arena.setWaitingPlayersSpawn(player.getLocation());
                 Sounds.ORB_PICKUP.play(player);
                 update(player);
              });
      this.setButton(menusConfig.getInt("menus.arena-configuration.items.set-waiting-spawn-location.slot") - 1, setWaitingLocationButton);

      ItemStack setTypeItem = createItem(
              Material.valueOf(menusConfig.getString("menus.arena-configuration.items.set-arena-type.material")),
              menusConfig.getString("menus.arena-configuration.items.set-arena-type.name"),
              menusConfig.getStringList("menus.arena-configuration.items.set-arena-type.lore"),
              (short) menusConfig.getInt("menus.arena-configuration.items.set-arena-type.data"),
              arena.getType() != null);
      InventoryButton setTypeButton = new InventoryButton()
              .creator(player1 -> setTypeItem)
              .consumer(event -> {
                 TheBridge.guiManager.openGUI(new ArenaTypeSelectionMenu(), player);
              });
      this.setButton(menusConfig.getInt("menus.arena-configuration.items.set-arena-type.slot") - 1, setTypeButton);

      ItemStack setGameTypeItem = createItem(
              Material.valueOf(menusConfig.getString("menus.arena-configuration.items.set-game-type.material")),
              menusConfig.getString("menus.arena-configuration.items.set-game-type.name"),
              menusConfig.getStringList("menus.arena-configuration.items.set-game-type.lore"),
              (short) menusConfig.getInt("menus.arena-configuration.items.set-game-type.data"),
              arena.getType() != null);
      InventoryButton setGameTypeButton = new InventoryButton()
              .creator(player1 -> setGameTypeItem)
              .consumer(event -> {
                 TheBridge.guiManager.openGUI(new GameTypeSelectionMenu(), player);
              });
      this.setButton(menusConfig.getInt("menus.arena-configuration.items.set-game-type.slot") - 1, setGameTypeButton);

      for (String teamConfigurationItemCategory : menusConfig.getConfigurationSection("menus.arena-configuration.team-configuration-items").getKeys(false)) {
         if (!arena.getTeamMap().containsKey(TeamColor.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".team")))) continue;
         ItemStack setRedTeamSpawnLocationItem = createItem(
                 Material.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".material")),
                 menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".name"),
                 menusConfig.getStringList("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".lore"),
                 (short) menusConfig.getInt("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".data"),
                 menusConfig.getBoolean("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".is-first") ?
                         arena.getTeamSpawnLocationMap().get(TeamColor.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".team"))) != null
                 : arena.getTeamRespawnLocationMap().get(TeamColor.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".team"))) != null
         );
         InventoryButton setRedTeamSpawnLocationButton = new InventoryButton()
                 .creator(player1 -> setRedTeamSpawnLocationItem)
                 .consumer(event -> {
                    if (menusConfig.getBoolean("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".is-first")) {
                       arena.getTeamSpawnLocationMap().put(TeamColor.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".team")), player.getLocation());
                    } else arena.getTeamRespawnLocationMap().put(TeamColor.valueOf(menusConfig.getString("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".team")), player.getLocation());
                    Sounds.ORB_PICKUP.play(player);
                    update(player);
                 });
         this.setButton(menusConfig.getInt("menus.arena-configuration.team-configuration-items." + teamConfigurationItemCategory + ".slot") - 1, setRedTeamSpawnLocationButton);
      }

      ItemStack finishSetupItem = createItem(
              Material.valueOf(menusConfig.getString("menus.arena-configuration.items.finish-setup.material")),
              menusConfig.getString("menus.arena-configuration.items.finish-setup.name"),
              menusConfig.getStringList("menus.arena-configuration.items.finish-setup.lore"),
              (short) menusConfig.getInt("menus.arena-configuration.items.finish-setup.data"),
              false);
      InventoryButton finishSetupButton = new InventoryButton()
              .creator(player1 -> finishSetupItem)
              .consumer(event -> {
                 if (arena.haveSetupProperly()) {
                    if (!ArenasData.loadedArenas.contains(arena)) ArenasData.loadedArenas.add(arena);
                    AdminCommand.setupMap.remove(player);
                    arena.setState(ArenaState.WAITING);
                    Utils.teleportToLobby(player);
                    Utils.sendMessage(player, config.getString("messages.exiting-setup"));
                    Sounds.ORB_PICKUP.play(player);
                    arena.getWorld().save();
                 } else {
                    player.closeInventory();
                    Utils.sendMessage(player, config.getString("messages.failed-to-finish-setup"));
                    Sounds.NOTE_BASS.play(player);
                 }
              });
      this.setButton(menusConfig.getInt("menus.arena-configuration.items.finish-setup.slot") - 1, finishSetupButton);

      super.decorate(player);
   }

   private ItemStack createItem(Material material, String displayName, List<String> lore, short data, boolean enchanted) {
      ItemStack itemStack = new ItemStack(material);
      if (data != 0) itemStack = new ItemStack(material, 1, data);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setDisplayName(Utils.colorize(displayName));
      if (lore != null) itemMeta.setLore(Utils.colorize(lore));
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      if (enchanted) itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }

   private void update(Player player) {
      TheBridge.guiManager.openGUI(new ArenaSetupMenu(), player);
   }
}
