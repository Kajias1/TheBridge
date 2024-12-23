package me.kajias.thebridge.gui.menus.team_select;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.configurations.ItemConfiguration;
import me.kajias.thebridge.configurations.MenuConfiguration;
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

public class TeamSelectMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   private final Arena arena;

   public TeamSelectMenu(Arena arena) {
      super(true);
      this.arena = arena;
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.team-select.size"), Utils.colorize(menusConfig.getString("menus.team-select.title")));
   }

   @Override
   public void decorate(Player player) {
      for (TeamColor teamColor : TeamColor.values()) {
         if (arena.getTeamMap().containsKey(teamColor)) {
            ItemStack teamJoinItem = createItem(
                    Material.valueOf(menusConfig.getString("menus.team-select.items." + teamColor.toString().toLowerCase() + ".material")),
                    menusConfig.getString("menus.team-select.items." + teamColor.toString().toLowerCase() + ".name")
                            .replace("%players_now%", String.valueOf(arena.getTeamMap().get(teamColor).size()))
                            .replace("%players_max%", String.valueOf(arena.getAllowedPlayerAmount() / arena.getTeamMap().size())),
                    (short) menusConfig.getInt("menus.team-select.items." + teamColor.toString().toLowerCase() + ".data"),
                    arena.getTeamColor(player) == teamColor,
                    teamColor);
            InventoryButton teamJoinButton = new InventoryButton()
                    .creator(player1 -> teamJoinItem)
                    .consumer(event -> {
                       if (arena.addToTeam(teamColor, player)) {
                          player.getInventory().getItem(ItemConfiguration.baseConfig.getConfig().getInt("hot-bar-items.team-select.slot") - 1)
                                  .setDurability((short) menusConfig.getInt("menus.team-select.items." + teamColor.toString().toLowerCase() + ".data"));
                       }
                       player.closeInventory();
                    });
            this.setButton(menusConfig.getInt("menus.team-select.items." + teamColor.toString().toLowerCase() + ".slot") - 1, teamJoinButton);
         }
      }

      super.decorate(player);
   }

   private ItemStack createItem(Material material, String displayName, short data, boolean enchanted, TeamColor teamColor) {
      ItemStack itemStack = new ItemStack(material);
      if (data != 0) itemStack = new ItemStack(material, 1, data);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setDisplayName(Utils.colorize(displayName));
      if (arena.getTeamMap().get(teamColor) != null) {
         List<String> teamSelectLore;
         if (!arena.getTeamMap().get(teamColor).isEmpty()) {
            teamSelectLore = Utils.colorize(menusConfig.getStringList("menus.team-select.team-select-button-lore.has-players"));
            for (String playerName : arena.getTeamMap().get(teamColor)) {
               teamSelectLore.add(Utils.colorize(menusConfig.getString("menus.team-select.player-list-row").replace("%player_name%", playerName)));
            }
         } else teamSelectLore = Utils.colorize(menusConfig.getStringList("menus.team-select.team-select-button-lore.empty"));
         itemMeta.setLore(teamSelectLore);
      }
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      if (enchanted) itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }
}
