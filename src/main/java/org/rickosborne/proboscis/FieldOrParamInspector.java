package org.rickosborne.proboscis;

/**
 * Something which might know what to do with a given {@link FieldOrParam}.
 */
public interface FieldOrParamInspector {
  FieldOrParamResolver findResolver(FieldOrParam fieldOrParam);
}
