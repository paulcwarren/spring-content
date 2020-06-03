package org.springframework.content.docx4j.config;

import internal.org.springframework.content.docx4j.WordToPdfRenditionProvider;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = WordToPdfRenditionProvider.class)
public class Docx4jConfiguration {
}
