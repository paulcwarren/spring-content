package org.springframework.content.cmis.support;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.cmis.CmisNavigationService;
import org.springframework.content.cmis.EnableCmis;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@EnableCmis(basePackages = "org.springframework.content.cmis.support",
			id = "1",
			name = "Example",
			description = "Spring Content Example",
			vendorName = "Spring Content",
			productName = "Spring Content CMIS Connector",
			productVersion = "1.0.0")
	@EnableJpaRepositories(
			basePackages={"org.springframework.content.cmis.support",
						  "org.springframework.versions"},
			considerNestedRepositories=true)
	@EnableFilesystemStores
	@Import(JpaLockingAndVersioningConfig.class)
	@EnableAutoConfiguration
	@Configuration
	public static class ApplicationConfig {
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
			factory.setPackagesToScan("org.springframework.content.cmis.support");
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
		public CmisNavigationService cmisNavigationService(FolderRepository folders, DocumentRepository docs) {

			return new CmisNavigationService<Folder>() {
				@Override
				public List getChildren(Folder parent) {

					List<Object> children = new ArrayList<>();
					List<Folder> folderChildern = folders.findAllByParent(parent);
					List<Document> documentChildren = docs.findAllByParent(parent);
					children.addAll(folderChildern);
					children.addAll(documentChildren);
					return children;
				}
			};
		}
	}
}
