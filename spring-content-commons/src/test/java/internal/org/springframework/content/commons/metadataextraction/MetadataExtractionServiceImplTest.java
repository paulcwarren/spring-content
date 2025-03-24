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
package internal.org.springframework.content.commons.metadataextraction;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tests the `extractMetadata` method in the `MetadataExtractionServiceImpl` class.
 * The method is responsible for iterating over multiple `MetadataExtractor` instances and aggregating
 * the metadata they extract into a single map.
 *
 * @author marcobelligoli
 */
public class MetadataExtractionServiceImplTest {

    @Test
    public void testExtractMetadataWithSingleExtractor() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor);

        Map<String, Object> mockMetadata = new HashMap<>();
        mockMetadata.put("author", "John Doe");
        Mockito.when(mockExtractor.extractMetadata(mockFile)).thenReturn(mockMetadata);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("John Doe", result.get("author"));
        Mockito.verify(mockExtractor).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithMultipleExtractors() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor1 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractor mockExtractor2 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor1, mockExtractor2);

        Map<String, Object> mockMetadata1 = new HashMap<>();
        mockMetadata1.put("author", "John Doe");
        Mockito.when(mockExtractor1.extractMetadata(mockFile)).thenReturn(mockMetadata1);

        Map<String, Object> mockMetadata2 = new HashMap<>();
        mockMetadata2.put("title", "Sample Document");
        Mockito.when(mockExtractor2.extractMetadata(mockFile)).thenReturn(mockMetadata2);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(2, result.size());
        Assert.assertEquals("John Doe", result.get("author"));
        Assert.assertEquals("Sample Document", result.get("title"));
        Mockito.verify(mockExtractor1).extractMetadata(mockFile);
        Mockito.verify(mockExtractor2).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithConflictingKeys() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor1 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractor mockExtractor2 = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor1, mockExtractor2);

        Map<String, Object> mockMetadata1 = new HashMap<>();
        mockMetadata1.put("key", "value1");
        Mockito.when(mockExtractor1.extractMetadata(mockFile)).thenReturn(mockMetadata1);

        Map<String, Object> mockMetadata2 = new HashMap<>();
        mockMetadata2.put("key", "value2");
        Mockito.when(mockExtractor2.extractMetadata(mockFile)).thenReturn(mockMetadata2);

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("value2", result.get("key")); // Last extractor's value overrides
        Mockito.verify(mockExtractor1).extractMetadata(mockFile);
        Mockito.verify(mockExtractor2).extractMetadata(mockFile);
    }

    @Test
    public void testExtractMetadataWithNoExtractors() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl();

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractMetadataWithEmptyMetadataFromExtractor() {

        File mockFile = Mockito.mock(File.class);
        MetadataExtractor mockExtractor = Mockito.mock(MetadataExtractor.class);
        MetadataExtractionServiceImpl service = new MetadataExtractionServiceImpl(mockExtractor);

        Mockito.when(mockExtractor.extractMetadata(mockFile)).thenReturn(new HashMap<>());

        Map<String, Object> result = service.extractMetadata(mockFile);

        Assert.assertTrue(result.isEmpty());
        Mockito.verify(mockExtractor).extractMetadata(mockFile);
    }
}