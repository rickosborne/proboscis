package org.rickosborne.proboscis;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HolderTest {

  @Test
  public void getValue() {
    final UUID value = UUID.randomUUID();
    final Holder<UUID> holder = Holder.of(value);
    assertEquals(value, holder.getValue(), "value");
  }

  @Test
  public void ifValueForNonNull() {
    final Holder<String> holder = Holder.empty();
    assertNull(holder.getValue(), "value starts null");
    final String value = UUID.randomUUID().toString();
    holder.setValue(value);
    final boolean[] blockCalled = {false};
    holder.ifValue(val -> {
      assertEquals(value, val);
      blockCalled[0] = true;
    });
    assertTrue(blockCalled[0], "block should have been called");
  }

  @Test
  public void ifValueForNull() {
    final Holder<?> holder = Holder.empty();
    assertNull(holder.getValue(), "value starts null");
    holder.ifValue(value -> fail("block should not have been called"));
  }

  @Test
  public void setValue() {
    final UUID value = UUID.randomUUID();
    final Holder<UUID> holder = Holder.empty();
    assertNull(holder.getValue(), "value starts null");
    holder.setValue(value);
    assertEquals(value, holder.getValue(), "value is set");
    holder.setValue(null);
    assertNull(holder.getValue(), "value can be nulled");
  }
}