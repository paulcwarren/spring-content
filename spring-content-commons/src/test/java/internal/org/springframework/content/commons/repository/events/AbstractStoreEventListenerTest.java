package internal.org.springframework.content.commons.repository.events;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AbstractStoreEventListener;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AbstractStoreEventListenerTest {

	private AbstractStoreEventListener<Object> listener;
	private StoreEvent event;
	
	//mocks
	private TestContentEventConsumer consumer;
	private ContentStore<Object,Serializable> store;
	{
		Describe("#onApplicationEvent", () -> {
			Context("given a content event listener", () -> {
				BeforeEach(() -> {
					consumer = mock(TestContentEventConsumer.class);
					store = (ContentStore<Object,Serializable>)mock(ContentStore.class);
					
					listener = new TestContentEventListener(consumer);
				});
				JustBeforeEach(() -> {
					listener.onApplicationEvent(event);
				});
				Context("given a before get content event", () -> {
					BeforeEach(() -> {
						event = new BeforeGetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeGetContentEvent> argumentCaptor = ArgumentCaptor.forClass(BeforeGetContentEvent.class);
						verify(consumer).onBeforeGetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onBeforeGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given an after get content event", () -> {
					BeforeEach(() -> {
						event = new AfterGetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterGetContentEvent> argumentCaptor = ArgumentCaptor.forClass(AfterGetContentEvent.class);
						verify(consumer).onAfterGetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onAfterGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before set content event", () -> {
					BeforeEach(() -> {
						event = new BeforeSetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeSetContentEvent> argumentCaptor = ArgumentCaptor.forClass(BeforeSetContentEvent.class);
						verify(consumer).onBeforeSetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onBeforeSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after set content event", () -> {
					BeforeEach(() -> {
						event = new AfterSetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterSetContentEvent> argumentCaptor = ArgumentCaptor.forClass(AfterSetContentEvent.class);
						verify(consumer).onAfterSetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onAfterSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before unset content event", () -> {
					BeforeEach(() -> {
						event = new BeforeUnsetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeUnsetContentEvent> argumentCaptor = ArgumentCaptor.forClass(BeforeUnsetContentEvent.class);
						verify(consumer).onBeforeUnsetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onBeforeUnsetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after unset content event", () -> {
					BeforeEach(() -> {
						event = new AfterUnsetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterUnsetContentEvent> argumentCaptor = ArgumentCaptor.forClass(AfterUnsetContentEvent.class);
						verify(consumer).onAfterUnsetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(), is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer).onAfterUnsetContent(argThat(is(event.getSource())));
					});
				});
			});
		});
	}
	
	@Test
	public void noop() {
	}

	public static class TestContentEventListener extends AbstractStoreEventListener<Object> {
		private TestContentEventConsumer consumer;

		public TestContentEventListener(TestContentEventConsumer consumer) {
			super();
			this.consumer = consumer;
		}

		@Override
		protected void onBeforeGetContent(BeforeGetContentEvent event) {
			consumer.onBeforeGetContent(event);
		}

		@Override
		protected void onBeforeGetContent(Object entity) {
			consumer.onBeforeGetContent(entity);
		}

		@Override
		protected void onAfterGetContent(AfterGetContentEvent event) {
			consumer.onAfterGetContent(event);
		}

		@Override
		protected void onAfterGetContent(Object entity) {
			consumer.onAfterGetContent(entity);
		}

		@Override
		protected void onBeforeSetContent(BeforeSetContentEvent event) {
			consumer.onBeforeSetContent(event);
		}

		@Override
		protected void onBeforeSetContent(Object entity) {
			consumer.onBeforeSetContent(entity);
		}

		@Override
		protected void onAfterSetContent(AfterSetContentEvent event) {
			consumer.onAfterSetContent(event);
		}

		@Override
		protected void onAfterSetContent(Object entity) {
			consumer.onAfterSetContent(entity);
		}

		@Override
		protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
			consumer.onBeforeUnsetContent(event);
		}

		@Override
		protected void onBeforeUnsetContent(Object entity) {
			consumer.onBeforeUnsetContent(entity);
		}

		@Override
		protected void onAfterUnsetContent(AfterUnsetContentEvent event) {
			consumer.onAfterUnsetContent(event);
		}

		@Override
		protected void onAfterUnsetContent(Object entity) {
			consumer.onAfterUnsetContent(entity);
		}
	}
	
	public interface TestContentEventConsumer {
		void onBeforeGetContent(BeforeGetContentEvent event);
		void onBeforeGetContent(Object entity);
		void onAfterGetContent(AfterGetContentEvent event);
		void onAfterGetContent(Object entity);
		void onBeforeSetContent(BeforeSetContentEvent event);
		void onBeforeSetContent(Object argThat);
		void onAfterSetContent(AfterSetContentEvent event);
		void onAfterSetContent(Object argThat);
		void onBeforeUnsetContent(BeforeUnsetContentEvent event);
		void onBeforeUnsetContent(Object argThat);
		void onAfterUnsetContent(AfterUnsetContentEvent event);
		void onAfterUnsetContent(Object argThat);
	}
	
	public static class EventSource {}
}
