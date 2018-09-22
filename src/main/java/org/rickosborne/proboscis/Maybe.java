package org.rickosborne.proboscis;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Like {@link java.util.Optional} but slightly different semantics.
 * There are times when you explicitly want to return null distinct from "pass" / abstain.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Maybe {
  private static final Maybe PASS = new Maybe(true, null);
  private final boolean pass;
  private final Object value;

  public static Maybe of(final Object value) {
    return new Maybe(false, value);
  }

  public static Maybe pass() {
    return PASS;
  }
}
