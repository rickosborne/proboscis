package org.rickosborne.proboscis;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

@Getter
public class ParameterizedTypeWrapper<T> implements ParameterizedType {
  private final Type[] actualTypeArguments;
  private final Class<T> ownerType = null;
  private final Class<?> rawType;

  public ParameterizedTypeWrapper(final Class<T> type, final Type... params) {
    this.rawType = type;
    actualTypeArguments = params;
  }

  public ParameterizedTypeWrapper(final ParameterizedType parameterizedType) {
    @SuppressWarnings("unchecked") final Class<T> typed = (Class<T>) parameterizedType.getRawType();
    this.rawType = typed;
    actualTypeArguments = parameterizedType.getActualTypeArguments();
  }

  @Override
  public int hashCode() {
    return Objects.hash(rawType, actualTypeArguments);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof ParameterizedType)) return false;
    final ParameterizedType other = (ParameterizedType) obj;
    return rawType.equals(other.getRawType())
      && Arrays.deepEquals(actualTypeArguments, other.getActualTypeArguments());
  }
}
