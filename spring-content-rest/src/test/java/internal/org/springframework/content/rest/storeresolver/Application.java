package internal.org.springframework.content.rest.storeresolver;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;


@SpringBootApplication
@ComponentScan(excludeFilters={
      @Filter(type = FilterType.REGEX,
            pattern = {
                  ".*MongoConfiguration"
      })
})
public class Application {

   public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
   
   @Configuration
   @Import({RestConfiguration.class, SecurityConfiguration.class})
   @EnableJpaRepositories(basePackages= "internal.org.springframework.content.rest.storeresolver", considerNestedRepositories = true)
   @EnableTransactionManagement
   @EnableJpaStores(basePackages="internal.org.springframework.content.rest.storeresolver")
   @EnableFilesystemStores(basePackages="internal.org.springframework.content.rest.storeresolver")
   public static class AppConfig {

       @Value("/org/springframework/content/jpa/schema-drop-h2.sql")
       private ClassPathResource dropReopsitoryTables;

       @Value("/org/springframework/content/jpa/schema-h2.sql")
       private ClassPathResource dataReopsitorySchema;

       @Bean
       DataSourceInitializer datasourceInitializer(DataSource dataSource) {
           ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

           databasePopulator.addScript(dropReopsitoryTables);
           databasePopulator.addScript(dataReopsitorySchema);
           databasePopulator.setIgnoreFailedDrops(true);

           DataSourceInitializer initializer = new DataSourceInitializer();
           initializer.setDataSource(dataSource);
           initializer.setDatabasePopulator(databasePopulator);

           return initializer;
       }

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
           factory.setPackagesToScan("internal.org.springframework.content.rest.storeresolver");
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
       public FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
           return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
       }

       @Bean
       public ContentRestConfigurer contentRestConfigurer() {
           return new ContentRestConfigurer() {
               @Override
               public void configure(RestConfiguration config) {
                   config.addStoreResolver("tEntities", new StoreResolver() {

                       @Override
                       public StoreInfo resolve(StoreInfo... stores) {
                           for (StoreInfo info : stores) {
                               if (info.getImplementation(FilesystemContentStore.class) != null) {
                                   return info;
                               }
                           }
                           return null;
                       }
                   });
               }
           };
        }
   }

   @Entity
   @Data
   public static class TEntity {

       @Id
       @GeneratedValue
       private Long id;

       @ContentId
       private String contentId;

       @ContentLength
       private Long contentLength;

       @MimeType
       private String mimeType;
   }

   public interface TEntityRepository extends JpaRepository<TEntity, Long> {}

   public interface TEntityFsStore extends FilesystemContentStore<TEntity, String> {}
   public interface TEntityJpaStore extends JpaContentStore<TEntity, String> {}
}
