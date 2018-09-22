package org.rickosborne.proboscis;

import java.util.Set;

/**
 * Typedef for lambdas that know how to find implementations for a given type.
 */
@FunctionalInterface
public interface ImplementationResolver {
  <T> Set<Class<? extends T>> implementationsOf(final Class<T> type);
}
