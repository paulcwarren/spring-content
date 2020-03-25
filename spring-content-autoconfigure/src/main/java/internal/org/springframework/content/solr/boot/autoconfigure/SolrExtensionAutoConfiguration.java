package internal.org.springframework.content.solr.boot.autoconfigure;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.content.solr.SolrIndexer;
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

   @Bean
   public Object solrFulltextEventListener() {
      return new SolrIndexer(solrClient, props);
   }
}
