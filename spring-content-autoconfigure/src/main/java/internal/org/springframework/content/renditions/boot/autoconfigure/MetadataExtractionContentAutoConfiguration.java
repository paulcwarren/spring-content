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
package internal.org.springframework.content.renditions.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.content.renditions.config.MetadataExtractionConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Autoconfiguration class for enabling metadata extraction functionality in a Spring Boot application.
 * <p>
 * This configuration class is activated when the {@link MetadataExtractionConfiguration} class is present in the classpath.
 * </p>
 * <p>
 * By including this class, metadata extraction features are automatically configured without requiring explicit registration
 * of the necessary components, simplifying integration into the application context.
 * </p>
 *
 * @author marcobelligoli
 */
@Configuration
@ConditionalOnClass(MetadataExtractionConfiguration.class)
@Import(MetadataExtractionConfiguration.class)
public class MetadataExtractionContentAutoConfiguration {

}
