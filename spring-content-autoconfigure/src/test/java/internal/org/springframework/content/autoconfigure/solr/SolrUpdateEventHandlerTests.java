package internal.org.springframework.content.autoconfigure.solr;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.operations.ContentOperations;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class SolrUpdateEventHandlerTests {

	private SolrUpdateEventHandler handler;
	
	// mocks
	private SolrClient solrClient;
	private ContentOperations ops;
	
	// args 
	private Object contentEntity;
	private Exception e;
	
	{
		Describe("SolrUpdateEventHandler", () -> {
			Context("#onAfterSetContent", () -> {
				BeforeEach(() -> {
					solrClient = mock(SolrClient.class);
					ops = mock(ContentOperations.class);
				});
				JustBeforeEach(() -> {
					handler = new SolrUpdateEventHandler(solrClient, ops);
					try {
						handler.onAfterSetContent(contentEntity);
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
					It("should call update", () -> {
						assertThat(e, is(nullValue()));
						verify(solrClient).request(anyObject(), anyString());
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
	
	@Test
	public void test() {
	}

}
