package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.ParsedMapData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

public class ParsedWorldTransformer {

  private final HashMap<String, Integer> colorMap = new HashMap<>();

  public ParsedWorldTransformer() {
    colorMap.put("white", 0);
    colorMap.put("orange", 1);
    colorMap.put("magenta", 2);
    colorMap.put("sky", 3);
    colorMap.put("yellow", 4);
    colorMap.put("lime", 5);
    colorMap.put("pink", 6);
    colorMap.put("gray", 7);
    colorMap.put("lgray", 8);
    colorMap.put("cyan", 9);
    colorMap.put("purple", 10);
    colorMap.put("blue", 11);
    colorMap.put("brown", 12);
    colorMap.put("green", 13);
    colorMap.put("red", 14);
    colorMap.put("black", 15);
  }

  public boolean transform(ParsedMapData map) {
    Set<String> transientLoadedChunks = new HashSet<>();
    long startedAt = System.nanoTime();
    try {
      java.util.Map<String, List<BlockWriteOperation>> operationsByChunk =
          collectOperationsByChunk(map);
      Set<String> requiredChunks = new HashSet<>(operationsByChunk.keySet());
      loadRequiredChunks(map, requiredChunks, transientLoadedChunks);
      applyOperationsByChunk(operationsByChunk);

      long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
      World world =
          map.getCorners() != null && map.getCorners().length > 0 && map.getCorners()[0] != null
              ? map.getCorners()[0].getWorld()
              : null;
      if (world != null) {
        Bukkit.getLogger()
            .info(
                "[Unparser] Transform complete for world "
                    + world.getName()
                    + " in "
                    + durationMs
                    + "ms ("
                    + operationsByChunk.size()
                    + " chunks, "
                    + countOperations(operationsByChunk)
                    + " operations)");
      }
      return true;
    } catch (Exception exception) {
      exception.printStackTrace();
      return false;
    } finally {
      unloadTransientChunks(map, transientLoadedChunks);
    }
  }

  private java.util.Map<String, List<BlockWriteOperation>> collectOperationsByChunk(
      ParsedMapData map) {
    java.util.Map<String, List<BlockWriteOperation>> operationsByChunk = new LinkedHashMap<>();

    collectBoundOperations(map, operationsByChunk);
    collectIronOperations(map, operationsByChunk);
    collectGoldOperations(map, operationsByChunk);
    collectCustomOperations(map, operationsByChunk);

    return operationsByChunk;
  }

  private void collectBoundOperations(
      ParsedMapData map, java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    if (map.getCorners()[0].getBlock().getX() != -256
        && map.getCorners()[0].getBlock().getZ() != -256
        && map.getCorners()[1].getBlock().getX() != 256
        && map.getCorners()[1].getBlock().getZ() != 256) {

      addOperation(
          operationsByChunk,
          map.getCorners()[0],
          new BlockWriteOperation(map.getCorners()[0].clone(), Material.WOOL, (byte) 0, null));
      addOperation(
          operationsByChunk,
          map.getCorners()[0],
          new BlockWriteOperation(
              map.getCorners()[0].clone().add(0, 1, 0), Material.GOLD_PLATE, null, null));

      if (map.getCorners()[1].getBlock().getY() == 256) {
        map.getCorners()[1].setY(map.getCorners()[0].getBlock().getY());
      }

      addOperation(
          operationsByChunk,
          map.getCorners()[1],
          new BlockWriteOperation(map.getCorners()[1].clone(), Material.WOOL, (byte) 0, null));
      addOperation(
          operationsByChunk,
          map.getCorners()[1],
          new BlockWriteOperation(
              map.getCorners()[1].clone().add(0, 1, 0), Material.GOLD_PLATE, null, null));
    }
  }

  private void collectIronOperations(
      ParsedMapData map, java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    for (String woolColor : map.getDataLocations().keySet()) {
      byte woolData = DyeColor.valueOf(woolColor.toUpperCase()).getWoolData();
      for (Location location : map.getIronLocations(woolColor)) {
        addOperation(
            operationsByChunk,
            location,
            new BlockWriteOperation(location.clone(), Material.WOOL, woolData, null));
        addOperation(
            operationsByChunk,
            location,
            new BlockWriteOperation(
                location.clone().add(0, 1, 0), Material.IRON_PLATE, null, null));
      }
    }
  }

  private void collectGoldOperations(
      ParsedMapData map, java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    for (String woolColor : map.getTeamLocsLocations().keySet()) {
      Integer color = colorMap.get(woolColor.toLowerCase(Locale.ROOT));
      byte woolData =
          color != null
              ? color.byteValue()
              : DyeColor.valueOf(woolColor.toUpperCase()).getWoolData();

      for (Location location : map.getGoldLocations(woolColor)) {
        addOperation(
            operationsByChunk,
            location,
            new BlockWriteOperation(location.clone(), Material.WOOL, woolData, null));
        addOperation(
            operationsByChunk,
            location,
            new BlockWriteOperation(
                location.clone().add(0, 1, 0), Material.GOLD_PLATE, null, null));
      }
    }
  }

  private void collectCustomOperations(
      ParsedMapData map, java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    for (String name : map.getCustomLocsLocations().keySet()) {
      for (Location location : map.getSpongeLocations(name)) {
        try {
          int id = Integer.parseInt(name);
          Material material = Material.getMaterial(id);
          if (material == null) {
            throw new IllegalArgumentException("Unknown material id");
          }
          addOperation(
              operationsByChunk,
              location,
              new BlockWriteOperation(location.clone(), material, null, null));
        } catch (Exception exception) {
          addOperation(
              operationsByChunk,
              location,
              new BlockWriteOperation(location.clone(), Material.SPONGE, null, null));
          addOperation(
              operationsByChunk,
              location,
              new BlockWriteOperation(
                  location.clone().add(0, 1, 0), Material.SIGN_POST, null, toSignLines(name)));
        }
      }
    }
  }

  private void applyOperationsByChunk(
      java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    for (List<BlockWriteOperation> operations : operationsByChunk.values()) {
      for (BlockWriteOperation operation : operations) {
        operation.apply();
      }
    }
  }

  private int countOperations(java.util.Map<String, List<BlockWriteOperation>> operationsByChunk) {
    int count = 0;
    for (List<BlockWriteOperation> operations : operationsByChunk.values()) {
      count += operations.size();
    }
    return count;
  }

  private void addOperation(
      java.util.Map<String, List<BlockWriteOperation>> operationsByChunk,
      Location location,
      BlockWriteOperation operation) {
    if (location == null || location.getWorld() == null) {
      return;
    }
    String key = chunkKey(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    operationsByChunk
        .computeIfAbsent(key, ignored -> new ArrayList<BlockWriteOperation>())
        .add(operation);
  }

  private String[] toSignLines(String value) {
    String[] lines = new String[] {"", "", "", ""};
    for (String part : value.split(" ")) {
      int size = part.length();
      for (int i = 0; i < 4; i++) {
        int extra = lines[i].isEmpty() ? 0 : 1;
        if (lines[i].length() + size + extra <= 15) {
          if (!lines[i].isEmpty()) {
            lines[i] += " ";
          }
          lines[i] += part;
          break;
        }
      }
    }
    return lines;
  }

  private void loadRequiredChunks(
      ParsedMapData map, Set<String> requiredChunks, Set<String> transientLoadedChunks) {
    if (map.getCorners() == null || map.getCorners().length == 0 || map.getCorners()[0] == null) {
      return;
    }

    World world = map.getCorners()[0].getWorld();
    if (world == null) {
      return;
    }

    for (String key : requiredChunks) {
      String[] parts = key.split(":");
      if (parts.length != 2) {
        continue;
      }

      int chunkX = Integer.parseInt(parts[0]);
      int chunkZ = Integer.parseInt(parts[1]);

      if (!world.isChunkLoaded(chunkX, chunkZ)) {
        world.loadChunk(chunkX, chunkZ);
        transientLoadedChunks.add(key);
      }
    }
  }

  private void unloadTransientChunks(ParsedMapData map, Set<String> transientLoadedChunks) {
    if (map.getCorners() == null || map.getCorners().length == 0 || map.getCorners()[0] == null) {
      return;
    }

    World world = map.getCorners()[0].getWorld();
    if (world == null) {
      return;
    }

    for (String key : transientLoadedChunks) {
      String[] parts = key.split(":");
      if (parts.length != 2) {
        continue;
      }

      int chunkX = Integer.parseInt(parts[0]);
      int chunkZ = Integer.parseInt(parts[1]);
      world.unloadChunkRequest(chunkX, chunkZ);
    }
  }

  private String chunkKey(int chunkX, int chunkZ) {
    return chunkX + ":" + chunkZ;
  }

  private static class BlockWriteOperation {
    private final Location location;
    private final Material material;
    private final Byte data;
    private final String[] signLines;

    private BlockWriteOperation(
        Location location, Material material, Byte data, String[] signLines) {
      this.location = location;
      this.material = material;
      this.data = data;
      this.signLines = signLines;
    }

    private void apply() {
      Block block = location.getBlock();
      block.setType(material);
      if (data != null) {
        block.setData(data);
      }

      if (signLines != null) {
        Sign signState = (Sign) block.getState();
        for (int i = 0; i < 4; i++) {
          signState.setLine(i, signLines[i]);
        }
        signState.update();
      }
    }
  }
}
