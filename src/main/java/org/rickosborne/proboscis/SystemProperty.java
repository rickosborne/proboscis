package org.rickosborne.proboscis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A field or param that accepts a value injected from a system property.
 * <code>
 *   \@SystemProperty("com.example.whatever")
 *   protected String whatever;
 * </code>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemProperty {
  String value();
  String defaultValue() default "";
}
