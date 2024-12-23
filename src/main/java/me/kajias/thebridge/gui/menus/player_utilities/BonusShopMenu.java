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
import java.util.Random;

public class BonusShopMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public BonusShopMenu() {
      super(true);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.bonus-shop.size"), Utils.colorize(menusConfig.getString("menus.bonus-shop.title")));
   }

   @Override
   public void decorate(Player player) {
      GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());

      for (String fillerItemCategory : menusConfig.getConfigurationSection("menus.bonus-shop.filler-items").getKeys(false)) {
         ItemStack fillerItem = new ItemStack(Material.getMaterial(menusConfig.getString("menus.bonus-shop.filler-items." + fillerItemCategory + ".material")), 1,
                 (short) menusConfig.getInt("menus.bonus-shop.filler-items." + fillerItemCategory + ".data"));
         ItemMeta fillerMeta = fillerItem.getItemMeta();
         fillerMeta.setDisplayName(" ");
         fillerItem.setItemMeta(fillerMeta);

         for (int index : menusConfig.getIntegerList("menus.bonus-shop.filler-items." + fillerItemCategory + ".slots"))
            this.getInventory().setItem(index - 1, fillerItem);
      }

      for (String bonusItemCategory : menusConfig.getConfigurationSection("menus.bonus-shop.items").getKeys(false)) {
         ItemStack bonusItem = createItem(
                 Material.getMaterial(menusConfig.getString("menus.bonus-shop.items." + bonusItemCategory + ".material")),
                 menusConfig.getString("menus.bonus-shop.items." + bonusItemCategory + ".name"),
                 menusConfig.getStringList("menus.bonus-shop.items." + bonusItemCategory + ".lore"),
                 menusConfig.getBoolean("menus.bonus-shop.items." + bonusItemCategory + ".enchanted")
         );
         InventoryButton bonusItemPurchaseButton = new InventoryButton()
                 .creator(player1 -> bonusItem)
                 .consumer(event -> {
                    if (!gamePlayer.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.bonus-shop.items." + bonusItemCategory + ".name")))) {
                       TheBridge.guiManager.openGUI(new BonusPurchaseMenu(
                               bonusItem,
                               menusConfig.getIntegerList("menus.bonus-shop.items." + bonusItemCategory + ".prices.anix"),
                               menusConfig.getIntegerList("menus.bonus-shop.items." + bonusItemCategory + ".prices.bonus"),
                               menusConfig.getStringList("menus.bonus-shop.items." + bonusItemCategory + ".prices.benefit")
                       ), player);
                    } else {
                       Utils.sendMessage(player, config.getString("messages.item-was-already-bought"));
                       Sounds.ORB_PICKUP.play(player);
                       player.closeInventory();
                    }
                 });
         this.setButton(menusConfig.getInt("menus.bonus-shop.items." + bonusItemCategory + ".slot") - 1, bonusItemPurchaseButton);
      }
      ItemStack blockColorMenuItem = createItem(
              Material.valueOf(config.getString("game-config.main-block-material")),
              menusConfig.getString("menus.bonus-shop.block-custom-color.name"),
              menusConfig.getStringList("menus.bonus-shop.block-custom-color.lore"),
              false
      );
      blockColorMenuItem.setDurability((short) new Random().nextInt(15));
      InventoryButton blockColorMenuItemButton = new InventoryButton()
              .creator(player1 -> blockColorMenuItem)
              .consumer(event -> {
                 TheBridge.guiManager.openGUI(new ColorSelectMenu(), player);
              });
      this.setButton(menusConfig.getInt("menus.bonus-shop.block-custom-color.slot") - 1, blockColorMenuItemButton);

      ItemStack customPickaxeMenuItem = createItem(
              gamePlayer.getPickaxeType(),
              menusConfig.getString("menus.bonus-shop.custom-pickaxe.name"),
              menusConfig.getStringList("menus.bonus-shop.custom-pickaxe.lore"),
              true
      );
      InventoryButton customPickaxeMenuItemButton = new InventoryButton()
              .creator(player1 -> customPickaxeMenuItem)
              .consumer(event -> {
                 TheBridge.guiManager.openGUI(new PickaxeSelectMenu(), player);
              });
      this.setButton(menusConfig.getInt("menus.bonus-shop.custom-pickaxe.slot") - 1, customPickaxeMenuItemButton);

      super.decorate(player);
   }

   private ItemStack createItem(Material material, String displayName, List<String> lore, boolean enchanted) {
      ItemStack itemStack = new ItemStack(material);
      ItemMeta itemMeta = itemStack.getItemMeta();
      itemMeta.setDisplayName(Utils.colorize(displayName));
      if (lore != null) itemMeta.setLore(Utils.colorize(lore));
      itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
      itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      if (enchanted) itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
      itemStack.setItemMeta(itemMeta);
      return itemStack;
   }
}
