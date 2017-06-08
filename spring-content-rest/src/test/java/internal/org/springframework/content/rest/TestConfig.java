package internal.org.springframework.content.rest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;

@Configuration
@EnableJpaRepositories
@EnableTransactionManagement
//@Import(RepositoryRestMvcConfiguration.class)
@EnableFilesystemStores
public class TestConfig extends JpaInfrastructureConfig {

	@Bean
	FileSystemResourceLoader fileSystemResourceLoader() {
		return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
	}

	@Bean
	public File filesystemRoot() {
		File baseDir = new File(System.getProperty("java.io.tmpdir"));
		File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
		filesystemRoot.mkdirs();
		return filesystemRoot;
	}

}

