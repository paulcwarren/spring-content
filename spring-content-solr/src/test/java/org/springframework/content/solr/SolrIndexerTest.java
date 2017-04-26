package org.springframework.content.solr;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;

@SuppressWarnings("unchecked")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrIndexerTest {

	private SolrIndexer handler;
	
	// mocks
	private SolrClient solrClient;
	private ContentStore<Object,Serializable> store;
	private SolrProperties props;
	
	// args 
	private Object contentEntity;
	private AfterSetContentEvent afterSetEvent;
	private BeforeUnsetContentEvent beforeUnsetEvent;
	private Throwable e;
	
	{
		Describe("SolrUpdateEventHandler", () -> {
			BeforeEach(() -> {
				solrClient = mock(SolrClient.class);
				store = mock(ContentStore.class);
				props = mock(SolrProperties.class);
				handler = new SolrIndexer(solrClient, props);
			});
			Context("#onAfterSetContent", () -> {
				JustBeforeEach(() -> {
					try {
						afterSetEvent = new AfterSetContentEvent(contentEntity, store);
						handler.onAfterSetContent(afterSetEvent);
					} catch (Throwable e) {
						this.e = e;
					}
				});
				Context("given a content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
						((ContentEntity)contentEntity).contentId = UUID.randomUUID().toString();
						((ContentEntity)contentEntity).contentLen = 128L;
						((ContentEntity)contentEntity).mimeType = "text/plain";
					});
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(solrClient).request(anyObject(), anyObject());
					});
                    It("should store the content type", () -> {
                        ArgumentCaptor<ContentStreamUpdateRequest> argument = forClass(ContentStreamUpdateRequest.class);
                        verify(solrClient).request(argument.capture(), anyObject());
                        assertThat(argument.getValue().getParams().get("literal.id"), is(((ContentEntity)contentEntity).getClass().getCanonicalName()  + ":" + ((ContentEntity)contentEntity).contentId ));
                    });
                    Context("given a username", () -> {
                        BeforeEach(() -> {
                            when(props.getUser()).thenReturn("username");
                            when(props.getPassword()).thenReturn("password");
                        });
                        It("should set basic credentials on the request", () -> {
                            ArgumentCaptor<ContentStreamUpdateRequest> argument = forClass(ContentStreamUpdateRequest.class);
                            verify(solrClient).request(argument.capture(), anyObject());
                            assertThat(argument.getValue().getBasicAuthUser(), is("username"));
                            assertThat(argument.getValue().getBasicAuthPassword(), is("password"));
                        });
                    });
                    Context("given a SolrServer Exception", () -> {
                        BeforeEach(() -> {
                            when(solrClient.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                        });
                        It("should throw a ContextAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given a IOException", () -> {
                        BeforeEach(() -> {
                            when(solrClient.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a ContextAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
				});
				Context("given a content entity with a null contentId", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
					});
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(solrClient, never()).request(anyObject(), anyString());
					});
				});
				Context("given a bogus content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new NotAContentEntity();
					});
					It("", ()->{
						assertThat(e, is(nullValue()));
						verify(solrClient, never()).request(anyObject(), anyString());
					});
				});
			});
			Context("#onBeforeUnsetContent", () -> {
				JustBeforeEach(() -> {
					try {
						beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
						handler.onBeforeUnsetContent(beforeUnsetEvent);
					} catch (Exception e) {
						this.e = e;
					}
				});
				Context("given a content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
						((ContentEntity)contentEntity).contentId = UUID.randomUUID().toString();
						((ContentEntity)contentEntity).contentLen = 128L;
						((ContentEntity)contentEntity).mimeType = "text/plain";
					});
                    It("should delete the correct id", () -> {
                        ArgumentCaptor<UpdateRequest> argument = forClass(UpdateRequest.class);
                        verify(solrClient).request(argument.capture(), anyObject());
                        String expected = ((ContentEntity)contentEntity).getClass().getCanonicalName()  + ":" + ((ContentEntity)contentEntity).contentId;
                        assertThat(argument.getValue().getDeleteById(), hasItem(expected));
                    });
                    Context("given a username", () -> {
                        BeforeEach(() -> {
                            when(props.getUser()).thenReturn("username");
                            when(props.getPassword()).thenReturn("password");
                        });
                        It("should set basic credentials on the request", () -> {
                            ArgumentCaptor<UpdateRequest> argument = forClass(UpdateRequest.class);
                            verify(solrClient).request(argument.capture(), anyObject());
                            assertThat(argument.getValue().getBasicAuthUser(), is("username"));
                            assertThat(argument.getValue().getBasicAuthPassword(), is("password"));
                        });
                    });
                    Context("given a SolrServer Exception", () -> {
                        BeforeEach(() -> {
                            when(solrClient.request(anyObject(), anyObject())).thenThrow(SolrServerException.class);
                        });
                        It("should throw a ContextAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
                    Context("given a IOException", () -> {
                        BeforeEach(() -> {
                            when(solrClient.request(anyObject(), anyObject())).thenThrow(IOException.class);
                        });
                        It("should throw a ContextAccessException", () -> {
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        });
                    });
				});
				Context("given a content entity with a null contentId", () -> {
					BeforeEach(() -> {
						contentEntity = new ContentEntity();
					});
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(solrClient, never()).deleteById(anyString());
					});
				});
				Context("given a bogus content entity", () -> {
					BeforeEach(() -> {
						contentEntity = new NotAContentEntity();
					});
					It("should never attempt deletion", ()->{
						assertThat(e, is(nullValue()));
						verify(solrClient, never()).deleteById(anyString());
					});
				});
			});
		});
	}

	public static class ContentEntity {
		@ContentId public String contentId;
		@ContentLength public Long contentLen;
		@MimeType public String mimeType;
	}
	
	public static class NotAContentEntity {
	}
	
	@Test
	public void test() {
	}

}
