package org.rickosborne.proboscis;

import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.function.Supplier;

/**
 * Abstraction between field and parameter for cases where you can treat them equally but {@link AnnotatedElement} isn't enough.
 */
public class FieldOrParam {
  private final Field field;
  private final Parameter parameter;
  @Getter
  private final boolean supplier;
  @Getter
  private final Class<?> type;
  @Getter
  private final Class<?> declaringClass;
  @Getter
  private final Integer paramIndex;
  @Getter
  private final ParameterizedType parameterizedType;
  @Getter
  private final ParameterizedType expectedType;

  private FieldOrParam(final Field field, final Parameter parameter, final Integer paramIndex, final ParameterizedType expectedType) {
    final Type simpleType;
    if (field != null) {
      simpleType = field.getType();
      this.declaringClass = field.getDeclaringClass();
    } else if (parameter != null) {
      simpleType = parameter.getType();
      this.declaringClass = parameter.getDeclaringExecutable().getDeclaringClass();
    } else {
      throw new IllegalArgumentException("Neither field nor parameter");
    }
    final Type maybeParameterized = field == null ? parameter.getParameterizedType() : field.getGenericType();
    parameterizedType = maybeParameterized instanceof ParameterizedType ? (ParameterizedType) maybeParameterized : null;
    if (simpleType == Supplier.class) {
      final Type[] types = parameterizedType.getActualTypeArguments();
      this.type = (Class<?>) types[0];
      this.supplier = true;
    } else {
      this.type = (Class<?>) simpleType;
      this.supplier = false;
    }
    this.field = field;
    this.parameter = parameter;
    this.paramIndex = paramIndex;
    this.expectedType = expectedType;
  }

  public static FieldOrParam forParam(final Parameter parameter, final int order, final ParameterizedType expectedType) {
    return new FieldOrParam(null, parameter, order, expectedType);
  }

  public static FieldOrParam forParam(final Parameter parameter, final int order) {
    return new FieldOrParam(null, parameter, order, parameter.getParameterizedType() instanceof ParameterizedType ? (ParameterizedType) parameter.getParameterizedType() : null);
  }

  public static FieldOrParam forField(final Field field, final ParameterizedType expectedType) {
    return new FieldOrParam(field, null, null, expectedType);
  }

  public static FieldOrParam forField(final Field field) {
    return new FieldOrParam(field, null, null, null);
  }

  protected AnnotatedElement asAnnotatedElement() {
    return field != null ? field : parameter;
  }

  public <A extends Annotation> A getAnnotation(final Class<A> annotation) {
    return (field == null ? parameter : field).getAnnotation(annotation);
  }

  public String getNamed() {
    return Util.nameOf(asAnnotatedElement());
  }

  @Override
  public String toString() {
    return asAnnotatedElement().toString();
  }
}
