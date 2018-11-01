package org.springframework.content.rest.config;

import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers")
public class RestConfiguration {

	@Autowired
	ContentStoreService stores;

	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		return new ContentHandlerMapping(stores);
	}

	@Bean
	StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler() {
		return new StoreByteRangeHttpRequestHandler();
	}
}
