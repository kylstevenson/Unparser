package io.github.kylstevenson.unparser.listener;

import io.github.kylstevenson.unparser.service.UnparseService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@RequiredArgsConstructor
public class UnparseWorldGuardListener implements Listener {

  private final UnparseService unparseService;
  private final String lockedWorldMessage;

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    Location destination = event.getTo();
    if (destination == null || destination.getWorld() == null) {
      return;
    }

    if (unparseService.isWorldLocked(destination.getWorld().getName())) {
      event.setCancelled(true);
      event.getPlayer().sendMessage(color(lockedWorldMessage));
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onJoin(PlayerJoinEvent event) {
    relocateIfLockedWorld(event.getPlayer());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRespawn(PlayerRespawnEvent event) {
    Location respawn = event.getRespawnLocation();
    if (respawn == null || respawn.getWorld() == null) {
      return;
    }

    if (unparseService.isWorldLocked(respawn.getWorld().getName())) {
      event.setRespawnLocation(getFallbackLocation());
      event.getPlayer().sendMessage(color(lockedWorldMessage));
    }
  }

  private void relocateIfLockedWorld(Player player) {
    World world = player.getWorld();
    if (world == null) {
      return;
    }

    if (unparseService.isWorldLocked(world.getName())) {
      player.teleport(getFallbackLocation());
      player.sendMessage(color(lockedWorldMessage));
    }
  }

  private Location getFallbackLocation() {
    World fallback = Bukkit.getWorlds().get(0);
    return fallback.getSpawnLocation();
  }

  private String color(String value) {
    return ChatColor.translateAlternateColorCodes('&', value);
  }
}
