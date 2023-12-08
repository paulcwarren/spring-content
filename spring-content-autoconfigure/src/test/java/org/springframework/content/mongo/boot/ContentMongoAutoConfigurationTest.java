package org.springframework.content.mongo.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.client.MongoClient;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.mongo.store.MongoContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ContentMongoAutoConfigurationTest {

	private ApplicationContextRunner contextRunner;

	{
		Describe("ContentMongoAutoConfiguration", () -> {
			BeforeEach(() -> {
				contextRunner = new ApplicationContextRunner()
						.withConfiguration(AutoConfigurations.of(MongoContentAutoConfiguration.class));
			});
			It("should load the context", () -> {
				contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
					Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
				});
			});
		});
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {
		@Override
		protected String getDatabaseName() {
			return MongoTestContainer.getTestDbName();
		}

		@Override
		@Bean
		public MongoClient mongoClient() {
			return MongoTestContainer.getMongoClient();
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Override
		@Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	@SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Document
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
	}

	public interface TestEntityContentRepository extends MongoContentStore<TestEntity, String> {
	}
}
