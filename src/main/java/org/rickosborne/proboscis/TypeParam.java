package org.rickosborne.proboscis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A constructor parameter that is injected with the class of the matching type parameter.
 * The injection order will match the type parameter order.
 * <pre>
 *   public class FruitGateway {
 *     private Repository&lt;Fruit&gt; repository;
 *   }
 *
 *   public class Repository&lt;T&gt; {
 *     private final Class&lt;T&gt; type;
 *
 *     public Repository(\@TypeParam final Class&lt;T&gt; type) {
 *       this.type = type;
 *     }
 *   }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeParam {
  int INDEX_DEFAULT = -1;

  int value() default INDEX_DEFAULT;
}
