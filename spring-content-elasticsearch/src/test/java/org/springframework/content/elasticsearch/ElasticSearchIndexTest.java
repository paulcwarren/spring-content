package org.springframework.content.elasticsearch;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URI;

import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestOperations;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class ElasticSearchIndexTest {

	private ElasticsearchIndexer indexer;

	private AfterSetContentEvent event;
	
	//mocks
	private RestOperations template;
	private ContentStore<Object,Serializable> store;
	{
		Describe("ElasticSearchIndexer", () -> {
			Context("#onAfterSetContent", () -> {
				BeforeEach(() -> {
					template = mock(RestOperations.class);
					
					indexer = new ElasticsearchIndexer(template);
					
					TestContent source = new TestContent();
					source.contentId = "some-id";
					store = mock(ContentStore.class);
					event = new AfterSetContentEvent(source, store);
					
					when(store.getContent(eq(source))).thenReturn(new ByteArrayInputStream("Hello from Paul and Jigar!".getBytes()));
				});
				JustBeforeEach(() -> {
					indexer.onAfterSetContent(event);
				});
				It("should send the content for indexing", () -> {
					ArgumentCaptor<HttpEntity> argument = ArgumentCaptor.forClass(HttpEntity.class);

					verify(template).exchange((String)anyObject(), (HttpMethod)anyObject(), argument.capture(), (Class<Object>)anyObject());
					
					HttpEntity entity = argument.getValue();
					assertTrue(((String)entity.getBody()).contains("\"contentId\":\"some-id\""));
					assertTrue(((String)entity.getBody()).contains("\"original-content\":\"SGVsbG8gZnJvbSBQYXVsIGFuZCBKaWdhciE=\""));
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
