package org.springframework.content.solr;

import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrIndexerStoreEventHandlerTest {

	private SolrIndexerStoreEventHandler handler;

	// mocks
	private ContentStore<Object, Serializable> store;
	private IndexService indexer;

	// args
	private Object contentEntity;
	private AfterSetContentEvent afterSetEvent;
	private BeforeUnsetContentEvent beforeUnsetEvent;
	private InputStream content;
	private StoreAccessException sae;
	private Throwable e;

	{
		Describe("SolrIndexerStoreEventHandler", () -> {
			BeforeEach(() -> {
				store = mock(ContentStore.class);
				content = mock(InputStream.class);
				indexer = mock(IndexService.class);
				handler = new SolrIndexerStoreEventHandler(indexer);
			});
			Context("#onAfterSetContent", () -> {
				JustBeforeEach(() -> {
					try {
						afterSetEvent = new AfterSetContentEvent(contentEntity, store);
						handler.onAfterSetContent(afterSetEvent);
					}
					catch (Throwable e) {
						this.e = e;
					}
				});
				Context("given a content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
						((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
						((ContentEntity) contentEntity).contentLen = 128L;
						((ContentEntity) contentEntity).mimeType = "text/plain";

						when(store.getContent(eq(contentEntity))).thenReturn(content);
					});
					It("should use the indexer to index the content", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer).index(eq(contentEntity), eq(content));
					});

//					Context("given a SolrServer Exception", () -> {
//						BeforeEach(() -> {
//							when(solrClient.request(anyObject(), anyObject()))
//									.thenThrow(SolrServerException.class);
//						});
//						It("should throw a ContextAccessException", () -> {
//							assertThat(e, is(instanceOf(StoreAccessException.class)));
//						});
//					});
					Context("given the indexer throws an Exception", () -> {
						BeforeEach(() -> {
							sae = new StoreAccessException("badness");
							doThrow(sae).when(indexer).index(anyObject(), anyObject());
						});
						It("should re-throw that exception", () -> {
							assertThat(e, is(sae));
						});
					});
				});
				Context("given a content entity with a null contentId", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
					});
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer, never()).index(anyObject(), anyObject());
					});
				});
				Context("given a bogus content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new NotAContentEntity();
					});
					It("", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer, never()).index(anyObject(), anyObject());
					});
				});
			});
			Context("#onBeforeUnsetContent", () -> {
				JustBeforeEach(() -> {
					try {
						beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
						handler.onBeforeUnsetContent(beforeUnsetEvent);
					}
					catch (Exception e) {
						this.e = e;
					}
				});
				Context("given a content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
						((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
						((ContentEntity) contentEntity).contentLen = 128L;
						((ContentEntity) contentEntity).mimeType = "text/plain";
					});
					It("should use the indexer to unindex the content", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer).unindex(eq(contentEntity));
					});
//					Context("given a username", () -> {
//						BeforeEach(() -> {
//							when(props.getUser()).thenReturn("username");
//							when(props.getPassword()).thenReturn("password");
//						});
//						It("should set basic credentials on the request", () -> {
//							ArgumentCaptor<UpdateRequest> argument = forClass(
//									UpdateRequest.class);
//							verify(solrClient).request(argument.capture(), anyObject());
//							assertThat(argument.getValue().getBasicAuthUser(),
//									is("username"));
//							assertThat(argument.getValue().getBasicAuthPassword(),
//									is("password"));
//						});
//					});
//					Context("given a SolrServer Exception", () -> {
//						BeforeEach(() -> {
//							when(solrClient.request(anyObject(), anyObject()))
//									.thenThrow(SolrServerException.class);
//						});
//						It("should throw a ContextAccessException", () -> {
//							assertThat(e, is(instanceOf(StoreAccessException.class)));
//						});
//					});
					Context("given a IOException", () -> {
						BeforeEach(() -> {
							sae = new StoreAccessException("badness");
							doThrow(sae).when(indexer).unindex(anyObject());
						});
						It("should throw a ContextAccessException", () -> {
							assertThat(e, is(sae));
						});
					});
				});
				Context("given a content entity with a null contentId", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
					});
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer, never()).unindex(anyObject());
					});
				});
				Context("given a bogus content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new NotAContentEntity();
					});
					It("should never attempt deletion", () -> {
						assertThat(e, is(nullValue()));
						verify(indexer, never()).unindex(anyObject());
					});
				});
			});
		});
	}

	public static class ContentEntity {
		@ContentId
		public String contentId;
		@ContentLength
		public Long contentLen;
		@MimeType
		public String mimeType;
	}

	public static class NotAContentEntity {
	}

	@Test
	public void test() {
	}

}
