package internal.org.springframework.content.s3.config;

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

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.AbstractS3ContentRepositoryConfiguration;
import org.springframework.content.s3.config.EnableS3ContentRepositories;
import org.springframework.content.s3.config.S3StoreConverter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class EnableS3ContentRepositoriesTest {

	private AnnotationConfigApplicationContext context;
	{
		Describe("EnableS3ContentRepositories", () -> {
			Context("given a context and a configuartion with an S3 content repository bean", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should have a Content Repository bean", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
				});
				It("should have an s3 store converter", () -> {
					assertThat(context.getBean("s3StoreConverter"), is(not(nullValue())));
				});
			});
			
			Context("given a context with a custom converter", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should use that converter", () -> {
					ConversionService converters = (ConversionService) context.getBean("s3StoreConverter");
					assertThat(converters.convert(UUID.fromString("e49d5464-26ce-11e7-93ae-92361f002671"), String.class), is("/e49d5464/26ce/11e7/93ae/92361f002671"));
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
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					} catch (NoSuchBeanDefinitionException e) {
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
	@EnableS3ContentRepositories(basePackages="contains.no.fs.repositores")
//	@EnableContextResourceLoader
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableS3ContentRepositories
//	@EnableContextResourceLoader
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}
	
	@Configuration
	@EnableS3ContentRepositories
//	@EnableContextResourceLoader
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public S3StoreConverter<UUID,String> uuidConverter() {
			return new S3StoreConverter<UUID,String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-","/"));
				}

			};
		}
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractS3ContentRepositoryConfiguration {

		@Autowired
		private AmazonS3 client;
		
		public Region region() {
			return Region.getRegion(Regions.US_WEST_1);
		}
		
		@Override
		public SimpleStorageResourceLoader simpleStorageResourceLoader() {
	        client.setRegion(region());
			return new SimpleStorageResourceLoader(client);
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
