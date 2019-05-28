package org.springframework.content.commons.repository.factory;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
//@ContextConfiguration(classes = StoreExtensionTest.TestConfiguration.class)
public class StoreExtensionTest {

// TODO: comment this test back in when we finish refactoring the store extension mechanism

//	@Autowired
//	private TestContentRepository repo;
//
//	@Autowired
//	private TestContentRepositoryExtension testExtensionService;
//
//	// mocks/spys
//	private TestContentRepositoryExtension spy;
//
//	{
//		Describe("AbstractContentStoreFactoryBean", () -> {
//			Context("given a repository extension bean", () -> {
//				Context("given an extension method is invoked on the proxy", () -> {
//					BeforeEach(() -> {
//						repo.someMethod();
//					});
//					It("should forward that method onto the extension", () -> {
//						verify(testExtensionService).someMethod();
//					});
//				});
//			});
//		});
//	}
//
//	@Configuration
//	public static class TestConfiguration {
//
//		@Bean
//		public TestContentRepositoryExtension textExtensionService() {
//			return spy(new TestContentRepositoryExtension());
//		}
//
//		@Bean
//		public Class<? extends Store<Serializable>> storeInterface() {
//			return TestContentRepository.class;
//		}
//
//		@Bean
//		public AbstractStoreFactoryBean contentStoreFactory() {
//			return new TestContentStoreFactory();
//		}
//	}
//
//	public static class TestContentStoreFactory extends AbstractStoreFactoryBean {
//		@Override
//		protected Object getContentStoreImpl() {
//			return new TestConfigStoreImpl();
//		}
//	}
//
//	public static class TestConfigStoreImpl
//			implements ContentStore<Object, Serializable> {
//
//		@Override
//		public void setContent(Object property, InputStream content) {
//		}
//
//		@Override
//		public void unsetContent(Object property) {
//		}
//
//		@Override
//		public InputStream getContent(Object property) {
//			return null;
//		}
//
//	}
//
//	public interface TestContentRepository extends Store<Serializable>,
//			ContentStore<Object, Serializable>, TestExtensionService {
//	}
//
//	public interface TestExtensionService {
//		Object someMethod();
//	}
//
//	public static class TestContentRepositoryExtension
//			implements TestExtensionService, StoreExtension {
//
//		@Override
//		public Object someMethod() {
//			return null;
//		}
//
//		@Override
//		public Set<Method> getMethods() {
//			Set<Method> methods = new HashSet<>();
//			try {
//				methods.add(TestExtensionService.class.getMethod("someMethod",
//						new Class<?>[] {}));
//			}
//			catch (NoSuchMethodException e) {
//				Assert.fail();
//			}
//			return methods;
//		}
//
//		@Override
//		public Object invoke(MethodInvocation invocation, StoreInvoker invoker) {
//			return ReflectionUtils.invokeMethod(invocation.getMethod(), this, null);
//		}
//	}

	@Test
	public void noop() {
	}
}
