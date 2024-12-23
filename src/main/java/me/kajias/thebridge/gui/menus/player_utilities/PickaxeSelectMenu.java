package me.kajias.thebridge.gui.menus.player_utilities;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.gui.InventoryButton;
import me.kajias.thebridge.gui.InventoryGUI;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
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

public class PickaxeSelectMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public PickaxeSelectMenu() {
      super(false);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.pickaxe-customization.size"), Utils.colorize(menusConfig.getString("menus.pickaxe-customization.title")));
   }

   @Override
   public void decorate(Player player) {
      GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());

      for (String fillerItemCategory : menusConfig.getConfigurationSection("menus.pickaxe-customization.filler-items").getKeys(false)) {
         ItemStack fillerItem = new ItemStack(Material.getMaterial(menusConfig.getString("menus.pickaxe-customization.filler-items." + fillerItemCategory + ".material")), 1,
                 (short) menusConfig.getInt("menus.pickaxe-customization.filler-items." + fillerItemCategory + ".data"));
         ItemMeta fillerMeta = fillerItem.getItemMeta();
         fillerMeta.setDisplayName(" ");
         fillerItem.setItemMeta(fillerMeta);

         for (int index : menusConfig.getIntegerList("menus.pickaxe-customization.filler-items." + fillerItemCategory + ".slots"))
            this.getInventory().setItem(index - 1, fillerItem);
      }

      for (String pickaxeItemCategory : menusConfig.getConfigurationSection("menus.pickaxe-customization.items").getKeys(false)) {
         ItemStack pickaxeItem = createItem(Material.valueOf(menusConfig.getString("menus.pickaxe-customization.items." + pickaxeItemCategory + ".material")));
         if (menusConfig.getConfigurationSection("menus.pickaxe-customization.items." + pickaxeItemCategory + ".name") != null) {
            ItemMeta pickaxeMeta = pickaxeItem.getItemMeta();
            pickaxeMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.pickaxe-customization.items." + pickaxeItemCategory + ".name")));
            pickaxeItem.setItemMeta(pickaxeMeta);
         }
         InventoryButton pickaxeItemPurchaseButton = new InventoryButton()
                 .creator(player1 -> pickaxeItem)
                 .consumer(event -> {
                    if (!gamePlayer.getBoughtBonuses().containsKey(pickaxeItem.getType().toString())) {
                       TheBridge.guiManager.openGUI(new PickaxePurchaseMenu(pickaxeItem,
                               menusConfig.getIntegerList("menus.pickaxe-customization.items." + pickaxeItemCategory + ".prices.anix"),
                               menusConfig.getIntegerList("menus.pickaxe-customization.items." + pickaxeItemCategory + ".prices.bonus"),
                               menusConfig.getStringList("menus.pickaxe-customization.items." + pickaxeItemCategory + ".prices.benefit")
                       ), player);
                    } else {
                       gamePlayer.setPickaxeType(pickaxeItem.getType());
                       Utils.sendMessage(player, config.getString("messages.pickaxe-selected"));
                       Sounds.ORB_PICKUP.play(player);
                       player.closeInventory();
                    }
                 });
         this.setButton(menusConfig.getInt("menus.pickaxe-customization.items." + pickaxeItemCategory + ".slot") - 1, pickaxeItemPurchaseButton);
      }

      super.decorate(player);
   }

   private ItemStack createItem(Material material) {
      ItemStack itemStack = new ItemStack(material);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }
}
