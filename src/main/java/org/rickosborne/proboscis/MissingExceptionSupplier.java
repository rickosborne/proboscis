package org.rickosborne.proboscis;

import java.util.function.BiFunction;

/**
 * Typedef for lambdas which can build exceptions for bean creation problems.
 */
@FunctionalInterface
public interface MissingExceptionSupplier extends BiFunction<String, Class<?>, RuntimeException> { }
