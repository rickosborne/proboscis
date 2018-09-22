package org.rickosborne.proboscis;

/**
 * Typedef for lambdas which can resolve values for injected fields or params.
 */
public interface ValueResolver {
  <T> T resolve(final FieldOrParam fieldOrParam);
}
