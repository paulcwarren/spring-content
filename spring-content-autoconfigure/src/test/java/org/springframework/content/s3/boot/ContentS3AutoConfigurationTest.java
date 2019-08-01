package org.springframework.content.s3.boot;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentS3AutoConfigurationTest {

	private static AmazonS3 client;

	static {
		client = mock(AmazonS3.class);
	}

	{
		Describe("FilesystemContentAutoConfiguration", () -> {
			Context("given a configuration with beans", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(client));

					context.close();
				});
			});

			Context("given a configuration without any beans", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfigWithoutBeans.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(not(nullValue())));

					context.close();
				});
			});

			Context("given a configuration with an explicit @EnableS3Stores annotation", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfigWithExplicitEnableS3Stores.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(not(nullValue())));

					context.close();
				});
			});
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig {

		@Bean
		public AmazonS3 s3Client() {
			return client;
		}
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableS3Stores
	public static class TestConfigWithExplicitEnableS3Stores {
		// will be supplied by auto-configuration
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends S3ContentStore<TestEntity, String> {
	}
}
