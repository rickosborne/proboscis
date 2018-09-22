package org.rickosborne.proboscis;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.java.Log;
import org.rickosborne.proboscis.ex.NoSuchBeanException;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Poor-man's dependency injection using JSR-330 {@link javax.inject} annotations.
 * This is not intended to be amazing, nor to even come close to Spring or Guice.
 * We want just enough functionality here to make DI braindead simple.
 * <pre>
 *   \@Getter
 *   public class Apple {
 *     private final Banana banana;
 *     private final IFruitBowl fruitBowl;
 *
 *     public Apple(final Banana banana, \@Named("fruitBowl") final IFruitBowl fruitBowl) {
 *       this.driver = driver;
 *       this.fruitBowl = fruitBowl;
 *     }
 *   }
 *
 *   \@Getter
 *   public class Banana {
 *     \@Inject
 *     \@SystemProperty("com.example.banana.color")
 *     private String color;
 *   }
 *
 *   public interface IFruitBowl {}
 *
 *   public class GlassBowl implements IFruitBowl {}
 *
 *   \@Named("fruitBowl")
 *   public class PlasticBowl implements IFruitBowl {}
 *
 *   public class FruitManager {
 *     public static void main() {
 *       final DependencyInjectionContext context = new DependencyInjectionContext();
 *       final Apple apple = context.requireBean(Apple.class);
 *       final String bananaColor = apple.getBanana().getColor();
 *     }
 *   }
 * </pre>
 */
@Log
public class DependencyInjectionContext {
  public static final String NO_NAME = "";
  private final Map<Object, String> knownBeans = new ConcurrentHashMap<>();
  private final Map<Class<?>, String> namedTypes = new ConcurrentHashMap<>(findNamedTypes());
  private final Map<ParameterizedInstance, String> parameterizedBeans = new ConcurrentHashMap<>();
  private final SystemPropertyInspector systemPropertyInspector = new SystemPropertyInspector();
  private final TypeParamInspector typeParamInspector = new TypeParamInspector();

  public static Map<Class<?>, String> findNamedTypes() {
    final Map<Class<?>, String> types = new HashMap<>();
    for (final Class<?> namedType : Loader.getReflections().getTypesAnnotatedWith(Named.class)) {
      final Named named = namedType.getAnnotation(Named.class);
      types.put(namedType, named.value());
    }
    return types;
  }

  public static boolean shouldBeInjected(final AnnotatedElement element) {
    return element != null && (element.isAnnotationPresent(Inject.class) || element.isAnnotationPresent(Named.class));
  }

  /**
   * Build or find me a thing.  See {@link #requireBean(Class)} if you want to throw an error.
   * @param type Type class
   * @param <T> Type param
   * @return Instance or null
   */
  public <T> T buildBean(final Class<T> type) {
    return buildParameterizedBean(null, type);
  }

  protected <T> T buildBean(final FieldOrParam fieldOrParam) {
    @SuppressWarnings("unchecked") final Class<T> typed = (Class<T>) fieldOrParam.getType();
    final ParameterizedType parameterizedType = fieldOrParam.getParameterizedType();
    if (parameterizedType == null) return buildBean(fieldOrParam.getNamed(), typed);
    else return buildParameterizedBean(parameterizedType, typed);
  }

  /**
   * Build or find me a thing with the (optional) given name.  See {@link #buildBean(Class)}.
   */
  public <T> T buildBean(final String name, final Class<T> type) {
    final T byName = findBeanByName(name, type);
    if (byName != null) return byName;
    final T known = findBeanByType(type);
    if (known != null) return known;
    // not yet built
    for (final Map.Entry<Class<?>, String> pair : namedTypes.entrySet()) {
      final String actualName = pair.getValue();
      final Class<?> actualType = pair.getKey();
      if (!type.isAssignableFrom(actualType)) continue;
      if (name != null && !name.equalsIgnoreCase(actualName)) continue;
      final Class<? extends T> typedType = actualType.asSubclass(type);
      final T built = buildBean(typedType);
      if (built != null) {
        knownBeans.put(built, actualName == null ? NO_NAME : actualName);
        return built;
      }
    }
    return null;
  }

  public <T> T buildParameterizedBean(final Class<T> type, final Type... paramTypes) {
    return buildParameterizedBean(new ParameterizedTypeWrapper<>(type, paramTypes), type);
  }

  public <T> T buildParameterizedBean(final ParameterizedType parameterizedType, final Class<T> type) {
    if (parameterizedType != null) {
      final T existing = findBeanByParameterizedType(parameterizedType, type);
      if (existing != null) return existing;
    } else {
      final T existing = findBeanByType(type);
      if (existing != null) return existing;
    }
    final T built = Loader.factoryFor(type, parameterizedType)
      .withBean(this)
      .withImplementationResolver(this::implementationsFor)
      .withFieldOrParamResolver(systemPropertyInspector)
      .withFieldOrParamResolver(typeParamInspector)
      .withFieldOrParamResolver(fieldOrParam -> fop -> {
        final Object bean = buildBean(fop);
        return bean == null ? null : Maybe.of(bean);
      })
      .withResolverExceptions(NoSuchBeanException::new)
      .get();
    if (built == null) return null;
    if (parameterizedType != null) parameterizedBeans.put(new ParameterizedInstance(built, parameterizedType), NO_NAME);
    else knownBeans.put(built, NO_NAME);
    injectFields(built);
    return built;
  }

  /**
   * Find a bean with the given name.
   * @return NULL if not found.
   */
  public <T> T findBeanByName(final String name, final Class<T> type) {
    if (name != null) {
      // match by name
      final Object known = knownBeans.get(name);
      if (known != null) return type.cast(known);
    }
    return null;
  }

  public <T> T findBeanByParameterizedType(@NonNull final ParameterizedType parameterizedType, final Class<T> type) {
    // match existing type
    for (final ParameterizedInstance pi : parameterizedBeans.keySet()) {
      if (pi.getParameterizedType().equals(parameterizedType)) return type.cast(pi.getObject());
    }
    return null;
  }

  /**
   * FInd the first bean matching the given type.
   * @return NULL if no matches.
   */
  public <T> T findBeanByType(final Class<T> type) {
    // match existing type
    for (final Object known : knownBeans.keySet()) {
      if (type.isInstance(known)) return type.cast(known);
    }
    return null;
  }

  /**
   * Find all classes which implement the given type.
   */
  public <T> Set<Class<? extends T>> implementationsFor(final Class<T> typeClass) {
    final HashSet<Class<? extends T>> impls = new HashSet<>();
    for (final Class<?> namedClass : namedTypes.keySet()) {
      if (typeClass.isAssignableFrom(namedClass)) impls.add(namedClass.asSubclass(typeClass));
    }
    return impls;
  }

  /**
   * Scan the given bean and inject any fields annotated with JSR-330 annotations.
   * @see Named
   * @see Inject
   */
  public void injectFields(final Object bean) {
    if (bean == null) return;
    final Class<?> type = bean.getClass();
    for (final Field field : type.getDeclaredFields()) {
      try {
        final String name = Util.nameOf(field);
        if (name == null && !shouldBeInjected(field)) continue;
        field.setAccessible(true);
        final Object originalValue = field.get(bean);
        if (originalValue != null) continue;
        final Object newValue = buildBean(name, field.getType());
        if (newValue == null) throw new IllegalArgumentException(type.getSimpleName() + "." + field.getName());
        field.set(bean, newValue);
      } catch (final IllegalAccessException e) {
        log.warning(type.getSimpleName() + "." + field.getName() + " is not readable: " + e.getMessage());
      }
    }
  }

  /**
   * If you built a bean on your own, register it for injection for later managed beans.
   */
  public <T> DependencyInjectionContext registerBean(final Class<T> type, final T bean, final String name) {
    knownBeans.put(bean, name == null ? NO_NAME : name);
    namedTypes.put(type, name == null ? NO_NAME : name);
    return this;
  }

  public <T> DependencyInjectionContext registerParameterizedBean(final T bean, final String name, final Class<T> type, final Class<?>... typeParams) {
    parameterizedBeans.put(new ParameterizedInstance(bean, new ParameterizedTypeWrapper<>(type, typeParams)), name == null ? NO_NAME : name);
    return this;
  }

  /**
   * Like {@link #buildBean(Class)} but throws if the bean cannot be built.
   * @throws NoSuchBeanException if the bean cannot be created
   */
  @NonNull
  public <T> T requireBean(final Class<T> type) {
    final T bean = buildBean(type);
    if (bean == null) throw new NoSuchBeanException(type);
    return bean;
  }

  @Value
  private static class ParameterizedInstance {
    private final Object object;
    private final ParameterizedType parameterizedType;
  }

  public static class SystemPropertyInspector implements FieldOrParamInspector {
    @Override
    public FieldOrParamResolver findResolver(final FieldOrParam fieldOrParam) {
      final SystemProperty systemProperty = fieldOrParam.getAnnotation(SystemProperty.class);
      if (systemProperty == null) return null;
      final String propertyName = systemProperty.value();
      if (propertyName.isEmpty()) throw new IllegalArgumentException("@SystemProperty for " + fieldOrParam + " requires a value");
      final String defaultValue = systemProperty.defaultValue();
      @RequiredArgsConstructor
      class SystemPropertyResolver implements FieldOrParamResolver {
        private final String defaultValue;
        private final String propertyName;

        @Override
        public Maybe apply(final FieldOrParam fieldOrParam) {
          return Maybe.of(System.getProperty(propertyName, defaultValue));
        }
      }
      return new SystemPropertyResolver(defaultValue, propertyName);
    }
  }

  @RequiredArgsConstructor
  private static class TypeParamResolver implements FieldOrParamResolver {
    private final int typeIndex;

    @Override
    public Maybe apply(final FieldOrParam fieldOrParam) {
      final ParameterizedType parameterizedType = fieldOrParam.getExpectedType();
      if (parameterizedType == null) throw new IllegalArgumentException("Not parameterized: " + fieldOrParam);
      final Type paramType = parameterizedType.getActualTypeArguments()[typeIndex];
      if (paramType instanceof Class) return Maybe.of(paramType);
      throw new IllegalArgumentException("Expected class: " + paramType);
    }
  }

  public static class TypeParamInspector implements FieldOrParamInspector {
    @Override
    public FieldOrParamResolver findResolver(final FieldOrParam fieldOrParam) {
      final TypeParam typeParam = fieldOrParam.getAnnotation(TypeParam.class);
      if (typeParam == null) return null;
      final int annotationIndex = typeParam.value();
      final Integer paramIndex = fieldOrParam.getParamIndex();
      if (annotationIndex == TypeParam.INDEX_DEFAULT && paramIndex == null) {
        throw new IllegalArgumentException("Must specify an index for @TypeParam: " + fieldOrParam);
      }
      final int typeIndex = annotationIndex == TypeParam.INDEX_DEFAULT ? paramIndex : annotationIndex;
      return new TypeParamResolver(typeIndex);
    }
  }
}
