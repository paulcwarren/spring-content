package org.springframework.content.cmis.configs;

import org.springframework.content.cmis.EnableCmis;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCmis(basePackages = "org.springframework.content.cmis.support",
		id = "1",
		name = "Example",
		description = "Spring Content Example",
		vendorName = "Spring Content",
		productName = "Spring Content CMIS Connector",
		productVersion = "1.0.0")
public class CmisConfig {
	//
}
