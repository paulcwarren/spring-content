package org.springframework.content.jpa.boot.autoconfigure;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaDatabaseInitializer;
import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaProperties;
import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.support.TestEntity;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ContentJpaAutoConfigurationTest {

	// mocks
	private static ContentJpaDatabaseInitializer initializer;

	private ApplicationContextRunner contextRunner;

	{
		initializer = mock(ContentJpaDatabaseInitializer.class);

		Describe("ContentJpaAutoConfiguration", () -> {
			BeforeEach(() -> {
				contextRunner = new ApplicationContextRunner()
						.withConfiguration(AutoConfigurations.of(JpaContentAutoConfiguration.class));
			});
			It("should have a content repository", () -> {
				contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
					Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
					Assertions.assertThat(context).hasSingleBean(ContentJpaDatabaseInitializer.class);
				});
			});
			Context("when a custom bean configuration is used", () -> {
				It("should use the supplied custom bean", () -> {
					contextRunner.withUserConfiguration(CustomBeanConfig.class).run((context) -> {
						Assertions.assertThat(context).getBean(ContentJpaDatabaseInitializer.class).isEqualTo(initializer);
					});
				});
			});
			Context("when an explicit @EnableFilesystemStores is used", () -> {
				It("should load the context", () -> {
					contextRunner.withUserConfiguration(ConfigWithExplicitEnableJpaStores.class).run((context) -> {
						Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
						Assertions.assertThat(context).hasSingleBean(ContentJpaProperties.class);
						Assertions.assertThat(context).hasSingleBean(ContentJpaDatabaseInitializer.class);
					});

				});
			});
		});
	}

	@Configuration
	public static class JpaTestConfig {
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}
	}

	@SpringBootApplication(exclude={JpaVersionsAutoConfiguration.class,SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@Import(JpaTestConfig.class)
	@PropertySource("classpath:/default.properties")
	public static class TestConfig {
	}

	@SpringBootApplication(exclude={JpaVersionsAutoConfiguration.class,SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@Import(JpaTestConfig.class)
	public static class CustomBeanConfig extends TestConfig {
		@Bean
		public ContentJpaDatabaseInitializer initializer() {
			return initializer;
		}
	}

	@SpringBootApplication(exclude={JpaVersionsAutoConfiguration.class,SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@Import(JpaTestConfig.class)
	@EnableJpaStores
	public static class ConfigWithExplicitEnableJpaStores {}

	@SpringBootApplication(exclude={JpaVersionsAutoConfiguration.class,SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@Import(JpaTestConfig.class)
	@PropertySource("classpath:/custom-jpa.properties")
	public static class CustomPropertiesConfig extends TestConfig {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}

	public interface TestEntityContentRepository extends JpaContentStore<TestEntity, String> {}
}
