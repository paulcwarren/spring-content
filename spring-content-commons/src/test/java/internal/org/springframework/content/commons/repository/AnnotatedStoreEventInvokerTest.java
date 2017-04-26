package internal.org.springframework.content.commons.repository;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.ReflectionService;
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
	private ContentStore<Object,Serializable> store;
	
	{
		Describe("#postProcessAfterInitialization", () -> {
			Context("when initialized with a ContentRepositoryEventHandler bean", () -> {
				BeforeEach(() -> {
					store = mock(ContentStore.class);
				});
				JustBeforeEach(() -> {
					reflectionService = mock(ReflectionService.class);
					invoker = new AnnotatedStoreEventInvoker(reflectionService);
					invoker.postProcessAfterInitialization(new CustomEventHandler(), "custom-bean");
				});
				It("register the handlers", () -> {
					assertThat(invoker.getHandlers().get(BeforeGetContentEvent.class).size(), is(1));
					assertThat(invoker.getHandlers().get(AfterGetContentEvent.class).size(), is(1));
					assertThat(invoker.getHandlers().get(BeforeSetContentEvent.class).size(), is(1));
					assertThat(invoker.getHandlers().get(AfterSetContentEvent.class).size(), is(1));
					assertThat(invoker.getHandlers().get(BeforeUnsetContentEvent.class).size(), is(1));
					assertThat(invoker.getHandlers().get(AfterUnsetContentEvent.class).size(), is(1));
				});
			});
		});
		
		Describe("#onApplicationEvent", () -> {
			BeforeEach(() -> {
				reflectionService = mock(ReflectionService.class);
				invoker = new AnnotatedStoreEventInvoker(reflectionService);
			});
			JustBeforeEach(() -> {
				invoker.postProcessAfterInitialization(new CustomEventHandler(), "custom-bean");
				invoker.onApplicationEvent(event);
			});
			Context("given an event handler and a BeforeGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeGetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeGetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and a AfterGetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterGetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterGetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and a BeforeSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeSetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeSetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and a BeforeSetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterSetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterSetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and a BeforeUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new BeforeUnsetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "beforeUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and a AfterUnsetContent event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new AfterUnsetContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService).invokeMethod(argThat(is(handler)), argThat(isA(CustomEventHandler.class)), argThat(is(event.getSource())));
				});
			});
			Context("given an event handler and an unknown event", () -> {
				BeforeEach(() -> {
					EventSource source = new EventSource();
					event = new UnknownContentEvent(source, store);
				});	
				It("should call that correct handler method", () -> {
					Method handler = ReflectionUtils.findMethod(CustomEventHandler.class, "afterUnsetContent", Object.class);
					assertThat(handler, is(not(nullValue())));
					
					verify(reflectionService, never()).invokeMethod(anyObject(), anyObject(), anyObject());
				});
			});
		});
	}
	
	@StoreEventHandler
	public class CustomEventHandler {
		
		@HandleBeforeGetContent
		public void beforeGetContent(Object contentObject) {}

		@HandleAfterGetContent
		public void afterGetContent(Object contentObject) {}

		@HandleBeforeSetContent
		public void beforeSetContent(Object contentObject) {}

		@HandleAfterSetContent
		public void afterSetContent(Object contentObject) {}

		@HandleBeforeUnsetContent
		public void beforeUnsetContent(Object contentObject) {}

		@HandleAfterUnsetContent
		public void afterUnsetContent(Object contentObject) {}
	}
	
	public class EventSource {
	}
	
	public class UnknownContentEvent extends StoreEvent {

		private static final long serialVersionUID = 4393640168031790561L;

		public UnknownContentEvent(Object source, ContentStore<Object,Serializable> store) {
			super(source, store);
		}
	}
	
	@Test
	public void noop() {
		fail("test");
	}
}
