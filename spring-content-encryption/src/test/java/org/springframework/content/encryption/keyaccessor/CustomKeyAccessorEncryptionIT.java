package org.springframework.content.encryption.keyaccessor;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.encryption.config.EncryptingContentStoreConfiguration;
import org.springframework.content.encryption.config.EncryptingContentStoreConfigurer;
import internal.org.springframework.content.rest.boot.autoconfigure.ContentRestAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
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
import org.springframework.content.encryption.keys.DataEncryptionKeyAccessor;
import org.springframework.content.encryption.keys.StoredDataEncryptionKey.UnencryptedSymmetricDataEncryptionKey;
import org.springframework.content.encryption.store.EncryptingContentStore;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.context.WebApplicationContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@SpringBootTest(classes = CustomKeyAccessorEncryptionIT.Application.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CustomKeyAccessorEncryptionIT {

    @Autowired
    private FileRepository repo;

    @Autowired
    private FileContentStore3 store;

    @Autowired
    private ContentEncryptionKeyRepository contentEncryptionKeyRepository;

    @Autowired
    private java.io.File filesystemRoot;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private FsFile f;

    {
        Describe("Client-side encryption with custom key storage", () -> {
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

                    assertThat(contentEncryptionKeyRepository.findById(f.getContentId()).isPresent(), is(true));
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
                Context("when the content is unset", () -> {
                    It("it should remove the content and clear the content key", () -> {
                        f = repo.findById(f.getId()).get();
                        String contentId = f.getContentId().toString();

                        given()
                                .delete("/fsFiles/" + f.getId() + "/content")
                                .then()
                                .statusCode(HttpStatus.SC_NO_CONTENT);

                        f = repo.findById(f.getId()).get();
                        assertThat(contentEncryptionKeyRepository.findById(UUID.fromString(contentId)).isEmpty(), is(true));
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
        public static class Config {

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
            public EncryptingContentStoreConfigurer<FileContentStore3> config(ContentEncryptionKeyRepository encryptionKeyRepository) {
                return new EncryptingContentStoreConfigurer<FileContentStore3>() {
                    @Override
                    public void configure(EncryptingContentStoreConfiguration<FileContentStore3> config) {
                        config.dataEncryptionKeyAccessor(new EntityStorageDataEncryptionKeyAccessor<>(encryptionKeyRepository));
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

        @ContentId private UUID contentId;
        @ContentLength private long contentLength;
        @MimeType private String contentMimeType;
    }

    public interface ContentEncryptionKeyRepository extends CrudRepository<ContentEncryptionKey, UUID> {


    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ContentEncryptionKey {
        @Id
        private UUID contentId;

        private String algorithm;

        private byte[] encryptionKey;

        private byte[] iv;
    }

    @RequiredArgsConstructor
    private static class EntityStorageDataEncryptionKeyAccessor<S> implements DataEncryptionKeyAccessor<S, UnencryptedSymmetricDataEncryptionKey> {
        private final ContentEncryptionKeyRepository contentEncryptionKeyRepository;

        @Override
        public Collection<UnencryptedSymmetricDataEncryptionKey> findKeys(S entity, ContentProperty contentProperty) {
            var contentId = (UUID)contentProperty.getContentId(entity);
            if(contentId == null) {
                return null;
            }
            return contentEncryptionKeyRepository.findById(contentId).stream()
                    .map(encryptionKeyEntity -> new UnencryptedSymmetricDataEncryptionKey(
                            encryptionKeyEntity.getAlgorithm(),
                            encryptionKeyEntity.getEncryptionKey(),
                            encryptionKeyEntity.getIv()
                    ))
                    .toList();
        }

        @Override
        public S setKeys(S entity, ContentProperty contentProperty,
                Collection<UnencryptedSymmetricDataEncryptionKey> dataEncryptionKeys
        ) {
            var contentId = (UUID)contentProperty.getContentId(entity);
            var maybeDataEncryptionKey = dataEncryptionKeys.stream().findFirst();

            if(maybeDataEncryptionKey.isEmpty()) {
                contentEncryptionKeyRepository.deleteById(contentId);
                return entity;
            }

            var dataEncryptionKey = maybeDataEncryptionKey.get();


            var encryptionKeyEntity = contentEncryptionKeyRepository.findById(contentId)
                    .orElseGet(() -> {
                        var contentEncryptionKey = new ContentEncryptionKey();
                        contentEncryptionKey.setContentId(contentId);
                        return contentEncryptionKey;
                    });

            encryptionKeyEntity.setAlgorithm(dataEncryptionKey.getAlgorithm());
            encryptionKeyEntity.setEncryptionKey(dataEncryptionKey.getKeyData());
            encryptionKeyEntity.setIv(dataEncryptionKey.getInitializationVector());

            contentEncryptionKeyRepository.save(encryptionKeyEntity);

            return entity;
        }
    }
}
