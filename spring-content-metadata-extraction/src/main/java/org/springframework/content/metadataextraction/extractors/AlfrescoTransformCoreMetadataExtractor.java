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
package org.springframework.content.metadataextraction.extractors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;
import org.springframework.content.metadataextraction.MetadataExtractionException;
import org.springframework.content.metadataextraction.MetadataExtractionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the {@link MetadataExtractor} interface for extracting metadata
 * by leveraging Alfresco Transform Core service. This class communicates with the
 * Alfresco Transform Core service to retrieve metadata for a given file.
 * <p>
 * This service is enabled conditionally based on the presence of the
 * "alfresco.transform.core.url" property in the application configuration.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "alfresco.transform.core.url")
public class AlfrescoTransformCoreMetadataExtractor implements MetadataExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoTransformCoreMetadataExtractor.class);
    private static final String ALFRESCO_TRANSFORM_CORE_TRANSFORM_URL = "/transform";
    private static final String TARGET_MIME_TYPE_METADATA_EXTRACTOR = "alfresco-metadata-extract";
    private final String alfrescoTransformCoreUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AlfrescoTransformCoreMetadataExtractor(@Value("${alfresco.transform.core.url}") String alfrescoTransformCoreUrl, RestTemplate restTemplate,
        ObjectMapper objectMapper) {

        this.alfrescoTransformCoreUrl = alfrescoTransformCoreUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> extractMetadata(File file) {

        try {
            String url = alfrescoTransformCoreUrl + ALFRESCO_TRANSFORM_CORE_TRANSFORM_URL;

            MultiValueMap<String, Object> requestParams = new LinkedMultiValueMap<>();
            requestParams.put("file", Collections.singletonList(MetadataExtractionUtils.getByteArrayResourceFromFile(file)));
            String sourceMimeType = Files.probeContentType(file.toPath());
            requestParams.put("sourceMimetype", Collections.singletonList(sourceMimeType));
            requestParams.put("targetMimetype", Collections.singletonList(TARGET_MIME_TYPE_METADATA_EXTRACTOR));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

            LOGGER.debug("Calling POST {} with params {}...", url, requestParams);
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(requestParams, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
            LOGGER.debug("POST to {} done. Returned: {}", url, response.getBody());

            Map<String, Object> map;
            map = objectMapper.readValue(response.getBody(), new TypeReference<>() {

            });
            return map;
        }
        catch (IOException e) {
            throw new MetadataExtractionException(e);
        }
    }
}
