package internal.org.springframework.content.commons.repository;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.hamcrest.MockitoHamcrest;
import org.springframework.content.commons.annotations.HandleAfterAssociate;
import org.springframework.content.commons.annotations.HandleAfterGetResource;
import org.springframework.content.commons.annotations.HandleAfterUnassociate;
import org.springframework.content.commons.annotations.HandleBeforeAssociate;
import org.springframework.content.commons.annotations.HandleBeforeGetResource;
import org.springframework.content.commons.annotations.HandleBeforeUnassociate;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;
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
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AnnotatedStoreEventInvokerTest {

	private AnnotatedStoreEventInvoker invoker;

	private StoreEvent event;

	// mocks
	private ReflectionService reflectionService;
	private ContentStore<Object, Serializable> store;

	// event handlers
	private HighestPriorityCustomEventHandler priorityHandler = new HighestPriorityCustomEventHandler();

	{
		Describe("#postProcessAfterInitialization", () -> {
			Context("when initialized with a StoreEventHandler bean", () -> {
				BeforeEach(() -> {
					store = mock(ContentStore.class);
				});
				JustBeforeEach(() -> {
					reflectionService = mock(ReflectionService.class);
					invoker = new AnnotatedStoreEventInvoker(reflectionService);
					invoker.postProcessAfterInitialization(new CustomEventHandler(),
							"custom-bean");
				});
				It("register the handlers", () -> {
					assertThat(
							invoker.getHandlers().get(BeforeGetResourceEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterGetResourceEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(BeforeAssociateEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterAssociateEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(BeforeUnassociateEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterUnassociateEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(BeforeGetContentEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterGetContentEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(BeforeSetContentEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterSetContentEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(BeforeUnsetContentEvent.class).size(),
							is(2));
					assertThat(
							invoker.getHandlers().get(AfterUnsetContentEvent.class) .size(),
							is(2));
				});

				Context("when initialized with another event handler of highest priority", () -> {
					JustBeforeEach(() -> {
						invoker.postProcessAfterInitialization(priorityHandler,
								"high-priority-custom-bean");
					});
					FIt("should order the handlers by priority", () -> {
						assertThat(
								invoker.getHandlers().get(BeforeGetResourceEvent.class).size(),
								is(3));

						assertThat(invoker.getHandlers().get(BeforeGetResourceEvent.class).get(0).handler, is(priorityHandler));
					});
				});
			});
		});

		Describe("#onApplicationEvent", () -> {
			BeforeEach(() -> {
				reflectionService = mock(ReflectionService.class);
				invoker = new AnnotatedStoreEventInvoker(reflectionService);
			});
			JustBeforeEach(() -> {
				invoker.postProcessAfterInitialization(new CustomEventHandler(),
						"custom-bean");
				invoker.onApplicationEvent(event);
			});
			Context("given an event handler and a BeforeGetResource event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeGetResourceEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeGetResource", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeGetResource event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeGetResourceEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeGetResource", BeforeGetResourceEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a AfterGetResource event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterGetResourceEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterGetResource", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterGetResource event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterGetResourceEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterGetResource", AfterGetResourceEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeAssociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeAssociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeAssociate", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeAssociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeAssociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeAssociate", BeforeAssociateEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a AfterAssociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterAssociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterAssociate", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterAssociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterAssociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterAssociate", AfterAssociateEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeUnassociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeUnassociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeUnassociate", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeUnassociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeUnassociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeUnassociate", BeforeUnassociateEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a AfterUnassociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterUnassociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterUnassociate", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterUnassociate event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterUnassociateEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterUnassociate", AfterUnassociateEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeGetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeGetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeGetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeGetContent", BeforeGetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a AfterGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterGetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterGetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterGetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterGetContent", AfterGetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeSetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeSetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeSetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeSetContent", BeforeSetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterSetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterSetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterSetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterSetContent", AfterSetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a BeforeUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeUnsetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a BeforeUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeUnsetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"beforeUnsetContent", BeforeUnsetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and a AfterUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterUnsetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event.getSource())));
				});
			});
			Context("given an event handler accepting the event and a AfterUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterUnsetContentEvent(source, store);
				});
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterUnsetContent", AfterUnsetContentEvent.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService).invokeMethod(argThat(is(handler)),
							argThat(isA(CustomEventHandler.class)),
							argThat(is(event)));
				});
			});
			Context("given an event handler and an unknown event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new UnknownContentEvent(source, store);
				});
				It("should not call an event handler", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class,
							"afterUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));

					verify(reflectionService, never()).invokeMethod(anyObject(),
							anyObject(), anyObject());
				});
			});
		});
	}

	@StoreEventHandler
	public class CustomEventHandler {

		@HandleBeforeGetResource
		public void beforeGetResource(Object contentObject) {
		}

		@HandleBeforeGetResource
		public void beforeGetResource(BeforeGetResourceEvent event) {
		}

		@HandleAfterGetResource
		public void afterGetResource(Object contentObject) {
		}

		@HandleAfterGetResource
		public void afterGetResource(AfterGetResourceEvent event) {
		}

		@HandleBeforeAssociate
		public void beforeAssociate(Object contentObject) {
		}

		@HandleBeforeAssociate
		public void beforeAssociate(BeforeAssociateEvent event) {
		}

		@HandleAfterAssociate
		public void afterAssociate(Object contentObject) {
		}

		@HandleAfterAssociate
		public void afterAssociate(AfterAssociateEvent event) {
		}

		@HandleBeforeUnassociate
		public void beforeUnassociate(Object contentObject) {
		}

		@HandleBeforeUnassociate
		public void beforeUnassociate(BeforeUnassociateEvent event) {
		}

		@HandleAfterUnassociate
		public void afterUnassociate(Object contentObject) {
		}

		@HandleAfterUnassociate
		public void afterUnassociate(AfterUnassociateEvent event) {
		}

		@HandleBeforeGetContent
		public void beforeGetContent(Object contentObject) {
		}

		@HandleBeforeGetContent
		public void beforeGetContent(BeforeGetContentEvent event) {
		}

		@HandleAfterGetContent
		public void afterGetContent(Object contentObject) {
		}

		@HandleAfterGetContent
		public void afterGetContent(AfterGetContentEvent event) {
		}

		@HandleBeforeSetContent
		public void beforeSetContent(Object contentObject) {
		}

		@HandleBeforeSetContent
		public void beforeSetContent(BeforeSetContentEvent event) {
		}

		@HandleAfterSetContent
		public void afterSetContent(Object contentObject) {
		}

		@HandleAfterSetContent
		public void afterSetContent(AfterSetContentEvent event) {
		}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(Object contentObject) {
		}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(BeforeUnsetContentEvent event) {
		}

		@HandleAfterUnsetContent
		public void afterUnsetContent(Object contentObject) {
		}

		@HandleAfterUnsetContent
		public void afterUnsetContent(AfterUnsetContentEvent event) {
		}
	}

	@StoreEventHandler
	public class HighestPriorityCustomEventHandler {

		@HandleBeforeGetResource
		@Order(Ordered.HIGHEST_PRECEDENCE)
		public void beforeGetResource(Object contentObject) {
		}
	}

		public class EventSource {
	}

	public class UnknownContentEvent extends StoreEvent {

		private static final long serialVersionUID = 4393640168031790561L;

		public UnknownContentEvent(Object source,
				ContentStore<Object, Serializable> store) {
			super(source, store);
		}
	}

	@Test
	public void noop() {
		fail("test");
	}
}
