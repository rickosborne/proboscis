package org.rickosborne.proboscis;

import lombok.Getter;
import lombok.NonNull;
import org.reflections.Reflections;
import org.rickosborne.proboscis.ex.NoSuchBeanException;

import java.lang.reflect.ParameterizedType;

/**
 * I figure out how to build things.
 * See {@link DependencyInjectionContext} for the most common use case of building beans in dependency injection scenarios.
 * However, methods like {@link #build(Class)} also come in handy even without a DI context.
 */
public class Loader {
  @Getter(lazy = true)
  private static final Reflections reflections = new Reflections(Loader.class.getPackage().getName());

  /**
   * Try to build an object of the given class.
   * It will look for a static method named "getInstance", a static factory method, or will try creating one with {@code new}.
   * Note that this method <strong>does not</strong> support parameter injection.
   * @throws UnsupportedOperationException if all of the above fail.
   */
  public static <T> T build(final Class<T> type) {
    return factoryFor(type, null).get();
//
//    try {
//      // Attempt 1: Find a method called "getInstance()"
//      try {
//        final Method getInstance = type.getDeclaredMethod("getInstance");
//        if (type.isAssignableFrom(getInstance.getReturnType()) && Modifier.isPublic(getInstance.getModifiers()) && getInstance.getParameterCount() == 0) {
//          return type.cast(getInstance.invoke(null));
//        }
//      } catch (final NoSuchMethodException ignored) { }
//      // Attempt 2: Find a static builder
//      for (final Method method : type.getDeclaredMethods()) {
//        if (Modifier.isStatic(method.getModifiers()) && type.isAssignableFrom(method.getReturnType()) && Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0) {
//          return type.cast(method.invoke(null));
//        }
//      }
//      // Attempt 3: Just try to build it
//      return type.newInstance();
//    } catch (final IllegalAccessException | InvocationTargetException | InstantiationException e) {
//      throw new UnsupportedOperationException("Could not build: " + type.getName(), e);
//    }
  }

  /**
   * When given a type and a name of a subtype, try to build the subtype.
   * This is useful in cases where you have a interface and a config which specifies which concrete implementation, but the caller doesn't know all possible concrete types or how to build them.
   * <code>
   *   final IFruit fruit = Loader.buildByName(IFruit.class, "banana");  // instantiates: class Banana implements IFruit
   * </code>
   * Additionally, a number of stop words can be provided which will be eliminated from the name <strong>of the implementation class</strong> to help find a match:
   * <code>
   *   final String fruitName = getConfig(...);
   *   // The fruit name in the configuration might say {@code Banana}, but the class might be {@code BananaModel}.
   *   final IFruit fruit = Loader.buildByName(IFruit.class, fruitName, "model");
   * </code>
   * Like {@link #build(Class)} this method <strong>does not</strong> support parameter injection!
   * @param type      Expected type
   * @param name      Name of concrete type
   * @param stopWords Words which can be eliminated from the concrete class name to help it match the given name
   * @param <T>       Expected type
   * @return          Instantiated item.
   * @throws NoSuchBeanException if a concrete class cannot be found that matches the given name.
   * @throws UnsupportedOperationException if the bean is found but cannot be instantiated
   */
  public static <T> T buildByName(@NonNull final Class<T> type, @NonNull final String name, final String... stopWords) {
    final String identifier = name.replace(" ", "");
    for (final Class<? extends T> implClass : getReflections().getSubTypesOf(type)) {
      if (Util.matches(identifier, implClass.getSimpleName(), stopWords)) {
        return build(implClass);
      }
    }
    throw new NoSuchBeanException(name, type);
  }

  /**
   * Create a factory that knows how to build the given type and parameters.
   * Generally used by {@link DependencyInjectionContext} but also useful when you're going to build a bunch of things later.
   * @param <T> Expected type
   * @param type Class of expected type
   * @param parameterizedType Parameterized representation of the expected type
   * @return A factory for {@link T} instances
   */
  public static <T> TypedFactory<T> factoryFor(final Class<T> type, final ParameterizedType parameterizedType) {
    return new TypedFactory<>(parameterizedType, type);
  }

}
