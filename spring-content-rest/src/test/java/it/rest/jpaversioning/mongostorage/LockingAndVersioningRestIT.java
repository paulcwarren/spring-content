package it.rest.jpaversioning.mongostorage;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.UUID;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.store.MongoContentStore;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.mongodb.client.MongoClient;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.context.WebApplicationContext;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = {LockingAndVersioningRestIT.Application.class}, webEnvironment=WebEnvironment.RANDOM_PORT)
public class LockingAndVersioningRestIT {

    @Autowired
    private VersionedDocumentAndVersioningRepository repo;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private VersionedDocument doc;

    {
        Describe("Spring Content REST Versioning With Mongo Storage", () -> {
            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
            });
            Context("given a versionable entity with content", () -> {
                BeforeEach(() -> {
                    doc = new VersionedDocument();
                    doc.setData("John");
                    doc = repo.save(doc);
                });
                It("should be able to version an entity and its content", () -> {
                    // assert content does not exist
                    given()
                            .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                            .when()
                            .get("/versionedDocumentsContent/" + doc.getId())
                            .then()
                            .assertThat()
                            .statusCode(HttpStatus.SC_NOT_FOUND);

                    String newContent = "This is some new content";

                    // POST the new content
                    given()
                            .contentType("plain/text")
                            .body(newContent.getBytes())
                            .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                            .when()
                            .put("/versionedDocumentsContent/" + doc.getId())
                            .then()
                            .statusCode(HttpStatus.SC_CREATED);

                    // assert that it now exists
                    given()
                            .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                            .header("accept", "plain/text")
                            .get("/versionedDocumentsContent/" + doc.getId())
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .assertThat()
                            .contentType(Matchers.startsWith("plain/text"))
                            .body(Matchers.equalTo(newContent));

                    given()
                            .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                            .header("accept", "application/json")
                            .put("/versionedDocuments/" + doc.getId() + "/lock")
                            .then()
                            .statusCode(HttpStatus.SC_OK);

                    // POST the new content as john
                    given()
                            .auth().with(SecurityMockMvcRequestPostProcessors.user("john123").password("password"))
                            .contentType("plain/text")
                            .body("john's content".getBytes())
                            .when()
                            .put("/versionedDocumentsContent/" + doc.getId())
                            .then()
                            .statusCode(is(409));

                    JsonPath response =
                            given()
                                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                                    .contentType("application/json")
                                    .body("{\"number\":\"1.1\",\"label\":\"some minor changes\"}".getBytes())
                                    .put("/versionedDocuments/" + doc.getId() + "/version")
                                    .then()
                                    .statusCode(HttpStatus.SC_OK)
                                    .extract().jsonPath();
                    assertThat(response.get("_links.self.href"), endsWith("versionedDocuments/" + (doc.getId() + 1)));

                    response =
                            given()
                                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                                    .get("/versionedDocuments/findAllVersionsLatest")
                                    .then()
                                    .statusCode(HttpStatus.SC_OK)
                                    .extract().jsonPath();
                    assertThat(response.get("_embedded.versionedDocuments[0].version"), is("1.1"));
                    assertThat(response.get("_embedded.versionedDocuments[0].successorId"), is(nullValue()));

                    response =
                            given()
                                    .auth().with(SecurityMockMvcRequestPostProcessors.user("paul123").password("password"))
                                    .get("/versionedDocuments/" + (doc.getId() + 1) + "/findAllVersions")
                                    .then()
                                    .statusCode(HttpStatus.SC_OK)
                                    .extract().jsonPath();
                    assertThat(response.get("_embedded.versionedDocuments[0].version"), is("1.0"));
                    assertThat(response.get("_embedded.versionedDocuments[0].successorId"), is((int)(doc.getId() + 1)));
                    assertThat(response.get("_embedded.versionedDocuments[1].version"), is("1.1"));
                    assertThat(response.get("_embedded.versionedDocuments[1].successorId"), is(nullValue()));
                });
            });
        });
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true,
                            basePackages={"it.rest.jpaversioning.mongostorage", "org.springframework.versions"})
    @EnableTransactionManagement
    @EnableMongoStores(basePackages = "it.rest.jpaversioning.mongostorage")
    @Import({JpaLockingAndVersioningConfig.class, RestConfiguration.class, SecurityConfiguration.class})
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public class VersioningConfig extends AbstractMongoClientConfiguration {
            @Override
            protected String getDatabaseName() {
                return MongoTestContainer.getTestDbName();
            }

            @Override
            @Bean
            public MongoClient mongoClient() {
                return MongoTestContainer.getMongoClient();
            }

            @Bean
            public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) throws Exception {
                return new GridFsTemplate(mongoDbFactory(), mongoConverter);
            }

            @Override
            @Bean
            public MongoDatabaseFactory mongoDbFactory() {
                return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
            }

            @Value("/org/springframework/versions/jpa/schema-drop-hsqldb.sql")
            private ClassPathResource dropRepositoryTables;

            @Value("/org/springframework/versions/jpa/schema-hsqldb.sql")
            private ClassPathResource dataRepositorySchema;

            @Bean
            DataSourceInitializer datasourceInitializer(DataSource dataSource) {
                ResourceDatabasePopulator databasePopulator =
                        new ResourceDatabasePopulator();

                databasePopulator.addScript(dropRepositoryTables);
                databasePopulator.addScript(dataRepositorySchema);
                databasePopulator.setIgnoreFailedDrops(true);

                DataSourceInitializer initializer = new DataSourceInitializer();
                initializer.setDataSource(dataSource);
                initializer.setDatabasePopulator(databasePopulator);

                return initializer;
            }

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
                factory.setPackagesToScan("it.rest.jpaversioning.mongostorage");
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
    }

    @Configuration
    @EnableWebSecurity
    public static class SecurityConfiguration {

        protected static String REALM = "SPRING_CONTENT";

        @Autowired
        public void configureGlobalSecurity(AuthenticationManagerBuilder auth) throws Exception {
            // Enable if spring-doc apps supports user accounts in the future
             auth.inMemoryAuthentication().
                     withUser(User.withDefaultPasswordEncoder().username("paul123").password("password").roles("USER")).
                     withUser(User.withDefaultPasswordEncoder().username("john123").password("password").roles("USER").
                             build());

        }

        @Bean
        public AuthenticationEntryPoint getBasicAuthEntryPoint() {
            return new AuthenticationEntryPoint();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .authorizeRequests()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .and().httpBasic().realmName(REALM).authenticationEntryPoint(getBasicAuthEntryPoint())
                    .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

            return http.build();
        }

        @Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return (web) -> web.ignoring().requestMatchers(HttpMethod.OPTIONS, "/**");
        }
    }

    public static class AuthenticationEntryPoint extends BasicAuthenticationEntryPoint {

        @Override
        public void commence(final HttpServletRequest request, final HttpServletResponse response,
                final AuthenticationException authException) throws IOException {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.addHeader("WWW-Authenticate", "Basic realm=" + getRealmName() + "");

            PrintWriter writer = response.getWriter();
            writer.println("HTTP Status 401 : " + authException.getMessage());
        }

        @Override
        public void afterPropertiesSet() {
            setRealmName(SecurityConfiguration.REALM);
            super.afterPropertiesSet();
        }
    }

    @Entity
    @Getter
    @Setter
    @EqualsAndHashCode
    @ToString
    @Table(name="VERSIONED_DOCUMENTS")
    public static class VersionedDocument {

        @Id @GeneratedValue private Long id;
        @Version private Long vstamp;
        @ContentId private UUID contentId;
        @ContentLength private long contentLen;
        @MimeType private String mimeType;
        @LockOwner private String lockOwner;
        @AncestorId private Long ancestorId;
        @AncestorRootId private Long ancestralRootId;
        @SuccessorId private Long successorId;
        @VersionNumber private String version;
        @VersionLabel private String label;
        private String data;

        public VersionedDocument() {
        }

        public VersionedDocument(VersionedDocument doc) {
            this.setContentId(doc.getContentId());
            this.setContentLen(doc.getContentLen());
            this.setMimeType(doc.getMimeType());
            this.setLockOwner(doc.getLockOwner());
            this.setData(doc.getData());
        }
    }

    public interface VersionedDocumentAndVersioningRepository extends JpaRepository<VersionedDocument, Long>, LockingAndVersioningRepository<VersionedDocument, Long> {
    }

    @StoreRestResource(path="versionedDocumentsContent")
    public interface VersionedDocumentStore extends MongoContentStore<VersionedDocument, UUID> {
    }

    @Test
    public void noop() {}
}
