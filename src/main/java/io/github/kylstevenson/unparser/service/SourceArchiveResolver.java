package io.github.kylstevenson.unparser.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SourceArchiveResolver {

  private final File sourceSafeRoot;

  public SourceArchiveResolver(String sourceSafeRoot) {
    String root = sourceSafeRoot == null ? "" : sourceSafeRoot.trim();
    this.sourceSafeRoot = root.isEmpty() ? null : new File(root);
  }

  public List<File> resolveZipFiles(List<String> sources) {
    Set<String> unique = new LinkedHashSet<>();
    for (String raw : sources) {
      for (String part : raw.split(";")) {
        String source = normalizeSource(part);
        if (source.isEmpty()) {
          continue;
        }
        collectZipFiles(new File(source), unique);
      }
    }

    List<File> files = new ArrayList<>();
    for (String path : unique) {
      files.add(new File(path));
    }
    return files;
  }

  private void collectZipFiles(File file, Set<String> out) {
    File canonicalFile = canonical(file);
    if (canonicalFile == null || !canonicalFile.exists()) {
      return;
    }
    if (!isAllowed(canonicalFile)) {
      return;
    }

    if (canonicalFile.isDirectory()) {
      File[] children = canonicalFile.listFiles();
      if (children == null) {
        return;
      }
      for (File child : children) {
        collectZipFiles(child, out);
      }
      return;
    }

    if (canonicalFile.getName().toLowerCase().endsWith(".zip")) {
      out.add(canonicalFile.getAbsolutePath());
    }
  }

  private boolean isAllowed(File file) {
    if (sourceSafeRoot == null) {
      return true;
    }

    File canonicalRoot = canonical(sourceSafeRoot);
    return canonicalRoot != null && file.toPath().startsWith(canonicalRoot.toPath());
  }

  private File canonical(File file) {
    try {
      return file.getCanonicalFile();
    } catch (IOException exception) {
      return null;
    }
  }

  private String normalizeSource(String raw) {
    String source = raw == null ? "" : raw.trim();
    if (source.length() >= 2) {
      if ((source.startsWith("\"") && source.endsWith("\""))
          || (source.startsWith("'") && source.endsWith("'"))) {
        source = source.substring(1, source.length() - 1).trim();
      }
    }
    return source;
  }
}
