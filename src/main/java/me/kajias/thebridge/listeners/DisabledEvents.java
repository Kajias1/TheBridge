package me.kajias.thebridge.listeners;

import me.kajias.thebridge.TheBridge;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class DisabledEvents implements Listener
{
   public DisabledEvents(TheBridge plugin) {
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onDrop(PlayerDropItemEvent e) {
      e.setCancelled(true);
   }

   @EventHandler
   public void onHunger(FoodLevelChangeEvent e) {
      e.setCancelled(true);
   }
}
