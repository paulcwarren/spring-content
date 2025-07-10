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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.metadataextraction.MetadataExtractionService;
import org.springframework.content.commons.metadataextraction.MetadataExtractor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the {@link MetadataExtractionService} interface.
 *
 * @author marcobelligoli
 */
public class MetadataExtractionServiceImpl implements MetadataExtractionService {

    private final List<MetadataExtractor> metadataExtractorList = new ArrayList<>();

    @Autowired(required = false)
    public MetadataExtractionServiceImpl(MetadataExtractor... metadataExtractors) {

        Collections.addAll(this.metadataExtractorList, metadataExtractors);
    }

    @Override
    public Map<String, Object> extractMetadata(File file) {

        Map<String, Object> fullMetadataMap = new HashMap<>();
        for (var metadataExtractor : metadataExtractorList) {
            fullMetadataMap.putAll(metadataExtractor.extractMetadata(file));
        }
        return fullMetadataMap;
    }
}
