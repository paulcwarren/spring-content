package internal.org.springframework.content.commons.repository.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import lombok.EqualsAndHashCode;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
import org.springframework.core.io.Resource;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class StoreMethodInterceptorTest {

	private static Method getResourceMethod;
	private static Method getResourceEntityMethod;
	private static Method associateMethod;
	private static Method unassociateMethod;
	private static Method getContentMethod;
	private static Method setContentMethod;
	private static Method unsetContentMethod;
	private static Method toStringMethod;

	static {
		getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource", Serializable.class);
		getResourceEntityMethod = ReflectionUtils.findMethod(AssociativeStore.class, "getResource", Object.class);
		associateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
		unassociateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "unassociate", Object.class);
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
		toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
	}

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

			Describe("#getContent", () -> {

				BeforeEach(() -> {

					result = new ByteArrayInputStream(new byte[]{});

					store = mock(ContentStore.class);
					when(store.getContent(anyObject())).thenReturn((InputStream)result);

					invocation = new TestMethodInvocation(store, getContentMethod, new Object[]{new Object()});
				});

				It("should proceed", () -> {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<AfterGetContentEvent> captor = ArgumentCaptor.forClass(AfterGetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeGetContentEvent.class)));
					inOrder.verify(store).getContent(anyObject());
					inOrder.verify(publisher).publishEvent(captor.capture());
					assertThat(captor.getValue().getResult(), is(result));
				});

				Context("when getContent is invoked with illegal arguments", () -> {

					BeforeEach(() -> {
						invocation = new TestMethodInvocation(store, getContentMethod, new Object[]{});
					});

					It("should proceed", () -> {
						assertThat(e, is(not(nullValue())));
					});
				});
			});

			Describe("#setContent", () -> {

				BeforeEach(() -> {

					result = new Object();

					store = mock(ContentStore.class);
					when(store.setContent(anyObject(), anyObject())).thenReturn(result);

					invocation = new TestMethodInvocation(store, setContentMethod, new Object[]{new Object(), new ByteArrayInputStream("test".getBytes())});
				});

				It("should proceed", () -> {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterSetContentEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterSetContentEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

					inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());
					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				});

				Context("when the BeforeSetContentEvent consumes the entire inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							try (InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getIs()) {
								assertThat(IOUtils.toString(is), is("test"));
							}
							return null;
						});
					});

					It("should still receive the inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterSetContentEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterSetContentEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());
						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
						assertThat(afterArgCaptor.getValue().getResult(), is(result));
					});
				});

				Context("when the BeforeSetContentEvent consumes partial inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getIs();
							assertThat((char)is.read(), is('t'));
							assertThat((char)is.read(), is('e'));
							return null;
						});
					});

					It("should still receive the inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						InOrder inOrder = Mockito.inOrder(publisher, store);

						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterSetContentEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterSetContentEvent.class);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());

						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
						assertThat(afterArgCaptor.getValue().getResult(), is(result));
					});
				});

				Context("when the BeforeSetContentEvent does not consume any of the inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							return null;
						});
					});

					It("should still receive the inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						InOrder inOrder = Mockito.inOrder(publisher, store);

						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterSetContentEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterSetContentEvent.class);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());

						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher).publishEvent(afterArgCaptor.capture());
						assertThat(afterArgCaptor.getValue().getResult(), is(result));
					});
				});

				Context("when setContent is invoked with illegal arguments", () -> {

					BeforeEach(() -> {
						invocation = new TestMethodInvocation(store, setContentMethod, new Object[]{});
					});

					It("should proceed", () -> {
						assertThat(e, is(not(nullValue())));
					});
				});
			});

			Describe("#unsetContent", () -> {

				BeforeEach(() -> {

					result = new Object();
					store = mock(ContentStore.class);
					when(store.unsetContent(anyObject())).thenReturn(result);

					invocation = new TestMethodInvocation(store, unsetContentMethod, new Object[]{new Object()});
				});

				Context("when unsetContent is invoked", () -> {

					It("should proceed", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<AfterUnsetContentEvent> captor = ArgumentCaptor.forClass(AfterUnsetContentEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeUnsetContentEvent.class)));

						verify(store).unsetContent(anyObject());

						inOrder.verify(publisher).publishEvent(captor.capture());
						assertThat(captor.getValue().getResult(), is(result));
					});
				});

				Context("when unsetContent is invoked with illegal arguments", () -> {

					BeforeEach(() -> {
						invocation = new TestMethodInvocation(store, unsetContentMethod, new Object[]{});
					});

					It("should not publish events", () -> {
						assertThat(e, is(not(nullValue())));
					});
				});
			});

			Describe("#getResource", () -> {

				Context("when getResource is invoked", () -> {

					BeforeEach(() -> {
						result = mock(Resource.class);

						store = mock(ContentStore.class);
						when(store.getResource(anyObject())).thenReturn((Resource)result);

						invocation = new TestMethodInvocation(store, getResourceMethod, new Object[]{""});
					});

					It("should proceed", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<AfterGetResourceEvent> captor = ArgumentCaptor.forClass(AfterGetResourceEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
						verify(store).getResource(anyObject());
						inOrder.verify(publisher).publishEvent(captor.capture());
						assertThat(captor.getValue().getResult(), is(result));
					});
				});

				Context("when getResource(entity) is invoked", () -> {

					BeforeEach(() -> {
						result = mock(Resource.class);

						store = mock(ContentStore.class);
						when(store.getResource(argThat(isA(ContentObject.class)))).thenReturn((Resource)result);

						invocation = new TestMethodInvocation(store, getResourceEntityMethod, new Object[]{new ContentObject("text/plain")});
					});

					It("should proceed", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<AfterGetResourceEvent> captor = ArgumentCaptor.forClass(AfterGetResourceEvent.class);

						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
						verify(store).getResource(argThat(isA(ContentObject.class)));
						inOrder.verify(publisher).publishEvent(captor.capture());
						assertThat(captor.getValue().getResult(), is(result));
					});
				});
			});

			Describe("#associate", () -> {

				BeforeEach(() -> {
					result = mock(Resource.class);

					store = mock(ContentStore.class);

					invocation = new TestMethodInvocation(store, associateMethod, new Object[]{"", 123});
				});

				Context("when associate is invoked", () -> {

					It("should proceed", () -> {
						ArgumentCaptor<AfterAssociateEvent> captor = ArgumentCaptor.forClass(AfterAssociateEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeAssociateEvent.class)));
						verify(store).associate(eq(""), eq(123));
						inOrder.verify(publisher).publishEvent(argThat(instanceOf(AfterAssociateEvent.class)));
					});
				});
			});

			Describe("#unassociate", () -> {

				BeforeEach(() -> {
					result = mock(Resource.class);

					store = mock(ContentStore.class);

					invocation = new TestMethodInvocation(store, unassociateMethod, new Object[]{"foo"});
				});

				Context("when unassociate is invoked", () -> {

					It("should proceed", () -> {
						ArgumentCaptor<AfterUnassociateEvent> captor = ArgumentCaptor.forClass(AfterUnassociateEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeUnassociateEvent.class)));
						verify(store).unassociate("foo");
						inOrder.verify(publisher).publishEvent(argThat(instanceOf(AfterUnassociateEvent.class)));
					});
				});
			});

			Describe("#toString", () -> {

				BeforeEach(() -> {
					result = mock(Resource.class);

					store = mock(ContentStore.class);

					invocation = new TestMethodInvocation(store, toStringMethod, new Object[]{});
				});

				Context("when toString is invoked", () -> {

					It("should proceed", () -> {
						verify(publisher, never()).publishEvent(anyObject());
					});
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

	private void onBeforeSetContentPublishEvent(PublishEventAction action) {
		doAnswer(new Answer(){
			@Override
			public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
				return action.doAction(invocationOnMock);
			}
		}).when(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));
	}

	public interface PublishEventAction {
		Object doAction(InvocationOnMock invocationOnMock) throws Exception;
	}

	public static class TestMethodInvocation extends SimpleMethodInvocation {

		TestMethodInvocation(Object targetObject, Method method, Object... arguments) {
			super(targetObject, method, arguments);
		}

		@Override
		public Object proceed() {
			return ReflectionUtils.invokeMethod(getMethod(), getThis(), getArguments());
		}
	}

	@EqualsAndHashCode
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
