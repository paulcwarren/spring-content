package internal.org.springframework.content.gcs.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.google.cloud.storage.BlobId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
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

import junit.framework.Assert;
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

    static {
        System.setProperty("spring.content.gcp.storage.bucket", "test-bucket");
    }

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
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            });

                            It("should be recorded as such on the entity's @ContentId", () -> {
                                assertThat(entity.getContentId(), is(resourceLocation));
                                assertThat(entity.getRenditionId(), is(resourceLocation));
                            });

                            Context("when the resource has content", () -> {
                                BeforeEach(() -> {
                                    try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                        os.write("Hello Client-side World!".getBytes());
                                    }
                                });

                                It("should not honor byte ranges", () -> {
                                    // relies on REST-layer to serve byte range
                                    Resource r = store.getResource(entity, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());
                                    try (InputStream is = r.getInputStream()) {
                                        assertThat(IOUtils.toString(is), is("Hello Client-side World!"));
                                    }
                                });
                            });
                            Context("when the resource is unassociated", () -> {

                                BeforeEach(() -> {
                                    store.unassociate(entity);
                                    store.unassociate(entity, PropertyPath.from("rendition"));
                                });

                                It("should reset the entity's @ContentId", () -> {
                                    assertThat(entity.getContentId(), is(nullValue()));
                                    assertThat(entity.getRenditionId(), is(nullValue()));
                                });
                            });

                            Context("when a invalid property path is used to associate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to load a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.getResource(entity, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to unassociate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.unassociate(entity, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
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
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
                    entity = repo.save(entity);
                });

                It("should be able to store new content", () -> {
                    // content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
                    } catch (IOException ioe) {}
                });

                It("should have content metadata", () -> {
                    // content
                    assertThat(entity.getContentId(), is(notNullValue()));
                    assertThat(entity.getContentId().trim().length(), greaterThan(0));
                    Assert.assertEquals(entity.getContentLen(), 27L);

                    //rendition
                    assertThat(entity.getRenditionId(), is(notNullValue()));
                    assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
                    Assert.assertEquals(entity.getRenditionLen(), 40L);
                });

                Context("when content is updated", () -> {
                    BeforeEach(() ->{
                        store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                        entity = repo.save(entity);
                    });

                    It("should have the updated content", () -> {
                        //content
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        //rendition
                        matches = false;
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

                Context("when content is updated with shorter content", () -> {
                    BeforeEach(() -> {
                        store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                        entity = repo.save(entity);
                    });
                    It("should store only the new content", () -> {
                        //content
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        //rendition
                        matches = false;
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

                Context("when content is updated and not overwritten", () -> {
                    It("should have the updated content", () -> {
                        String contentId = entity.getContentId();
                        assertThat(contentId, is(not(nullValue())));
                        assertThat(storage.get(BlobId.of("test-bucket", contentId)).exists(), is(true));

                        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().overwriteExistingContent(false).build());
                        entity = repo.save(entity);

                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        assertThat(entity.getContentId(), is(not(contentId)));

                        assertThat(storage.get(BlobId.of("test-bucket", entity.getContentId())).exists(), is(true));
                    });
                });

                Context("when content is unset", () -> {
                    BeforeEach(() -> {
                        resourceLocation = entity.getContentId().toString();
                        entity = store.unsetContent(entity);
                        entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                        entity = repo.save(entity);
                    });

                    It("should have no content", () -> {
                        //content
                        try (InputStream content = store.getContent(entity)) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        Assert.assertEquals(entity.getContentLen(), 0);
                        assertThat(storage.get(BlobId.of("test-bucket", resourceLocation)), is(nullValue()));

                        //rendition
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getRenditionId(), is(Matchers.nullValue()));
                        Assert.assertEquals(entity.getRenditionLen(), 0);
                    });
                });

                Context("when content is unset but kept", () -> {
                    BeforeEach(() -> {
                        resourceLocation = entity.getContentId().toString();
                        entity = store.unsetContent(entity, PropertyPath.from("content"), UnsetContentParams.builder().disposition(UnsetContentParams.Disposition.Keep).build());
                        entity = repo.save(entity);
                    });

                    It("should have no content", () -> {
                        //content
                        try (InputStream content = store.getContent(entity)) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        Assert.assertEquals(entity.getContentLen(), 0);
                        assertThat(storage.get(BlobId.of("test-bucket", resourceLocation)).exists(), is(true));
                    });
                });

                Context("when an invalid property path is used to setContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                    });
                });

                Context("when an invalid property path is used to getContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.getContent(entity, PropertyPath.from("does.not.exist"));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                    });
                });

                Context("when an invalid property path is used to unsetContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
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

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

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

        @jakarta.persistence.Id
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
