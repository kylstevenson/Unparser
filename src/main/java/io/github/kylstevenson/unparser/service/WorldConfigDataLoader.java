package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.ParsedMapData;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.World;

public class WorldConfigDataLoader {

  public ParsedMapData load(World world) {
    String name = "null";
    String author = "null";

    java.util.Map<String, List<Location>> dataLocations = new HashMap<>();
    java.util.Map<String, List<Location>> teamLocations = new HashMap<>();
    java.util.Map<String, List<Location>> customLocations = new HashMap<>();

    try {
      List<String> lines =
          Files.readAllLines(
              new File(world.getWorldFolder().getPath() + File.separator + "WorldConfig.dat")
                  .toPath());

      List<Location> current = null;
      int minX = -256;
      int minY = 0;
      int minZ = -256;
      int maxX = 256;
      int maxY = 256;
      int maxZ = 256;

      for (String line : lines) {
        int separatorIndex = line.indexOf(':');
        if (separatorIndex <= 0 || separatorIndex == line.length() - 1) {
          continue;
        }

        String key = line.substring(0, separatorIndex).toUpperCase(Locale.ROOT);
        String value = line.substring(separatorIndex + 1);

        switch (key) {
          case "MAP_NAME":
            name = value;
            break;
          case "MAP_AUTHOR":
            author = value;
            break;
          case "TEAM_NAME":
            current = teamLocations.computeIfAbsent(value, ignored -> new ArrayList<>());
            break;
          case "TEAM_SPAWNS":
          case "DATA_LOCS":
          case "CUSTOM_LOCS":
            appendLocations(world, value, current);
            break;
          case "DATA_NAME":
            current = dataLocations.computeIfAbsent(value, ignored -> new ArrayList<>());
            break;
          case "CUSTOM_NAME":
            current = customLocations.computeIfAbsent(value, ignored -> new ArrayList<>());
            break;
          case "MIN_X":
            minX = Integer.parseInt(value);
            break;
          case "MAX_X":
            maxX = Integer.parseInt(value);
            break;
          case "MIN_Z":
            minZ = Integer.parseInt(value);
            break;
          case "MAX_Z":
            maxZ = Integer.parseInt(value);
            break;
          case "MIN_Y":
            minY = Integer.parseInt(value);
            break;
          case "MAX_Y":
            maxY = Integer.parseInt(value);
            break;
          default:
            break;
        }
      }

      Location[] corners = new Location[2];
      corners[0] = new Location(world, minX, minY, minZ);
      corners[1] = new Location(world, maxX, maxY, maxZ);

      return new ParsedMapData(
          name, author, corners, dataLocations, teamLocations, customLocations);
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }

  private void appendLocations(World world, String value, List<Location> target) {
    if (target == null) {
      return;
    }

    String[] locations = value.split(":");
    for (String rawLocation : locations) {
      Location location = parseLocation(world, rawLocation);
      if (location != null) {
        target.add(location);
      }
    }
  }

  private Location parseLocation(World world, String location) {
    String[] coords = location.split(",");
    if (coords.length < 3) {
      return null;
    }

    try {
      return new Location(
          world,
          Integer.parseInt(coords[0]) + 0.5,
          Integer.parseInt(coords[1]),
          Integer.parseInt(coords[2]) + 0.5);
    } catch (Exception exception) {
      exception.printStackTrace();
      return null;
    }
  }
}
