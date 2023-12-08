package org.springframework.content.solr.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.solr.DeprecatedSolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrAutoConfigurationTest {

   private ApplicationContextRunner contextRunner;

   {
      Describe("solr", () -> {
         Context("given an application context with a SolrClient bean and SolrAutoConfiguration", () -> {
            BeforeEach(() -> {
               contextRunner = new ApplicationContextRunner()
                 .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));
            });
            It("should include the autoconfigured annotated event handler bean", () -> {
               contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                  Assertions.assertThat(context).getBean("solrFulltextEventListener").isNotNull();
               });
            });
         });
      });
   }

   @Test
   public void test() {
   }

   @SpringBootApplication(exclude= ElasticsearchAutoConfiguration.class)
   public static class TestConfig {

      @Autowired
      private SolrProperties props;
      @Autowired
      private SolrClient solrClient;

      public TestConfig() {
      }

      @Bean
      public IndexService solrIndexService() {
         return new SolrFulltextIndexServiceImpl(solrClient, props);
      }

      @Bean
      public Object deprecatedSolrFulltextEventListener() {
         return new DeprecatedSolrIndexerStoreEventHandler(solrIndexService());
      }

      @Bean
      public Object solrFulltextEventListener() {
         return new SolrIndexerStoreEventHandler(solrIndexService());
      }   }
}
