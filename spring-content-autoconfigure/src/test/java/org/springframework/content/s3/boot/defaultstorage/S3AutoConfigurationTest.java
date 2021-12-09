package org.springframework.content.s3.boot.defaultstorage;

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
import static org.mockito.Mockito.mock;
import static org.springframework.support.TestUtils.setEnv;

import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import software.amazon.awssdk.services.s3.S3Client;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class S3AutoConfigurationTest {

	static {
		mock(AmazonS3.class);

		try {
		    Map<String,String> props = new HashMap<>();
		    props.put("AWS_REGION", Regions.US_WEST_1.getName());
		    props.put("AWS_ACCESS_KEY_ID", "user");
		    props.put("AWS_SECRET_KEY", "password");
		    setEnv(props);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	{
		Describe("S3 auto configuration with default storage", () -> {

            Context("given a default storage type of s3", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.storage.type.default", "s3");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.storage.type.default");
                });
                It("should create an S3Client bean", () -> {
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfigWithoutBeans.class);
                    context.refresh();

                    assertThat(context.getBean(S3Client.class), is(not(nullValue())));
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
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfigWithoutBeans.class);
                    context.refresh();

                    try {
                        context.getBean(S3Client.class);
                        fail("expected no S3Client bean but bean found");
                    } catch (NoSuchBeanDefinitionException nsbe) {
                    }
                });
            });

            Context("given no default storage type", () -> {

                It("should create an S3Client bean", () -> {
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfigWithoutBeans.class);
                    context.refresh();

                    assertThat(context.getBean(S3Client.class), is(not(nullValue())));
                });
            });
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableS3Stores(basePackageClasses=S3AutoConfigurationTest.class)
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
