package org.springframework.content.s3.boot.defaultstorage;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;
import software.amazon.awssdk.services.s3.S3Client;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class S3AutoConfigurationTest {

	static {
		mock(S3Client.class);
    }

    private ApplicationContextRunner contextRunner;

    {
		Describe("S3 auto configuration with default storage", () -> {
            BeforeEach(() -> {
                contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
            });
            Context("given a default storage type of s3", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "s3");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should create an S3Client bean", () -> {
                    contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                        Assertions.assertThat(context).hasSingleBean(S3Client.class);
                    });
                });
            });

    		Context("given a default storage type other than s3", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "fs");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should not create an S3Client bean", () -> {
                    contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                        Assertions.assertThat(context).doesNotHaveBean(S3Client.class);
                    });
                });
            });

            Context("given no default storage type", () -> {
                It("should create an S3Client bean", () -> {
                    contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                        Assertions.assertThat(context).hasSingleBean(S3Client.class);
                    });
                });
            });
		});
	}

    @SpringBootApplication(exclude={ElasticsearchAutoConfiguration.class})
	@EnableS3Stores(basePackageClasses=S3AutoConfigurationTest.class)
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
