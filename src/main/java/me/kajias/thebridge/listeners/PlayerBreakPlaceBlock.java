package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import me.kajias.thebridge.data.PlayersData;
import me.kajias.thebridge.enums.ArenaState;
import me.kajias.thebridge.enums.BlockColorType;
import me.kajias.thebridge.objects.Arena;
import me.kajias.thebridge.objects.GamePlayer;
import me.kajias.thebridge.objects.Region;
import me.kajias.thebridge.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Random;

public class PlayerBreakPlaceBlock implements Listener
{
   private static final FileConfiguration config = TheBridge.INSTANCE.getConfig();

   public PlayerBreakPlaceBlock(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onPlayerPlaceBlock(BlockPlaceEvent e) {
      Player player = e.getPlayer();
      Arena arena = Arena.getPlayerArenaMap().get(player);
      if (arena != null && arena.getState() == ArenaState.STARTED) {
         for (Block nearbyBlock : Utils.sphereAround(e.getBlock().getLocation(), 6))
            if (nearbyBlock.getType() == Material.ENDER_PORTAL) {
               e.setCancelled(true);
               Utils.sendMessage(player, config.getString("messages.cant-place-near-portals"));
               return;
            }
         boolean isInAllowedArea = false;
         for (Region region : arena.getRegions()) {
            if (region.isInside(e.getBlock())) {
               isInAllowedArea = true;
               break;
            }
         }
         if (isInAllowedArea) {
            GamePlayer gamePlayer = PlayersData.getPlayerData(player.getUniqueId());
            if (!gamePlayer.getAvailableBlockColors().isEmpty() && gamePlayer.getBlockColorType() == BlockColorType.RAINBOW) {
               e.getItemInHand().setDurability(Short.parseShort(gamePlayer.getAvailableBlockColors().get(new Random().nextInt(gamePlayer.getAvailableBlockColors().size()))));
            }
            arena.getPlacedBlocks().add(e.getBlock());
         } else {
            e.setCancelled(true);
            Utils.sendMessage(player, config.getString("messages.cant-place-block-here"));
         }
      }
   }

   @EventHandler
   public void onPlayerBreakBlock(BlockBreakEvent e) {
      Player player = e.getPlayer();
      Arena arena = Arena.getPlayerArenaMap().get(player);
      if (arena != null && arena.getState() == ArenaState.STARTED) {
         if (!arena.getPlacedBlocks().contains(e.getBlock())) {
            e.setCancelled(true);
         }
      }
   }
}
