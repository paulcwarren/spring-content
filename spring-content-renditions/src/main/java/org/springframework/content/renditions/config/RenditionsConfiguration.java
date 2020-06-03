package org.springframework.content.renditions.config;

import org.springframework.content.renditions.renderers.PdfToJpegRenderer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses=PdfToJpegRenderer.class)
public class RenditionsConfiguration {
}
