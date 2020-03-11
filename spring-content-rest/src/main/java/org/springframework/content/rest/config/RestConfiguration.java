package org.springframework.content.rest.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import internal.org.springframework.content.rest.controllers.ContentServiceHandlerMethodArgumentResolver;
import internal.org.springframework.content.rest.controllers.ResourceETagMethodArgumentResolver;
import internal.org.springframework.content.rest.controllers.ResourceHandlerMethodArgumentResolver;
import internal.org.springframework.content.rest.controllers.ResourceTypeMethodArgumentResolver;
import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.DefaultRepositoryInvokerFactory;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers, org.springframework.data.rest.extensions, org.springframework.data.rest.versioning")
public class RestConfiguration implements InitializingBean {

	private static final URI NO_URI = URI.create("");

	@Autowired
	ContentStoreService stores;

	@Autowired(required = false)
	private List<ContentRestConfigurer> configurers = new ArrayList<>();

	private URI baseUri = NO_URI;
	private StoreCorsRegistry corsRegistry;
	private boolean fullyQualifiedLinks = false;

	public RestConfiguration() {
		this.corsRegistry = new StoreCorsRegistry();
	}

	public URI getBaseUri() {
		return baseUri;
	}

	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	public boolean fullyQualifiedLinks() {
		return fullyQualifiedLinks;
	}

	public void setFullyQualifiedLinks(boolean fullyQualifiedLinks) {
		this.fullyQualifiedLinks = fullyQualifiedLinks;
	}

	public StoreCorsRegistry getCorsRegistry() {
		return corsRegistry;
	}

	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		ContentHandlerMapping mapping = new ContentHandlerMapping(stores, this);
		mapping.setCorsConfigurations(this.getCorsRegistry().getCorsConfigurations());
		return mapping;
	}

	@Bean
	StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler() {
		return new StoreByteRangeHttpRequestHandler();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (ContentRestConfigurer configurer : configurers) {
			configurer.configure(this);
		}
	}

	@Configuration
	public static class WebConfig implements WebMvcConfigurer, InitializingBean {

		@Autowired
		private RestConfiguration config;

		@Autowired
		private ApplicationContext context;

		@Autowired(required = false)
		private Repositories repositories;

		@Autowired(required = false)
		private RepositoryInvokerFactory repoInvokerFactory;

		@Autowired
		private ContentStoreService stores;

		@Override
		public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {

			argumentResolvers.add(new ResourceHandlerMethodArgumentResolver(config, repositories, repoInvokerFactory, stores));
			argumentResolvers.add(new ResourceTypeMethodArgumentResolver(config, repositories, repoInvokerFactory, stores));
			argumentResolvers.add(new ResourceETagMethodArgumentResolver(config, repositories, repoInvokerFactory, stores));
			argumentResolvers.add(new ContentServiceHandlerMethodArgumentResolver(config, repositories, repoInvokerFactory, stores));
		}

		@Override
		public void afterPropertiesSet() throws Exception {

			if (repositories == null) {
				repositories = new Repositories(context);
			}

			if (repoInvokerFactory == null) {
				repoInvokerFactory = new DefaultRepositoryInvokerFactory(repositories);
			}
		}
	}
}
