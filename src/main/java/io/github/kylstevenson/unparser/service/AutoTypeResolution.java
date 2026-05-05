package io.github.kylstevenson.unparser.service;

import io.github.kylstevenson.unparser.model.GameType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AutoTypeResolution {

  private final GameType gameType;
  private final String reason;

  public boolean isResolved() {
    return gameType != null;
  }
}
