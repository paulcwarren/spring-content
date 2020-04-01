package org.springframework.content.solr;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.fragments.SearchableImpl;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

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
			It("should have a Solr indexing store event handler bean", () -> {
				assertThat(context.getBean(SolrIndexer.class), is(not(nullValue())));
			});
			It("should have a Searchable implementation bean", () -> {
				assertThat(context.getBeansOfType(SearchableImpl.class), is(not(nullValue())));
			});
			It("should have a solr-based fulltext index service bean", () -> {
				assertThat(context.getBean(IndexService.class), is(instanceOf(SolrFulltextIndexServiceImpl.class)));
			});
		});
	}

	@Configuration
	@EnableFilesystemStores
	@EnableFullTextSolrIndexing
	public static class TestConfiguration {

		@Bean
		public SolrClient solrClient() {
			SolrClient sc = new HttpSolrClient.Builder("http://some/url").build();
			return sc;
		}

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
			return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
		}

		// Developer bean - would usually be supplied by app developer
		@Bean
		public ConversionService contentConversionService() {
			return mock(ConversionService.class);
		}
	}

	public interface TContentStore extends ContentStore<Object, Serializable>, Searchable<Serializable> {}

	@Test
	public void noop() {
	}

}
