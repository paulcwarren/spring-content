package org.springframework.content.s3.boot;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

public class ContentS3AutoConfigurationTest {

	@Test
	public void configurationWithBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MatcherAssert.assertThat(context.getBean(TestEntityContentRepository.class),
				CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));

		context.close();
	}

	@Test
	public void configurationWithoutBeans() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfigWithoutBeans.class);
		context.refresh();

		MatcherAssert.assertThat(context.getBean(TestEntityContentRepository.class),
				CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));

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

	@Entity
	@Content
	public class TestEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
