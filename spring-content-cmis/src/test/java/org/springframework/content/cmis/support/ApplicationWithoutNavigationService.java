package org.springframework.content.cmis.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.cmis.configs.CmisConfig;
import org.springframework.content.cmis.configs.JpaConfig;
import org.springframework.content.cmis.configs.SecurityConfig;
import org.springframework.content.cmis.configs.StorageConfig;
import org.springframework.context.annotation.Import;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

@SpringBootApplication
@Import({
		JpaConfig.class,
		StorageConfig.class,
		JpaLockingAndVersioningConfig.class,
		SecurityConfig.class,
		CmisConfig.class
})
public class ApplicationWithoutNavigationService {

	public static void main(String[] args) {
		SpringApplication.run(ApplicationWithoutNavigationService.class, args);
	}
}
