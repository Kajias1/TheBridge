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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ColorBlockListMenu extends InventoryGUI
{
    private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
    private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();
    private static final List<Integer> blockedColorSlots = menusConfig.getIntegerList("menus.color-black-list.blocked-color-slots");
    private static final List<Integer> allowedColorSlots = menusConfig.getIntegerList("menus.color-black-list.allowed-color-slots");

    public ColorBlockListMenu() {
        super(false);
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menusConfig.getInt("menus.color-black-list.size"), Utils.colorize(menusConfig.getString("menus.color-black-list.title")));
    }

    @Override
    public void decorate(Player player) {
        GamePlayer playerData = PlayersData.getPlayerData(player.getUniqueId());

        for (String fillerItemCategory : menusConfig.getConfigurationSection("menus.color-black-list.filler-items").getKeys(false)) {
            ItemStack fillerItem = new ItemStack(Material.getMaterial(menusConfig.getString("menus.color-black-list.filler-items." + fillerItemCategory + ".material")), 1,
                    (short) menusConfig.getInt("menus.color-black-list.filler-items." + fillerItemCategory + ".data"));
            ItemMeta fillerMeta = fillerItem.getItemMeta();
            fillerMeta.setDisplayName(" ");
            fillerItem.setItemMeta(fillerMeta);

            for (int index : menusConfig.getIntegerList("menus.color-black-list.filler-items." + fillerItemCategory + ".slots"))
                this.getInventory().setItem(index, fillerItem);
        }

        ItemStack explainer = Utils.getCustomTextureHead(menusConfig.getString("menus.color-black-list.explainer.player-skull-id"));
        ItemMeta explainerMeta = explainer.getItemMeta();
        explainerMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.color-black-list.explainer.name")));
        explainerMeta.setLore(Utils.colorize(menusConfig.getStringList("menus.color-black-list.explainer.lore")));
        explainer.setItemMeta(explainerMeta);
        this.getInventory().setItem(menusConfig.getInt("menus.color-black-list.explainer.slot"), explainer);

        int allowedColorIndex = 0;
        for (String boughtBlockColor : playerData.getAvailableBlockColors()) {
            ItemStack allowedColorItem = new ItemStack(Material.valueOf(config.getString("game-config.main-block-material")), 1, (short) Integer.parseInt(boughtBlockColor));
            ItemMeta allowedColorMeta = allowedColorItem.getItemMeta();
            allowedColorMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.color-black-list.move-to-blocked-list-hover-message")));
            allowedColorItem.setItemMeta(allowedColorMeta);
            InventoryButton moveToBlockedListButton = new InventoryButton()
                    .creator(player1 -> allowedColorItem)
                    .consumer(event -> {
                        if (playerData.getAvailableBlockColors().size() > 1) {
                            playerData.getBlockColorBlackList().add(boughtBlockColor);
                            TheBridge.guiManager.openGUI(new ColorBlockListMenu(), player);
                        } else {
                            player.closeInventory();
                            Sounds.NOTE_BASS_GUITAR.play(player);
                            Utils.sendMessage(player, menusConfig.getString("menus.color-black-list.cant-move-to-blocked-list-all-colors"));
                        }
                    });
            this.setButton(allowedColorSlots.get(allowedColorIndex), moveToBlockedListButton);
            allowedColorIndex++;
        }

        int blockColorIndex = 0;
        for (String blockedColor : playerData.getBlockColorBlackList()) {
            ItemStack blockedColorItem = new ItemStack(Material.valueOf(config.getString("game-config.main-block-material")), 1, (short) Integer.parseInt(blockedColor));
            ItemMeta blockedColorMeta = blockedColorItem.getItemMeta();
            blockedColorMeta.setDisplayName(Utils.colorize(menusConfig.getString("menus.color-black-list.move-to-allowed-list-hover-message")));
            blockedColorItem.setItemMeta(blockedColorMeta);
            InventoryButton moveToAllowedListButton = new InventoryButton()
                    .creator(player1 -> blockedColorItem)
                    .consumer(event -> {
                        playerData.getBlockColorBlackList().remove(blockedColor);
                        TheBridge.guiManager.openGUI(new ColorBlockListMenu(), player);
                    });
            this.setButton(blockedColorSlots.get(blockColorIndex), moveToAllowedListButton);
            blockColorIndex++;
        }

        super.decorate(player);
    }
}
