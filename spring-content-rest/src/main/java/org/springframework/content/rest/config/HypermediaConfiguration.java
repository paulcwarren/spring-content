package org.springframework.content.rest.config;

import internal.org.springframework.content.rest.links.ContentLinksResourceProcessor;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;

@Configuration
@Import(RestConfiguration.class)
public class HypermediaConfiguration {

	@Bean
	public RepresentationModelProcessor<PersistentEntityResource> contentLinksProcessor(Stores stores, RestConfiguration config) {
		return new ContentLinksResourceProcessor(stores, config);
	}
}
