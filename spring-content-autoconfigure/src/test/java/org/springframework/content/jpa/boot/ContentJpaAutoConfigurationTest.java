package org.springframework.content.jpa.boot;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.jpa.boot.autoconfigure.ContentJpaDatabaseInitializer;
import internal.org.springframework.content.jpa.config.JpaStorePropertiesImpl;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
public class ContentJpaAutoConfigurationTests {

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
		});

	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig {

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
			factory.setPackagesToScan("examples"); // Tell Hibernate where to find
													// Entities
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

	@Configuration
	public static class CustomBeanConfig extends TestConfig {
		@Bean
		public ContentJpaDatabaseInitializer initializer() {
			return initializer;
		}
	}

	@Configuration
	@PropertySource("classpath:/custom-jpa.properties")
	public static class CustomPropertiesConfig extends TestConfig {
	}

	@Entity
	@Content
	public class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
