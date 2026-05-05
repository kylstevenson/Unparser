package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.GameType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public class GameTypeParser {

  public GameType parse(String value) {
    if (value == null) {
      return null;
    }

    for (GameType gameType : GameType.values()) {
      if (gameType.name().equalsIgnoreCase(value) || gameType.getName().equalsIgnoreCase(value)) {
        return gameType;
      }
    }

    return null;
  }

  public Map<String, GameType> readAliases(FileConfiguration config) {
    Map<String, GameType> aliases = new HashMap<>();
    if (!config.isConfigurationSection("type-detection.aliases")) {
      return aliases;
    }

    for (String key : config.getConfigurationSection("type-detection.aliases").getKeys(false)) {
      String typeName = config.getString("type-detection.aliases." + key);
      GameType gameType = parse(typeName);
      if (gameType != null) {
        aliases.put(key, gameType);
      }
    }

    return aliases;
  }
}
