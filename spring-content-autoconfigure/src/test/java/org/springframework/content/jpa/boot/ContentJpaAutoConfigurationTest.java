package org.springframework.content.jpa.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaDatabaseInitializer;
import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaProperties;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ContentJpaAutoConfigurationTest {

	private AnnotationConfigApplicationContext context;

	// mocks
	private static ContentJpaDatabaseInitializer initializer;

	{
		initializer = mock(ContentJpaDatabaseInitializer.class);

		Describe("ContentJpaAutoConfiguration", () -> {
			BeforeEach(() -> {
				context = new AnnotationConfigApplicationContext();
				context.register(TestConfig.class);
			});
			JustBeforeEach(() -> {
				context.refresh();
			});
			AfterEach(() -> {
				context.close();
			});
			It("should have a content repository", () -> {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			});
			It("should have a database initializer", () -> {
				assertThat(context.getBean(ContentJpaDatabaseInitializer.class),
						is(not(nullValue())));
			});
			Context("when a custom bean configuration is used", () -> {
				BeforeEach(() -> {
					context.register(CustomBeanConfig.class);
				});
				It("should use the supplied custom bean", () -> {
					assertThat(context.getBean(ContentJpaDatabaseInitializer.class),
							is(initializer));
				});
			});
			Context("when an explicit @EnableFilesystemStores is used", () -> {
				BeforeEach(() -> {
					context.register(ConfigWithExplicitEnableJpaStores.class);
				});
				It("should load the context", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(ContentJpaProperties.class), is(not(nullValue())));
					assertThat(context.getBean(ContentJpaDatabaseInitializer.class), is(not(nullValue())));
				});
			});
		});
	}

	@Configuration
	@PropertySource("classpath:/default.properties")
	@EnableAutoConfiguration(exclude= MongoAutoConfiguration.class)
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class TestConfig {
	}

	@Configuration
	@EnableAutoConfiguration(exclude=MongoAutoConfiguration.class)
	public static class CustomBeanConfig extends TestConfig {
		@Bean
		public ContentJpaDatabaseInitializer initializer() {
			return initializer;
		}
	}

	@Configuration
	@EnableJpaStores
	public static class ConfigWithExplicitEnableJpaStores {}

	@Configuration
	@PropertySource("classpath:/custom-jpa.properties")
	public static class CustomPropertiesConfig extends TestConfig {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}

	public interface TestEntityContentRepository extends JpaContentStore<TestEntity, String> {}
}
