package org.rickosborne.proboscis;

/**
 * A {@link java.util.function.Consumer} which allows for pass-through of exceptions.
 */
@FunctionalInterface
public interface ThrowingConsumer<T, E extends Throwable> {
  void accept(final T value) throws E;
}
