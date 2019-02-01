package org.springframework.content.rest.config;

import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers, org.springframework.data.rest.extensions")
public class RestConfiguration implements InitializingBean {

	private static final URI NO_URI = URI.create("");

	@Autowired
	ContentStoreService stores;

	@Autowired(required = false)
	private List<ContentRestConfigurer> configurers = new ArrayList<>();

	private URI baseUri = NO_URI;

	public URI getBaseUri() {
		return baseUri;
	}

	public void setBaseUri(URI baseUri) {
		this.baseUri = baseUri;
	}

	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		return new ContentHandlerMapping(stores, this);
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
}
