package org.rickosborne.proboscis;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class MaybeTest {
  @Test
  public void canBeNullWithoutPassing() {
    final Maybe maybe = Maybe.of(null);
    assertFalse(maybe.isPass(), "not a pass");
    assertNull(maybe.getValue(), "null value");
  }

  @Test
  public void nonNullValueIsNotAPass() {
    final UUID value = UUID.randomUUID();
    final Maybe maybe = Maybe.of(value);
    assertEquals(value, maybe.getValue(), "supplied value");
    assertFalse(maybe.isPass());
  }

  @Test
  public void pass() {
    final Maybe pass = Maybe.pass();
    assertTrue(pass.isPass());
    assertNull(pass.getValue());
  }
}