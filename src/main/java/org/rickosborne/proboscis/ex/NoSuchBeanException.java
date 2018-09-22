package org.rickosborne.proboscis.ex;

import lombok.Getter;

@Getter
public class NoSuchBeanException extends NullPointerException {
  private final String name;
  private final Class<?> type;

  public NoSuchBeanException(final String name, final Class<?> type) {
    super(formatNameAndType(name, type));
    this.name = name;
    this.type = type;
  }

  public NoSuchBeanException(final Class<?> type) {
    this(null, type);
  }

  private static String formatNameAndType(final String name, final Class<?> type) {
    if (name == null && type == null) return null;
    if (name == null) return type.getSimpleName();
    if (type == null) return name;
    return type.getSimpleName() + "(" + name + ")";
  }
}
