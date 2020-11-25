package internal.org.springframework.content.rest.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@Configuration
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
@EnableTransactionManagement
// @Import(RepositoryRestMvcConfiguration.class)
@EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.support")
@Profile("store")
public class BaseUriConfig extends JpaInfrastructureConfig {

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

	@Bean
	public RepositoryRestConfigurer repositoryRestConfigurer() {
		return new RepositoryRestConfigurer() {

			@Override
			public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
				config.setBasePath("/api");
			}
		};
	}

	@Bean
	public ContentRestConfigurer configurer() {
		return new ContentRestConfigurer() {
			@Override
			public void configure(RestConfiguration config) {
				config.setBaseUri(URI.create("/contentApi"));
			}
		};
	}

	@Bean
	public RenditionProvider textToHtml() {
		return new RenditionProvider() {

			@Override
			public String consumes() {
				return "text/plain";
			}

			@Override
			public String[] produces() {
				return new String[] { "text/html" };
			}

			@Override
			public InputStream convert(InputStream fromInputSource, String toMimeType) {
				String input = null;
				try {
					input = IOUtils.toString(fromInputSource);
				}
				catch (IOException e) {
				}
				return new ByteArrayInputStream(
						String.format("<html><body>%s</body></html>", input).getBytes());
			}
		};
	}
}
