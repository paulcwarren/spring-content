package org.springframework.content.cmis.config;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.sql.DataSource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.cmis.CmisServiceBridge;
import org.junit.runner.RunWith;

import org.springframework.content.cmis.CmisDocument;
import org.springframework.content.cmis.CmisFolder;
import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.cmis.EnableCmis;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jRunner.class)
public class EnableCmisTest {

	private AnnotationConfigWebApplicationContext context;

	{
		Describe("EnableFilesystemStores", () -> {

			Context("given a context and a configuration that enables CMIS", () -> {

				BeforeEach(() -> {
					context = new AnnotationConfigWebApplicationContext();
					context.setServletContext(new MockServletContext());
					context.register(TestConfig.class);
					context.refresh();
				});

				It("should have a CmisServiceBridge bean", () -> {
					assertThat(context.getBean(CmisServiceBridge.class), is(not(nullValue())));
				});

				AfterEach(() -> {
					context.close();
				});
			});
		});
	}

	@EnableCmis(basePackages = "org.springframework.content.cmis.config",
			id = "1",
			name = "Example",
			description = "Spring Content Example",
			vendorName = "Spring Content",
			productName = "Spring Content CMIS Connector",
			productVersion = "1.0.0")
	@EnableJpaRepositories(considerNestedRepositories=true)
	@EnableFilesystemStores
	@Configuration
	public static class TestConfig {
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.H2).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.H2);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan("org.springframework.content.cmis.config");
			factory.setDataSource(dataSource());

			return factory;
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			JpaTransactionManager txManager = new JpaTransactionManager();
			txManager.setEntityManagerFactory(entityManagerFactory().getObject());
			return txManager;
		}

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
			return new FileSystemResourceLoader(Files.createTempDirectory("").toString());
		}

		@Bean
		public CmisNavigationService cmisNavigationService() {
			return new CmisNavigationService() {
				@Override
				public List getChildren(Object parent) {
					return null;
				}
			};
		}
	}

	@Entity
	@CmisDocument
	public static class Document {
		@Id
		@ContentId
		private Long id;
	}

	public interface DocumentRepository extends CrudRepository<Document, Long>{}

	public interface DocumentStorage extends ContentStore<Document, Long> {}

	@Entity
	@CmisFolder
	public static class Folder {
		@Id
		private Long id;
	}

	public interface FolderRepository extends CrudRepository<Folder, Long>{}
}