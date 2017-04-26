package org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.ReflectionUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class )
@Ginkgo4jConfiguration(threads=1)
@ContextConfiguration(classes = StoreExtensionTest.TestConfiguration.class)
public class StoreExtensionTest {

    @Autowired
    private TestContentRepository repo;

    @Autowired
    private TestContentRepositoryExtension testExtensionService;

    // mocks/spys
    private TestContentRepositoryExtension spy;

    {
        Describe("AbstractContentStoreFactoryBean", () -> {
            Context("given a repository extension bean", () -> {
                Context("given an extension method is invoked on the proxy", () -> {
                    BeforeEach(() -> {
                        repo.someMethod();
                    });
                    It("should forward that method onto the extension", () -> {
                        verify(testExtensionService).someMethod();
                    });
                });
            });
        });
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public TestContentRepositoryExtension textExtensionService() {
            return spy(new TestContentRepositoryExtension());
        }

        @Bean
        public Class<? extends Store<Serializable>> storeInterface() {
            return TestContentRepository.class;
        }

        @Bean
        public AbstractStoreFactoryBean contentStoreFactory() {
            return new TestContentStoreFactory();
        }
    }

    public static class TestContentStoreFactory extends AbstractStoreFactoryBean {
        @Override
        protected Object getContentStoreImpl() {
            return new TestConfigStoreImpl();
        }
    }

    public static class TestConfigStoreImpl implements ContentStore<Object,Serializable> {

		@Override public void setContent(Object property, InputStream content) {}

		@Override public void unsetContent(Object property) {}

		@Override public InputStream getContent(Object property) { return null; }

    }
    
    public interface TestContentRepository extends Store<Serializable>, ContentStore<Object, Serializable>, TestExtensionService {
    }

    public interface TestExtensionService {
        Object someMethod();
    }

    public static class TestContentRepositoryExtension implements TestExtensionService, StoreExtension {

        @Override
        public Object someMethod() {
            return null;
        }

        @Override
        public Set<Method> getMethods() {
            Set<Method> methods = new HashSet<>();
            try {
                methods.add(TestExtensionService.class.getMethod("someMethod",new Class<?>[]{}));
            } catch (NoSuchMethodException e) {
                Assert.fail();
            }
            return methods;
        }

        @Override
        public Object invoke(MethodInvocation invocation, StoreInvoker invoker) {
            return ReflectionUtils.invokeMethod(invocation.getMethod(), this, null);
        }
    }

    @Test
    public void noop() {}
}
