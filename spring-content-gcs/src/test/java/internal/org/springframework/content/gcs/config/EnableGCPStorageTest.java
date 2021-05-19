package internal.org.springframework.content.gcs.config;

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
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;

import lombok.Data;

@RunWith(Ginkgo4jRunner.class)
public class EnableGCPStorageTest {

	private AnnotationConfigApplicationContext context;

	// mocks
	static GCPStorageConfigurer configurer;
	static Storage client;

	{
		Describe("EnableGCPStorage", () -> {
			Context("given a context and a configuration with an GCS ContentStore",
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
							assertThat(context.getBean(TestEntityContentStore.class),
									is(not(nullValue())));
						});
						It("should have an Placement Service", () -> {
							assertThat(context.getBean("gcpStoragePlacementService"),
									is(not(nullValue())));
						});
					});

			Context("given a context with a configurer", () -> {
				BeforeEach(() -> {
					configurer = mock(GCPStorageConfigurer.class);

					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should call that configurer to help setup the store", () -> {
					verify(configurer).configureGCPStorageConverters(anyObject());
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
				It("should not contains any S3 repository beans", () -> {
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
	@EnableGCPStorage(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public GCPStorageConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public GCPStorageConfigurer configurer() {
			return new GCPStorageConfigurer() {

				@Override
				public void configureGCPStorageConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
	}

	@Configuration
	public static class InfrastructureConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
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
