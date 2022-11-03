package org.springframework.content.encryption;

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
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.store.JpaContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultOperations;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = EncryptionIT.Application.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EncryptionIT {

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore store;

    @Autowired
    private FileRepository repo2;

    @Autowired
    private FileContentStore2 store2;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private EnvelopeEncryptionService encrypter;

    @Autowired
    private VaultOperations vaultOperations;

    @LocalServerPort
    int port;

    private File f;

    {
        Describe("Client-side encryption", () -> {
            BeforeEach(() -> {
                RestAssured.port = port;

                f = repo.save(new File());
            });
            Context("given content", () -> {
                BeforeEach(() -> {
//                    TEntity t = new TEntity();
////                    t = store2.setContent(t, PropertyPath.from("content"), new ByteArrayInputStream("foo".getBytes()), 3L);
//                    t = store2.setContent(t, PropertyPath.from("content"), new ByteArrayInputStream("foo".getBytes()));
////                    Resource r = store2.getResource(t, PropertyPath.from("content"));
////                    assertThat(IOUtils.toString(r.getInputStream()), is("foo"));
//                    InputStream contentStream = store2.getContent(t, PropertyPath.from("content"));
//                    assertThat(IOUtils.toString(contentStream), is("foo"));

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

                    String contents = IOUtils.toString(new FileInputStream(new java.io.File(filesystemRoot, f.getContentId().toString())));
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
                                    .header("range", "bytes=18-27")
                                    .get("/files/" + f.getId() + "/content")
                                    .then()
                                    .statusCode(HttpStatus.SC_PARTIAL_CONTENT)
                                    .assertThat()
                                    .contentType(Matchers.startsWith("text/plain"))
                                    .and().extract().response();

                    assertThat(r.asString(), is("encryption"));
                });
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
                        assertThat(new java.io.File(filesystemRoot, contentId).exists(), is(false));
                    });
                });
            });
        });
    }

    @Test
    public void noop() {}

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableJpaStores
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
            public EncryptingContentStoreConfigurer config() {
                return new EncryptingContentStoreConfigurer<FileContentStore>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration config) {
                        config.encryptionKeyContentProperty("key").keyring("filecontentstore");
                    }
                };
            }

            @Bean
            public EncryptingContentStoreConfigurer config2() {
                return new EncryptingContentStoreConfigurer<FileContentStore2>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration config) {
                        config.encryptionKeyContentProperty("key2").keyring("filecontentstore2");
                    }
                };
            }
        }
    }

    public interface FileRepository extends CrudRepository<File, Long> {}

    public interface FileContentStore extends FilesystemContentStore<File, UUID>, EncryptingContentStore<File, UUID> {}

    public interface FileRepository2 extends CrudRepository<TEntity, Long> {}

    public interface FileContentStore2 extends JpaContentStore<TEntity, UUID>, EncryptingContentStore<TEntity, UUID> {}

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

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

        @JsonIgnore
        private byte[] contentKey2;

        @ContentId private UUID contentId;
        @ContentLength private long contentLength;
        @MimeType private String contentMimeType;
    }
}
