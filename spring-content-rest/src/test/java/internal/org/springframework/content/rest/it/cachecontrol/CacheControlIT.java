package internal.org.springframework.content.rest.it.cachecontrol;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.it.SecurityConfiguration;
import internal.org.springframework.content.rest.support.TestEntity;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.CacheControl;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.context.WebApplicationContext;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = CacheControlIT.Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class CacheControlIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CacheControlRepository repo;

    @Autowired
    private CacheControlStore store;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    {
        Describe("CacheControl", () -> {

            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });

            It("given the pattern matches it should apply the configured cache settings", () -> {

                TestEntity tentity = new TestEntity();
                tentity = repo.save(tentity);
                tentity = store.setContent(tentity, new ByteArrayInputStream("some content".getBytes()));
                tentity.setMimeType("text/plain");
                tentity = repo.save(tentity);

                given()
                        .accept("text/plain")
                .when()
                    .get("/testEntities/" + tentity.getId())
                .then()
                    .statusCode(200)
                    .header("Cache-Control", "max-age=60");
            });

            It("given the pattern doesn't match it should not apply the configured cache settings", () -> {

                TestEntity tentity = new TestEntity();
                tentity = repo.save(tentity);
                tentity = store.setContent(tentity, new ByteArrayInputStream("some content".getBytes()));
                tentity.setMimeType("text/plain");
                tentity = repo.save(tentity);

                given()
                        .accept("text/plain")
                .when()
                    .get("/testEntities/" + tentity.getId() + "/content")
                .then()
                    .statusCode(200)
                    .header("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            });
        });
    }

    public interface CacheControlRepository extends CrudRepository<TestEntity, Long> {
    }

    private interface CacheControlStore extends FilesystemContentStore<TestEntity, UUID> {
    }

    @SpringBootApplication(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class
    })
    public static class Application {

       public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

       @Configuration
       @Import({RestConfiguration.class, SecurityConfiguration.class})
       @EnableJpaRepositories(basePackages="internal.org.springframework.content.rest.it.cachecontrol", considerNestedRepositories = true)
       @EnableFilesystemStores(basePackages="internal.org.springframework.content.rest.it.cachecontrol")
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
               factory.setPackagesToScan("internal.org.springframework.content.rest.support");
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
           public ContentRestConfigurer configurer() {

               return new ContentRestConfigurer() {
                   @Override
                   public void configure(RestConfiguration config) {
                       config
                           .cacheControl()
                               .antMatcher("/testEntities/*", CacheControl.maxAge(Duration.ofSeconds(60)));
                   }
               };
           }
       }
    }

    @Test
    public void noop() {
    }
}
