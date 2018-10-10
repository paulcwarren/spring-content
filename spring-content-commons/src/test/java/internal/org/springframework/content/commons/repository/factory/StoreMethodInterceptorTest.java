package internal.org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.events.AfterAssociateEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnassociateEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeAssociateEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnassociateEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.springframework.core.io.Resource;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class StoreMethodInterceptorTest {

	private StoreMethodInterceptor interceptor;

	// mocks
	private ContentStore<Object, Serializable> store;
	private MethodInvocation invocation;
	private StoreExtension extension;
	private ApplicationEventPublisher publisher;

	private Object result;
	private Exception e;

	private Map<Method, StoreExtension> extensions = null;

	{
		Describe("#invoke", () -> {
			BeforeEach(() -> {
				store = mock(ContentStore.class);
				publisher = mock(ApplicationEventPublisher.class);
			});
			JustBeforeEach(() -> {
				interceptor = new StoreMethodInterceptor(store, Object.class,
						String.class, extensions, publisher);
				try {
					interceptor.invoke(invocation);
				}
				catch (Exception invokeException) {
					e = invokeException;
				}
			});
			Context("when getContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent",
							Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { new ContentObject("plain/text") });

					result = mock(Resource.class);
					when(invocation.proceed()).thenReturn(result);
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterGetContentEvent> captor = ArgumentCaptor.forClass(AfterGetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher)
							.publishEvent(argThat(isA(BeforeGetContentEvent.class)));
					inOrder.verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when getContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method getContentMethod = storeClazz.getMethod("getContent",
							Object.class);

					when(invocation.getMethod()).thenReturn(getContentMethod);

					// no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher, never())
							.publishEvent(argThat(isA(BeforeGetContentEvent.class)));
					inOrder.verify(invocation).proceed();
					inOrder.verify(publisher, never())
							.publishEvent(argThat(isA(AfterGetContentEvent.class)));
				});
			});
			Context("when setContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent",
							Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { new ContentObject("plain/text") });

					result = mock(Resource.class);
					when(invocation.proceed()).thenReturn(result);
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterSetContentEvent> captor = ArgumentCaptor.forClass(AfterSetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher)
							.publishEvent(argThat(isA(BeforeSetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when setContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method setContentMethod = storeClazz.getMethod("setContent",
							Object.class, InputStream.class);

					when(invocation.getMethod()).thenReturn(setContentMethod);

					// no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should proceed", () -> {
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher, never())
							.publishEvent(argThat(isA(BeforeSetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher, never())
							.publishEvent(argThat(isA(AfterSetContentEvent.class)));
				});
			});
			Context("when unsetContent is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent",
							Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { new ContentObject("plain/text") });
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterUnsetContentEvent> captor = ArgumentCaptor.forClass(AfterUnsetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher)
							.publishEvent(argThat(isA(BeforeUnsetContentEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when unsetContent is invoked with illegal arguments", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = ContentStore.class;
					final Method unsetContentMethod = storeClazz.getMethod("unsetContent",
							Object.class);

					when(invocation.getMethod()).thenReturn(unsetContentMethod);

					// no arguments!
					when(invocation.getArguments()).thenReturn(new Object[] {});
				});
				It("should not publish events", () -> {
					ArgumentCaptor<AfterUnsetContentEvent> captor = ArgumentCaptor.forClass(AfterUnsetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher, never()).publishEvent(anyObject());
					verify(invocation).proceed();
					inOrder.verify(publisher, never()).publishEvent(anyObject());
				});
			});
			Context("when getResource is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = Store.class;
					final Method getResourceMethod = storeClazz.getMethod("getResource",
							Serializable.class);

					when(invocation.getMethod()).thenReturn(getResourceMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { "some-id" });

					result = mock(Resource.class);
					when(invocation.proceed()).thenReturn(result);
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterGetResourceEvent> captor = ArgumentCaptor.forClass(AfterGetResourceEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when getResource(entity) is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = AssociativeStore.class;
					final Method getResourceMethod = storeClazz.getMethod("getResource",
							Object.class);

					when(invocation.getMethod()).thenReturn(getResourceMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { "some-id" });

					result = mock(Resource.class);
					when(invocation.proceed()).thenReturn(result);
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterGetResourceEvent> captor = ArgumentCaptor.forClass(AfterGetResourceEvent.class);

					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when associate is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = AssociativeStore.class;
					final Method getResourceMethod = storeClazz.getMethod("associate",
							Object.class, Serializable.class);

					when(invocation.getMethod()).thenReturn(getResourceMethod);
					when(invocation.getArguments()).thenReturn(
							new Object[] { new ContentObject("plain/text"), "some-id" });
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterAssociateEvent> captor = ArgumentCaptor.forClass(AfterAssociateEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeAssociateEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when unassociate is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = AssociativeStore.class;
					final Method getResourceMethod = storeClazz.getMethod("unassociate",
							Object.class);

					when(invocation.getMethod()).thenReturn(getResourceMethod);
					when(invocation.getArguments())
							.thenReturn(new Object[] { new ContentObject("plain/text") });
				});
				It("should proceed", () -> {
					ArgumentCaptor<AfterUnassociateEvent> captor = ArgumentCaptor.forClass(AfterUnassociateEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, invocation);

					inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeUnassociateEvent.class)));
					verify(invocation).proceed();
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});
			});
			Context("when toString is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);

					Class<?> storeClazz = Object.class;
					final Method toStringMethod = storeClazz.getMethod("toString");

					when(invocation.getMethod()).thenReturn(toStringMethod);
				});
				It("should proceed", () -> {
					verify(publisher, never()).publishEvent(anyObject());
					verify(invocation).proceed();
				});
			});
			Context("when an extension method is invoked", () -> {
				BeforeEach(() -> {
					invocation = mock(MethodInvocation.class);
					extension = mock(StoreExtension.class);

					extensions = Collections
							.singletonMap(AContentRepositoryExtension.class.getMethod(
									"getCustomContent", Object.class), extension);

					final Method getCustomMethod = AContentRepositoryExtension.class
							.getMethod("getCustomContent", Object.class);
					when(invocation.getMethod()).thenReturn(getCustomMethod);
					when(invocation.getArguments()).thenReturn(
							new Object[] { new ContentObject("application/pdf") });
				});
				Context("when an extension implementation is provided", () -> {
					It("should invoke the extension's implementation", () -> {
						verify(extension).invoke(eq(invocation), anyObject());
					});
					It("should never proceed with the real invocation", () -> {
						verify(invocation, never()).proceed();
						verify(publisher, never()).publishEvent(anyObject());
					});
				});
				Context("when the extension implementation is not provided", () -> {
					BeforeEach(() -> {
						extensions = new HashMap<>();
					});
					It("should throw a Missing Extension error", () -> {
						assertThat(e.getMessage(),
								startsWith("No implementation found for"));
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
