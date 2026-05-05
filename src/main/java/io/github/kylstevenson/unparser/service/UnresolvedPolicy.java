package io.github.kylstevenson.unparser.service;

public enum UnresolvedPolicy {
  SKIP,
  FAIL,
  FALLBACK;

  public static UnresolvedPolicy fromConfig(String value) {
    if (value == null) {
      return SKIP;
    }

    String normalized = value.trim().toUpperCase();
    for (UnresolvedPolicy policy : values()) {
      if (policy.name().equals(normalized)) {
        return policy;
      }
    }
    return SKIP;
  }
}
