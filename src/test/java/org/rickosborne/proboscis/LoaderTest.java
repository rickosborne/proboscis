package org.rickosborne.proboscis;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.rickosborne.proboscis.ex.NoSuchBeanException;

import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class LoaderTest {

  @Test
  public void buildByName() {
    final ILoaderTest bean = Loader.buildByName(ILoaderTest.class, "Arg", "No");
    assertNotNull(bean, "bean");
    assertEquals(NoArg.class, bean.getClass(), () -> "type: " + bean.getClass().getSimpleName());
  }

  @Test
  public void buildByNameThrowsForNotFound() {
    final String name = "bogus";
    final NoSuchBeanException error = assertThrows(NoSuchBeanException.class, () -> Loader.buildByName(ILoaderTest.class, name));
    assertEquals(name, error.getName());
    assertEquals(ILoaderTest.class, error.getType());
  }

  @Test
  public void buildFindsStaticBuilderNotNamedGetInstance() {
    final NotCalledGetInstance bean = Loader.build(NotCalledGetInstance.class);
    assertNotNull(bean, "Bean with build()");
    assertTrue(bean.isCorrect(), "Created by build()");
  }

  @Test
  public void buildThrowsUnsupportedForNoWay() {
    final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> Loader.build(NoWayToBuild.class));
    assertTrue(ex.getMessage().contains(NoWayToBuild.class.getSimpleName()), "helpful class name in error message");
  }

  @Test
  public void buildWithGetInstance() {
    final RequiresGetInstance bean = Loader.build(RequiresGetInstance.class);
    assertNotNull(bean, "Bean with getInstance()");
    assertTrue(bean.isCorrect(), "Created by getInstance()");
  }

  @Test
  public void buildWithNoArgConstructor() {
    final NoArg bean = Loader.build(NoArg.class);
    assertNotNull(bean, "NoArg");
  }

  @Test
  public void factory() {
    final Supplier<EachUnique> supplier = Loader.factoryFor(EachUnique.class, null);
    final EachUnique first = supplier.get();
    assertNotNull(first, "first");
    final EachUnique second = supplier.get();
    assertNotNull(second, "second");
    assertNotEquals(first, second);
  }

  public interface ILoaderTest {}

  @SuppressWarnings("unused")
  public static class AlterNoArg implements ILoaderTest {}

  public static class NoArg implements ILoaderTest {}

  @RequiredArgsConstructor
  @SuppressWarnings("unused")
  public static class NoWayToBuild {
    private final boolean correct;

    protected static NoWayToBuild build() {
      return new NoWayToBuild(false);
    }

    private static NoWayToBuild getInstance() {
      return new NoWayToBuild(false);
    }
  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class NotCalledGetInstance {
    private final boolean correct;

    @SuppressWarnings("unused")
    public static NotCalledGetInstance build() {
      return new NotCalledGetInstance(true);
    }
  }

  @Getter
  @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
  public static class RequiresGetInstance {
    private final boolean correct;

    @SuppressWarnings("unused")
    public static RequiresGetInstance getInstance() {
      return new RequiresGetInstance(true);
    }
  }

  @Getter
  @EqualsAndHashCode
  public static class EachUnique {
    private final UUID id = UUID.randomUUID();
  }
}