package org.rickosborne.proboscis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TypedFactoryTest {
  @Test
  public void callsResolversEachTime() {
    @Getter
    class TestResolver implements FieldOrParamInspector {
      protected UUID lastId;
      protected int calls = 0;

      @Override
      public FieldOrParamResolver findResolver(final FieldOrParam fieldOrParam) {
        return fop -> {
          calls++;
          lastId = UUID.randomUUID();
          return Maybe.of(lastId);
        };
      }
    }
    final TestResolver resolver = new TestResolver();
    final TypedFactory<NeedsId> factory = Loader.factoryFor(NeedsId.class, null)
      .withFieldOrParamResolver(resolver);
    assertNull(resolver.getLastId(), "should not have been called yet");
    assertEquals(0, resolver.getCalls(), "should not have been called yet");
    final NeedsId first = factory.get();
    assertEquals(resolver.getLastId(), first.getId(), "first Id");
    assertEquals(1, resolver.getCalls());
    final NeedsId second = factory.get();
    assertEquals(resolver.getLastId(), second.getId(), "second Id");
    assertEquals(2, resolver.getCalls());
    assertNotEquals(first, second);
  }

  @RequiredArgsConstructor
  @Getter
  @EqualsAndHashCode
  private static class NeedsId {
    private final UUID id;
  }
}