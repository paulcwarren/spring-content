package it.autoversioning;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.AutoVersioningRepository;
import org.springframework.versions.SubjectId;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.jpa.config.AutoVersion;
import org.springframework.versions.jpa.config.JpaAutoVersioningConfig;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = {AutoVersioningIT.Application.class}, webEnvironment=WebEnvironment.RANDOM_PORT)
public class AutoVersioningIT {

    @Autowired
    private TestRepository repo;

    private TestEntity entity = null;

    {
        Describe("AutoVersioning", () -> {

            Context("when an auto-versioned entity is created", () -> {

                BeforeEach(() -> {

                    entity = new TestEntity();
                    entity.setName("test");
                    entity = repo.save(entity);
                });

                It("should record a version in the version history", () -> {

                    assertThat(repo.findAllVersions(entity).size(), is(1));
                    assertThat(repo.findAllVersions(entity).get(0).getName(), is("test"));
                });
            });

            Context("when an auto-versioned entity is saved multiple times", () -> {

                BeforeEach(() -> {

                    entity = new TestEntity();
                    entity.setName("test");
                    entity = repo.save(entity);
                    entity.setName("test2");
                    entity = repo.save(entity);
                });

                It("should record a version in the version history", () -> {

                    assertThat(repo.findAllVersions(entity).size(), is(2));

                    List<TestEntityVersion> versions = repo.findAllVersions(entity);

                    assertThat(versions.get(0).getName(), is("test"));
                    assertThat(versions.get(0).getAncestralRootId(), is(versions.get(0).getId()));
                    assertThat(versions.get(0).getAncestorId(), is(nullValue()));
                    assertThat(versions.get(0).getSuccessorId(), is(versions.get(1).getId()));
                    assertThat(versions.get(0).getSubjectId(), is(entity.getId()));

                    assertThat(repo.findAllVersions(entity).get(1).getName(), is("test2"));
                    assertThat(versions.get(1).getAncestralRootId(), is(versions.get(0).getId()));
                    assertThat(versions.get(1).getAncestorId(), is(versions.get(0).getId()));
                    assertThat(versions.get(1).getSuccessorId(), is(nullValue()));
                    assertThat(versions.get(1).getSubjectId(), is(entity.getId()));
                });
            });
        });
    }

    @SpringBootApplication
    @EnableTransactionManagement
    @EnableJpaRepositories(considerNestedRepositories=true, basePackages={"it.autoversioning","org.springframework.versions"})
    @Import(JpaAutoVersioningConfig.class)
    public static class Application {

        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public static class TestConfig {

    //        @Bean
    //        File filesystemRoot() {
    //            try {
    //                return Files.createTempDirectory("").toFile();
    //            }
    //            catch (IOException ioe) {
    //            }
    //            return null;
    //        }
    //
    //        @Bean
    //        FileSystemResourceLoader fileSystemResourceLoader() {
    //            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
    //        }

            @Value("/org/springframework/versions/jpa/schema-drop-h2.sql")
            private ClassPathResource dropRepositoryTables;

            @Value("/org/springframework/versions/jpa/schema-h2.sql")
            private ClassPathResource createRepositorySchema;

            @Bean
            DataSourceInitializer datasourceInitializer(DataSource dataSource) {
                ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

                databasePopulator.addScript(dropRepositoryTables);
                databasePopulator.addScript(createRepositorySchema);
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
                factory.setPackagesToScan("it.autoversioning");
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

    public interface TestRepository extends JpaRepository<TestEntity, Long>, AutoVersioningRepository<TestEntity, Long, TestEntityVersion> {}

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @AutoVersion(versionType=TestEntityVersion.class)
    public static class TestEntity {

        @Id
        @GeneratedValue
        private Long id;

        @Version
        private Long vstamp;

        private String name;
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TestEntityVersion {

        public TestEntityVersion(TestEntity subject) {
            this.setName(subject.getName());
        }

        @Id
        @GeneratedValue
        private Long id;

        @SubjectId
        private Long subjectId;

        @AncestorRootId
        private Long ancestralRootId;

        @AncestorId
        private Long ancestorId;

        @SuccessorId
        private Long successorId;

        private String name;
    }

    @Test
    public void noop() {}
}
