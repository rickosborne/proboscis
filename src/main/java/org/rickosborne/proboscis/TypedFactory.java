package org.rickosborne.proboscis;

import lombok.RequiredArgsConstructor;

import javax.inject.Provider;
import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A factory which can figure out how to supply objects given additional help.
 * Also tries to divide out the inspection time from the building time.
 * @param <T> Supplied type
 */
@RequiredArgsConstructor
class TypedFactory<T> implements Supplier<T>, Provider<T> {
  private final Holder<Supplier<T>> builder = Holder.empty();
  private final List<ImplementationResolver> implementationResolvers = new LinkedList<>();
  private final List<FieldOrParamInspector> inspectors = new LinkedList<>();
  private final Holder<MissingExceptionSupplier> missingExceptionSupplier = Holder.empty();
  private final ParameterizedType parameterizedType;
  private final Class<T> type;

  private FieldOrParamResolver findResolver(final FieldOrParam element) {
    for (final FieldOrParamInspector inspector : inspectors) {
      final FieldOrParamResolver injector = inspector.findResolver(element);
      if (injector != null) return injector;
    }
    return null;
  }

  private <U> TypedFactory<U> forOtherType(final Class<U> otherType, final ParameterizedType parameterizedType) {
    final TypedFactory<U> factory = new TypedFactory<>(parameterizedType, otherType);
    factory.implementationResolvers.addAll(implementationResolvers);
    factory.inspectors.addAll(inspectors);
    factory.missingExceptionSupplier.setValue(missingExceptionSupplier.getValue());
    return factory;
  }

  private <U extends T> TypedFactory<U> forSubclass(final Class<U> subclass) {
    return forOtherType(subclass, null);
  }

  private Supplier<T> fromConstructor() {
    for (final Constructor<?> constructor : type.getDeclaredConstructors()) {
      if (Modifier.isPublic(constructor.getModifiers())) {
        final Supplier<T> maybe = fromExecutable(constructor);
        if (maybe != null) return maybe;
      }
    }
    return null;
  }

  private Supplier<T> fromExecutable(final Executable executable) {
    final int parameterCount = executable.getParameterCount();
    final Parameter[] parameters = executable.getParameters();
    final FieldOrParamResolver[] resolvers = new FieldOrParamResolver[parameterCount];
    final FieldOrParam[] fieldsOrParams = new FieldOrParam[parameterCount];
    for (int i = 0; i < parameters.length; i++) {
      final Parameter parameter = parameters[i];
      final FieldOrParam fieldOrParam = FieldOrParam.forParam(parameter, i, parameterizedType);
      final FieldOrParamResolver resolver = findResolver(fieldOrParam);
      if (resolver == null) return null;
      resolvers[i] = resolver;
      fieldsOrParams[i] = fieldOrParam;
    }
    return new SupplierFromExecutable<>(executable, fieldsOrParams, resolvers, type);
  }

  private Supplier<T> fromImplementationResolver() {
    for (final ImplementationResolver implementationResolver : implementationResolvers) {
      final Set<Class<? extends T>> implementors = implementationResolver.implementationsOf(type);
      if (implementors == null) continue;
      return () -> {
        for (final Class<? extends T> implementor : implementors) {
          final T maybe = forSubclass(implementor).get();
          if (maybe != null) return maybe;
        }
        return null;
      };
    }
    return null;
  }

  private Supplier<T> fromStaticBuilder() {
    for (final Method method : type.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers())
        && Modifier.isPublic(method.getModifiers())
        && type.isAssignableFrom(method.getReturnType())) {
        final Supplier<T> maybe = fromExecutable(method);
        if (maybe != null) return maybe;
      }
    }
    return null;
  }

  public T get() {
    return builder.computeIfAbsent(() -> {
      final Supplier<T> maybeFromStatic = fromStaticBuilder();
      if (maybeFromStatic != null) return maybeFromStatic;
      final Supplier<T> maybeFromConstructor = fromConstructor();
      if (maybeFromConstructor != null) return maybeFromConstructor;
      final Supplier<T> maybeFromImplResolver = fromImplementationResolver();
      if (maybeFromImplResolver != null) return maybeFromImplResolver;
      throw new UnsupportedOperationException("Could not build: " + type.getName());
    }).get();
  }

  private Object supplyIfNeeded(final Object object, final FieldOrParam fieldOrParam) {
    final boolean wantSupplier = fieldOrParam.isSupplier();
    final boolean gotSupplier = object instanceof Supplier;
    if (wantSupplier == gotSupplier) return object;
    if (wantSupplier) return new SingletonSupplier(object);
    return ((Supplier<?>) object).get();
  }

  public TypedFactory<T> withBean(final Object object) {
    final Maybe maybe = Maybe.of(object);
    inspectors.add(fieldOrParam -> {
      final Class<?> fopType = fieldOrParam.getType();
      if (fopType.isInstance(object)) {
        return fop -> maybe;
      } else {
        return null;
      }
    });
    return this;
  }

  public TypedFactory<T> withFieldOrParamResolver(final FieldOrParamInspector resolver) {
    inspectors.add(resolver);
    return this;
  }

  public TypedFactory<T> withImplementationResolver(final ImplementationResolver implementationResolver) {
    implementationResolvers.add(implementationResolver);
    inspectors.add(fieldOrParam -> {
      final String name = fieldOrParam.getNamed();
      final Class<?> implType = fieldOrParam.getType();
      @SuppressWarnings("unchecked") final Set<Class<?>> maybes = implementationResolver.implementationsOf((Class) implType);
      if (maybes != null) {
        for (final Class<?> maybeImpl : maybes) {
          if (name != null) {
            final String implName = Util.nameOf(maybeImpl);
            if (!name.equalsIgnoreCase(implName)) continue;
          }
          final TypedFactory<?> builder = forOtherType(maybeImpl, fieldOrParam.getParameterizedType());
          final Object impl = builder.get();
          if (impl != null) {
            final Maybe maybe = Maybe.of(impl);
            return fop -> maybe;
          }
        }
      }
      return null;
    });
    return this;
  }

  public TypedFactory<T> withResolverExceptions(final MissingExceptionSupplier exceptionSupplier) {
    missingExceptionSupplier.setValue(exceptionSupplier);
    return this;
  }

  @RequiredArgsConstructor
  private class SupplierFromExecutable<U> implements Supplier<U> {
    private final Executable executable;
    private final FieldOrParam[] fieldsOrParams;
    private final FieldOrParamResolver[] resolvers;
    private final Class<U> type;

    private Object[] buildArgs() {
      final Object[] args = new Object[resolvers.length];
      for (int i = 0; i < resolvers.length; i++) {
        final FieldOrParamResolver injector = resolvers[i];
        final FieldOrParam fieldOrParam = fieldsOrParams[i];
        final Maybe maybeValue = injector.apply(fieldOrParam);
        if (maybeValue == null || maybeValue.isPass()) {
          missingExceptionSupplier.ifValue(mes -> {
            throw mes.apply(fieldOrParam.getNamed(), fieldOrParam.getType());
          });
        } else {
          args[i] = supplyIfNeeded(maybeValue.getValue(), fieldOrParam);
        }
      }
      return args;
    }

    @Override
    public U get() {
      try {
        final Object[] args = buildArgs();
        final Object maybe;
        if (executable instanceof Method) maybe = ((Method) executable).invoke(null, args);
        else if (executable instanceof Constructor) maybe = ((Constructor<?>) executable).newInstance(args);
        else throw new UnsupportedOperationException("Unknown executable type: " + executable);
        return type.cast(maybe);
      } catch (final IllegalAccessException | InvocationTargetException | InstantiationException e) {
        throw new UnsupportedOperationException("Could not build: " + type.getSimpleName() + "." + executable.getName(), e);
      }
    }
  }

  @RequiredArgsConstructor
  private class SingletonSupplier implements Supplier<Object> {
    private final Object object;

    @Override
    public Object get() {
      return object;
    }
  }
}
