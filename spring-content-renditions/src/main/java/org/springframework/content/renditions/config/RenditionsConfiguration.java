package org.springframework.content.renditions.config;

import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import internal.org.springframework.renditions.RenditionServiceImpl;

@Configuration
@ComponentScan(basePackageClasses=PdfToJpegRenderer.class)
public class RenditionsConfiguration {

    @Bean
    public RenditionService renditionService(RenditionProvider... providers) {
        return new RenditionServiceImpl(providers);
    }
}
