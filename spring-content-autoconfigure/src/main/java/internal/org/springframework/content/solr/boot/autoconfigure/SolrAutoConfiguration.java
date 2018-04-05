package internal.org.springframework.content.solr.boot.autoconfigure;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;


@Configuration
@ConditionalOnClass(SolrClient.class)
public class SolrAutoConfiguration {
	
    @Bean
    @ConditionalOnMissingBean(SolrProperties.class)
    public SolrProperties solrProperties() {
        SolrProperties solrConfig =  new SolrProperties();
        solrConfig.setUrl("http://localhost:8983/solr/solr");
        return solrConfig;
    }
    
    @Bean
    @ConditionalOnMissingBean(SolrClient.class)
    public SolrClient solrClient() {
        return new HttpSolrClient.Builder(solrProperties().getUrl())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name="solrConversionService")
    public ConversionService solrConversionService() {
        return new DefaultFormattingConversionService();
    }
}
