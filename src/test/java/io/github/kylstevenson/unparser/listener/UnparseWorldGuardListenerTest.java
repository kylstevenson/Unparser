package io.github.kylstevenson.unparser.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.kylstevenson.unparser.service.UnparseService;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.junit.jupiter.api.Test;

class UnparseWorldGuardListenerTest {

  @Test
  void teleportToLockedWorldIsCancelledAndNotifiesPlayer() {
    UnparseService unparseService = mock(UnparseService.class);
    when(unparseService.isWorldLocked("locked_world")).thenReturn(true);

    UnparseWorldGuardListener listener =
        new UnparseWorldGuardListener(unparseService, "&cThis world is locked.");

    Player player = mock(Player.class);
    World world = mock(World.class);
    when(world.getName()).thenReturn("locked_world");
    Location from = new Location(world, 0, 64, 0);
    Location to = new Location(world, 10, 64, 10);
    PlayerTeleportEvent event =
        new PlayerTeleportEvent(player, from, to, PlayerTeleportEvent.TeleportCause.COMMAND);

    listener.onTeleport(event);

    assertTrue(event.isCancelled());
    verify(player).sendMessage(org.mockito.ArgumentMatchers.contains("This world is locked."));
  }

  @Test
  void respawnOutsideLockedWorldIsLeftUnchanged() {
    UnparseService unparseService = mock(UnparseService.class);
    when(unparseService.isWorldLocked("open_world")).thenReturn(false);

    UnparseWorldGuardListener listener =
        new UnparseWorldGuardListener(unparseService, "&cThis world is locked.");

    Player player = mock(Player.class);
    World world = mock(World.class);
    when(world.getName()).thenReturn("open_world");
    Location respawn = new Location(world, 5, 70, 5);
    PlayerRespawnEvent event = new PlayerRespawnEvent(player, respawn, false);

    listener.onRespawn(event);

    assertEquals(respawn, event.getRespawnLocation());
  }
}
