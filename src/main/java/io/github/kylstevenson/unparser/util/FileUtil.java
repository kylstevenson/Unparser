package io.github.kylstevenson.unparser.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FileUtil {

  private static final int MAX_ENTRIES = 10000; // 10k entries
  private static final long MAX_TOTAL_UNCOMPRESSED_BYTES = 2L * 1024L * 1024L * 1024L; // 2GB
  private static final long MAX_SINGLE_ENTRY_BYTES = 512L * 1024L * 1024L; // 512MB

  public static void deleteDirectory(File directoryToBeDeleted) {
    File[] allContents = directoryToBeDeleted.listFiles();
    if (allContents != null) {
      for (File file : allContents) {
        deleteDirectory(file);
      }
    }
    directoryToBeDeleted.delete();
  }

  public static boolean unzip(String zipFilePath, String destDir) {
    File destination = new File(destDir);
    if (!destination.exists() && !destination.mkdirs()) {
      return false;
    }

    Path destinationPath = destination.toPath().toAbsolutePath().normalize();
    byte[] buffer = new byte[8192];

    try (FileInputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis)) {

      ZipEntry entry;
      int entryCount = 0;
      long totalExtractedBytes = 0L;
      while ((entry = zis.getNextEntry()) != null) {
        entryCount++;
        if (entryCount > MAX_ENTRIES) {
          zis.closeEntry();
          return false;
        }

        String normalizedName = entry.getName().replace('\\', '/');

        if (normalizedName.startsWith("__MACOSX/") || normalizedName.equals(".DS_Store")) {
          zis.closeEntry();
          continue;
        }

        Path targetPath = destinationPath.resolve(normalizedName).normalize();
        if (!targetPath.startsWith(destinationPath)) {
          zis.closeEntry();
          return false;
        }

        File targetFile = targetPath.toFile();
        if (entry.isDirectory() || normalizedName.endsWith("/")) {
          if (!targetFile.exists() && !targetFile.mkdirs()) {
            zis.closeEntry();
            return false;
          }
          zis.closeEntry();
          continue;
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
          zis.closeEntry();
          return false;
        }

        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
          int len;
          long entryBytes = 0L;
          while ((len = zis.read(buffer)) > 0) {
            entryBytes += len;
            totalExtractedBytes += len;
            if (entryBytes > MAX_SINGLE_ENTRY_BYTES
                || totalExtractedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
              zis.closeEntry();
              return false;
            }
            fos.write(buffer, 0, len);
          }
        }

        zis.closeEntry();
      }

      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
}
