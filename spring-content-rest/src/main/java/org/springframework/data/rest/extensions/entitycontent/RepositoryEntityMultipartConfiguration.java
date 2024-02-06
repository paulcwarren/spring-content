package org.springframework.data.rest.extensions.entitycontent;

import org.springframework.context.annotation.*;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;

@Configuration
@Conditional(SpringDataRestPresentCondition.class)
public class RepositoryEntityMultipartConfiguration {

    @Bean
    public RepositoryRestConfigurer entityMultipartHttpMessageConverterConfigurer() {
        return new RepositoryRestConfigurer() {
            public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
                // Add your custom message converter to the list
                messageConverters.add(new RepositoryEntityMultipartHttpMessageConverter(messageConverters));
            }
        };
    }

}
