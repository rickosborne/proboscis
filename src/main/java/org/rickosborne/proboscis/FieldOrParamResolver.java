package org.rickosborne.proboscis;

import java.util.function.Function;

@FunctionalInterface
public interface FieldOrParamResolver extends Function<FieldOrParam, Maybe> { }
