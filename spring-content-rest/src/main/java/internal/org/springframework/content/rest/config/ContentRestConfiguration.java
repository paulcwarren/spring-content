
package internal.org.springframework.content.rest.config;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.rest.webmvc.config.RootResourceInformationHandlerMethodArgumentResolver;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import internal.org.springframework.content.rest.links.ContentLinksResourceProcessor;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers")
public class ContentRestConfiguration extends HateoasAwareSpringDataWebConfiguration {
	
	@Autowired
	Repositories repositories;
	
	@Autowired
	RepositoryInvokerFactory repositoryInvokerFactory;
	
	@Autowired
	ResourceMappings repositoryMappings;
	
	@Autowired 
	ContentStoreService storeService;
	
	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		return new ContentHandlerMapping(repositories, repositoryMappings, storeService);
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		super.addArgumentResolvers(argumentResolvers);
		argumentResolvers.add(rootResourceInformationArgumentResolver());
	}
	
	HandlerMethodArgumentResolver rootResourceInformationArgumentResolver() {
		return new RootResourceInformationHandlerMethodArgumentResolver(repositories, repositoryInvokerFactory, resourceMetadataArgumentResolver());
	}
	
	ResourceMetadataHandlerMethodArgumentResolver resourceMetadataArgumentResolver() {
		return new ResourceMetadataHandlerMethodArgumentResolver(repositories, repositoryMappings, new BaseUri(URI.create("")));
	}
	
	@Bean 
	public ResourceProcessor<PersistentEntityResource> contentLinksProcessor() {
        return new ContentLinksResourceProcessor(storeService);
    }

}
