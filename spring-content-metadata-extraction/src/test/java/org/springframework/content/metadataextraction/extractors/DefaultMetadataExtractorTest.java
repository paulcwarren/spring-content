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

import org.junit.Test;
import org.springframework.content.metadataextraction.MetadataExtractionException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit test class for the {@link DefaultMetadataExtractor}.
 *
 * <p>This class contains test cases to validate the functionality of the
 * {@link DefaultMetadataExtractor#extractMetadata(File)} method.</p>
 *
 * <p>The following test scenarios are covered:</p>
 * <ul>
 *   <li>Test the extraction of metadata from a valid file.</li>
 *   <li>Test the behavior when a null file is provided.</li>
 *   <li>Test the handling of {@link IOException} during metadata extraction.</li>
 * </ul>
 *
 * @author marcobelligoli
 */
public class DefaultMetadataExtractorTest {

    private final DefaultMetadataExtractor metadataExtractor = new DefaultMetadataExtractor();

    @Test
    public void testExtractMetadataWithValidFile()
        throws URISyntaxException {

        File input = getFile();

        var result = metadataExtractor.extractMetadata(input);

        assertNotNull(result);
        assertNotNull(result.get("fileName"));
        assertNotNull(result.get("lastModifiedTime"));
        assertNotNull(result.get("lastAccessTime"));
        assertNotNull(result.get("size"));
        assertNotNull(result.get("mimeType"));
        assertNotNull(result.get("creationTime"));
        assertNotNull(result.get("fileExtension"));
    }

    @Test
    public void testExtractMetadataWithNullFile() {

        Map<String, Object> metadata = metadataExtractor.extractMetadata(null);
        assertTrue(metadata.isEmpty());
    }

    @Test
    public void testExtractMetadataWithIOException() {

        File nonExistentFile = new File("/path/to/nonexistent/file");

        assertThrows(MetadataExtractionException.class, () -> metadataExtractor.extractMetadata(nonExistentFile));
    }

    private static File getFile()
        throws URISyntaxException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource("sample.jpeg");
        assert resource != null;
        return new File(resource.toURI());
    }
}
