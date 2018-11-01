package internal.org.springframework.content.solr;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.solr.EnableFullTextSolrIndexing;
import org.springframework.content.solr.SolrIndexer;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes = EnableFullTextSolrIndexingTest.TestConfiguration.class)
public class EnableFullTextSolrIndexingTest {

	@Autowired
	private ApplicationContext context;

	{
		Describe("EnableFullTextSolrIndexing", () -> {
			It("should have a SolrProperties bean", () -> {
				assertThat(context.getBean(SolrProperties.class), is(not(nullValue())));
			});
			It("should have a SolrIndexer bean", () -> {
				assertThat(context.getBean(SolrIndexer.class), is(not(nullValue())));
			});
		});
	}

	@Configuration
	@EnableFullTextSolrIndexing
	@Import(ContentStoreConfiguration.class)
	public static class TestConfiguration {

		@Bean
		public SolrClient solrClient() {
			SolrClient sc = new HttpSolrClient.Builder("http://some/url").build();
			return sc;
		}

	}

	public static class ContentStoreConfiguration {

		// Developer bean - would usually be supplied by app developer
		@Bean
		public ConversionService conversionService() {
			return mock(ConversionService.class);
		}

	}

	@Test
	public void noop() {
	}

}
