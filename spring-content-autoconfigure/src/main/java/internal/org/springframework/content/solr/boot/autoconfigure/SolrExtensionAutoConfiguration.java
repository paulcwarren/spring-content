package internal.org.springframework.content.solr.boot.autoconfigure;

import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.solr.DeprecatedSolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;

@Configuration
@ConditionalOnClass(SolrClient.class)
@ComponentScan(basePackageClasses = SolrAutoConfiguration.class)
public class SolrExtensionAutoConfiguration {

   @Autowired
   private SolrProperties props;

   @Autowired
   private SolrClient solrClient;

   @Autowired
   @Qualifier("solrConversionService")
   private ConversionService solrConversionService;

   public SolrExtensionAutoConfiguration() {
   }



   @ConditionalOnMissingBean(name = "solrIndexService")
   @Bean
   public IndexService solrIndexService() {
      return new SolrFulltextIndexServiceImpl(solrClient, props);
   }

   @ConditionalOnMissingBean(name = "solrFulltextEventListener")
   @Bean
   public Object solrFulltextEventListener() {
      return new SolrIndexerStoreEventHandler(solrIndexService());
   }

   @ConditionalOnMissingBean(name = "deprecatedSolrFulltextEventListener")
   @Bean
   public Object deprecatedSolrFulltextEventListener() {
      return new DeprecatedSolrIndexerStoreEventHandler(solrIndexService());
   }
}
