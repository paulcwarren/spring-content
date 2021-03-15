package internal.org.springframework.content.azure.config;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.azure.storage.blob.BlobServiceClientBuilder;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import lombok.Data;

@RunWith(Ginkgo4jRunner.class)
public class EnableAzureStorageTest {

	private AnnotationConfigApplicationContext context;

	// mocks
	static AzureStorageConfigurer configurer;
	static BlobServiceClientBuilder storage;

	{
		Describe("EnableAzureStorage", () -> {

			Context("given a context and a configuration with an Azure ContentStore",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(TestConfig.class);
							context.refresh();
						});

						AfterEach(() -> {
							context.close();
						});

						It("should have a ContentStore bean", () -> {
							assertThat(context.getBean(TestEntityContentStore.class), is(not(nullValue())));
						});

						It("should have an Placement Service", () -> {
							assertThat(context.getBean("azureStoragePlacementService"), is(not(nullValue())));
						});
					});

			Context("given a context with a configurer", () -> {

				BeforeEach(() -> {
					configurer = mock(AzureStorageConfigurer.class);

					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});

				AfterEach(() -> {
					context.close();
				});

				It("should call that configurer to help setup the store", () -> {
					verify(configurer).configureAzureStorageConverters(anyObject());
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

				It("should not contains any Azure Storage beans", () -> {
					try {
						context.getBean(TestEntityContentStore.class);
						fail("expected no such bean");
					}
					catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});
	}

	@Test
	public void noop() {
	}

	@Configuration
	@EnableAzureStorage(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return new AzureStorageConfigurer() {

				@Override
				public void configureAzureStorageConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
	}

	@Configuration
	public static class InfrastructureConfig {

        @Value("#{environment.AZURE_STORAGE_ENDPOINT}")
        private String endpoint;

        @Value("#{environment.AZURE_STORAGE_CONNECTION_STRING}")
        private String connString;

        @Bean
        public BlobServiceClientBuilder storage() {
                return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .connectionString(connString);
        }
	}

	@Data
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentStore
			extends ContentStore<TestEntity, String> {
	}
}
