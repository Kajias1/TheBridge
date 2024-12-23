package me.kajias.thebridge.configurations;

import me.kajias.thebridge.utils.Utils;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemConfiguration
{
   public static final BaseConfiguration baseConfig = new BaseConfiguration("items");

   public static boolean isHotBarItem(ItemStack item) {
      if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;

      List<String> itemsNameList = new ArrayList<>();
      for (String itemName : baseConfig.getConfig().getConfigurationSection("hot-bar-items").getKeys(false))
         itemsNameList.add(Utils.colorize(baseConfig.getConfig().getString("hot-bar-items." + itemName + ".name")));

      return itemsNameList.contains(item.getItemMeta().getDisplayName());
   }

   public static void initialize() {
      baseConfig.load();
   }
}
