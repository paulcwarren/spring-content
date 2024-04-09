package internal.org.springframework.content.commons.store.factory;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.commons.config.StoreFragment;
import internal.org.springframework.content.commons.config.StoreFragments;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.AfterStoreEvent;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.events.*;
import org.springframework.content.commons.store.AssociativeStore;
import org.springframework.content.commons.store.Store;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.security.util.SimpleMethodInvocation;
import org.springframework.util.ReflectionUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.internal.verification.VerificationModeFactory.times;

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
	private static Method setContentFromResourceMethod;
	private static Method unsetContentMethod;
	private static Method toStringMethod;

	static {
		getResourceMethod = ReflectionUtils.findMethod(Store.class, "getResource", Serializable.class);
		getResourceEntityMethod = ReflectionUtils.findMethod(AssociativeStore.class, "getResource", Object.class);
		associateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "associate", Object.class, Serializable.class);
		unassociateMethod = ReflectionUtils.findMethod(AssociativeStore.class, "unassociate", Object.class);
		getContentMethod = ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class);
		setContentMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class);
		setContentFromResourceMethod = ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class);
		unsetContentMethod = ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class);
		toStringMethod = ReflectionUtils.findMethod(Object.class, "toString");
	}

	private StoreMethodInterceptor interceptor;

	// mocks
	private ContentStore<Object, Serializable> store;
	private MethodInvocation invocation;
	private ApplicationEventPublisher publisher;

	private Object result;
	private Exception e;

	private ByteArrayInputStream modifiedStream = null;

	{
		Describe("#invoke", () -> {

			BeforeEach(() -> {
				store = mock(ContentStore.class);
				publisher = mock(ApplicationEventPublisher.class);
			});

			JustBeforeEach(() -> {
				interceptor = new StoreMethodInterceptor();
				StoreFragments fragments = new StoreFragments(Collections.singletonList(new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir"))))));
				interceptor.setStoreFragments(fragments);
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

					ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher, times(1)).publishEvent(argThat(isA(StoreEvent.class)));
					inOrder.verify(store).getContent(anyObject());
					inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
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
					when(store.setContent(anyObject(), any(InputStream.class))).thenReturn(result);

					invocation = new TestMethodInvocation(store, setContentMethod, new Object[]{new Object(), new ByteArrayInputStream("test".getBytes())});
				});

				It("should proceed", () -> {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
					ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
					assertThat(beforeArgCaptor.getValue().getResource(), is(nullValue()));
					assertThat(beforeArgCaptor.getValue().getInputStream(), is(not(nullValue())));

					inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());
					try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
				});

				Context("when the BeforeSetContentEvent consumes the entire inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							try (InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getInputStream()) {
								assertThat(IOUtils.toString(is), is("test"));
							}
							return null;
						});
					});

					It("should still receive the inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());
						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
						assertThat(afterArgCaptor.getValue().getResult(), is(result));
					});
				});

				Context("when the BeforeSetContentEvent consumes partial inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							InputStream is = ((BeforeSetContentEvent)invocationOnMock.getArgument(0)).getInputStream();
							assertThat((char)is.read(), is('t'));
							assertThat((char)is.read(), is('e'));
							return null;
						});
					});

					It("should still receive the inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						InOrder inOrder = Mockito.inOrder(publisher, store);

						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());

						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
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
						ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());

						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(IOUtils.toString(setContentInputStream), is("test"));
						}

						inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
						assertThat(afterArgCaptor.getValue().getResult(), is(result));
					});
				});

				Context("when the BeforeSetContentEvent replaces the inputstream", () -> {

					BeforeEach(() -> {
						onBeforeSetContentPublishEvent((invocationOnMock) -> {
							modifiedStream = new ByteArrayInputStream("encrypted".getBytes());
							((BeforeSetContentEvent)invocationOnMock.getArgument(0)).setInputStream(modifiedStream);
							return null;
						});
					});

					It("should still receive the replaced inputstream in the setContent invocation", () -> {
						assertThat(e, is(nullValue()));

						InOrder inOrder = Mockito.inOrder(publisher, store);

						ArgumentCaptor<InputStream> setContentArgCaptor = ArgumentCaptor.forClass(InputStream.class);
						ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);

						inOrder.verify(publisher).publishEvent(argThat(isA(BeforeSetContentEvent.class)));

						inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());

						try (InputStream setContentInputStream = setContentArgCaptor.getValue()) {
							assertThat(setContentInputStream, is(modifiedStream));
						}

						inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
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

			Describe("#setContent from Resource", () -> {

				BeforeEach(() -> {

					result = new Object();

					store = mock(ContentStore.class);
					when(store.setContent(anyObject(), any(Resource.class))).thenReturn(result);

					invocation = new TestMethodInvocation(store, setContentFromResourceMethod, new Object[]{new Object(), new InputStreamResource(new ByteArrayInputStream("test".getBytes()))});
				});

				It("should proceed", () -> {
					assertThat(e, is(nullValue()));

					ArgumentCaptor<BeforeSetContentEvent> beforeArgCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
					ArgumentCaptor<Resource> setContentArgCaptor = ArgumentCaptor.forClass(Resource.class);
					ArgumentCaptor<AfterStoreEvent> afterArgCaptor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher, times(1)).publishEvent(beforeArgCaptor.capture());
					assertThat(beforeArgCaptor.getValue().getResource(), is(not(nullValue())));
					assertThat(beforeArgCaptor.getValue().getInputStream(), is(nullValue()));

					inOrder.verify(store).setContent(anyObject(), setContentArgCaptor.capture());
					try (InputStream setContentInputStream = setContentArgCaptor.getValue().getInputStream()) {
						assertThat(IOUtils.toString(setContentInputStream), is("test"));
					}

					inOrder.verify(publisher, times(1)).publishEvent(afterArgCaptor.capture());
					assertThat(afterArgCaptor.getValue().getResult(), is(result));
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

					InOrder inOrder = Mockito.inOrder(publisher, store);

					inOrder.verify(publisher).publishEvent(argThat(isA(BeforeUnsetContentEvent.class)));
					inOrder.verify(store).unsetContent(anyObject());

					ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
					inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
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
						when(store.getResource(any(Serializable.class))).thenReturn((Resource)result);

						invocation = new TestMethodInvocation(store, getResourceMethod, new Serializable(){});
					});

					It("should proceed", () -> {
						assertThat(e, is(nullValue()));

						ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher, times(1)).publishEvent(argThat(instanceOf(StoreEvent.class)));
						verify(store).getResource(any(Serializable.class));
						inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
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


						InOrder inOrder = Mockito.inOrder(publisher, store);

						inOrder.verify(publisher).publishEvent(argThat(instanceOf(BeforeGetResourceEvent.class)));
						inOrder.verify(store).getResource(argThat(isA(ContentObject.class)));

						ArgumentCaptor<AfterStoreEvent> captor = ArgumentCaptor.forClass(AfterStoreEvent.class);
						inOrder.verify(publisher, times(1)).publishEvent(captor.capture());
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
						inOrder.verify(store).associate(eq(""), eq(123));
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
		});

		Describe("#findMethod", () -> {

			It("should resolve the method when not overridden", () -> {
				store = mock(ContentStore.class);
				publisher = mock(ApplicationEventPublisher.class);
				interceptor = new StoreMethodInterceptor();
				try {
					Method m = ReflectionUtils.findMethod(TestContentStore.class, "unsetContent", Object.class);
					assertThat(m, is(not(nullValue())));
					Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
					assertThat(actual, is(ReflectionUtils.findMethod(StoreImpl.class, "unsetContent", Object.class)));
				}
				catch (Exception invokeException) {
					e = invokeException;
				}
			});

			It("should resolve the method when it is overridden in the interface", () -> {
				store = mock(ContentStore.class);
				publisher = mock(ApplicationEventPublisher.class);
				interceptor = new StoreMethodInterceptor();
				try {
					Method m = ReflectionUtils.findMethod(TestContentStore.class, "setContent", TEntity.class, InputStream.class);
					assertThat(m, is(not(nullValue())));
					Method actual = interceptor.getMethod(m, new StoreFragment(TestContentStore.class, new StoreImpl(TestContentStore.class, store, publisher, Paths.get(System.getProperty("java.io.tmpdir")))));
					assertThat(actual, is(ReflectionUtils.findMethod(StoreImpl.class, "setContent", Object.class, InputStream.class)));
				}
				catch (Exception invokeException) {
					e = invokeException;
				}
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

	@Getter
	@Setter
	public class TEntity {

		private UUID contentId;
	}

	public interface TestContentStore extends ContentStore<TEntity, UUID> {
		@Override
		TEntity setContent(TEntity property, InputStream content);
	}
}
