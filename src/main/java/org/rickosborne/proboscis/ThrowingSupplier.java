package org.rickosborne.proboscis;

/**
 * A {@link java.util.function.Supplier} which passes along exceptions.
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
  T get() throws E;
}
