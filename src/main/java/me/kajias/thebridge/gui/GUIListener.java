package me.kajias.thebridge.gui;

import me.kajias.thebridge.TheBridge;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class GUIListener implements Listener
{
   private final GUIManager guiManager;

   public GUIListener(GUIManager guiManager, TheBridge plugin) {
      this.guiManager = guiManager;
      Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
   }

   @EventHandler
   public void onClick(InventoryClickEvent event) {
      this.guiManager.handleClick(event);
   }

   @EventHandler
   public void onOpen(InventoryOpenEvent event) {
      this.guiManager.handleOpen(event);
   }

   @EventHandler
   public void onClose(InventoryCloseEvent event) {
      this.guiManager.handleClose(event);
   }
}
