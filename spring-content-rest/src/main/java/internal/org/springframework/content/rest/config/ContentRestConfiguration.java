
package internal.org.springframework.content.rest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import internal.org.springframework.content.rest.links.ContentLinksResourceProcessor;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.ContentRestByteRangeHttpRequestHandler;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers")
public class ContentRestConfiguration extends HateoasAwareSpringDataWebConfiguration {
	
	@Autowired(required=false)
	Repositories repositories;
	
	@Autowired 
	ContentStoreService stores;
	
	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		return new ContentHandlerMapping(repositories, stores);
	}
	
	@Bean
	ContentRestByteRangeHttpRequestHandler ascRestRequestHandler() {
		return new ContentRestByteRangeHttpRequestHandler();
	}

	@Bean 
	public ResourceProcessor<PersistentEntityResource> contentLinksProcessor() {
        return new ContentLinksResourceProcessor(stores);
    }

}
