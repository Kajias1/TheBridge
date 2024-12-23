package me.kajias.thebridge.gui.menus.arena_select;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.ArenasData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.gui.InventoryButton;
import me.kajias.thebridge.gui.InventoryGUI;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ArenaSelectMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public ArenaSelectMenu() {
      super(true);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.arena-select.size"), Utils.colorize(menusConfig.getString("menus.arena-select.title")));
   }

   @Override
   public void decorate(Player player) {
      List<Integer> arenaSlots = menusConfig.getIntegerList("menus.arena-select.slots");

      ItemStack fillerItem = new ItemStack(Material.STAINED_GLASS, 1, (short) 15);
      ItemMeta fillerItemMeta = fillerItem.getItemMeta();
      fillerItemMeta.setDisplayName(" ");
      fillerItem.setItemMeta(fillerItemMeta);
      for (int i : arenaSlots) {
         this.getInventory().setItem(i, fillerItem);
      }

      List<Arena> arenas = ArenasData.loadedArenas.stream().filter(a -> a.getState() == ArenaState.WAITING)
              .sorted(Comparator.comparing(arena -> arena.getPlayers().size())).collect(Collectors.toList());
      Collections.reverse(arenas);

      for (int i = 0; i < arenaSlots.size() + 1; i++) {
         Arena arena;

         try {
            arena = arenas.get(i);

            ItemStack arenaIcon = new ItemStack(Material.getMaterial(menusConfig.getString("menus.arena-select.material")), Integer.max(1, arena.getPlayers().size()), (short) (arena.getPlayers().isEmpty() ? 5 : 1));
            ItemMeta arenaIconMeta = arenaIcon.getItemMeta();
            arenaIconMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.arena-select.display-name").replace("%arena_name%", arena.getName())));
            List<String> arenaIconLore = new ArrayList<>();
            for (String s : menusConfig.getStringList("menus.arena-select.lore")) {
               arenaIconLore.add(Utils.colorize(s
                       .replace("%arena_players%", String.valueOf(arena.getPlayers().size()))
                       .replace("%arena_players_total%", String.valueOf(arena.getAllowedPlayerAmount()))
                       .replace("%arena_type%", arena.getTypeString())
                       .replace("%game_type%", arena.getGameTypeString())
                       .replace("%arena_name%", arena.getName())
               ));
            }
            arenaIconMeta.setLore(arenaIconLore);
            arenaIcon.setItemMeta(arenaIconMeta);
            InventoryButton arenaJoinButton = new InventoryButton()
                    .creator(player1 -> arenaIcon)
                    .consumer(event -> {
                       arena.addPlayer(player);
                       player.closeInventory();
                    });
            int slot = arenaSlots.get(i + 1);
            this.setButton(slot - 1, arenaJoinButton);
         } catch (IndexOutOfBoundsException e) {
            break;
         }
      }

      super.decorate(player);
   }
}
