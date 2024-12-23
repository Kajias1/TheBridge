package me.kajias.thebridge.gui.menus.arena_setup;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.enums.ArenaType;
import me.kajias.thebridge.enums.GameType;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import me.kajias.thebridge.commands.AdminCommand;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.gui.InventoryButton;
import me.kajias.thebridge.gui.InventoryGUI;
import me.kajias.thebridge.objects.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ArenaTypeSelectionMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();

   public ArenaTypeSelectionMenu() {
      super(false);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.arena-type-selection.size"), Utils.colorize(menusConfig.getString("menus.arena-type-selection.title")));
   }

   @Override
   public void decorate(Player player) {
      Arena arena = AdminCommand.setupMap.get(player);

      int n = 0;
      for (ArenaType arenaType : ArenaType.values()) {
         ItemStack arenaTypeItem = createItem(
                 Material.valueOf(menusConfig.getString("menus.arena-type-selection.arena-type-button.material")),
                 Utils.colorize(menusConfig.getString("menus.arena-type-selection.arena-type-button.name").replace("%type%", arenaType.toString())),
                 (short) n
         );
         InventoryButton setTypeButton = new InventoryButton()
                 .creator(player1 -> arenaTypeItem)
                 .consumer(event -> {
                    arena.setType(arenaType);
                    Sounds.ORB_PICKUP.play(player);
                    TheBridge.guiManager.openGUI(new ArenaSetupMenu(), player);
                 });
         this.setButton(n, setTypeButton);
         n++;
      }

      super.decorate(player);
   }

   private ItemStack createItem(Material material, String displayName, short data) {
      ItemStack itemStack = new ItemStack(material);
      if (data != 0) itemStack = new ItemStack(material, 1, data);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setDisplayName(Utils.colorize(displayName));
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }
}
