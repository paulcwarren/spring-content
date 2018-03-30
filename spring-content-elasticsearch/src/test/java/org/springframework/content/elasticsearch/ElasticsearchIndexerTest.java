package org.springframework.content.elasticsearch;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.elasticsearch.StreamConverter;
import org.hamcrest.CoreMatchers;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jRunner.class)
public class ElasticsearchIndexerTest {

	private ElasticsearchIndexer indexer;

	private AfterSetContentEvent setEvent;
	private BeforeUnsetContentEvent unsetEvent;
	private Exception result;
	
	//mocks
	private RestOperations template;
	private StreamConverter streamConverter;
	private ContentStore<Object,Serializable> store;
	{
		Describe("ElasticsearchIndexer", () -> {
			BeforeEach(() -> {
				template = mock(RestOperations.class);
				streamConverter=mock(StreamConverter.class);
				
				indexer = new ElasticsearchIndexer(template,streamConverter);
			});
			Context("#onAfterSetContent", () -> {
				BeforeEach(() -> {
					byte[] content = "Hello from Paul and Jigar!".getBytes();
					when(streamConverter.convert(anyObject())).thenReturn(content);

					TestContent source = new TestContent();
					source.contentId = "some-id";
					store = mock(ContentStore.class);
					setEvent = new AfterSetContentEvent(source, store);
					
					when(store.getContent(eq(source))).thenReturn(new ByteArrayInputStream(content));
				});
				JustBeforeEach(() -> {
					try {
						indexer.onAfterSetContent(setEvent);
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
						
						verify(template).exchange(argThat(endsWith("/some-id")), eq(HttpMethod.PUT), argument.capture(), (Class<Object>)anyObject());
						
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
										indexer.onAfterSetContent(setEvent);
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
									indexer.onAfterSetContent(setEvent);
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
						when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenThrow(RestClientException.class);
					});
					It("should throw a StoreAccessException", () -> {
						assertThat(result, is(not(nullValue())));
						assertThat(result, is(instanceOf(StoreAccessException.class)));
					});
				});
			});
			Context("#onBeforeUnsetContent", () -> {
				BeforeEach(() -> {
					unsetEvent = mock(BeforeUnsetContentEvent.class);

					TestContent content = new TestContent();
					content.contentId = "some-id";
					when(unsetEvent.getSource()).thenReturn(content);
				});
				JustBeforeEach(() -> {
					try {
						indexer.onBeforeUnsetContent(unsetEvent);
					} catch (Exception e) {
						result = e;
					}
				});
				Context("when elasticsearch is available", () -> {
					It("should send a DELETE request", () -> {
						verify(template).exchange(argThat(CoreMatchers.endsWith("/some-id")), eq(HttpMethod.DELETE), eq(null), (Class<Object>)anyObject());
					});
					Context("when removing index failed", () -> {
						It("should throw a StoreAccessException", () -> {
							for (int i=400; i<=511; i++) {
								try {
									HttpStatus status = HttpStatus.valueOf(i);
									ResponseEntity<Object> response = new ResponseEntity(status);
									when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenReturn(response);
									
									try {
										result = null;
										indexer.onBeforeUnsetContent(unsetEvent);
									} catch (Exception e) {
										result = e;
									}

									assertThat(result, is(instanceOf(StoreAccessException.class)));
								} catch (IllegalArgumentException iae) {
									// ignore
								}
							}
						});
					});
				});
				Context("when elasticsearch is not available", () -> {
					BeforeEach(() -> {
						when(template.exchange((String)anyObject(), (HttpMethod)anyObject(), (HttpEntity)anyObject(), (Class<Object>)anyObject())).thenThrow(RestClientException.class);
					});
					It("should throw a StoreAccessException", () -> {
						assertThat(result, is(instanceOf(StoreAccessException.class)));
					});
				});
			});
		});
	}
	
	public static class TestContent {
		@ContentId public String contentId;
	}
	
}
