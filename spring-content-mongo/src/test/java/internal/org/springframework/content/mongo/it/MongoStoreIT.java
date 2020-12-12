package internal.org.springframework.content.mongo.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import javax.persistence.Id;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.GenericGenerator;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.store.ValueGenerator;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.gridfs.model.GridFSFile;

import internal.org.springframework.content.mongo.store.DefaultMongoStoreImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;

@RunWith(Ginkgo4jRunner.class)
public class MongoStoreIT {
	private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
	private GridFsTemplate gridFsTemplate;
	private GridFSFile gridFSFile;
	private ObjectId gridFSId;
	private TestEntity property;
	private GridFsResource resource;
	private Resource genericResource;
	private PlacementService placer;

	private InputStream content;
	private InputStream result;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private String resourceLocation;


	{
		Describe("DefaultMongoStoreImpl", () -> {

			BeforeEach(() -> {
				context = new AnnotationConfigApplicationContext();
				context.register(TestConfig.class);
				context.refresh();

				repo = context.getBean(TestEntityRepository.class);
				store = context.getBean(TestEntityStore.class);

				RandomString random  = new RandomString(5);
				resourceLocation = random.nextString();
			});

			AfterEach(() -> {
				context.close();
			});

			Describe("Store", () -> {

				Context("#getResource", () -> {

					BeforeEach(() -> {
						genericResource = store.getResource(resourceLocation);
					});

					AfterEach(() -> {
						((DeletableResource)genericResource).delete();
					});

					It("should get Resource", () -> {
						assertThat(genericResource, is(instanceOf(Resource.class)));
					});

					It("should not exist", () -> {
						assertThat(genericResource.exists(), is(false));
					});

					Context("given content is added to that resource", () -> {

						BeforeEach(() -> {
							try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
								try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
									IOUtils.copy(is, os);
								}
							}
						});

						It("should store that content", () -> {
							assertThat(genericResource.exists(), is(true));

							boolean matches = false;
							try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
								try (InputStream actual = genericResource.getInputStream()) {
									matches = IOUtils.contentEquals(expected, actual);
									assertThat(matches, Matchers.is(true));
								}
							}
						});

						Context("given that resource is then updated", () -> {

							BeforeEach(() -> {
								try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
									try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
										IOUtils.copy(is, os);
									}
								}
							});

							It("should store that updated content", () -> {
								assertThat(genericResource.exists(), is(true));

								try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
									try (InputStream actual = genericResource.getInputStream()) {
										assertThat(IOUtils.contentEquals(expected, actual), is(true));
									}
								}
							});
						});

						Context("given that resource is then deleted", () -> {

							BeforeEach(() -> {
								try {
									((DeletableResource) genericResource).delete();
								} catch (Exception e) {
									this.e = e;
								}
							});

							It("should not exist", () -> {
								assertThat(e, is(nullValue()));
							});
						});
					});
				});
			});

			Describe("AssociativeStore", () -> {

				Context("given a new entity", () -> {

					BeforeEach(() -> {
						property = new TestEntity();
						property = repo.save(property);
					});

					It("should not have an associated resource", () -> {
						assertThat(property.getContentId(), is(nullValue()));
						assertThat(store.getResource(property), is(nullValue()));
					});

					Context("given a resource", () -> {

						BeforeEach(() -> {
							genericResource = store.getResource(resourceLocation);
						});

						Context("when the resource is associated", () -> {

							BeforeEach(() -> {
								store.associate(property, resourceLocation);
							});

							It("should be recorded as such on the entity's @ContentId", () -> {
								assertThat(property.getContentId(), is(resourceLocation));
							});

							Context("when the resource is unassociated", () -> {

								BeforeEach(() -> {
									store.unassociate(property);
								});

								It("should reset the entity's @ContentId", () -> {
									assertThat(property.getContentId(), is(nullValue()));
								});
							});
						});
					});
				});
			});

			Describe("ContentStore", () -> {

				BeforeEach(() -> {
					property = new TestEntity();
					property = repo.save(property);

					store.setContent(property, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				});

				It("should be able to store new content", () -> {
					try (InputStream content = store.getContent(property)) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {}
				});

				It("should have content metadata", () -> {
					Assert.assertThat(property.getContentId(), is(notNullValue()));
					Assert.assertThat(property.getContentId().trim().length(), greaterThan(0));
					Assert.assertEquals(property.getContentLen(), 27L);
				});

				Context("when content is updated", () -> {
					BeforeEach(() ->{
						store.setContent(property, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
						property = repo.save(property);
					});

					It("should have the updated content", () -> {
						boolean matches = false;
						try (InputStream content = store.getContent(property)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						}
					});
				});

				Context("when content is updated with shorter content", () -> {
					BeforeEach(() -> {
						store.setContent(property, new ByteArrayInputStream("Hello Spring World!".getBytes()));
						property = repo.save(property);
					});
					It("should store only the new content", () -> {
						boolean matches = false;
						try (InputStream content = store.getContent(property)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
							assertThat(matches, is(true));
						}
					});
				});

				Context("when content is deleted", () -> {
					BeforeEach(() -> {
						resourceLocation = property.getContentId();
						property = store.unsetContent(property);
						property = repo.save(property);
					});

					It("should have no content", () -> {
						try (InputStream content = store.getContent(property)) {
							Assert.assertThat(content, is(Matchers.nullValue()));
						}

						Assert.assertThat(property.getContentId(), is(Matchers.nullValue()));
						Assert.assertEquals(property.getContentLen(), 0);
					});
				});

				Context("when content is deleted and the id field is shared with javax id", () -> {

					It("should not reset the id field", () -> {
						SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
						SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

						SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

						sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
						String id = sharedIdContentIdEntity.getContentId();
						sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
						assertThat(sharedIdContentIdEntity.getContentId(), is(id));
						assertThat(sharedIdContentIdEntity.getContentLen(), is(0L));
					});
				});

				Context("when content is deleted and the id field is shared with spring id", () -> {

					It("should not reset the id field", () -> {
						SharedSpringIdRepository SharedSpringIdRepository = context.getBean(SharedSpringIdRepository.class);
						SharedSpringIdStore SharedSpringIdStore = context.getBean(SharedSpringIdStore.class);

						SharedSpringIdContentIdEntity SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(new SharedSpringIdContentIdEntity());

						SharedSpringIdContentIdEntity = SharedSpringIdStore.setContent(SharedSpringIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(SharedSpringIdContentIdEntity);
						String id = SharedSpringIdContentIdEntity.getContentId();
						SharedSpringIdContentIdEntity = SharedSpringIdStore.unsetContent(SharedSpringIdContentIdEntity);
						assertThat(SharedSpringIdContentIdEntity.getContentId(), is(id));
						assertThat(SharedSpringIdContentIdEntity.getContentLen(), is(0L));
					});
				});

                Context("when content is updated and the content id field is computed from a custom value generator", () -> {

                    It("should assign a new content Id", () -> {

                        TEntityWithGenRepository repoWithGen = context.getBean(TEntityWithGenRepository.class);
                        TEntityWithGenStore storeWithGen = context.getBean(TEntityWithGenStore.class);

                        MongoStoreIT.TEntityWithGenerator entity = new MongoStoreIT.TEntityWithGenerator();
                        entity = storeWithGen.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        entity = repoWithGen.save(entity);
                        String firstContentId = entity.getContentId();

                        entity = storeWithGen.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        entity = repoWithGen.save(entity);
                        String secondContentId = entity.getContentId();

                        assertThat(firstContentId, is(not(secondContentId)));
                    });
                });

                Context("when content is updated and the content id field is not computed", () -> {

                    It("should assign a new content Id", () -> {

                        property = new TestEntity();
                        property = repo.save(property);
                        store.setContent(property, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        property = repo.save(property);
                        String firstContentId = property.getContentId();

                        store.setContent(property, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        property = repo.save(property);
                        String secondContentId = property.getContentId();

                        assertThat(firstContentId, is(secondContentId));
                    });
                });
			});
		});
	}

	@Test
	public void test() {
		// noop
	}

	@Configuration
	@EnableMongoRepositories(considerNestedRepositories = true)
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {

		@Value("#{environment.MONGODB_URL}")
		private String mongoDbUrl = "mongodb://localhost:27017";

		@Override
		protected String getDatabaseName() {
			return "spring-content";
		}

		@Override
        @Bean
		public MongoClient mongoClient() {
			return MongoClients.create(mongoDbUrl);
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

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	public static class TestEntity implements ContentProperty {

		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
		}

		@Override
        public String getContentId() {
			return this.contentId;
		}

		@Override
        public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
        public long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {}
	public interface TestEntityStore extends ContentStore<TestEntity, String> {}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@javax.persistence.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
        public String getContentId() {
			return this.contentId;
		}

		@Override
        public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
        public long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedIdRepository extends MongoRepository<SharedIdContentIdEntity, String> {}
	public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

	public static class SharedSpringIdContentIdEntity implements ContentProperty {

		@org.springframework.data.annotation.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
        public String getContentId() {
			return this.contentId;
		}

		@Override
        public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
        public long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedSpringIdRepository extends MongoRepository<SharedSpringIdContentIdEntity, String> {}
	public interface SharedSpringIdStore extends ContentStore<SharedSpringIdContentIdEntity, String> {}

    @Document
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TEntityWithGenerator {

        @Id
        private String id;

        @ContentId
        @GenericGenerator(strategy=MongoStoreIT.TestContentIdGenerator.class)
        private String contentId;

        @ContentLength
        private long contentLen;
    }

    public interface TEntityWithGenRepository extends MongoRepository<TEntityWithGenerator, String> {}
    public interface TEntityWithGenStore extends ContentStore<TEntityWithGenerator, String> {}

    public static class TestContentIdGenerator implements ValueGenerator<MongoStoreIT.TEntityWithGenerator, String> {

        @Override
        public String generate(TEntityWithGenerator entity) {

            return UUID.randomUUID().toString();
        }

        @Override
        public boolean regenerate(TEntityWithGenerator entity) {

            return true;
        }
    }
}
