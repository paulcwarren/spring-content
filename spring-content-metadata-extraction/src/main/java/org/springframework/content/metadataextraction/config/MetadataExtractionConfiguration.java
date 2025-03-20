/* Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License. */
package org.springframework.content.metadataextraction.config;

import internal.org.springframework.content.commons.renditions.MetadataExtractionServiceImpl;
import org.springframework.content.commons.renditions.MetadataExtractionService;
import org.springframework.content.commons.renditions.MetadataExtractor;
import org.springframework.content.metadataextraction.extractors.DefaultMetadataExtractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class for metadata extraction services.
 * <p>
 * This configuration enables component scanning to detect and register beans related to
 * metadata extraction, including implementations of {@link MetadataExtractor}.
 * It also defines a bean for the {@link MetadataExtractionService}, which aggregates
 * various {@link MetadataExtractor} implementations to perform metadata extraction
 * on a given file.
 * </p>
 *
 * @author marcobelligoli
 */
@Configuration
@ComponentScan(basePackageClasses = DefaultMetadataExtractor.class)
public class MetadataExtractionConfiguration {

    @Bean
    public MetadataExtractionService metadataExtractionService(MetadataExtractor... metadataExtractors) {

        return new MetadataExtractionServiceImpl(metadataExtractors);
    }
}
