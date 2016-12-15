package internal.org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.isA;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.ContentRepositoryExtension;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ContentRepositoryMethodInterceptorTest {

	private ContentRepositoryMethodInteceptor interceptor;
	
	// mocks
	private MethodInvocation invocation;
	private ContentRepositoryExtension extension;
	private ApplicationEventPublisher publisher;
	
	private Map<Method, ContentRepositoryExtension> extensions = null;
	
	{
		Describe("#invoke", () -> {
			BeforeEach(() -> {
				publisher = mock(ApplicationEventPublisher.class);
			});
			JustBeforeEach(() -> {
				interceptor = new ContentRepositoryMethodInteceptor(extensions, publisher);
				interceptor.invoke(invocation);
			});
			Context("when getContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent", Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);
					when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("plain/text")});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeGetContentEvent.class)));
					inOrder.verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(argThat(isA(AfterGetContentEvent.class)));
				});
			});
			Context("when getContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent", Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);
					
					//no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher, never()).publishEvent(argThat(isA(BeforeGetContentEvent.class)));
					inOrder.verify(invocation).proceed();
					inOrder.verify(publisher, never()).publishEvent(argThat(isA(AfterGetContentEvent.class)));
				});
			});
			Context("when setContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent", Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);
					when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("plain/text")});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(argThat(isA(AfterSetContentEvent.class)));
				});
			});
			Context("when setContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent", Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);
					
					// no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher, never()).publishEvent(argThat(isA(BeforeSetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher, never()).publishEvent(argThat(isA(AfterSetContentEvent.class)));
				});
			});
			Context("when unsetContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent", Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);
					when(invocation.getArguments()).thenReturn(new Object[] {new ContentObject("plain/text")});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeUnsetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(argThat(isA(AfterUnsetContentEvent.class)));
				});
			});
			Context("when unsetContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					
					Class<?> storeClazz  = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent", Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);
					
					// no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should not publish events", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);
					
					inOrder.verify(publisher, never()).publishEvent(anyObject());
					verify(invocation).proceed();
					inOrder.verify(publisher, never()).publishEvent(anyObject());
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
						verify(publisher, never()).publishEvent(anyObject());
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
