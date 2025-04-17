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
package org.springframework.content.commons.metadataextraction;

import java.io.File;
import java.util.Map;

/**
 * A service that extracts metadata from a given file.
 * <p>
 * The extracted metadata will be returned as a map where the keys represent
 * the metadata property names and the values represent the respective metadata values.
 * </p>
 * <p>
 * This service retrieves all instances of {@link MetadataExtractor} present in the Spring context and,
 * for each of them, performs metadata extraction from the provided file.
 * </p>
 *
 * @author marcobelligoli
 */
public interface MetadataExtractionService {

    /**
     * Extracts metadata from the specified file.
     *
     * @param file the file from which metadata will be extracted
     * @return a map containing the extracted metadata, where keys represent
     * metadata property names, and values represent the respective metadata values
     */
    Map<String, Object> extractMetadata(File file);
}
