package org.springframework.content.encryption.s3;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.encryption.keys.ContentPropertyDataEncryptionKeyAccessor;
import internal.org.springframework.content.encryption.keys.VaultTransitDataEncryptionKeyWrapper;
import java.util.List;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.encryption.store.EncryptingContentStore;
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
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.context.WebApplicationContext;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = EncryptionIT.Application.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EncryptionIT {

    private static final String BUCKET = "test-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private static Object mutex = new Object();

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore store;

    @Autowired
    private S3Client client;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VaultTemplate vaultTemplate;

    private File f;


    static {
        System.setProperty("spring.content.s3.bucket", "test-bucket");
    }

    {
        Describe("Client-side encryption with s3 storage", () -> {
            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

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

                    vaultTemplate.opsForTransit().createKey("my-key");
                }

                f = repo.save(new File());
            });
            Context("given content", () -> {
                BeforeEach(() -> {
                    given()
                            .contentType("text/plain")
                            .body("Hello Client-side encryption World!")
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
                    MockMvcResponse r =
                            given()
                                    .header("accept", "text/plain")
                                    .header("range", "bytes=14-27")
                                    .get("/files/" + f.getId() + "/content")
                                    .then()
                                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                    .assertThat()
                                    .contentType(Matchers.startsWith("text/plain"))
                                    .and().extract().response();

                    assertThat(r.asString(), is("ide encryption"));

                    r =
                            given()
                                    .header("accept", "text/plain")
                                    .header("range", "bytes=19-27")
                                    .get("/files/" + f.getId() + "/content")
                                    .then()
                                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                    .assertThat()
                                    .contentType(Matchers.startsWith("text/plain"))
                                    .and().extract().response();

                    assertThat(r.asString(), is("ncryption"));
                });
                Context("when the keyring is rotated", () -> {
                    BeforeEach(() -> {
                        vaultTemplate.opsForTransit().rotate("my-key");
                    });
                    /*It("should not change the stored content key", () -> {
                        f = repo.findById(f.getId()).get();

                        assertThat(new String(f.getContentKey()), startsWith("vault:v1"));
                    });*/
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
                    /*
                    It("should update the content key version when next stored", () -> {
                        given()
                                .contentType("text/plain")
                                .body("Hello Client-side encryption World!")
                                .when()
                                .post("/files/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_OK);

                        f = repo.findById(f.getId()).get();
                        assertThat(new String(f.getContentKey()), startsWith("vault:"));
                        assertThat(new String(f.getContentKey()), not(startsWith("vault:v1")));
                    });
                     */
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

    @SpringBootApplication(exclude={FilesystemContentAutoConfiguration.class})
    @ImportAutoConfiguration(ContentRestAutoConfiguration.class)
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
            public S3Client amazonS3() throws URISyntaxException {
                return LocalStack.getAmazonS3Client();
            }

            @Bean
            public EncryptingContentStoreConfigurer config() {
                return new EncryptingContentStoreConfigurer<FileContentStore>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<FileContentStore> config) {
                        config.encryptionKeyContentProperty("key")
                                .dataEncryptionKeyWrappers(List.of(new VaultTransitDataEncryptionKeyWrapper(
                                        vaultTemplate().opsForTransit(),
                                        "my-key"
                                )));
                    }
                };
            }
        }
    }

    public interface FileRepository extends CrudRepository<File, Long> {}

    public interface FileContentStore extends S3ContentStore<File, UUID>, EncryptingContentStore<File, UUID> {}

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
}
