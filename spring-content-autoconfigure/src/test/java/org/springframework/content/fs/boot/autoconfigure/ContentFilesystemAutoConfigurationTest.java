package org.springframework.content.fs.boot.autoconfigure;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentFilesystemAutoConfigurationTest {

	private ApplicationContextRunner contextRunner;

	{
		Describe("FilesystemContentAutoConfiguration", () -> {
			BeforeEach(() -> {
				contextRunner = new ApplicationContextRunner()
						.withConfiguration(AutoConfigurations.of(FilesystemContentAutoConfiguration.class));
			});
			Context("given a default configuration", () -> {
				It("should load the context", () -> {
					contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
						Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
					});
				});
			});

			Context("given an environment specifying a filesystem root using spring prefix", () -> {
				BeforeEach(() -> {
					System.setProperty("spring.content.fs.filesystem-root",
							"${java.io.tmpdir}/UPPERCASE/NOTATION/");
				});
				AfterEach(() -> {
					System.clearProperty("spring.content.fs.filesystem-root");
				});
				It("should have a filesystem properties bean with the correct root set",
						() -> {
							contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
								Assertions.assertThat(context).hasSingleBean(FilesystemContentAutoConfiguration.FilesystemProperties.class);
								Assertions.assertThat(context).getBean(FilesystemContentAutoConfiguration.FilesystemProperties.class).extracting("filesystemRoot").matches((val) -> val.toString().endsWith("/UPPERCASE/NOTATION/"));
							});
						});
			});

			Context("given a configuration that contributes a loader bean", () -> {
				It("should have that loader bean in the context", () -> {
					contextRunner.withUserConfiguration(ConfigWithLoaderBean.class).run((context) -> {
						Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
						Assertions.assertThat(context).getBean(FileSystemResourceLoader.class).extracting("filesystemRoot").matches((val) -> val.toString().endsWith("/some/random/path/"));
					});
				});
			});

			Context("given a configuration with explicit @EnableFilesystemStores annotation", () -> {
				It("should load the context", () -> {
					contextRunner.withUserConfiguration(ConfigWithExplicitEnableFilesystemStores.class).run((context) -> {
						Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
						Assertions.assertThat(context).getBean(FileSystemResourceLoader.class);
					});
				});
			});
		});
	}

	@SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	public static class TestConfig {
	}

	@SpringBootApplication
	public static class ConfigWithLoaderBean {

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader("/some/random/path/");
		}

	}

	@SpringBootApplication
	@EnableFilesystemStores
	public static class ConfigWithExplicitEnableFilesystemStores {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, String> {
	}
}
