package internal.org.springframework.content.s3.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
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

import junit.framework.Assert;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class S3StoreWithEntityConverterIT {

    private static final String BUCKET = "spring-eg-content-s3";
    private static final String OTHER_BUCKET = "other-other-other-bucket";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket";

    private static TestData[] testDataSets = null;

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);

        testDataSets = new TestData[] {
                new TestData("Default Converter", new Class[] {TestConfig.class}, OTHER_BUCKET),
                new TestData("Custom Converter", new Class[] {CustomConverterConfig.class, TestConfig.class}, OTHER_OTHER_BUCKET),
        };
    }

    @Data
    @AllArgsConstructor
    private static class TestData {
        private String name;
        private Class[] config;
        private String bucket;
    }

    private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;
    private S3Client client;

    private static S3Client s3ClientSpy;

    private String resourceLocation;

    {
        for (TestData testDataSet : testDataSets) {

            Describe(testDataSet.getName(), () -> {

                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(testDataSet.getConfig());
                    context.refresh();

                    repo = context.getBean(TestEntityRepository.class);
                    store = context.getBean(TestEntityStore.class);
                    client = context.getBean(S3Client.class);

                    try {
                        HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                                .bucket(testDataSet.getBucket())
                                .build();

                        client.headBucket(headBucketRequest);
                    } catch (NoSuchBucketException e) {

                        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                                .bucket(testDataSet.getBucket())
                                .build();
                        client.createBucket(bucketRequest);
                    }

                    RandomString random  = new RandomString(5);
                    resourceLocation = random.nextString();
                });

                AfterEach(() -> {
                    context.close();
                });

                Describe("given an entity with content", () -> {

                    BeforeEach(() -> {
                        entity = new TestEntity();
                        entity.setContentType("text/plain");
                        entity = repo.save(entity);

                        store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    });

                    It("should store new content in bucket '" + testDataSet.getBucket() + "'", () -> {
                        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
                        verify(s3ClientSpy).putObject(captor.capture(), any(RequestBody.class));
                        assertThat(captor.getValue().bucket(), is(testDataSet.getBucket()));

                        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                .bucket(testDataSet.getBucket())
                                .key(entity.getContentId().toString())
                                .build();

                        client.headObject(headObjectRequest);
                    });

                    It("should have content metadata", () -> {
                        // content
                        assertThat(entity.getContentId(), is(notNullValue()));
                        assertThat(entity.getContentId().toString().trim().length(), greaterThan(0));
                        Assert.assertEquals(entity.getContentLen(), 27L);
                    });

                    Context("when content is deleted", () -> {
                        BeforeEach(() -> {
                            resourceLocation = entity.getContentId().toString();
                            entity = store.unsetContent(entity, PropertyPath.from("content"));
                            entity = repo.save(entity);
                        });

                        It("should delete content from bucket '" + testDataSet.getBucket() + "'", () -> {
                            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
                            verify(s3ClientSpy).deleteObject(captor.capture());
                            assertThat(captor.getValue().bucket(), is(testDataSet.getBucket()));

                            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                    .bucket(testDataSet.getBucket())
                                    .key(resourceLocation)
                                    .build();

                            try {
                                client.headObject(headObjectRequest);
                                fail("expected object not to exist");
                            } catch (NoSuchKeyException  e) {}
                        });
                    });
                });
            });
        }
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

//        @Bean
//        public S3Client client() throws URISyntaxException {
//            s3ClientSpy = spy(LocalStack.getAmazonS3Client());
//            return s3ClientSpy;
//        }
@Autowired
private Environment env;

        @Bean
        public S3Client client() throws URISyntaxException {
            AwsCredentials awsCredentials = AwsBasicCredentials.create(env.getProperty("AWS_ACCESS_KEY"), env.getProperty("AWS_SECRET_KEY"));
            StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(awsCredentials);

            S3Client s3Client = S3Client.builder()
                    .region(Region.US_WEST_1)
                    .credentialsProvider(credentialsProvider)
                    .build();

            s3ClientSpy = spy(s3Client);
            return s3ClientSpy;
        }
    }

    @Configuration
    public static class CustomConverterConfig {

      @Bean
      public S3StoreConfigurer configurer() {
          return new S3StoreConfigurer() {

              @Override
              public void configureS3StoreConverters(ConverterRegistry registry) {

                  registry.addConverter(new Converter<ContentPropertyInfo<TestEntity, Serializable>, S3ObjectId>() {
                      @Override
                      public S3ObjectId convert(ContentPropertyInfo<TestEntity, Serializable> info) {
                          return new S3ObjectId(OTHER_OTHER_BUCKET, info.contentId().toString());
                      }
                  });


                  registry.addConverter(new Converter<ContentPropertyInfo<FakeEntity, Serializable>, S3ObjectId>() {
                      @Override
                      public S3ObjectId convert(ContentPropertyInfo<FakeEntity, Serializable> info) {
                          throw new IllegalStateException("wrong converter called");
                      }
                  });
              }
          };
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

    public static class FakeEntity {
    }

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @Bucket
        private String bucket = "other-other-other-bucket";

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
            this.contentId = contentId;
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}
}
