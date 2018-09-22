package org.rickosborne.proboscis;

import lombok.experimental.UtilityClass;

import javax.inject.Named;
import java.lang.reflect.AnnotatedElement;

@UtilityClass
class Util {
  static boolean matches(final String expected, final String actual, final String... stopWords) {
    if (actual.equalsIgnoreCase(expected)) return true;
    if (stopWords == null || stopWords.length == 0) return false;
    String smaller = actual;
    for (final String stopWord : stopWords) {
      smaller = smaller.replaceAll(stopWord, "");
    }
    return smaller.equalsIgnoreCase(expected);
  }

  public static String nameOf(final AnnotatedElement element) {
    if (element == null) return null;
    final Named named = element.getAnnotation(Named.class);
    return named == null || named.value().isEmpty() ? null : named.value();
  }
}
