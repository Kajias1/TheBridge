package me.kajias.thebridge.objects;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class Region
{
   public static final HashMap<Player, Location> playerLocationMap1 = new HashMap<>();
   public static final HashMap<Player, Location> playerLocationMap2 = new HashMap<>();

   private final UUID uuid;
   private final Location position1;
   private final Location position2;

   public Region(Location position1, Location position2) {
      this.uuid = UUID.randomUUID();
      this.position1 = position1;
      this.position2 = position2;
   }

   public Region(UUID uuid, Location position1, Location position2) {
      this.uuid = uuid;
      this.position1 = position1;
      this.position2 = position2;
   }

   public boolean isInside(Block block) {
      Location blockLoc = block.getLocation();
      return blockLoc.getX() >= Math.min(position1.getX(), position2.getX()) &&
              blockLoc.getY() >= Math.min(position1.getY(), position2.getY()) &&
              blockLoc.getZ() >= Math.min(position1.getZ(), position2.getZ()) &&
              blockLoc.getX() <= Math.max(position1.getX(), position2.getX()) &&
              blockLoc.getY() <= Math.max(position1.getY(), position2.getY()) &&
              blockLoc.getZ() <= Math.max(position1.getZ(), position2.getZ());
   }

   public UUID getUniqueId() {
      return uuid;
   }

   public Location getPosition1() {
      return position1;
   }

   public Location getPosition2() {
      return position2;
   }
}
