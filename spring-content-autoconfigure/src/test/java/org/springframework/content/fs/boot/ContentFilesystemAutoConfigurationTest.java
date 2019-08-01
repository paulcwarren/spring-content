package org.springframework.content.fs.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentFilesystemAutoConfigurationTest {

	{
		Describe("FilesystemContentAutoConfiguration", () -> {
			Context("given a default configuration", () -> {
				It("should load the context", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));

					context.close();
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
							AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
							context.register(TestConfig.class);
							context.refresh();

							assertThat(context.getBean(
									FilesystemContentAutoConfiguration.FilesystemProperties.class)
									.getFilesystemRoot(),
									endsWith("/UPPERCASE/NOTATION/"));

							context.close();
						});
			});

			Context("given a configuration that contributes a loader bean", () -> {
				It("should have that loader bean in the context", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(ConfigWithLoaderBean.class);
					context.refresh();

					FileSystemResourceLoader loader = context
							.getBean(FileSystemResourceLoader.class);
					assertThat(loader.getFilesystemRoot(), is("/some/random/path/"));

					context.close();
				});
			});

			Context("given a configuration with explicit @EnableFilesystemStores annotation", () -> {
				It("should load the context", () -> {
					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(ConfigWithExplicitEnableFilesystemStores.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(FileSystemResourceLoader.class), is(not(nullValue())));

					context.close();
				});
			});

		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class TestConfig {
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class ConfigWithLoaderBean {

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader("/some/random/path/");
		}

	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	@EnableFilesystemStores
	public static class ConfigWithExplicitEnableFilesystemStores {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, String> {
	}
}
