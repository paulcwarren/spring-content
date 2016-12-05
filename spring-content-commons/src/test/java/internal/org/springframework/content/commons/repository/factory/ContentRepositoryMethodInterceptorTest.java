package internal.org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentStore;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ContentRepositoryMethodInterceptorTest {

	private ContentRepositoryMethodInteceptor interceptor;
	
	// mocks
	private MethodInvocation invocation;
	private ContentRepositoryExtension extension;
	
	private Map<Method, ContentRepositoryExtension> extensions = null;
	
	{
		Describe("ContentRepositoryMethodInterceptor", () -> {
			JustBeforeEach(() -> {
				interceptor = new ContentRepositoryMethodInteceptor(extensions);
				interceptor.invoke(invocation);
			});
			Context("when the method invoked is getContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent", Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);
				});
				It("should proceed", () -> {
					verify(invocation).proceed();
				});
			});
			Context("when the method invoked is setContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent", Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);
				});
				It("should proceed", () -> {
					verify(invocation).proceed();
				});
			});
			Context("when the method invoked is unsetContent", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent", Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);
				});
				It("should proceed", () -> {
					verify(invocation).proceed();
				});
			});
			Context("when an extension method is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					extension = mock(ContentRepositoryExtension.class);
					
					extensions = Collections.singletonMap(AContentRepositoryExtension.class.getMethod("getCustomContent", Object.class), extension);
					
					final Method getCustomMethod = AContentRepositoryExtension.class.getMethod("getCustomContent", Object.class);
					when(invocation.getMethod()).thenReturn(getCustomMethod);
					when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("application/pdf")});
				});
				Context("when an extension implementation is available", () -> {
					It("should invoke the extension's implementation", () -> {
						verify(extension).invoke(eq(invocation), anyObject());
					});
					It("should never proceed with the real invocation", () -> {
						verify(invocation, never()).proceed();
					});
				}); 
			});
		});
	}
	
	public static class ContentObject {
		@MimeType
		public String mimeType;
		
		public ContentObject(String mimeType) {
			this.mimeType = mimeType; 
		}
	}
	
	public interface AContentRepositoryExtension<S> {
		void getCustomContent(S property);
	}
}
