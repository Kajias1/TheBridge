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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PickaxePurchaseMenu extends InventoryGUI
{
    private static final FileConfiguration menusConfig = MenuConfiguration.baseConfig.getConfig();
    private final static FileConfiguration config = TheBridge.INSTANCE.getConfig();

    private final static List<Integer> yellowFillerSlots = Arrays.asList(0, 1, 2, 3, 4, 5, 9, 18, 19, 20, 21, 22, 23);
    private final static List<Integer> purpleFillerSlots = Arrays.asList(6, 7, 8, 15, 17, 24, 25, 26);
    private final static List<Integer> sellingItemSlots = Arrays.asList(10, 11, 12, 13, 14, 16);
    private final static List<Integer> sellingItemPeriods = Arrays.asList(15, 31, 60, 90, 170, 365);

    private final ItemStack sellingPickaxeItem;
    private final List<Integer> pricesAnix;
    private final List<Integer> pricesBonus;
    private final List<String> benefit;

    public PickaxePurchaseMenu(ItemStack sellingPickaxeItem, List<Integer> pricesAnix, List<Integer> pricesBonus, List<String> benefit) {
        super(false);
        this.sellingPickaxeItem = sellingPickaxeItem;
        this.pricesAnix = pricesAnix;
        this.pricesBonus = pricesBonus;
        this.benefit = benefit;
    }

    @Override
    protected Inventory createInventory() {
        return Bukkit.createInventory(null, menusConfig.getInt("menus.item-purchase.size"), Utils.colorize(menusConfig.getString("menus.item-purchase.title")));
    }

    @Override
    public void decorate(Player player) {
        // This was hardcoded deliberately, do not change
        ItemStack fillerYellow = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
        ItemMeta fillerYellowMeta = fillerYellow.getItemMeta();
        fillerYellowMeta.setDisplayName(" ");
        fillerYellow.setItemMeta(fillerYellowMeta);
        ItemStack fillerPurple = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 2);
        ItemMeta fillerPurpleMeta = fillerPurple.getItemMeta();
        fillerPurpleMeta.setDisplayName(" ");
        fillerPurple.setItemMeta(fillerPurpleMeta);

        for (int i : yellowFillerSlots) {
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> fillerYellow)
                    .consumer(event -> {}));
        }
        for (int i : purpleFillerSlots) {
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> fillerPurple)
                    .consumer(event -> {}));
        }

        int index = 0;
        for (int i : sellingItemSlots) {
            ArrayList<String> sellingPickaxeItemDescription = new ArrayList<>();
            ItemStack sellingPickaxeItem = this.sellingPickaxeItem.clone();
            ItemMeta sellingPickaxeItemMeta = sellingPickaxeItem.getItemMeta();

            List<String> descriptionFormat = menusConfig.getStringList("menus.item-purchase.item-description-format.both-prices");
            int period = sellingItemPeriods.get(index);
            if (period == sellingItemPeriods.get(sellingItemPeriods.size() - 1)) descriptionFormat = menusConfig.getStringList("menus.item-purchase.item-description-format.anix-prices");
            int priceAnix = pricesAnix.get(index);
            int priceBonus;
            try {
                priceBonus = pricesBonus.get(index);
            } catch (IndexOutOfBoundsException e) {
                priceBonus = 0;
            }
            for (String s : descriptionFormat) {
                s = s.replace("%price_anix%", String.valueOf(priceAnix));
                if (priceBonus >= 0)
                    s = s.replace("%price_bonus%", String.valueOf(priceBonus));
                try {
                    s = s.replace("%color%", menusConfig.getStringList("menus.item-purchase.item-description-format.colors").get(index));
                } catch (IndexOutOfBoundsException e) {
                    s = s.replace("%color%", "");
                };
                s = s.replace("%benefit%", benefit.get(index));
                s = s.replace("%period%", String.valueOf(period));
                sellingPickaxeItemDescription.add(Utils.colorize( s));
            }

            sellingPickaxeItemMeta.setLore(sellingPickaxeItemDescription);
            sellingPickaxeItem.setItemMeta(sellingPickaxeItemMeta);

            GamePlayer playerData = PlayersData.getPlayerData(player.getUniqueId());

            int finalPriceBonus = priceBonus;
            this.setButton(i, new InventoryButton()
                    .creator(player1 -> sellingPickaxeItem)
                    .consumer(event -> {
                        if (event.getClick().isRightClick() && event.getSlot() == sellingItemSlots.get(sellingItemSlots.size() - 1)) {
                            Utils.sendMessage(player, config.getString("messages.cant-buy-for-bonuses"));
                        } else {
                            boolean sufficientMoney = false;
                            if (event.getClick().isLeftClick()) {
                                if (TheBridge.getPlayerPoints().getAPI().look(player.getUniqueId()) >= priceAnix) {
                                    sufficientMoney = true;
                                    TheBridge.getPlayerPoints().getAPI().take(player.getUniqueId(), priceAnix);
                                }
                            }
                            if (event.getClick().isRightClick() && event.getSlot() != sellingItemSlots.get(sellingItemSlots.size() - 1)) {
                                if (TheBridge.getEconomy().getBalance(Bukkit.getOfflinePlayer(player.getUniqueId())) >= finalPriceBonus) {
                                    sufficientMoney = true;
                                    TheBridge.getEconomy().withdrawPlayer(Bukkit.getOfflinePlayer(player.getUniqueId()), priceAnix);
                                }
                            }

                            if (!sufficientMoney) {
                                Utils.sendMessage(player, config.getString("messages.insufficient-money"));
                            } else {
                                playerData.getBoughtBonuses().put(sellingPickaxeItem.getType().toString(), period);
                                playerData.setPickaxeType(sellingPickaxeItem.getType());
                                Utils.sendMessage(player, config.getString("messages.bonus-purchase-successful"));
                                PlayersData.savePlayerData(playerData);
                                Sounds.ORB_PICKUP.play(player);
                            }
                        }
                        player.closeInventory();
                    }));
            index++;
        }

        super.decorate(player);
    }
}
