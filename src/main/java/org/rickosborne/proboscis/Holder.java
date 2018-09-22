package org.rickosborne.proboscis;

import lombok.*;

/**
 * Like a single-item array, allow for mutability in places you probably shouldn't.
 */
@Getter
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Holder<T> {
  private T value;

  public static <T> Holder<T> empty() {
    return new Holder<>();
  }

  public static <T> Holder<T> of(final T value) {
    return new Holder<>(value);
  }

  public <E extends Throwable> void ifValue(final ThrowingConsumer<T, E> block) throws E {
    if (value != null) block.accept(value);
  }

  public synchronized <E extends Throwable> T computeIfAbsent(@NonNull final ThrowingSupplier<T, E> block) throws E {
    if (value == null) value = block.get();
    return value;
  }
}
