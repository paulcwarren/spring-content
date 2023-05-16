package internal.org.springframework.content.rest.it.http_405;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.it.SecurityConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = MethodNotAllowedExceptionIT.Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class MethodNotAllowedExceptionIT {

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private UnexportedContentStore store;

    @Autowired
    private TestEntity2Repo repo2;

    @Autowired
    private TestEntity2Store store2;

    @Autowired
    private WebApplicationContext webApplicationContext;

    {
        Describe("when getContent method is not exported", () -> {

            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });

            It("should throw a 405 Not Allowed", () -> {

                TEntity tentity = new TEntity();
                tentity = store.setContent(tentity, new ByteArrayInputStream("some content".getBytes()));
                tentity = repo.save(tentity);

                given()
                    .accept("text/plain")
                .when()
                    .get("/tEntities/" + tentity.getId())
                .then()
                    .statusCode(405);
            });
        });

        Describe("when no setContent methods are not exported", () -> {

            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });

            FIt("should throw a 405 Not Allowed", () -> {

                TEntity tentity = new TEntity();
                tentity = repo.save(tentity);

                given()
                    .contentType("text/plain")
                    .body("some content".getBytes())
                .when()
                    .post("/tEntities/" + tentity.getId())
                .then()
                    .statusCode(405);
            });
        });

        Describe("when unsetContent method are not exported", () -> {

            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });

            It("should throw a 405 Not Allowed", () -> {

                TEntity tentity = new TEntity();
                tentity = store.setContent(tentity, new ByteArrayInputStream("some content".getBytes()));
                tentity = repo.save(tentity);

                given()
                    .accept("text/plain")
                .when()
                    .delete("/tEntities/" + tentity.getId())
                .then()
                    .statusCode(405);
            });
        });

        Describe("when a content property is not exported", () -> {
            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });
            It("should throw a 405 Not Allowed for all requests", () -> {
                TEntity2 tentity = new TEntity2();
                tentity = store2.setContent(tentity, new ByteArrayInputStream("some content".getBytes()));
                tentity = repo2.save(tentity);

                given()
                    .accept("text/plain")
                .when()
                    .get("/tEntity2s/" + tentity.getId() + "/content")
                .then()
                    .statusCode(405);

                given()
                    .contentType("text/plain")
                    .body("some content".getBytes())
                .when()
                    .post("/tEntity2s/" + tentity.getId() + "/content")
                .then()
                    .statusCode(405);

                given()
                    .accept("text/plain")
                .when()
                    .delete("/tEntity2s/" + tentity.getId() + "/content")
                .then()
                    .statusCode(405);
            });
        });
    }

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<TEntity, Long> {
    }

    public interface UnexportedContentStore extends FilesystemContentStore<TEntity, UUID> {

        @RestResource(exported=false)
        @Override
        public InputStream getContent(TEntity property, PropertyPath path);

        @RestResource(exported=false)
        @Override
        public TEntity setContent(TEntity property, PropertyPath path, InputStream content);

        @RestResource(exported=false)
        @Override
        public TEntity setContent(TEntity property, PropertyPath path, InputStream content, long contentLen);

        @RestResource(exported=false)
        @Override
        public TEntity setContent(TEntity property, PropertyPath path, Resource resourceContent);

        @RestResource(exported=false)
        @Override
        public TEntity unsetContent(TEntity property, PropertyPath path);
}

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TEntity {
        private @Id @GeneratedValue Long id;
        private @ContentId UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TEntity2 {
        private @Id @GeneratedValue Long id;
        private @ContentId @RestResource(exported=false) UUID contentId;
        private @ContentLength Long len;
        private @MimeType String mimeType;
    }

    public interface TestEntity2Repo extends CrudRepository<TEntity2, Long> {}
    public interface TestEntity2Store extends FilesystemContentStore<TEntity2, UUID> {}

    @SpringBootApplication(exclude = {
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class,
            MongoRepositoriesAutoConfiguration.class
    })
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Import({RestConfiguration.class, SecurityConfiguration.class})
        @EnableJpaRepositories(basePackages="internal.org.springframework.content.rest.it.http_405", considerNestedRepositories = true)
        @EnableFilesystemStores(basePackages="internal.org.springframework.content.rest.it.http_405")
        public class TestConfig {

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
                factory.setPackagesToScan("internal.org.springframework.content.rest.it.http_405");
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
            public FileSystemResourceLoader filesystemRoot() throws IOException {
                return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
            }
        }
    }

    @Test
    public void noop() {
    }
}