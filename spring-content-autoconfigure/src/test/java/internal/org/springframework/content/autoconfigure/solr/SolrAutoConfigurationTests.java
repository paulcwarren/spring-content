package internal.org.springframework.content.autoconfigure.solr;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;

import java.io.InputStream;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class)
public class SolrAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;
	
	{
		Describe("solr", () -> {
			Context("given an application context with a SolrClient bean and SolrAutoConfiguration", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
				});

				It("should include the autoconfigured annotated event handler bean", () -> {
					MatcherAssert.assertThat(context, CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));
					MatcherAssert.assertThat(context.getBean("solrFulltextEventListener"), CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));
				});
			});
		});
	}
	
	@Test
	public void test() {
	}
	
	@Configuration
	public static class TestConfig extends SolrAutoConfiguration {
		
		@Bean
		public SolrClient solrClient() {
			return new HttpSolrServer("http://localhost:8983/solr/somesolrcollection");
		}

		@Bean
		public ContentOperations contentOperations() {
			return new TestContentOperations();
		}
	}
	
	public static class TestContentOperations implements ContentOperations {

		@Override
		public <T> void setContent(T metadata, InputStream content) {
		}

		@Override
		public <T> void unsetContent(T property) {
		}
		
		@Override
		public <T> InputStream getContent(T property) {
			return null;
		}
	}
}
