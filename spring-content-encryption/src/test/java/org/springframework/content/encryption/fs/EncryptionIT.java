package org.springframework.content.encryption.fs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
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
import org.springframework.content.encryption.VaultContainerSupport;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = EncryptionIT.Application.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EncryptionIT {

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore3 store;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

    {
        Describe("Client-side encryption with fs storage", () -> {
            BeforeEach(() -> {
                RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

                f = repo.save(new FsFile());
            });
            Context("given content", () -> {
                BeforeEach(() -> {
                    given()
                            .contentType("text/plain")
                            .body("Hello Client-side encryption World!")
                            .when()
                            .post("/fsFiles/" + f.getId() + "/content")
                            .then()
                            .statusCode(HttpStatus.SC_CREATED);
                });
                It("should be stored encrypted", () -> {
                    Optional<FsFile> fetched = repo.findById(f.getId());
                    assertThat(fetched.isPresent(), is(true));
                    f = fetched.get();

                    String contents = IOUtils.toString(new FileInputStream(new java.io.File(filesystemRoot, f.getContentId().toString())));
                    assertThat(contents, is(not("Hello Client-side encryption World!")));
                });
                It("should be retrieved decrypted", () -> {
                    given()
                            .header("accept", "text/plain")
                            .get("/fsFiles/" + f.getId() + "/content")
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
                                    .header("range", "bytes=16-27")
                                    .get("/fsFiles/" + f.getId() + "/content")
                                    .then()
                                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                    .assertThat()
                                    .contentType(Matchers.startsWith("text/plain"))
                                    .and().extract().response();

                    assertThat(r.asString(), is("e encryption"));
                });
                /*
                Context("when the keyring is rotated", () -> {
                    BeforeEach(() -> {
                        encrypter.rotate("filecontentstore");
                    });
                    It("should not change the stored content key", () -> {
                        f = repo.findById(f.getId()).get();

                        assertThat(new String(f.getContentKey()), startsWith("vault:v1"));
                    });
                    It("should still retrieve content decrypted", () -> {
                        given()
                                .header("accept", "text/plain")
                                .get("/fsFiles/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_OK)
                                .assertThat()
                                .contentType(Matchers.startsWith("text/plain"))
                                .body(Matchers.equalTo("Hello Client-side encryption World!"));
                    });
                    It("should update the content key version when next stored", () -> {
                        given()
                                .contentType("text/plain")
                                .body("Hello Client-side encryption World!")
                                .when()
                                .post("/fsFiles/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_OK);

                        f = repo.findById(f.getId()).get();
                        assertThat(new String(f.getContentKey()), startsWith("vault:"));
                        assertThat(new String(f.getContentKey()), not(startsWith("vault:v1")));
                    });
                }); */
                Context("when the content is unset", () -> {
                    It("it should remove the content and clear the content key", () -> {
                        f = repo.findById(f.getId()).get();
                        String contentId = f.getContentId().toString();

                        given()
                                .delete("/fsFiles/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_NO_CONTENT);

                        f = repo.findById(f.getId()).get();
                        assertThat(f.getContentKey(), is(nullValue()));
                        assertThat(new java.io.File(filesystemRoot, contentId).exists(), is(false));
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}

    @SpringBootApplication(exclude={S3ContentAutoConfiguration.class})
    @ImportAutoConfiguration(ContentRestAutoConfiguration.class)
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableFilesystemStores
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
            public java.io.File filesystemRoot() {
                try {
                    return Files.createTempDirectory("").toFile();
                } catch (IOException ioe) {}
                return null;
            }

            @Bean
            public FileSystemResourceLoader fileSystemResourceLoader() {
                return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
            }

            @Bean
            public EncryptingContentStoreConfigurer<FileContentStore3> config() {
                return new EncryptingContentStoreConfigurer<FileContentStore3>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<FileContentStore3> config) {
                        config.encryptionKeyContentProperty("key");
                    }
                };
            }
        }
    }

    public interface FileRepository extends CrudRepository<FsFile, Long> {}

    public interface FileContentStore3 extends FilesystemContentStore<FsFile, UUID>, EncryptingContentStore<FsFile, UUID> {}

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class FsFile {
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
