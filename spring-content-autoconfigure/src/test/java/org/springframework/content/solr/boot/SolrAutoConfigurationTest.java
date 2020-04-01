package org.springframework.content.solr.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;

import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.solr.SolrIndexer;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class SolrAutoConfigurationTest {

   private AnnotationConfigApplicationContext context;

   {
      Describe("solr", () -> {
         Context("given an application context with a SolrClient bean and SolrAutoConfiguration",
               () -> {
                  BeforeEach(() -> {
                     context = new AnnotationConfigApplicationContext();
                     context.register(StarterTestConfig.class);
                     context.register(TestConfig.class);
                     context.refresh();
                  });

                  It("should include the autoconfigured annotated event handler bean",
                        () -> {
                           MatcherAssert.assertThat(context, CoreMatchers.is(
                                 CoreMatchers.not(CoreMatchers.nullValue())));
                           MatcherAssert.assertThat(
                                 context.getBean("solrFulltextEventListener"),
                                 CoreMatchers.is(CoreMatchers
                                       .not(CoreMatchers.nullValue())));
                        });
               });
      });
   }

   @Test
   public void test() {
   }

   @Configuration
   public static class StarterTestConfig extends SolrAutoConfiguration {
   }

   @Configuration
   @ComponentScan(basePackageClasses = StarterTestConfig.class)
   public static class TestConfig {

      @Autowired
      private SolrProperties props;
      @Autowired
      private SolrClient solrClient;
      @Autowired
      private ConversionService contentConversionService;

      public TestConfig() {
      }

      @Bean
      public IndexService solrIndexService() {
         return new SolrFulltextIndexServiceImpl(solrClient, props);
      }

      @Bean
      public Object solrFulltextEventListener() {
         return new SolrIndexer(solrIndexService());
      }
   }
}
