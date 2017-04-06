package org.springframework.content.s3.boot;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;

import internal.org.springframework.content.commons.placementstrategy.UUIDPlacementStrategy;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.AbstractS3ContentRepositoryConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;

public class ContentS3AutoConfigurationTests {

	@Test
	public void contextLoads() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(TestConfig.class);
		context.refresh();

		MatcherAssert.assertThat(context.getBean(TestEntityContentRepository.class), CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));
		MatcherAssert.assertThat(context.getBean(UUIDPlacementStrategy.class), CoreMatchers.is(CoreMatchers.not(CoreMatchers.nullValue())));

		context.close();
	}

	@Configuration
	@AutoConfigurationPackage
	@EnableAutoConfiguration
	public static class TestConfig extends AbstractS3ContentRepositoryConfiguration {

		@Autowired
		private AmazonS3 client;

		@Override
		public String bucket() {
			return "spring-eg-content-s3";
		}
		@Override
		public Region region() {
			return Region.getRegion(Regions.US_WEST_1);
		}

		@Override
		public SimpleStorageResourceLoader simpleStorageResourceLoader() {
	        client.setRegion(region());
			return new SimpleStorageResourceLoader(client);
		}
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

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
