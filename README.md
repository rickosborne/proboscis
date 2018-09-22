# Proboscis

This is a proof-of-concept dependency injection framework.
You should really use something else like [Guice] or [Spring].

See [the license](LICENSE.txt) before you do anything with this code.
Rethink your life choices before you do anything with this code.

[Guice]: https://github.com/google/guice
[Spring]: https://spring.io/

## Examples

```java
public class App implements IApp {
  private final DependencyInjectionContext diContext;
  private final List<IAppApare> beans = new LinkedList<>();
  
  public App() {
    diContext = new DependencyInjectionContext();
    diContext.registerBean(IApp.class, this, null);
    for (final Class<? extends IAppAware> implClass : Loader.findImplementations(IAppAware.class)) {
      final IAppAware appAware = diContext.buildBean(implClass);
      beans.add(busAware);
    }
  }
}

// Brought in via the code above
public class ExampleService implements IAppAware {
  public ExampleService(
    @SystemProperty("com.example.whatever") final String whatever,
    final ExampleRepository<SomeEntity> repository
  ) {
    // ...
  }
}

@Named  // Equivalent of Spring's @Component
@RequiredArgsConstructor
public class ExampleRepository<EntityT> implements CrudRepository<EntityT, Integer> {
  @TypeParam  // When this is instantiated for ExampleService, SomeEntity.class is injected here
  private final Class<EntityT> entityType;
}

// Not DI-aware at all
@Data
public class SomeEntity {
  @Id
  private Integer id;
}
```