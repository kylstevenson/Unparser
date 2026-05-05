package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.GameType;
import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class GameTypeResolver {

  private final Map<String, GameType> aliases;

  public GameTypeResolver(Map<String, GameType> aliases) {
    this.aliases = aliases == null ? Collections.emptyMap() : aliases;
  }

  public AutoTypeResolution resolve(File zipFile, boolean preferFolderFirst) {
    if (preferFolderFirst) {
      AutoTypeResolution fromFolder = resolveFromFolder(zipFile);
      if (fromFolder != null) {
        return fromFolder;
      }
    }

    AutoTypeResolution fromEnumNamePrefix = resolveFromEnumNamePrefix(zipFile);
    if (fromEnumNamePrefix != null) {
      return fromEnumNamePrefix;
    }

    AutoTypeResolution fromAdditionalPrefixes = resolveFromAdditionalPrefixes(zipFile);
    if (fromAdditionalPrefixes != null) {
      return fromAdditionalPrefixes;
    }

    if (!preferFolderFirst) {
      AutoTypeResolution fromFolder = resolveFromFolder(zipFile);
      if (fromFolder != null) {
        return fromFolder;
      }
    }

    String normalized = normalize(zipFile.getName().replaceAll("(?i)\\.zip$", ""));

    for (Map.Entry<String, GameType> entry : aliases.entrySet()) {
      if (normalized.contains(normalize(entry.getKey()))) {
        return new AutoTypeResolution(entry.getValue(), entry.getKey());
      }
    }

    return new AutoTypeResolution(null, "No reliable type match");
  }

  private AutoTypeResolution resolveFromFolder(File zipFile) {
    File parent = zipFile.getParentFile();
    if (parent == null) {
      return null;
    }

    GameType fromFolder = matchGameType(parent.getName());
    if (fromFolder == null) {
      return null;
    }

    return new AutoTypeResolution(fromFolder, parent.getName());
  }

  private AutoTypeResolution resolveFromEnumNamePrefix(File zipFile) {
    String fileName = zipFile.getName();
    for (GameType gameType : GameType.values()) {
      String expectedPrefix = gameType.name() + "_";
      if (startsWithIgnoreCase(fileName, expectedPrefix)) {
        return new AutoTypeResolution(gameType, expectedPrefix);
      }
    }
    return null;
  }

  private AutoTypeResolution resolveFromAdditionalPrefixes(File zipFile) {
    String fileName = zipFile.getName();
    for (GameType gameType : GameType.values()) {
      for (String prefix : gameType.getAutoPrefixes()) {
        if (startsWithIgnoreCase(fileName, prefix)) {
          return new AutoTypeResolution(gameType, prefix);
        }
      }
    }
    return null;
  }

  private GameType matchGameType(String value) {
    String normalized = normalize(value);
    for (GameType gameType : GameType.values()) {
      if (normalize(gameType.getName()).equals(normalized)
          || normalize(gameType.name()).equals(normalized)) {
        return gameType;
      }
    }
    return null;
  }

  private String normalize(String value) {
    return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }

  private boolean startsWithIgnoreCase(String value, String prefix) {
    return value.regionMatches(true, 0, prefix, 0, prefix.length());
  }
}
