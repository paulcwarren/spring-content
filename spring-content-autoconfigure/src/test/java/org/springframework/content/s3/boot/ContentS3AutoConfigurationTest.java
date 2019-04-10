package org.springframework.content.s3.boot;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ContentS3AutoConfigurationTest {

	@Test
	public void configurationWithBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		assertThat(context.getBean(TestEntityContentRepository.class),
				is(not(nullValue())));

		context.close();
	}

	@Test
	public void configurationWithoutBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfigWithoutBeans.class);
		context.refresh();

		assertThat(context.getBean(TestEntityContentRepository.class),
				is(not(nullValue())));

		context.close();
	}

	@Test
	public void configurationWithExplicitEnableS3Stores() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfigWithExplicitEnableS3Stores.class);
		context.refresh();

		assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
		assertThat(context.getBean(AmazonS3.class), is(not(nullValue())));

		context.close();
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig {

		public Region region() {
			return Region.getRegion(Regions.US_WEST_1);
		}

		@Bean
		public AmazonS3 s3Client() {
			AmazonS3 s3Client = new AmazonS3Client();
			s3Client.setRegion(region());
			return s3Client;
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

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
