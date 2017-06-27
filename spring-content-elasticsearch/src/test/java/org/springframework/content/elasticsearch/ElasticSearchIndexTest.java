package org.springframework.content.elasticsearch;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import org.hamcrest.core.IsInstanceOf;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties.Storage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.elasticsearch.StreamConverter;
import internal.org.springframework.content.elasticsearch.StreamConverterImpl;

@RunWith(Ginkgo4jRunner.class)
public class ElasticSearchIndexTest {

	private ElasticsearchIndexer indexer;

	private AfterSetContentEvent event;
	private Exception result;
	
	//mocks
	private RestOperations template;
	private StreamConverter streamConverter;
	private ContentStore<Object,Serializable> store;
	{
		Describe("ElasticsearchIndexer", () -> {
			Context("#onAfterSetContent", () -> {
				BeforeEach(() -> {
					template = mock(RestOperations.class);
					streamConverter=mock(StreamConverter.class);
					
					byte[] content = "Hello from Paul and Jigar!".getBytes();
					
					when(streamConverter.convert(anyObject())).thenReturn(content);
					
					indexer = new ElasticsearchIndexer(template,streamConverter);
					
					TestContent source = new TestContent();
					source.contentId = "some-id";
					store = mock(ContentStore.class);
					event = new AfterSetContentEvent(source, store);
					
					when(store.getContent(eq(source))).thenReturn(new ByteArrayInputStream(content));
				});
				JustBeforeEach(() -> {
					try {
						indexer.onAfterSetContent(event);
					} catch (Exception e) {
						result = e;
					}
				});
				Context("when the content stream cant be converted", () -> {
					BeforeEach(() -> {
						when(streamConverter.convert(anyObject())).thenThrow(IOException.class);
					});
					It("should throw an ERROR of StoreAccessException type", () ->{
						assertThat(result, is(instanceOf(StoreAccessException.class)));
					});
				});
				Context("when elasticsearch is available", () -> {
					It("should send the content for indexing", () -> {
						ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);
						
						verify(template).exchange((String)anyObject(), (HttpMethod)anyObject(), argument.capture(), (Class<Object>)anyObject());
						
						HttpEntity entity = argument.getValue();
						assertTrue(((String)entity.getBody()).contains("\"contentId\":\"some-id\""));
						assertTrue(((String)entity.getBody()).contains("\"original-content\":\"SGVsbG8gZnJvbSBQYXVsIGFuZCBKaWdhciE=\""));
					});
					Context("when elasticsearch returns with a 400 error response", () -> {
						It("should throw a StoreAccessException", () -> {
							
							// Test all 5xx cases
							for(int i=400;i<=451;i++) {
								ResponseEntity<Object> response = mock(ResponseEntity.class);
								try {
									HttpStatus httpStatus = HttpStatus.valueOf(i);
									when(response.getStatusCode()).thenReturn(httpStatus);
									when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenReturn(response);
	
									try {
										result = null;
										indexer.onAfterSetContent(event);
									} catch (Exception e) {
										result = e;
									}
									
									assertThat(result, is(not(nullValue())));
									assertThat(result, is(instanceOf(StoreAccessException.class)));
								} catch (IllegalArgumentException iae) {
									// we dont care
								}
							}
						});
					});
					Context("when elasticsearch returns with a 500 error response", () -> {
						It("should throw a StoreAccessException", () -> {
							
							// Test all 5xx cases
							for(int i=500;i<=511;i++) {
								ResponseEntity<Object> response = mock(ResponseEntity.class);
								HttpStatus httpStatus = HttpStatus.valueOf(i);
								when(response.getStatusCode()).thenReturn(httpStatus);
								when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenReturn(response);

								try {
									result = null;
									indexer.onAfterSetContent(event);
								} catch (Exception e) {
									result = e;
								}
								
								assertThat(result, is(not(nullValue())));
								assertThat(result, is(instanceOf(StoreAccessException.class)));
							}
						});
					});
				});
				Context("when elasticsearch isnt available", () -> {
					BeforeEach(() -> {
						when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenThrow(StoreAccessException.class);
					});
					It("should throw a StoreAccessException", () -> {
						assertThat(result, is(not(nullValue())));
					});
				});
			});
			Context("#onBeforeUnsetContent", () -> {
				It("", () -> {
					
				});
			});
		});
	}
	
	public static class TestContent {
		@ContentId public String contentId;
	}
	
}
