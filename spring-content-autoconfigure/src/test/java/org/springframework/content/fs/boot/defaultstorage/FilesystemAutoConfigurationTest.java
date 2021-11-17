package org.springframework.content.fs.boot.defaultstorage;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.support.TestEntity;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
import internal.org.springframework.content.renditions.boot.autoconfigure.RenditionsContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class FilesystemAutoConfigurationTest {

	{
		Describe("FilesystemContentAutoConfiguration", () -> {

            Context("given a default storage type of fs", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "fs");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should create an FileSystemResourceLoader bean", () -> {
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    assertThat(context.getBean(FileSystemResourceLoader.class), is(not(nullValue())));
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
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    try {
                        context.getBean(FileSystemResourceLoader.class);
                        fail("context should not have a a FileSystemResourceLoader bean");
                    } catch (NoSuchBeanDefinitionException nsbe) {
                    }
                });
            });

            Context("given no default storage type", () -> {

                It("should create an FileSystemResourceLoader bean", () -> {
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfig.class);
                    context.refresh();

                    assertThat(context.getBean(FileSystemResourceLoader.class), is(not(nullValue())));
                });
            });
        });
	}

	@Configuration
    @EnableAutoConfiguration(exclude= {
            ElasticsearchAutoConfiguration.class,
            MongoAutoConfiguration.class,
            JpaContentAutoConfiguration.class,
            JpaVersionsAutoConfiguration.class,
            MongoContentAutoConfiguration.class,
            RenditionsContentAutoConfiguration.class,
            S3ContentAutoConfiguration.class})
	@EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
	public static class TestConfig {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
