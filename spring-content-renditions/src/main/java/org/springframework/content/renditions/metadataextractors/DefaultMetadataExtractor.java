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
package org.springframework.content.renditions.metadataextractors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.content.commons.renditions.MetadataExtractor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

/**
 * A service implementation of the {@link MetadataExtractor} interface that extracts base metadata
 * from a given file and provides it as a map of key-value pairs.
 * <p>
 * This implementation retrieves detailed metadata using Java NIO's file attributes and utility
 * classes. Metadata extracted by this class includes:
 * <ul>
 *     <li><b>fileName</b></li>
 *     <li><b>fileExtension</b></li>
 *     <li><b>size</b></li>
 *     <li><b>creationTime</b></li>
 *     <li><b>lastModifiedTime</b></li>
 *     <li><b>lastAccessTime</b></li>
 * </ul>
 * </p>
 * <p>
 * If an error occurs during metadata extraction, a {@link MetadataExtractionException} is
 * thrown, encapsulating the root cause of the error. This class also logs the extraction process
 * for debugging purposes.
 * </p>
 *
 * @author marcobelligoli
 */
@Service
public class DefaultMetadataExtractor implements MetadataExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMetadataExtractor.class);

    @Override
    public Map<String, Object> extractMetadata(File file) {

        LOGGER.debug("Starting extractMetadata...");
        Map<String, Object> metadata = new HashMap<>();
        try {
            if (file != null) {
                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                metadata.put("fileName", file.getName());
                metadata.put("fileExtension", FilenameUtils.getExtension(file.getName()));
                metadata.put("size", attr.size());
                metadata.put("mimeType", Files.probeContentType(file.toPath()));
                metadata.put("creationTime", attr.creationTime().toString());
                metadata.put("lastModifiedTime", attr.lastModifiedTime().toString());
                metadata.put("lastAccessTime", attr.lastAccessTime().toString());
            }
        }
        catch (IOException e) {
            throw new MetadataExtractionException(e);
        }
        LOGGER.debug("extractMetadata done");
        return metadata;
    }
}
