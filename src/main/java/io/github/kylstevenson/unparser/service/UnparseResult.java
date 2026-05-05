package io.github.kylstevenson.unparser.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UnparseResult {

  private final boolean success;
  private final boolean dryRun;
  private final String message;
  private final long unzipMs;
  private final long createWorldMs;
  private final long loadConfigMs;
  private final long writeMetadataMs;
  private final long transformMs;
  private final long totalMs;

  public String timingsLine() {
    return "&7Timings: total=&e"
        + totalMs
        + "ms &7(unzip=&e"
        + unzipMs
        + "ms&7, create=&e"
        + createWorldMs
        + "ms&7, config=&e"
        + loadConfigMs
        + "ms&7, mapdat=&e"
        + writeMetadataMs
        + "ms&7, transform=&e"
        + transformMs
        + "ms&7)";
  }
}
