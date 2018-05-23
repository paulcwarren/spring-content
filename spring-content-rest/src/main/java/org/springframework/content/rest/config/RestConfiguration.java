package org.springframework.content.rest.config;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.web.config.HateoasAwareSpringDataWebConfiguration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import internal.org.springframework.content.rest.mappings.ContentHandlerMapping;
import internal.org.springframework.content.rest.mappings.StoreByteRangeHttpRequestHandler;

@Configuration
@ComponentScan("internal.org.springframework.content.rest.controllers")
public class RestConfiguration extends HateoasAwareSpringDataWebConfiguration {
	
	@Autowired 
	ContentStoreService stores;

	public RestConfiguration(ApplicationContext context, @Qualifier("mvcConversionService") ObjectFactory<ConversionService> conversionService) {
		super(context, conversionService);
	}

	@Bean
	RequestMappingHandlerMapping contentHandlerMapping() {
		return new ContentHandlerMapping(stores);
	}
	
	@Bean
	StoreByteRangeHttpRequestHandler byteRangeRestRequestHandler() {
		return new StoreByteRangeHttpRequestHandler();
	}
}
