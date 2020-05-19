package internal.org.springframework.content.mongo.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.mongo.config.EnableMongoContentRepositories;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jRunner.class)
public class EnableMongoStoresTest {

	private AnnotationConfigApplicationContext context;
	{
		Describe("EnableMongoStores", () -> {
			Context("given an enabled configuration with a mongo content repository bean",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(TestConfig.class);
							context.refresh();
						});
						AfterEach(() -> {
							context.close();
						});
						It("should have a mongo content repository bean", () -> {
							assertThat(context.getBean(TestEntityContentRepository.class),
									is(not(nullValue())));
						});
						It("should have a mongo store converter", () -> {
							assertThat(context.getBean("mongoStorePlacementService"),
									is(not(nullValue())));
						});
					});

			Context("given a context with a custom converter", () -> {
				BeforeEach(() -> {
					context = new AnnotationConfigApplicationContext();
					context.register(ConverterConfig.class);
					context.refresh();
				});
				AfterEach(() -> {
					context.close();
				});
				It("should use that converter", () -> {
					ConversionService converters = (ConversionService) context
							.getBean("mongoStorePlacementService");
					assertThat(
							converters.convert(
									UUID.fromString(
											"e49d5464-26ce-11e7-93ae-92361f002671"),
									String.class),
							is("/e49d5464/26ce/11e7/93ae/92361f002671"));
				});
			});

			Context("given an enabled configuration with no mongo content repository beans",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(EmptyConfig.class);
							context.refresh();
						});
						AfterEach(() -> {
							context.close();
						});
						It("should load the context but have no mongo repository beans",
								() -> {
									try {
										context.getBean(
												TestEntityContentRepository.class);
										fail("expected no such bean");
									}
									catch (NoSuchBeanDefinitionException e) {
										assertThat(true, is(true));
									}
								});
					});
		});

		Describe("EnableMongoContentRepositories", () -> {
			Context("given an enabled configuration with a mongo content repository bean",
					() -> {
						BeforeEach(() -> {
							context = new AnnotationConfigApplicationContext();
							context.register(EnableMongoContentRepositoriesConfig.class);
							context.refresh();
						});
						AfterEach(() -> {
							context.close();
						});
						It("should have a mongo content repository bean", () -> {
							assertThat(context.getBean(TestEntityContentRepository.class),
									is(not(nullValue())));
						});
						It("should have a mongo store converter", () -> {
							assertThat(context.getBean("mongoStorePlacementService"),
									is(not(nullValue())));
						});
					});
		});
	}

	@Test
	public void noop() {
		// noop
	}

	@Configuration
	@EnableMongoStores(basePackages = "contains.no.mongo.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
		//
	}

	@Configuration
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	@EnableMongoContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableMongoContentRepositoriesConfig {
		//
	}

	@Configuration
	@EnableMongoContentRepositories
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public MongoStoreConverter<UUID, String> uuidConverter() {
			return new MongoStoreConverter<UUID, String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-", "/"));
				}

			};
		}
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {

		@Override
		protected String getDatabaseName() {
			return "spring-content";
		}

		@Bean
		public MongoClient mongoClient() {
			return MongoClients.create("mongodb://localhost:27017");
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) throws Exception {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	@Content
	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
