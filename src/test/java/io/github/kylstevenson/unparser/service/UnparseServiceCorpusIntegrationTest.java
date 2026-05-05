package io.github.kylstevenson.unparser.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.kylstevenson.unparser.model.ParsedMapData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnparseServiceCorpusIntegrationTest {

  @TempDir Path tempDir;

  @Test
  void worldConfigFixturesParseIntoStructuredMapData() throws IOException {
    Path corpusRoot = Paths.get("src", "test", "resources", "world-configs");
    assertTrue(Files.exists(corpusRoot), "world-configs corpus directory is missing");

    List<Path> configFiles;
    try (Stream<Path> stream = Files.walk(corpusRoot)) {
      configFiles =
          stream
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().endsWith(".WorldConfig.dat"))
              .collect(Collectors.toList());
    }

    assertFalse(configFiles.isEmpty(), "No WorldConfig fixture files found");

    WorldConfigDataLoader loader = new WorldConfigDataLoader();
    int validated = 0;

    for (Path configFile : configFiles) {
      String worldName = configFile.getFileName().toString().replace(".WorldConfig.dat", "");
      Path worldFolder = tempDir.resolve(worldName);
      Files.createDirectories(worldFolder);
      Files.copy(configFile, worldFolder.resolve("WorldConfig.dat"));

      World world = mock(World.class);
      when(world.getWorldFolder()).thenReturn(worldFolder.toFile());

      ParsedMapData mapData = loader.load(world);
      assertNotNull(mapData, "Expected parsed data for fixture: " + configFile);
      assertNotNull(mapData.getCorners(), "Expected corners for fixture: " + configFile);
      assertTrue(
          mapData.getCorners().length == 2, "Expected two corners for fixture: " + configFile);
      assertNotNull(mapData.getDataLocations(), "Expected data locations map: " + configFile);
      assertNotNull(mapData.getTeamLocsLocations(), "Expected team locations map: " + configFile);
      assertNotNull(
          mapData.getCustomLocsLocations(), "Expected custom locations map: " + configFile);

      validated++;
    }

    assertTrue(validated > 0, "No fixtures were validated");
  }
}
