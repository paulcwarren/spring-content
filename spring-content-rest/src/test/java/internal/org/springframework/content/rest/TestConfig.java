package internal.org.springframework.content.rest;

import java.io.File;

import org.springframework.content.fs.config.EnableFilesystemContentRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
//@Import(RepositoryRestMvcConfiguration.class)
@EnableFilesystemContentRepositories
public class TestConfig extends JpaInfrastructureConfig {
	@Bean
	public File filesystemRoot() {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
		filesystemRoot.mkdirs();
		return filesystemRoot;
	}
}

