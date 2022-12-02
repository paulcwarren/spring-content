package org.springframework.content.encryption.s3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import internal.org.springframework.content.fragments.EncryptingContentStoreConfiguration;
import internal.org.springframework.content.fragments.EncryptingContentStoreConfigurer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.encryption.EncryptingContentStore;
import org.springframework.content.encryption.EnvelopeEncryptionService;
import org.springframework.content.encryption.LocalStack;
import org.springframework.content.encryption.VaultContainerSupport;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultOperations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;


@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = EncryptionIT.Application.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EncryptionIT {

    private static Object mutex = new Object();

    static {
        try {
            setEnv(Collections.singletonMap("AWS_REGION", "us-west-1"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore store;

//    @Autowired
//    private FileRepository repo2;
//
//    @Autowired
//    private FileContentStore2 store2;

//    @Autowired
//    private java.io.File filesystemRoot;

    @Autowired
    private S3Client client;

    @Autowired
    private EnvelopeEncryptionService encrypter;

    @Autowired
    private VaultOperations vaultOperations;

    @LocalServerPort
    int port;

    private File f;


    static {
        System.setProperty("spring.content.s3.bucket", "test-bucket");
    }

    {
        Describe("Client-side encryption with s3 storage", () -> {
            BeforeEach(() -> {
                RestAssured.port = port;

                synchronized(mutex) {
                    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                            .bucket("test-bucket")
                            .build();

                    try {
                        client.headBucket(headBucketRequest);
                    } catch (NoSuchBucketException e) {

                        CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                                .bucket("test-bucket")
                                .build();
                        client.createBucket(bucketRequest);
                    }
                }

                f = repo.save(new File());
            });
            Context("given content", () -> {
                BeforeEach(() -> {
                    given()
                            .contentType("text/plain")
                            .content("Hello Client-side encryption World!")
                            .when()
                            .post("/files/" + f.getId() + "/content")
                            .then()
                            .statusCode(HttpStatus.SC_CREATED);
                });
                It("should be stored encrypted", () -> {
                    Optional<File> fetched = repo.findById(f.getId());
                    assertThat(fetched.isPresent(), is(true));
                    f = fetched.get();

                    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                            .bucket("test-bucket")
                            .key(f.getContentId().toString())
                            .build();

                    ResponseInputStream<GetObjectResponse> resp = client.getObject(getObjectRequest);
                    String contents = IOUtils.toString(resp);
                    assertThat(contents, is(not("Hello Client-side encryption World!")));
                });
                It("should be retrieved decrypted", () -> {
                    given()
                            .header("accept", "text/plain")
                            .get("/files/" + f.getId() + "/content")
                            .then()
                            .statusCode(HttpStatus.SC_OK)
                            .assertThat()
                            .contentType(Matchers.startsWith("text/plain"))
                            .body(Matchers.equalTo("Hello Client-side encryption World!"));
                });
                It("should handle byte-range requests", () -> {
                    Response r =
                            given()
                                    .header("accept", "text/plain")
                                    .header("range", "bytes=16-27")
                                    .get("/files/" + f.getId() + "/content")
                                    .then()
                                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                    .assertThat()
                                    .contentType(Matchers.startsWith("text/plain"))
                                    .and().extract().response();

                    assertThat(r.asString(), is("e encryption"));
                });
                Context("when the keyring is rotated", () -> {
                    BeforeEach(() -> {
                        encrypter.rotate("fsfile");
                    });
                    It("should not change the stored content key", () -> {
                        f = repo.findById(f.getId()).get();

                        assertThat(new String(f.getContentKey()), startsWith("vault:v1"));
                    });
                    It("should still retrieve content decrypted", () -> {
                        given()
                                .header("accept", "text/plain")
                                .get("/files/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_OK)
                                .assertThat()
                                .contentType(Matchers.startsWith("text/plain"))
                                .body(Matchers.equalTo("Hello Client-side encryption World!"));
                    });
                    It("should update the content key version when next stored", () -> {
                        given()
                                .contentType("text/plain")
                                .content("Hello Client-side encryption World!")
                                .when()
                                .post("/files/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_OK);

                        f = repo.findById(f.getId()).get();
                        assertThat(new String(f.getContentKey()), startsWith("vault:"));
                        assertThat(new String(f.getContentKey()), not(startsWith("vault:v1")));
                    });
                });
                Context("when the content is unset", () -> {
                    It("it should remove the content and clear the content key", () -> {
                        f = repo.findById(f.getId()).get();
                        String contentId = f.getContentId().toString();

                        given()
                                .delete("/files/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_NO_CONTENT);

                        f = repo.findById(f.getId()).get();
                        assertThat(f.getContentKey(), is(nullValue()));

                        //todo: refactor to check s3 bucket
//                        assertThat(new java.io.File(filesystemRoot, contentId).exists(), is(false));
                        HeadObjectRequest getObjectRequest = HeadObjectRequest.builder()
                                .bucket("test-bucket")
                                .key(contentId)
                                .build();

                        try {
                            client.headObject(getObjectRequest);
                            fail("expected object not to exist");
                        } catch (NoSuchKeyException nske) {}
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableS3Stores
    static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public static class Config extends AbstractVaultConfiguration {

            @Override
            public VaultEndpoint vaultEndpoint() {

                String host = VaultContainerSupport.getVaultContainer().getHost();
                int port = VaultContainerSupport.getVaultContainer().getMappedPort(8200);

                VaultEndpoint vault = VaultEndpoint.create(host, port);
                vault.setScheme("http");
                return vault;
            }

            @Override
            public ClientAuthentication clientAuthentication() {
                return new TokenAuthentication("my-root-token");
            }

            @Bean
            public EnvelopeEncryptionService encrypter(VaultOperations vaultOperations) {
                return new EnvelopeEncryptionService(vaultOperations);
            }
//            @Bean
//            public java.io.File filesystemRoot() {
//                try {
//                    return Files.createTempDirectory("").toFile();
//                } catch (IOException ioe) {}
//                return null;
//            }

//            @Bean
//            public FileSystemResourceLoader fileSystemResourceLoader() {
//                return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
//            }

            @Bean
            public S3Client amazonS3() throws URISyntaxException {
//                AwsCredentials creds = AwsBasicCredentials.create(System.getenv("AWS_ACCESS_KEY_ID"), System.getenv("AWS_SECRET_KEY"));
//                AwsCredentialsProvider credsProvider = StaticCredentialsProvider.create(creds);
//                Region region = Region.US_WEST_1;
//                return S3Client.builder()
//                        .credentialsProvider(credsProvider)
//                        .region(region)
//                        .build();
                return LocalStack.getAmazonS3Client();
            }

            @Bean
            public EncryptingContentStoreConfigurer config() {
                return new EncryptingContentStoreConfigurer<FileContentStore>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration config) {
                        config.encryptionKeyContentProperty("key").keyring("fsfile");
                    }
                };
            }

//            @Bean
//            public EncryptingContentStoreConfigurer config2() {
//                return new EncryptingContentStoreConfigurer<FileContentStore2>() {
//                    @Override
//                    public void configure(EncryptingContentStoreConfiguration config) {
//                        config.encryptionKeyContentProperty("key2").keyring("filecontentstore2");
//                    }
//                };
//            }
        }
    }

    public interface FileRepository extends CrudRepository<File, Long> {}

    public interface FileContentStore extends S3ContentStore<File, UUID>, EncryptingContentStore<File, UUID> {}

//    public interface FileRepository2 extends CrudRepository<TEntity, Long> {}
//
//    public interface FileContentStore2 extends S3ContentStore<TEntity, UUID>, EncryptingContentStore<TEntity, UUID> {}

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class File {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        @JsonIgnore
        private byte[] contentKey;

        @ContentId private UUID contentId;
        @ContentLength private long contentLength;
        @MimeType private String contentMimeType;
    }

//    @Entity
//    @Getter
//    @Setter
//    @NoArgsConstructor
//    public static class TEntity {
//        @Id
//        @GeneratedValue(strategy = GenerationType.AUTO)
//        private Long id;
//
//        private String name;
//
//        @JsonIgnore
//        private byte[] contentKey2;
//
//        @ContentId private UUID contentId;
//        @ContentLength private long contentLength;
//        @MimeType private String contentMimeType;
//    }

    @SuppressWarnings("unchecked")
    public static void setEnv(Map<String, String> newenv) throws Exception {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(newenv);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>)theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for(Class cl : classes) {
                if("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
