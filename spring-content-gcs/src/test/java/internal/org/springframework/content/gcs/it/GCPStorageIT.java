package internal.org.springframework.content.gcs.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class GCPStorageIT {

    private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;
    private Storage storage;

    private String resourceLocation;

    {
        Describe("DefaultGCPStorageImpl", () -> {

            BeforeEach(() -> {
                context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();

                repo = context.getBean(TestEntityRepository.class);
                store = context.getBean(TestEntityStore.class);
                storage = context.getBean(Storage.class);

                RandomString random  = new RandomString(5);
                resourceLocation = random.nextString();
            });

            AfterEach(() -> {
                context.close();
            });

            Describe("Store", () -> {

                Context("#getResource", () -> {

                    BeforeEach(() -> {
                        genericResource = store.getResource(resourceLocation);
                    });

                    AfterEach(() -> {
                        if (genericResource != null) {
                            ((DeletableResource)genericResource).delete();
                        }

                        Page<Blob> blobs = storage.list("delete-me-please-please", Storage.BlobListOption.currentDirectory());
                        for(Blob blob : blobs.iterateAll()) {
                            storage.delete(blob.getBlobId());
                        }
                    });

                    It("should get Resource", () -> {
                        assertThat(genericResource, is(instanceOf(Resource.class)));
                    });

                    It("should not exist", () -> {
                        assertThat(genericResource.exists(), is(false));
                    });

                    Context("given content is added to that resource", () -> {

                        BeforeEach(() -> {
                            try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        });

                        It("should store that content", () -> {
                            assertThat(genericResource.exists(), is(true));

                            boolean matches = false;
                            try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    matches = IOUtils.contentEquals(expected, actual);
                                    assertThat(matches, Matchers.is(true));
                                }
                            }
                        });

                        Context("given that resource is then updated", () -> {

                            BeforeEach(() -> {
                                try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                    try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                        IOUtils.copy(is, os);
                                    }
                                }
                            });

                            It("should store that updated content", () -> {
                                assertThat(genericResource.exists(), is(true));

                                try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                    try (InputStream actual = genericResource.getInputStream()) {
                                        assertThat(IOUtils.contentEquals(expected, actual), is(true));
                                    }
                                }
                            });
                        });

                        Context("given that resource is then deleted", () -> {

                            BeforeEach(() -> {
                                try {
                                    ((DeletableResource) genericResource).delete();
                                } catch (Exception e) {
                                    this.e = e;
                                }
                            });

                            It("should not exist", () -> {
                                assertThat(e, is(nullValue()));
                            });
                        });
                    });
                });
            });

            Describe("AssociativeStore", () -> {

                Context("given a new entity", () -> {

                    BeforeEach(() -> {
                        entity = new TestEntity();
                        entity = repo.save(entity);
                    });

                    It("should not have an associated resource", () -> {
                        assertThat(entity.getContentId(), is(nullValue()));
                        assertThat(store.getResource(entity), is(nullValue()));
                    });

                    Context("given a resource", () -> {

                        BeforeEach(() -> {
                            genericResource = store.getResource(resourceLocation);
                        });

                        Context("when the resource is associated", () -> {

                            BeforeEach(() -> {
                                store.associate(entity, resourceLocation);
                            });

                            It("should be recorded as such on the entity's @ContentId", () -> {
                                assertThat(entity.getContentId(), is(resourceLocation));
                            });

                            Context("when the resource is unassociated", () -> {

                                BeforeEach(() -> {
                                    store.unassociate(entity);
                                });

                                It("should reset the entity's @ContentId", () -> {
                                    assertThat(entity.getContentId(), is(nullValue()));
                                });
                            });
                        });
                    });
                });
            });

            Describe("ContentStore", () -> {

                BeforeEach(() -> {
                    entity = new TestEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                });

                It("should be able to store new content", () -> {
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}
                });

                It("should have content metadata", () -> {
                    assertThat(entity.getContentId(), is(notNullValue()));
                    assertThat(entity.getContentId().trim().length(), greaterThan(0));
                    assertEquals(entity.getContentLen(), 27L);
                });

                Context("when content is updated", () -> {
                    BeforeEach(() ->{
                        store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                        entity = repo.save(entity);
                    });

                    It("should have the updated content", () -> {
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

                Context("when content is updated with shorter content", () -> {
                    BeforeEach(() -> {
                        store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                        entity = repo.save(entity);
                    });
                    It("should store only the new content", () -> {
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

                Context("when content is deleted", () -> {
                    BeforeEach(() -> {
                        resourceLocation = entity.getContentId();
                        entity = store.unsetContent(entity);
                        entity = repo.save(entity);
                    });

                    It("should have no content", () -> {
                        try (InputStream content = store.getContent(entity)) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        assertEquals(entity.getContentLen(), 0);
                    });
                });

                Context("when content is deleted and the content id field is shared with entity id", () -> {

                    It("should not reset the id field", () -> {
                        SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
                        SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

                        SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                        sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                        String id = sharedIdContentIdEntity.getContentId();
                        sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                        assertThat(sharedIdContentIdEntity.getContentId(), is(id));
                        assertThat(sharedIdContentIdEntity.getContentLen(), is(0L));
                    });
                });

//                Context("when content is deleted and the id field is shared with spring id", () -> {
//
//                    It("should not reset the id field", () -> {
//                        SharedSpringIdRepository SharedSpringIdRepository = context.getBean(SharedSpringIdRepository.class);
//                        SharedSpringIdStore SharedSpringIdStore = context.getBean(SharedSpringIdStore.class);
//
//                        SharedSpringIdContentIdEntity SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(new SharedSpringIdContentIdEntity());
//
//                        SharedSpringIdContentIdEntity = SharedSpringIdStore.setContent(SharedSpringIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
//                        SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(SharedSpringIdContentIdEntity);
//                        String id = SharedSpringIdContentIdEntity.getContentId();
//                        SharedSpringIdContentIdEntity = SharedSpringIdStore.unsetContent(SharedSpringIdContentIdEntity);
//                        assertThat(SharedSpringIdContentIdEntity.getContentId(), is(id));
//                        assertThat(SharedSpringIdContentIdEntity.getContentLen(), is(0L));
//                    });
//                });

            });
        });
    }

    @Test
    public void test() {
        // noop
    }

    @Configuration
    @EnableJpaRepositories(basePackages="internal.org.springframework.content.gcs.it", considerNestedRepositories = true)
    @EnableGCPStorage(basePackages="internal.org.springframework.content.gcs.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
        }

//        @Bean
//        public static Storage storage(CredentialsProvider credentialsProvider,
//                GcpProjectIdProvider projectIdProvider) throws IOException {
//            return StorageOptions.newBuilder()
//                    .setCredentials(credentialsProvider.getCredentials())
//                    .setProjectId(projectIdProvider.getProjectId()).build().getService();
//        }
//
//        @Bean
//        public GcpProjectIdProvider gcpProjectIdProvider() {
//            return new DefaultGcpProjectIdProvider();
//        }
//
//        @Bean
//        public CredentialsProvider credentialsProvider() {
//            try {
//                return new DefaultCredentialsProvider(Credentials::new);
//            }
//            catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
    }

    @Configuration
    public static class InfrastructureConfig {

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
            factory.setPackagesToScan("internal.org.springframework.content.gcs.it");
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

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public TestEntity(String contentId) {
            this.contentId = new String(contentId);
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class SharedIdContentIdEntity {

        @javax.persistence.Id
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        @ContentLength
        private long contentLen;
    }

    public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {}
    public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

//    @Entity
//    @Setter
//    @Getter
//    @NoArgsConstructor
//    public static class SharedSpringIdContentIdEntity {
//
//        @org.springframework.data.annotation.Id
//        @ContentId
//        private String contentId;
//
//        @ContentLength
//        private long contentLen;
//    }
//
//    public interface SharedSpringIdRepository extends JpaRepository<SharedSpringIdContentIdEntity, String> {}
//    public interface SharedSpringIdStore extends ContentStore<SharedSpringIdContentIdEntity, String> {}
}
