package org.springframework.content.s3.boot.autoconfigure;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;
import org.springframework.support.TestUtils;
import org.springframework.util.ReflectionUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import software.amazon.awssdk.services.s3.S3Client;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentS3AutoConfigurationTest {

	private static AmazonS3 client;

	static {
		client = mock(AmazonS3.class);

		try {
		    Map<String,String> props = new HashMap<>();
		    props.put("AWS_REGION", Regions.US_WEST_1.getName());
		    props.put("AWS_ACCESS_KEY_ID", "user");
		    props.put("AWS_SECRET_KEY", "password");
		    TestUtils.setEnv(props);
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

	{
		Describe("FilesystemContentAutoConfiguration", () -> {
			Context("given a configuration with beans", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(not(nullValue())));
					assertThat(context.getBean(AmazonS3.class), is(client));

					context.close();
				});
			});

			Context("given a configuration without any beans", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfigWithoutBeans.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(S3Client.class), is(not(nullValue())));

					S3Client client = context.getBean(S3Client.class);

//                    Field endpointField = getField(DefaultS3Client.class, "clientConfiguration");
//                    URI endpoint = (URI) endpointField.get(client);
//                    assertThat(endpoint.toString(), is("https://s3.us-west-1.amazonaws.com"));
//
//                    Field providerField = getField(AmazonS3Client.class, "awsCredentialsProvider");
//                    AWSCredentialsProvider provider = (AWSCredentialsProvider) providerField.get(client);
//                    assertThat(provider.getCredentials().getAWSAccessKeyId(), is("user"));
//                    assertThat(provider.getCredentials().getAWSSecretKey(), is("password"));
//
//                    Field coField = getField(AmazonS3Client.class, "clientOptions");
//                    S3ClientOptions options = (S3ClientOptions) coField.get(client);
//                    assertThat(options.isPathStyleAccess(), is(false));

					context.close();
				});
			});

			Context("given a configuration with an explicit @EnableS3Stores annotation", () -> {
				It("should load the context", () -> {

					AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
					context.register(TestConfigWithExplicitEnableS3Stores.class);
					context.refresh();

					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
					assertThat(context.getBean(S3Client.class), is(not(nullValue())));

					context.close();
				});
			});

            Context("given an environment specifying s3 properties", () -> {
                BeforeEach(() -> {
                    System.setProperty("spring.content.s3.endpoint", "http://some-endpoint");
                    System.setProperty("spring.content.s3.accessKey", "foo");
                    System.setProperty("spring.content.s3.secretKey", "bar");
                    System.setProperty("spring.content.s3.pathStyleAccess", "true");
                });
                AfterEach(() -> {
                    System.clearProperty("spring.content.s3.endpoint");
                    System.clearProperty("spring.content.s3.accessKey");
                    System.clearProperty("spring.content.s3.secretKey");
                    System.clearProperty("spring.content.s3.pathStyleAccess");
                });
                It("should have a filesystem properties bean with the correct root set", () -> {
                    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                    context.register(TestConfigWithProperties.class);
                    context.refresh();

                    S3Client client = context.getBean(S3Client.class);

//                    Field endpointField = getField(AmazonWebServiceClient.class, "endpoint");
//                    URI endpoint = (URI) endpointField.get(client);
//                    assertThat(endpoint.toString(), is("http://some-endpoint"));
//
//                    Field providerField = getField(AmazonS3Client.class, "awsCredentialsProvider");
//                    AWSCredentialsProvider provider = (AWSCredentialsProvider) providerField.get(client);
//                    assertThat(provider.getCredentials().getAWSAccessKeyId(), is("foo"));
//                    assertThat(provider.getCredentials().getAWSSecretKey(), is("bar"));
//
//                    Field coField = getField(AmazonS3Client.class, "clientOptions");
//                    S3ClientOptions options = (S3ClientOptions) coField.get(client);
//                    assertThat(options.isPathStyleAccess(), is(true));

                    context.close();
                });
            });
		});
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig {

		@Bean
		public AmazonS3 s3Client() {
			return client;
		}
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	@EnableS3Stores
	public static class TestConfigWithExplicitEnableS3Stores {
		// will be supplied by auto-configuration
	}

	@Configuration
    @AutoConfigurationPackage
    @EnableAutoConfiguration
    public static class TestConfigWithProperties {
        // will be supplied by auto-configuration
    }

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends S3ContentStore<TestEntity, String> {
	}

    private static Field getField(Class<?> clazz, String field) {

        Field f = ReflectionUtils.findField(clazz, field);
        ReflectionUtils.makeAccessible(f);
        return f;
    }
}
