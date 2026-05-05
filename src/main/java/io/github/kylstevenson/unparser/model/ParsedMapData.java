package io.github.kylstevenson.unparser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

@Getter
@RequiredArgsConstructor
public class ParsedMapData {

  private final String name;
  private final String author;

  private final Location[] corners;

  private final Map<String, List<Location>> dataLocations;
  private final Map<String, List<Location>> teamLocsLocations;
  private final Map<String, List<Location>> customLocsLocations;

  public List<Location> getIronLocations(String key) {
    return dataLocations.computeIfAbsent(key, k -> new ArrayList<>());
  }

  public List<Location> getGoldLocations(String key) {
    return teamLocsLocations.computeIfAbsent(key, k -> new ArrayList<>());
  }

  public List<Location> getSpongeLocations(String key) {
    return customLocsLocations.computeIfAbsent(key, k -> new ArrayList<>());
  }
}
