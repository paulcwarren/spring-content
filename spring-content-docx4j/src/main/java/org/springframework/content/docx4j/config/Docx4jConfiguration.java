package org.springframework.content.docx4j.config;

import internal.org.springframework.content.docx4j.JpegToPngRenditionProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = JpegToPngRenditionProvider.class)
public class Docx4jConfiguration {
}
