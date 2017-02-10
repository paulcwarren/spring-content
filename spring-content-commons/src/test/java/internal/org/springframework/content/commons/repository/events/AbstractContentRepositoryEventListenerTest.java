package internal.org.springframework.content.commons.repository.events;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.ContentRepositoryEvent;
import org.springframework.content.commons.repository.events.AbstractContentRepositoryEventListener;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AbstractContentRepositoryEventListenerTest {

	private AbstractContentRepositoryEventListener<Object> listener;
	private ContentRepositoryEvent event;
	
	//mocks
	private TestContentEventConsumer consumer;
	{
		Describe("#onApplicationEvent", () -> {
			Context("given a content event listener", () -> {
				BeforeEach(() -> {
					consumer = mock(TestContentEventConsumer.class);
					
					listener = new TestContentEventListener(consumer);
				});
				JustBeforeEach(() -> {
					listener.onApplicationEvent(event);
				});
				Context("given a before get content event", () -> {
					BeforeEach(() -> {
						event = new BeforeGetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onBeforeGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given an after get content event", () -> {
					BeforeEach(() -> {
						event = new AfterGetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onAfterGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before set content event", () -> {
					BeforeEach(() -> {
						event = new BeforeSetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onBeforeSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after set content event", () -> {
					BeforeEach(() -> {
						event = new AfterSetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onAfterSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before unset content event", () -> {
					BeforeEach(() -> {
						event = new BeforeUnsetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onBeforeUnsetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after unset content event", () -> {
					BeforeEach(() -> {
						event = new AfterUnsetContentEvent(new EventSource());
					});
					It("should call the event consumer", () -> {
						verify(consumer).onAfterUnsetContent(argThat(is(event.getSource())));
					});
				});
			});
		});
	}
	
	@Test
	public void noop() {
	}

	public static class TestContentEventListener extends AbstractContentRepositoryEventListener<Object> {
		private TestContentEventConsumer consumer;

		public TestContentEventListener(TestContentEventConsumer consumer) {
			super();
			this.consumer = consumer;
		}

		@Override
		protected void onBeforeGetContent(Object entity) {
			consumer.onBeforeGetContent(entity);
		}

		@Override
		protected void onAfterGetContent(Object entity) {
			consumer.onAfterGetContent(entity);
		}

		@Override
		protected void onBeforeSetContent(Object entity) {
			consumer.onBeforeSetContent(entity);
		}

		@Override
		protected void onAfterSetContent(Object entity) {
			consumer.onAfterSetContent(entity);
		}

		@Override
		protected void onBeforeUnsetContent(Object entity) {
			consumer.onBeforeUnsetContent(entity);
		}

		@Override
		protected void onAfterUnsetContent(Object entity) {
			consumer.onAfterUnsetContent(entity);
		}
	}
	
	public interface TestContentEventConsumer {
		void onBeforeGetContent(Object entity);
		void onAfterUnsetContent(Object argThat);
		void onBeforeUnsetContent(Object argThat);
		void onAfterGetContent(Object entity);
		void onBeforeSetContent(Object argThat);
		void onAfterSetContent(Object argThat);
	}
	
	public static class EventSource {}
}
