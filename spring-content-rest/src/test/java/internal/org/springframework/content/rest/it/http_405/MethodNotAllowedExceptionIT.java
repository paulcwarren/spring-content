package internal.org.springframework.content.rest.it.http_405;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.jayway.restassured.RestAssured.given;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
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

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;

import internal.org.springframework.content.rest.it.SecurityConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = MethodNotAllowedExceptionIT.Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class MethodNotAllowedExceptionIT {

    @Autowired
    private PreferResourceForPutsAndPostsRepository repo;

    @Autowired
    private UnexportedContentStore store;

    @LocalServerPort
    int port;

    {
        Describe("when getContent method is not exported", () -> {

            BeforeEach(() -> {
                RestAssured.port = port;
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

        Describe("when no setContent methods are exported", () -> {

            BeforeEach(() -> {
                RestAssured.port = port;
            });

            It("should throw a 405 Not Allowed", () -> {

                TEntity tentity = new TEntity();
                tentity = repo.save(tentity);

                given()
                    .contentType("text/plain")
                    .content("some content".getBytes())
                .when()
                    .post("/tEntities/" + tentity.getId())
                .then()
                    .statusCode(405);
            });
        });

        Describe("when unsetContent method are exported", () -> {

            BeforeEach(() -> {
                RestAssured.port = port;
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
    }

    public interface PreferResourceForPutsAndPostsRepository extends CrudRepository<TEntity, Long> {
    }

    public interface UnexportedContentStore extends FilesystemContentStore<TEntity, UUID> {

        @RestResource(exported=false)
        @Override
        public InputStream getContent(TEntity property);

        @RestResource(exported=false)
        @Override
        public TEntity setContent(TEntity property, InputStream content);

        @RestResource(exported=false)
        @Override
        public TEntity setContent(TEntity property, Resource resourceContent);

        @RestResource(exported=false)
        @Override
        public TEntity unsetContent(TEntity property);
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


    @SpringBootApplication
    @ComponentScan(excludeFilters={
            @Filter(type = FilterType.REGEX,
                    pattern = {
                            ".*MongoConfiguration"
            })
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
