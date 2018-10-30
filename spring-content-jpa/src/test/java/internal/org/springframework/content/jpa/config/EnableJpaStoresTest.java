package internal.org.springframework.content.jpa.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.jpa.config.EnableJpaContentRepositories;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1) // required
public class EnableJpaStoresTest {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	{
		Describe("EnableJpaStores", () -> {

			basicConfigurationTests(context, TestConfig.class);

			Context("given a context with an empty configuration", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(EmptyConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should not contain any jpa repository beans", () -> {
					try {
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					}
					catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});

		Describe("EnableJpaContentRepositories", () -> {
			basicConfigurationTests(context, EnableJpaContentRepositoriesConfig.class);
		});
	}

	private static void basicConfigurationTests(AnnotationConfigApplicationContext context, Class configClass) {
		Context("given a context and a configuration with a jpa content repository bean",
				() -> {
					BeforeEach(() -> {
						context.register(configClass);
						context.refresh();
					});
					AfterEach(() -> {
						context.close();
					});
					It("should have a content repository bean", () -> {
						assertThat(context.getBean(TestEntityContentRepository.class),
								is(not(nullValue())));
					});
					It("should have a delegating blob resource loader", () -> {
						assertThat(
								context.getBean(DelegatingBlobResourceLoader.class),
								is(not(nullValue())));
					});
					It("should have a generic blob resource loader", () -> {
						assertThat(context.getBean("genericBlobResourceLoader"),
								is(not(nullValue())));
					});
					It("should have a MySQL blob resource loader", () -> {
						BlobResourceLoader loader = (BlobResourceLoader)context.getBean("mysqlBlobResourceLoader");
						assertThat(loader, is(not(nullValue())));
						assertThat(loader.getDatabaseName(), is("MySQL"));
						assertThat(loader.getResource("some-id"), is(instanceOf(MySQLBlobResource.class)));
					});
					It("should have a SQL Server blob resource loader", () -> {
						BlobResourceLoader loader = (BlobResourceLoader)context.getBean("sqlServerBlobResourceLoader");
						assertThat(loader, is(not(nullValue())));
						assertThat(loader.getDatabaseName(), is("Microsoft SQL Server"));
						assertThat(loader.getResource("some-id"), is(instanceOf(SQLServerBlobResource.class)));
					});
				});

	}

	@Test
	public void noop() {
	}

	@Configuration
	@EnableJpaStores(basePackages = "contains.no.jpa.repositories")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableJpaStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableJpaContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableJpaContentRepositoriesConfig {
	}

	@Configuration
	@EnableTransactionManagement
	public static class InfrastructureConfig {
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

	@Content
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}

	@Repository
	public class JpaTestContentRepository implements TestEntityContentRepository {

		@PersistenceContext
		private EntityManager em;

		@Override
		public void setContent(TestEntity property, InputStream content) {
		}

		@Override
		public void unsetContent(TestEntity property) {
		}

		@Override
		public InputStream getContent(TestEntity property) {
			return null;
		}
	}
}
