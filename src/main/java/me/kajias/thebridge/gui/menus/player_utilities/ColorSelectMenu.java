package me.kajias.thebridge.gui.menus.player_utilities;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.configurations.MenuConfiguration;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.BlockColorType;
import me.kajias.thebridge.gui.InventoryButton;
import me.kajias.thebridge.gui.InventoryGUI;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.utils.Sounds;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorSelectMenu extends InventoryGUI
{
   private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public ColorSelectMenu() {
      super(false);
   }

   @Override
   protected Inventory createInventory() {
      return Bukkit.createInventory(null, menusConfig.getInt("menus.block-color-shop.size"), Utils.colorize(menusConfig.getString("menus.block-color-shop.title")));
   }

   @Override
   public void decorate(Player player) {
      GamePlayer playerData = PlayersData.getPlayerData(player.getUniqueId());

      for (String colorName : menusConfig.getConfigurationSection("menus.block-color-shop.colors").getKeys(false)) {
         ItemStack colorItem = new ItemStack(Material.getMaterial(config.getString("game-config.main-block-material")), 1,
                 (short) menusConfig.getInt("menus.block-color-shop.colors." + colorName + ".data"));
         ItemMeta colorItemMeta = colorItem.getItemMeta();
         colorItemMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.block-color-shop.hover-text")));
         colorItem.setItemMeta(colorItemMeta);
         InventoryButton colorSelectButton = new InventoryButton()
                 .creator(player1 -> colorItem)
                 .consumer(event -> {
                    String color = String.valueOf(colorItem.getDurability());
                    if (playerData.getBoughtBlockColors().containsKey(color)) {
                       playerData.setSelectedBlockColor(color);
                       playerData.setBlockColorType(BlockColorType.SINGLE);
                       Utils.sendMessage(player, config.getString("messages.color-selected"));
                       Sounds.ORB_PICKUP.play(player);
                       player.closeInventory();
                    } else {
                       TheBridge.guiManager.openGUI(new ColorPurchaseMenu(colorItem,
                               menusConfig.getIntegerList("menus.block-color-shop.colors." + colorName + ".prices.anix"),
                               menusConfig.getIntegerList("menus.block-color-shop.colors." + colorName + ".prices.bonus"),
                               menusConfig.getStringList("menus.block-color-shop.colors." + colorName + ".prices.benefit")
                       ), player);
                    }
                 });
         this.setButton(menusConfig.getInt("menus.block-color-shop.colors." + colorName + ".slot"), colorSelectButton);
      }

      ItemStack randomColorTypeItem = Utils.getCustomTextureHead(menusConfig.getString("menus.block-color-shop.random-color-toggle.player-skull-id"));
      ItemMeta randomColorTypeItemMeta = randomColorTypeItem.getItemMeta();
      randomColorTypeItemMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.block-color-shop.random-color-toggle.name")));
      randomColorTypeItemMeta.setLore(Utils.colorize(menusConfig.getStringList("menus.block-color-shop.random-color-toggle.lore")));
      randomColorTypeItem.setItemMeta(randomColorTypeItemMeta);
      InventoryButton randomColorTypeSelectButton = new InventoryButton()
              .creator(player1 -> randomColorTypeItem)
              .consumer(event -> {
                 if (playerData.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.block-color-shop.random-color-toggle.name")))) {
                    playerData.setBlockColorType(BlockColorType.RANDOM);
                    Utils.sendMessage(player, config.getString("messages.block-color-type.random-block-color-type-selected"));
                    Sounds.ORB_PICKUP.play(player);
                    player.closeInventory();
                 } else {
                    TheBridge.guiManager.openGUI(new BlockColorTypePurchaseMenu(randomColorTypeItem, BlockColorType.RANDOM,
                            menusConfig.getIntegerList("menus.block-color-shop.random-color-toggle.prices.anix"),
                            menusConfig.getIntegerList("menus.block-color-shop.random-color-toggle.prices.bonus"),
                            menusConfig.getStringList("menus.block-color-shop.random-color-toggle.prices.benefit")
                    ), player);
                 }
              });
      this.setButton(menusConfig.getInt("menus.block-color-shop.random-color-toggle.slot"), randomColorTypeSelectButton);

      ItemStack rainbowColorTypeItem = Utils.getCustomTextureHead(menusConfig.getString("menus.block-color-shop.rainbow-color-toggle.player-skull-id"));
      ItemMeta rainbowColorTypeItemMeta = rainbowColorTypeItem.getItemMeta();
      rainbowColorTypeItemMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.block-color-shop.rainbow-color-toggle.name")));
      rainbowColorTypeItemMeta.setLore(Utils.colorize(menusConfig.getStringList("menus.block-color-shop.rainbow-color-toggle.lore")));
      rainbowColorTypeItem.setItemMeta(rainbowColorTypeItemMeta);
      InventoryButton rainbowColorTypeSelectButton = new InventoryButton()
              .creator(player1 -> rainbowColorTypeItem)
              .consumer(event -> {
                 if (playerData.getBoughtBonuses().containsKey(Utils.stripColor(menusConfig.getString("menus.block-color-shop.rainbow-color-toggle.name")))) {
                    playerData.setBlockColorType(BlockColorType.RAINBOW);
                    Utils.sendMessage(player, config.getString("messages.block-color-type.rainbow-block-color-type-selected"));
                    Sounds.ORB_PICKUP.play(player);
                    player.closeInventory();
                 } else {
                    TheBridge.guiManager.openGUI(new BlockColorTypePurchaseMenu(rainbowColorTypeItem, BlockColorType.RAINBOW,
                            menusConfig.getIntegerList("menus.block-color-shop.rainbow-color-toggle.prices.anix"),
                            menusConfig.getIntegerList("menus.block-color-shop.rainbow-color-toggle.prices.bonus"),
                            menusConfig.getStringList("menus.block-color-shop.rainbow-color-toggle.prices.benefit")
                    ), player);
                 }
              });
      this.setButton(menusConfig.getInt("menus.block-color-shop.rainbow-color-toggle.slot"), rainbowColorTypeSelectButton);

      ItemStack colorBlockListMenuItem = new ItemStack(Material.getMaterial(config.getString("game-config.main-block-material")));
      ItemMeta colorBlockListMenuMeta = colorBlockListMenuItem.getItemMeta();
      colorBlockListMenuMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.block-color-shop.color-block-list-menu-button.name")));
      colorBlockListMenuMeta.setLore(Utils.colorize(menusConfig.getStringList("menus.block-color-shop.color-block-list-menu-button.lore")));
      colorBlockListMenuItem.setItemMeta(colorBlockListMenuMeta);
      InventoryButton colorBlockListMenuButton = new InventoryButton().creator(player1 -> colorBlockListMenuItem)
              .consumer(event -> {
                 if (!playerData.getBoughtBlockColors().isEmpty()) {
                    TheBridge.guiManager.openGUI(new ColorBlockListMenu(), player);
                 } else {
                    player.closeInventory();
                    Sounds.NOTE_BASS_GUITAR.play(player);
                    Utils.sendMessage(player, config.getString("messages.no-colors-bought-yet"));
                 }
              });
      this.setButton(menusConfig.getInt("menus.block-color-shop.color-block-list-menu-button.slot"), colorBlockListMenuButton);

      super.decorate(player);
   }
}
