package internal.org.springframework.content.commons.repository.events;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreEvent;
import org.springframework.content.commons.repository.events.*;

import java.io.InputStream;
import java.io.Serializable;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class DeprecatedAbstractStoreEventListenerTest {

	private AbstractStoreEventListener<Object> listener;
	private StoreEvent event;

	// mocks
	private TestContentEventConsumer consumer;
	private ContentStore<Object, Serializable> store;
	{
		Describe("#onApplicationEvent", () -> {
			Context("given a content event listener", () -> {
				BeforeEach(() -> {
					consumer = mock(TestContentEventConsumer.class);
					store = (ContentStore<Object, Serializable>) mock(ContentStore.class);

					listener = new TestContentEventListener(consumer);
				});
				JustBeforeEach(() -> {
					listener.onApplicationEvent(event);
				});
				Context("given a before get resource event", () -> {
					BeforeEach(() -> {
						event = new BeforeGetResourceEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeGetResourceEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeGetResourceEvent.class);
						verify(consumer).onBeforeGetResource(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeGetResource(argThat(is(event.getSource())));
					});
				});
				Context("given an after get resource event", () -> {
					BeforeEach(() -> {
						event = new AfterGetResourceEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterGetResourceEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterGetResourceEvent.class);
						verify(consumer).onAfterGetResource(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterGetResource(argThat(is(event.getSource())));
					});
				});
				Context("given a before associate event", () -> {
					BeforeEach(() -> {
						event = new BeforeAssociateEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeAssociateEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeAssociateEvent.class);
						verify(consumer).onBeforeAssociate(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeAssociate(argThat(is(event.getSource())));
					});
				});
				Context("given an after associate event", () -> {
					BeforeEach(() -> {
						event = new AfterAssociateEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterAssociateEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterAssociateEvent.class);
						verify(consumer).onAfterAssociate(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterAssociate(argThat(is(event.getSource())));
					});
				});
				Context("given a before unassociate event", () -> {
					BeforeEach(() -> {
						event = new BeforeUnassociateEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeUnassociateEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeUnassociateEvent.class);
						verify(consumer).onBeforeUnassociate(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeUnassociate(argThat(is(event.getSource())));
					});
				});
				Context("given an after unassociate event", () -> {
					BeforeEach(() -> {
						event = new AfterUnassociateEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterUnassociateEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterUnassociateEvent.class);
						verify(consumer).onAfterUnassociate(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterUnassociate(argThat(is(event.getSource())));
					});
				});
				Context("given a before get content event", () -> {
					BeforeEach(() -> {
						event = new BeforeGetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeGetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeGetContentEvent.class);
						verify(consumer).onBeforeGetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given an after get content event", () -> {
					BeforeEach(() -> {
						event = new AfterGetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterGetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterGetContentEvent.class);
						verify(consumer).onAfterGetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterGetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before set content event", () -> {
					BeforeEach(() -> {
						event = new BeforeSetContentEvent(new EventSource(), store, (InputStream)null);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeSetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeSetContentEvent.class);
						verify(consumer).onBeforeSetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after set content event", () -> {
					BeforeEach(() -> {
						event = new AfterSetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterSetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterSetContentEvent.class);
						verify(consumer).onAfterSetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterSetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a before unset content event", () -> {
					BeforeEach(() -> {
						event = new BeforeUnsetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<BeforeUnsetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(BeforeUnsetContentEvent.class);
						verify(consumer).onBeforeUnsetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onBeforeUnsetContent(argThat(is(event.getSource())));
					});
				});
				Context("given a after unset content event", () -> {
					BeforeEach(() -> {
						event = new AfterUnsetContentEvent(new EventSource(), store);
					});
					It("should call the event consumer", () -> {
						ArgumentCaptor<AfterUnsetContentEvent> argumentCaptor = ArgumentCaptor
								.forClass(AfterUnsetContentEvent.class);
						verify(consumer).onAfterUnsetContent(argumentCaptor.capture());
						assertThat(argumentCaptor.getValue(), is(event));
						assertThat(argumentCaptor.getValue().getSource(),
								is(event.getSource()));
						assertThat(argumentCaptor.getValue().getStore(), is(store));
					});
					It("should call the event source consumer", () -> {
						verify(consumer)
								.onAfterUnsetContent(argThat(is(event.getSource())));
					});
				});
			});
		});
	}

	@Test
	public void noop() {
	}

	public static class TestContentEventListener
			extends AbstractStoreEventListener<Object> {
		final private TestContentEventConsumer consumer;

		public TestContentEventListener(TestContentEventConsumer consumer) {
			super();
			this.consumer = consumer;
		}

		@Override
		protected void onBeforeGetResource(BeforeGetResourceEvent event) {
			consumer.onBeforeGetResource(event);
		}

		@Override
		protected void onBeforeGetResource(Object entity) {
			consumer.onBeforeGetResource(entity);
		}

		@Override
		protected void onAfterGetResource(AfterGetResourceEvent event) {
			consumer.onAfterGetResource(event);
		}

		@Override
		protected void onAfterGetResource(Object entity) {
			consumer.onAfterGetResource(entity);
		}

		@Override
		protected void onBeforeAssociate(BeforeAssociateEvent event) {
			consumer.onBeforeAssociate(event);
		}

		@Override
		protected void onBeforeAssociate(Object entity) {
			consumer.onBeforeAssociate(entity);
		}

		@Override
		protected void onAfterAssociate(AfterAssociateEvent event) {
			consumer.onAfterAssociate(event);
		}

		@Override
		protected void onAfterAssociate(Object entity) {
			consumer.onAfterAssociate(entity);
		}

		@Override
		protected void onBeforeUnassociate(BeforeUnassociateEvent event) {
			consumer.onBeforeUnassociate(event);
		}

		@Override
		protected void onBeforeUnassociate(Object entity) {
			consumer.onBeforeUnassociate(entity);
		}

		@Override
		protected void onAfterUnassociate(AfterUnassociateEvent event) {
			consumer.onAfterUnassociate(event);
		}

		@Override
		protected void onAfterUnassociate(Object entity) {
			consumer.onAfterUnassociate(entity);
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
		void onBeforeGetResource(BeforeGetResourceEvent event);

		void onBeforeGetResource(Object entity);

		void onAfterGetResource(AfterGetResourceEvent event);

		void onAfterGetResource(Object entity);

		void onBeforeAssociate(BeforeAssociateEvent event);

		void onBeforeAssociate(Object entity);

		void onAfterAssociate(AfterAssociateEvent event);

		void onAfterAssociate(Object entity);

		void onBeforeUnassociate(BeforeUnassociateEvent event);

		void onBeforeUnassociate(Object entity);

		void onAfterUnassociate(AfterUnassociateEvent event);

		void onAfterUnassociate(Object entity);

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

	public static class EventSource {
	}
}
