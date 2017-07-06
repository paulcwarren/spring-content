package internal.org.springframework.content.fs.config;

import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.config.FilesystemStoreConfigurer;
import org.springframework.content.fs.config.FilesystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;

@SuppressWarnings("deprecation")
@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class EnableFilesystemStoresTest {

	private AnnotationConfigApplicationContext context;
	
	// mocks
	static FilesystemStoreConfigurer configurer;
	
	{
		Describe("EnableFilesystemStores", () -> {

			Context("given a context and a configuartion with a filesystem content repository bean", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should have a ContentRepository bean", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
				});
				It("should have a filesystem conversion service bean", () -> {
					assertThat(context.getBean("filesystemStoreConverter"), is(not(nullValue())));
				});
				It("should have a FileSystemResourceLoader bean", () -> {
					assertThat(context.getBean("fileSystemResourceLoader"), is(not(nullValue())));
				});
			});

			Context("given a context with a configurer", () -> {
				BeforeEach(() -> {
					configurer = mock(FilesystemStoreConfigurer.class);
					
					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should call that configurer to help customize the store", () -> {
					verify(configurer).configureFilesystemStoreConverters(anyObject());
				});
			});
			
			Context("given a context with an empty configuration", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(EmptyConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should not contain any filesystem repository beans", () -> {
					try {
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					} catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});
		
		Describe("EnableFilesystemContentRepositories", () -> {

			Context("given a context and a configuration with a filesystem content repository bean", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(BackwardCompatibilityConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should have a ContentRepository bean", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
				});
			});
		});

	}


	@Test
	public void noop() {
	}

	@Configuration
	@EnableFilesystemStores(basePackages="contains.no.fs.repositories")
    @PropertySource("classpath:/test.properties")
	public static class EmptyConfig {
	}

	@Configuration
	@EnableFilesystemStores
	@PropertySource("classpath:/test.properties")
	public static class TestConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}
	}

	@Configuration
	@EnableFilesystemStores
	@PropertySource("classpath:/test.properties")
	public static class ConverterConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		public FilesystemStoreConverter<UUID,String> uuidConverter() {
			return new FilesystemStoreConverter<UUID,String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-","/"));
				}
			};
		}
		
		@Bean 
		public FilesystemStoreConfigurer configurer() {
			return configurer;
		}

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}
	}

	@EnableFilesystemContentRepositories
	@PropertySource("classpath:/test.properties")
	public static class BackwardCompatibilityConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}

	}

	@Content
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
