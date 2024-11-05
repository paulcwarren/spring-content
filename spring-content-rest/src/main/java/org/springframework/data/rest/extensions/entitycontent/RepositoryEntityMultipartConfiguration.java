package org.springframework.data.rest.extensions.entitycontent;

import org.springframework.context.annotation.*;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.List;

@Configuration
@Conditional(SpringDataRestPresentCondition.class)
public class RepositoryEntityMultipartConfiguration {

    @Bean
    public RepositoryRestConfigurer entityMultipartHttpMessageConverterConfigurer() {
        return new RepositoryRestConfigurer() {
            public void configureHttpMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
                var multipartConverters = messageConverters.stream()
                        .map(RepositoryEntityMultipartHttpMessageConverter::new)
                        .toList();

                messageConverters.addAll(multipartConverters);
            }
        };
    }

}
