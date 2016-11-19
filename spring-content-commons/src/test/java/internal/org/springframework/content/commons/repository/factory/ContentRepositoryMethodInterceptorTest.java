package internal.org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.ContentStore;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ContentRepositoryMethodInterceptorTest {

	private ContentRepositoryMethodInteceptor interceptor;
	
	// mocks
	private MethodInvocation invocation;
	private RenditionService renditions;
	
	{
		Describe("ContentRepositoryMethodInterceptor", () -> {
			JustBeforeEach(() -> {
				try {
					interceptor = new ContentRepositoryMethodInteceptor(renditions);
					interceptor.invoke(invocation);
				} catch (Throwable e) {
					fail(e.getMessage());
				}
			});
			Context("when the method invoked is getContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					renditions = mock(RenditionService.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent", Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);
				});
				It("should proceed", () -> {
					try {
						verify(invocation).proceed();
					} catch (Throwable e) {
						fail(e.getMessage());
					}
				});
			});
			Context("when the method invoked is setContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					renditions = mock(RenditionService.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent", Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);
				});
				It("should proceed", () -> {
					try {
						verify(invocation).proceed();
					} catch (Throwable e) {
						fail(e.getMessage());
					}
				});
			});
			Context("when the method invoked is unsetContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					renditions = mock(RenditionService.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent", Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);
				});
				It("should proceed", () -> {
					try {
						verify(invocation).proceed();
					} catch (Throwable e) {
						fail(e.getMessage());
					}
				});
			});
		});
	}
}
