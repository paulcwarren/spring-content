package internal.org.springframework.content.rest.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.io.DefaultMediaResource;
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.MimeType;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@Configuration
@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
@EnableTransactionManagement
// @Import(RepositoryRestMvcConfiguration.class)
@EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.support")
@Profile("store")
public class StoreConfig extends JpaInfrastructureConfig {

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
	public RenditionProvider textToHtml() {
		System.out.println("adding renderer");
		return new RenditionProvider() {

			@Override
			public String consumes() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Boolean consumes(String s) {
				throw new UnsupportedOperationException();
			}

			@Override
			public String[] produces() {
				throw new UnsupportedOperationException();
			}

			@Override
			public Resource convert(Resource fromInputSource, String toMimeType) {
				String input = null;
				try {
					input = IOUtils.toString(fromInputSource.getInputStream());
				} catch (IOException e) {
				}
				return new DefaultMediaResource(
						new InputStreamResource(new ByteArrayInputStream(
								String.format("<html><body>%s</body></html>", input).getBytes())),
						"text/html", "some.html");
			}

			@Override
			public RenditionCapability isCapable(String fromMimeType, String toMimeType) {
				if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf("text/html"))
						&& MimeType.valueOf("text/plain").includes(MimeType.valueOf(fromMimeType)))
					return RenditionCapability.GOOD_CAPABILITY;
				return RenditionCapability.NOT_CAPABLE;
			}
		};
	}

}
