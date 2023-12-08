package org.springframework.content.fs.boot.defaultstorage;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class FilesystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    {
		Describe("FilesystemContentAutoConfiguration", () -> {
            BeforeEach(() -> {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(FilesystemContentAutoConfiguration.class));
            });
            Context("given a default storage type of fs", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "fs");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should create an FileSystemResourceLoader bean", () -> {
                    contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                        Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                    });
                });
            });

            Context("given a default storage type other than fs", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "s3");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should not create an FileSystemResourceLoader bean", () -> {
                    contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                        Assertions.assertThat(context).doesNotHaveBean(FileSystemResourceLoader.class);
                    });
                });
            });

            Context("given no default storage type", () -> {
                It("should create an FileSystemResourceLoader bean", () -> {
                    contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                        Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
                    });
                });
            });
        });
	}

    @SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	public static class TestConfig {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
