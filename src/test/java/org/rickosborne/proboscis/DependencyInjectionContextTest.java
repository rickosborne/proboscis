package org.rickosborne.proboscis;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.rickosborne.proboscis.ex.NoSuchBeanException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyInjectionContextTest {
  public static final String MANUAL_NAMED = "testManual";
  public static final String MISSING_NAMED = "testMissing";
  public static final String TEST_NAMED = "testNamed";
  public static final String TEST_PROPERTY = "testProperty";
  public static final String TEST_PROPERTY_DEFAULT = "testDefault";

  @Test
  public void buildBeanFailsWhenNamedDependenciesAreMissing() {
    final NoSuchBeanException error = assertThrows(NoSuchBeanException.class, () -> emptyContext().buildBean(RequiresMissingNamedBean.class));
    assertEquals(ITestBean.class, error.getType(), "type");
    assertEquals(MISSING_NAMED, error.getName(), "name");
  }

  @Test
  public void buildBeanRecursivelyBuildsNamedDependencies() {
    final RequiresByNameBean bean = emptyContext().buildBean(RequiresByNameBean.class);
    assertNotNull(bean, "RequiresByNameBean");
    assertNotNull(bean.getNamed(), "Named Arg");
    assertEquals(TestNamedBean.class, bean.getNamed().getClass(), "instanceof TestNamedBean");
  }

  @Test
  public void buildBeanRecursivelyBuildsUnnamedDependencies() {
    final RequiresByTypeBean bean = emptyContext().buildBean(RequiresByTypeBean.class);
    assertNotNull(bean, "RequiresByTypeBean");
    assertNotNull(bean.getUnnamed(), "Named Arg");
  }

  @Test
  public void buildBeanWorksForNoArgConstructors() {
    final NoArgConstructedBean bean = emptyContext().buildBean(NoArgConstructedBean.class);
    assertNotNull(bean, "NoArgConstructedBean");
  }

  @Test
  public void buildBeanWorksForStaticBuilder() {
    final StaticConstructedBean bean = emptyContext().buildBean(StaticConstructedBean.class);
    assertNotNull(bean, "StaticConstructedBean");
    assertTrue(bean.isBuiltWithStatic(), "builtWithStatic");
  }

  private DependencyInjectionContext emptyContext() {
    return new DependencyInjectionContext();
  }

  @Test
  public void injection() {
    final RequiresInjectionBean bean = emptyContext().buildBean(RequiresInjectionBean.class);
    assertNotNull(bean, "RequiresInjectionBean");
    assertNotNull(bean.getNamed(), "named");
    assertTrue(bean.getNamed() instanceof TestNamedBean, "named type");
    assertNotNull(bean.getUnnamed(), "unnamed");
  }

  @Test
  public void buildHandlesSuppliers() {
    final RequiresSupplier bean = emptyContext().buildBean(RequiresSupplier.class);
    assertNotNull(bean, "RequiresSupplier");
    final Supplier<TestUnnamedBean> supplier = bean.getSupplier();
    assertNotNull(supplier, "supplier");
    final TestUnnamedBean supplied = supplier.get();
    assertNotNull(supplied, "TestUnnamedBean");
  }


  @Getter
  @RequiredArgsConstructor
  public static class RequiresSupplier {
    private final Supplier<TestUnnamedBean> supplier;
  }

  @Test
  public void manualBeanRegistration() {
    final DependencyInjectionContext context = emptyContext();
    final NoSuchBeanException error = assertThrows(NoSuchBeanException.class, () -> context.requireBean(RequiresManualBean.class));
    assertEquals(error.getType(), ManualBean.class, "NoSuchBean type");
    final ManualBean manualBean = new ManualBean();
    context.registerBean(ManualBean.class, manualBean, MANUAL_NAMED);
    final RequiresManualBean bean = context.requireBean(RequiresManualBean.class);
    assertNotNull(bean, "RequiresManualBean");
    assertEquals(manualBean, bean.getManual(), "Same Manual Bean");
  }

  @Test
  public void requireThrowsForMissingName() {
    final NoSuchBeanException error = assertThrows(NoSuchBeanException.class, () -> emptyContext().requireBean(RequiresMissingNamedBean.class));
    assertEquals(MISSING_NAMED, error.getName(), "Error name");
  }

  @Test
  public void systemPropertyInConstructor() {
    final String propValue = UUID.randomUUID().toString();
    System.setProperty(TEST_PROPERTY, propValue);
    final RequiresSystemProperty withValue = emptyContext().buildBean(RequiresSystemProperty.class);
    assertEquals(propValue, withValue.getValue(), "Set value");
    System.clearProperty(TEST_PROPERTY);
    final RequiresSystemProperty withDefault = emptyContext().buildBean(RequiresSystemProperty.class);
    assertEquals(TEST_PROPERTY_DEFAULT, withDefault.getValue());
  }

  @Test
  public void typeParamsAreInjected() {
    final HasParameterizedParam tested = emptyContext().buildBean(HasParameterizedParam.class);
    assertNotNull(tested, "bean");
    assertNotNull(tested.getRtp(), "parameterized param");
    assertEquals(ManualBean.class, tested.getRtp().getParamClass(), "parameterized type class");
  }

  @RequiredArgsConstructor
  @Getter
  public static class HasParameterizedParam {
    private final RequiresTypeParam<ManualBean> rtp;
  }

  @Getter
  @Named
  public static class RequiresTypeParam<ParamT> {
    private final Class<ParamT> paramClass;

    public RequiresTypeParam(@TypeParam final Class<ParamT> paramClass) {
      this.paramClass = paramClass;
    }
  }

  public interface ITestBean {}

  /**
   * This bean is not Named and therefore would not be found by scanning.
   */
  @EqualsAndHashCode
  public static class ManualBean {
    private final UUID id = UUID.randomUUID();
  }

  public static class NoArgConstructedBean {}

  @Getter
  public static class RequiresByNameBean {
    private final ITestBean named;

    public RequiresByNameBean(@Named(TEST_NAMED) final ITestBean named) {
      this.named = named;
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class RequiresByTypeBean {
    private final TestUnnamedBean unnamed;
  }

  @Getter
  @Setter
  public static class RequiresInjectionBean {
    @Named(TEST_NAMED)
    private ITestBean named;

    @Inject
    private TestUnnamedBean unnamed;
  }

  @Getter
  public static class RequiresManualBean {
    private final ManualBean manual;

    public RequiresManualBean(@Named(MANUAL_NAMED) final ManualBean manual) {
      this.manual = manual;
    }
  }

  @Getter
  public static class RequiresMissingNamedBean {
    private final ITestBean named;

    public RequiresMissingNamedBean(@Named(MISSING_NAMED) final ITestBean named) {
      this.named = named;
    }
  }

  @Getter
  public static class RequiresSystemProperty {
    private final String value;

    public RequiresSystemProperty(@SystemProperty(value = TEST_PROPERTY, defaultValue = TEST_PROPERTY_DEFAULT) final String propValue) {
      this.value = propValue;
    }
  }

  @RequiredArgsConstructor
  @Getter
  public static class StaticConstructedBean {
    private final boolean builtWithStatic;

    @SuppressWarnings("unused")
    public static StaticConstructedBean build() {
      return new StaticConstructedBean(true);
    }
  }

  @Named(TEST_NAMED)
  public static class TestNamedBean implements ITestBean {}

  @Named
  public static class TestUnnamedBean implements ITestBean {}
}