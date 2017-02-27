package internal.org.springframework.content.mongo.config;

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.operations.ContentOperations;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.mongo.config.EnableMongoContentRepositories;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;

@RunWith(Ginkgo4jRunner.class)
public class ContentRepositoryTest {

	private AnnotationConfigApplicationContext context;
	{
		Describe("EnableMongoRepositories", () -> {
			Context("given an enabled configuration with a mongo content repository bean", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(TestConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should have a mongo content repository bean", () -> {
					assertThat(context.getBean(TestEntityContentRepository.class), is(not(nullValue())));
				});
				It("should have a mongoContentTemplate bean", () -> {
					assertThat(context.getBean("mongoContentTemplate"), is(not(nullValue())));
				});
				It("should have a ContentOperations bean", () -> {
					assertThat(context.getBean(ContentOperations.class), is(not(nullValue())));
				});
			});
			Context("given an enabled configuration with no mongo content repository beans", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(EmptyConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should load the context but have no mongo repository beans", () -> {
					try {
						context.getBean(TestEntityContentRepository.class);
						fail("expected no such bean");
					} catch (NoSuchBeanDefinitionException e) {
						assertThat(true, is(true));
					}
				});
			});
		});
	}


	@Test
	public void noop() {
		// noop
	}

	@Configuration
	@EnableMongoContentRepositories(basePackages="contains.no.mongo.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableMongoContentRepositories
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoConfiguration {
		@Bean
		public GridFsTemplate gridFsTemplate() throws Exception {
			return new GridFsTemplate(mongoDbFactory(), mappingMongoConverter());
		}
		@Override
		protected String getDatabaseName() {
			return "spring-content";
		}
		@Override
		public MongoDbFactory mongoDbFactory() throws Exception {
			return super.mongoDbFactory();
		}
		@Override
		public Mongo mongo() throws Exception {
	        return new MongoClient();
		}
	}

	@Content
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
