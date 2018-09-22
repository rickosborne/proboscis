package org.rickosborne.proboscis;

import lombok.Getter;
import org.junit.jupiter.api.Test;

import javax.inject.Named;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class FieldOrParamTest {

  public static final String FIELD_ANNOTATION = "fieldAnnotation";
  public static final String FIELD_NAME = "fieldName";
  public static final String PARAM_ANNOTATION = "paramAnnotation";
  public static final String PARAM_NAME = "paramName";

  @Test
  public void fieldWithNonSupplier() throws NoSuchFieldException {
    final Field field = FieldOrParamTestType.class.getDeclaredField("unnamedField");
    final FieldOrParam fieldOrParam = FieldOrParam.forField(field);
    assertEquals(Integer.class, fieldOrParam.getType(), "type");
    assertFalse(fieldOrParam.isSupplier(), "supplier");
  }

  @Test
  public void fieldWithSupplier() throws NoSuchFieldException {
    final Field field = FieldOrParamTestType.class.getDeclaredField("uuidSupplier");
    final FieldOrParam fieldOrParam = FieldOrParam.forField(field);
    assertEquals(UUID.class, fieldOrParam.getType(), "type");
    assertTrue(fieldOrParam.isSupplier(), "supplier");
  }

  @Test
  public void getNamedField() throws NoSuchFieldException {
    final Field field = FieldOrParamTestType.class.getDeclaredField("namedField");
    final FieldOrParam fieldOrParam = FieldOrParam.forField(field);
    assertEquals(FIELD_NAME, fieldOrParam.getNamed());
  }

  @Test
  public void getNamedParam() {
    final Parameter parameter = getParam(0);
    final FieldOrParam fieldOrParam = FieldOrParam.forParam(parameter, 0);
    assertEquals(PARAM_NAME, fieldOrParam.getNamed());
  }

  private Parameter getParam(final int number) {
    final Constructor<?> constructor = FieldOrParamTestType.class.getDeclaredConstructors()[0];
    return constructor.getParameters()[number];
  }

  @Test
  public void paramWithNonSupplier() {
    final Parameter param = getParam(1);
    final FieldOrParam fieldOrParam = FieldOrParam.forParam(param, 1);
    assertEquals(Integer.class, fieldOrParam.getType());
    assertFalse(fieldOrParam.isSupplier());
  }

  @Test
  public void paramWithSupplier() {
    final Parameter param = getParam(2);
    final FieldOrParam fieldOrParam = FieldOrParam.forParam(param, 2);
    assertEquals(UUID.class, fieldOrParam.getType());
    assertTrue(fieldOrParam.isSupplier());
  }

  @Test
  public void fieldWithAnnotation() throws NoSuchFieldException {
    final Field field = FieldOrParamTestType.class.getDeclaredField("unnamedField");
    final FieldOrParam fieldOrParam = FieldOrParam.forField(field);
    final TestAnnotation annotation = fieldOrParam.getAnnotation(TestAnnotation.class);
    assertNotNull(annotation, "annotation");
    assertEquals(FIELD_ANNOTATION, annotation.value(), "annotation value");
  }

  @Test
  public void paramWithAnnotation() {
    final Parameter param = getParam(1);
    final FieldOrParam fieldOrParam = FieldOrParam.forParam(param, 1);
    final TestAnnotation annotation = fieldOrParam.getAnnotation(TestAnnotation.class);
    assertNotNull(annotation, "annotation");
    assertEquals(PARAM_ANNOTATION, annotation.value(), "annotation value");
  }

  @Retention(RetentionPolicy.RUNTIME)
  private @interface TestAnnotation {
    String value();
  }

  @Getter
  private static class FieldOrParamTestType {
    @Named(FIELD_NAME)
    private final String namedField;

    @TestAnnotation(FIELD_ANNOTATION)
    private final Integer unnamedField;

    private final Supplier<UUID> uuidSupplier;

    public FieldOrParamTestType(
      @Named(PARAM_NAME) final String namedParam,
      @TestAnnotation(PARAM_ANNOTATION) final Integer unnamedParam,
      final Supplier<UUID> uuidSupplier
    ) {
      namedField = namedParam;
      unnamedField = unnamedParam;
      this.uuidSupplier = uuidSupplier;
    }
  }
}