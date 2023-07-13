package internal.org.springframework.content.s3.it;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageResource;
import jakarta.persistence.*;
import junit.framework.Assert;
import lombok.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.s3.config.EnableS3Stores;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class S3StoreIT {

    private static final String BUCKET = "aws-test-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;
    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    private S3Client client;

    private String resourceLocation;

    {
        Describe("S3StoreIT", () -> {

            BeforeEach(() -> {
                context = new AnnotationConfigApplicationContext();
                context.register(TestConfig.class);
                context.refresh();

                repo = context.getBean(TestEntityRepository.class);
                store = context.getBean(TestEntityStore.class);
                client = context.getBean(S3Client.class);

                embeddedRepo = context.getBean(EmbeddedRepository.class);
                embeddedStore = context.getBean(EmbeddedStore.class);

                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket(BUCKET)
                        .build();

                try {
                    client.headBucket(headBucketRequest);
                } catch (NoSuchBucketException e) {

                    CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                            .bucket(BUCKET)
                            .build();
                    client.createBucket(bucketRequest);

                    boolean found = false;
                    while (!found) {
                        headBucketRequest = HeadBucketRequest.builder()
                                .bucket(BUCKET)
                                .build();
                        try {
                            client.headBucket(headBucketRequest);
                            found = true;
                        } catch (NoSuchBucketException e2) {
                        }

                        System.out.println("sleeping...");
                        Thread.sleep(100);
                    }
                }

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
                        ((DeletableResource)genericResource).delete();
                    });

                    It("should get Resource", () -> {
                        assertThat(genericResource, is(instanceOf(Resource.class)));
                    });

                    It("should not exist", () -> {
                        assertThat(genericResource.exists(), is(false));
                    });

                    It("should be a RangeableResource", () -> {
                        assertThat(genericResource, is(instanceOf(RangeableResource.class)));
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

                        Context("given a byte range is requested", () -> {

                            It("should return a partial content input stream and the partial content", () -> {

                                ((RangeableResource)genericResource).setRange("bytes=6-19");

                                try (InputStream expected = new ByteArrayInputStream("Spring Content".getBytes())) {
                                    try (InputStream actual = genericResource.getInputStream()) {
                                        assertThat(actual, is(instanceOf(SimpleStorageResource.PartialContentInputStream.class)));
                                        assertThat(IOUtils.toString(expected, "UTF-8"), is(IOUtils.toString(actual, "UTF-8")));
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
                    entity.setContentType("text/plain");
                    entity.setRenditionContentType("text/html");
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
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

                It("should set Content-Type of stored content to value from field annotated with @MimeType", () -> {
                    // content
                    S3StoreResource resource = (S3StoreResource) store.getResource(entity);
                    assertThat(resource.contentType(), is(notNullValue()));
                    assertThat(resource.contentType(), is(entity.getContentType()));

                    //rendition
                    S3StoreResource renditionResource = (S3StoreResource) store.getResource(entity, PropertyPath.from("rendition"));
                    assertThat(renditionResource.contentType(), is(notNullValue()));
                    assertThat(renditionResource.contentType(), is(entity.getRenditionContentType()));
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
                        client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(contentId).build());

                        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
                        entity = repo.save(entity);

                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        assertThat(entity.getContentId(), is(not(contentId)));
                        client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(entity.getContentId()).build());
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

                        try {
                            client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
                            fail("expected content to be removed but is still exists");
                        } catch (NoSuchKeyException nske) {
                        }

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

                        client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
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

                Context("@Embedded content", () -> {
                    Context("given a entity with a null embedded content object", () -> {
                        It("should return null when content is fetched", () -> {
                            EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                            assertThat(embeddedStore.getContent(entity, PropertyPath.from("content")), is(nullValue()));
                        });

                        It("should be successful when content is set", () -> {
                            EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                            embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                            try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                                assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes())), is(true));
                            }
                        });

                        It("should return null when content is unset", () -> {
                            EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                            EntityWithEmbeddedContent expected = new EntityWithEmbeddedContent(entity.getId(), entity.getContent());
                            assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content")), is(expected));
                            int i = 0;
                        });
                    });
                });
            });
        });
    }

    @Test
    public void test() {
        // noop
    }

    @Configuration
    @EnableJpaRepositories(basePackages="internal.org.springframework.content.s3.it", considerNestedRepositories = true)
    @EnableS3Stores(basePackages="internal.org.springframework.content.s3.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
        @Bean
        public S3Client client() throws URISyntaxException {
            return LocalStack.getAmazonS3Client();
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
            factory.setPackagesToScan("internal.org.springframework.content.s3.it");
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

        @MimeType
        private String contentType;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        @MimeType
        private String renditionContentType;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name="entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UUID.randomUUID().toString();

        @Embedded
        private EmbeddedContent content;
    }

    @Embeddable
    @NoArgsConstructor
    @Data
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {}
    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {}
}
