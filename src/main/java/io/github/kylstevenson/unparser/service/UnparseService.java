package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.GameType;
import io.github.kylstevenson.unparser.model.ParsedMapData;
import io.github.kylstevenson.unparser.util.FileUtil;
import io.github.kylstevenson.unparser.world.VoidWorldGenerator;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

@RequiredArgsConstructor
public class UnparseService {

  private final String mapsFolder;
  private final boolean logMetrics;
  private final WorldConfigDataLoader worldConfigDataLoader = new WorldConfigDataLoader();
  private final MapMetadataWriter mapMetadataWriter = new MapMetadataWriter();
  private final ParsedWorldTransformer parsedWorldTransformer = new ParsedWorldTransformer();
  private final Set<String> lockedWorldNames = new HashSet<>();

  public UnparseResult unparseMap(File zipFile, GameType gameType, String admin, boolean dryRun) {
    Logger logger = Bukkit.getLogger();
    long pipelineStart = System.nanoTime();
    long unzipMs = 0L;
    long createWorldMs = 0L;
    long loadConfigMs = 0L;
    long writeMetadataMs = 0L;
    long transformMs = 0L;
    String worldName = zipFile.getName().replaceAll("\\.zip$", "").replaceAll(" ", "_");

    if (worldExists(worldName, gameType)) {
      return failure("&cError that world already exists", dryRun, 0, 0, 0, 0, 0, 0);
    }

    String fullWorldName = getFullWorldName(worldName, gameType);
    File worldDirectory = new File(fullWorldName);
    lockWorld(fullWorldName);

    try {
      long unzipStart = System.nanoTime();
      if (!FileUtil.unzip(zipFile.getAbsolutePath(), worldDirectory.getPath())) {
        return failure(
            "&cAn error occurred unzipping " + zipFile.getAbsolutePath(),
            dryRun,
            elapsedMs(unzipStart),
            0,
            0,
            0,
            0,
            elapsedMs(pipelineStart));
      }
      unzipMs = elapsedMs(unzipStart);

      long createWorldStart = System.nanoTime();
      World world = createWorld(fullWorldName);
      if (world == null) {
        FileUtil.deleteDirectory(worldDirectory);
        return failure(
            "&cAn error occurred creating the world",
            dryRun,
            unzipMs,
            elapsedMs(createWorldStart),
            0,
            0,
            0,
            elapsedMs(pipelineStart));
      }
      createWorldMs = elapsedMs(createWorldStart);

      long loadConfigStart = System.nanoTime();
      ParsedMapData map = worldConfigDataLoader.load(world);
      if (map == null) {
        cleanupWorld(world);
        return failure(
            "&cAn error occurred generating world data",
            dryRun,
            unzipMs,
            createWorldMs,
            elapsedMs(loadConfigStart),
            0,
            0,
            elapsedMs(pipelineStart));
      }
      loadConfigMs = elapsedMs(loadConfigStart);

      if (dryRun) {
        cleanupWorld(world);
        return success(
            "&7Dry-run validated &e" + zipFile.getName() + "&7 successfully",
            true,
            unzipMs,
            createWorldMs,
            loadConfigMs,
            0,
            0,
            elapsedMs(pipelineStart));
      }

      new File(world.getWorldFolder(), "WorldConfig.dat").delete();

      long writeMetadataStart = System.nanoTime();
      if (!mapMetadataWriter.write(map, world, gameType, admin)) {
        cleanupWorld(world);
        return failure(
            "&cAn error occurred generating map data",
            false,
            unzipMs,
            createWorldMs,
            loadConfigMs,
            elapsedMs(writeMetadataStart),
            0,
            elapsedMs(pipelineStart));
      }
      writeMetadataMs = elapsedMs(writeMetadataStart);

      long transformStart = System.nanoTime();
      if (!parsedWorldTransformer.transform(map)) {
        cleanupWorld(world);
        return failure(
            "&cAn error occurred trying to unparse the world",
            false,
            unzipMs,
            createWorldMs,
            loadConfigMs,
            writeMetadataMs,
            elapsedMs(transformStart),
            elapsedMs(pipelineStart));
      }
      transformMs = elapsedMs(transformStart);

      long totalMs = elapsedMs(pipelineStart);
      if (logMetrics) {
        logger.info(
            "[Unparser] Pipeline complete for "
                + fullWorldName
                + " in "
                + totalMs
                + "ms (unzip="
                + unzipMs
                + "ms, createWorld="
                + createWorldMs
                + "ms, loadConfig="
                + loadConfigMs
                + "ms, writeMapDat="
                + writeMetadataMs
                + "ms, transform="
                + transformMs
                + "ms)");
      }

      return success(
          "&7Successfully unparsed &e" + zipFile.getName(),
          false,
          unzipMs,
          createWorldMs,
          loadConfigMs,
          writeMetadataMs,
          transformMs,
          totalMs);
    } finally {
      unlockWorld(fullWorldName);
    }
  }

  public boolean isWorldLocked(String worldName) {
    synchronized (lockedWorldNames) {
      return lockedWorldNames.contains(worldName);
    }
  }

  public Set<String> getLockedWorldNames() {
    synchronized (lockedWorldNames) {
      return Collections.unmodifiableSet(new HashSet<>(lockedWorldNames));
    }
  }

  private void lockWorld(String worldName) {
    synchronized (lockedWorldNames) {
      lockedWorldNames.add(worldName);
    }
  }

  private void unlockWorld(String worldName) {
    synchronized (lockedWorldNames) {
      lockedWorldNames.remove(worldName);
    }
  }

  private World createWorld(String worldName) {
    WorldCreator worldCreator = new WorldCreator(worldName);
    worldCreator.generator(new VoidWorldGenerator());
    return Bukkit.createWorld(worldCreator);
  }

  private void cleanupWorld(World world) {
    File worldFolder = world.getWorldFolder();
    Bukkit.unloadWorld(world, false);
    FileUtil.deleteDirectory(worldFolder);
  }

  private boolean worldExists(String name, GameType gameType) {
    File file = new File(getFullWorldName(name, gameType));
    return file.exists() && file.isDirectory();
  }

  private String getFullWorldName(String name, GameType gameType) {
    return mapsFolder + File.separator + gameType.getName() + File.separator + name;
  }

  private long elapsedMs(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000L;
  }

  private UnparseResult success(
      String message,
      boolean dryRun,
      long unzipMs,
      long createWorldMs,
      long loadConfigMs,
      long writeMetadataMs,
      long transformMs,
      long totalMs) {
    return new UnparseResult(
        true,
        dryRun,
        message,
        unzipMs,
        createWorldMs,
        loadConfigMs,
        writeMetadataMs,
        transformMs,
        totalMs);
  }

  private UnparseResult failure(
      String message,
      boolean dryRun,
      long unzipMs,
      long createWorldMs,
      long loadConfigMs,
      long writeMetadataMs,
      long transformMs,
      long totalMs) {
    return new UnparseResult(
        false,
        dryRun,
        message,
        unzipMs,
        createWorldMs,
        loadConfigMs,
        writeMetadataMs,
        transformMs,
        totalMs);
  }
}
