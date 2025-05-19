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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.content.metadataextraction.MetadataExtractionException;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for validating the functionality of the {@link AlfrescoTransformCoreMetadataExtractor}.
 * This test suite contains unit tests to ensure correct metadata extraction behavior,
 * proper handling of errors, and correct communication with the Alfresco Transform Core service.
 * <p>
 * It sets up a mock wire server to simulate the behavior of the Alfresco Transform Core service.
 * Additionally, it verifies both successful metadata extraction and error scenarios.
 * </p>
 */
class AlfrescoTransformCoreMetadataExtractorTest {

    private static final String ALFRESCO_TRANSFORM_CORE_HOST = "localhost";
    private static WireMockServer wireMockServer;
    private static AlfrescoTransformCoreMetadataExtractor metadataExtractor;

    @BeforeAll
    static void setup() {

        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        var port = wireMockServer.port();
        WireMock.configureFor(ALFRESCO_TRANSFORM_CORE_HOST, port);
        var baseUrl = String.format("http://%s:%d", ALFRESCO_TRANSFORM_CORE_HOST, port);
        metadataExtractor = new AlfrescoTransformCoreMetadataExtractor(baseUrl);
    }

    @AfterAll
    static void tearDown() {

        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static void mockAlfrescoTransformCoreResponse(String response) {

        stubFor(post(urlEqualTo("/transform")).willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));
    }

    @Test
    void testExtractMetadata()
        throws URISyntaxException {

        File input = getFile();

        mockAlfrescoTransformCoreResponse(
            "{\"{http://www.alfresco.org/model/content/1.0}author\":\"John Doe\",\"{http://www.alfresco.org/model/content/1.0}created\":\"2024-04-16T07:39:22Z\",\"{http://www.alfresco.org/model/content/1.0}title\":null}");

        var result = metadataExtractor.extractMetadata(input);

        assertNotNull(result);
        assertEquals("John Doe", result.get("{http://www.alfresco.org/model/content/1.0}author"));
        assertEquals("2024-04-16T07:39:22Z", result.get("{http://www.alfresco.org/model/content/1.0}created"));
        assertNull(result.get("{http://www.alfresco.org/model/content/1.0}title"));
    }

    @Test
    void testExtractMetadataWithError()
        throws URISyntaxException {

        File input = getFile();

        mockAlfrescoTransformCoreResponse("{\"{http://www.alfresco.org/model/content/1.0}title:null}");

        assertThrows(MetadataExtractionException.class, () -> metadataExtractor.extractMetadata(input));
    }

    private static File getFile()
        throws URISyntaxException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("sample.jpeg");
        assert resource != null;
        return new File(resource.toURI());
    }
}
