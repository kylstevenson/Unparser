package io.github.kylstevenson.unparser.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParsedWorldArchiveCorpusTest {

  @TempDir Path tempDir;

  @Test
  void everyWorldConfigFixtureCanBeZippedAndUnzipped() throws IOException {
    Path corpusRoot = Paths.get("src", "test", "resources", "world-configs");
    assertTrue(Files.exists(corpusRoot), "world-configs corpus directory is missing");

    List<Path> fixtureFiles;
    try (Stream<Path> stream = Files.walk(corpusRoot)) {
      fixtureFiles =
          stream
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().endsWith(".WorldConfig.dat"))
              .collect(Collectors.toList());
    }

    assertFalse(fixtureFiles.isEmpty(), "No WorldConfig fixtures were found");

    for (Path fixtureFile : fixtureFiles) {
      String relative = corpusRoot.relativize(fixtureFile).toString().replace('\\', '/');
      String worldName =
          fixtureFile.getFileName().toString().replace(".WorldConfig.dat", "").replace(' ', '_');
      Path archive = tempDir.resolve(worldName + ".zip");
      Path output = tempDir.resolve(worldName + "_out");

      try (ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(archive))) {
        outputStream.putNextEntry(new ZipEntry("WorldConfig.dat"));
        outputStream.write(Files.readAllBytes(fixtureFile));
        outputStream.closeEntry();
      }

      boolean unzipSuccess = FileUtil.unzip(archive.toString(), output.toString());
      assertTrue(unzipSuccess, "Failed to unzip generated archive: " + relative);

      Path extractedConfig = output.resolve("WorldConfig.dat");
      assertTrue(Files.exists(extractedConfig), "WorldConfig.dat missing after unzip: " + relative);
    }
  }
}
