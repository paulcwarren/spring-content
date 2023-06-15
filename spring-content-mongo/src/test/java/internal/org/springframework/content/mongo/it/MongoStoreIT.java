package internal.org.springframework.content.mongo.it;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
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
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.model.GridFSFile;

import internal.org.springframework.content.mongo.store.DefaultMongoStoreImpl;
import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;

@RunWith(Ginkgo4jRunner.class)
public class MongoStoreIT {
	private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
	private GridFsTemplate gridFsTemplate;
	private GridFSFile gridFSFile;
	private ObjectId gridFSId;
	private TestEntity entity;
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

				gridFsTemplate = context.getBean(GridFsTemplate.class);

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
                        entity = new TestEntity();
                        entity = repo.save(entity);
                    });

                    It("should not have an associated resource", () -> {
                        assertThat(entity.getContentId(), is(nullValue()));
                        assertThat(store.getResource(entity), is(nullValue()));
                    });

                    Context("given a resource", () -> {

                        BeforeEach(() -> {
                            genericResource = store.getResource(resourceLocation);
                        });

                        Context("when the resource is associated", () -> {

                            BeforeEach(() -> {
                                store.associate(entity, resourceLocation);
                                store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                            });

                            It("should be recorded as such on the entity's @ContentId", () -> {
                                assertThat(entity.getContentId(), is(resourceLocation));
                                assertThat(entity.getRenditionId(), is(resourceLocation));
                            });


							Context("when the resource has content", () -> {
								BeforeEach(() -> {
									try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
										os.write("Hello Client-side World!".getBytes());
									}
								});

								It("should not honor byte ranges", () -> {
									// relies on REST-layer to serve byte range
									Resource r = store.getResource(entity, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());
									try (InputStream is = r.getInputStream()) {
										assertThat(IOUtils.toString(is), is("Hello Client-side World!"));
									}
								});
							});

                            Context("when the resource is unassociated", () -> {

                                BeforeEach(() -> {
                                    store.unassociate(entity);
                                    store.unassociate(entity, PropertyPath.from("rendition"));
                                });

                                It("should reset the entity's @ContentId", () -> {
                                    assertThat(entity.getContentId(), is(nullValue()));
                                    assertThat(entity.getRenditionId(), is(nullValue()));
                                });
                            });

                            Context("when a invalid property path is used to associate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to load a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.getResource(entity, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to unassociate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.unassociate(entity, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });
                        });
                    });
                });
			});

			Describe("ContentStore", () -> {

                BeforeEach(() -> {
                    entity = new TestEntity();
                    entity = repo.save(entity);

                    store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                });

                It("should be able to store new content", () -> {
                    // content
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}

                    //rendition
                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}
                });

                It("should have content metadata", () -> {
                    // content
                    assertThat(entity.getContentId(), is(notNullValue()));
                    assertThat(entity.getContentId().trim().length(), greaterThan(0));
                    Assert.assertEquals(entity.getContentLen(), 27L);

                    //rendition
                    assertThat(entity.getRenditionId(), is(notNullValue()));
                    assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
                    Assert.assertEquals(entity.getRenditionLen(), 27L);
                });

                Context("when content is updated", () -> {
                    BeforeEach(() ->{
                        store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                        entity = repo.save(entity);
                    });

                    It("should have the updated content", () -> {
                        //content
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        //rendition
                        matches = false;
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

                Context("when content is updated with shorter content", () -> {
                    BeforeEach(() -> {
                        store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                        entity = repo.save(entity);
                    });
                    It("should store only the new content", () -> {
                        //content
                        boolean matches = false;
                        try (InputStream content = store.getContent(entity)) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }

                        //rendition
                        matches = false;
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    });
                });

				Context("when content is updated and not overwritten", () -> {
					It("should have the updated content", () -> {
						String contentId = entity.getContentId();
						assertThat(gridFsTemplate.getResource(contentId).exists(), is(true));

						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
						entity = repo.save(entity);

						boolean matches = false;
						try (InputStream content = store.getContent(entity)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						}

						assertThat(gridFsTemplate.getResource(contentId).exists(), is(true));

						assertThat(entity.getContentId(), is(not(contentId)));

						assertThat(gridFsTemplate.getResource(entity.getContentId()).exists(), is(true));
					});
				});

				Context("when content is unset", () -> {
                    BeforeEach(() -> {
                        resourceLocation = entity.getContentId().toString();
                        entity = store.unsetContent(entity);
                        entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                        entity = repo.save(entity);
                    });

                    It("should have no content", () -> {
                        //content
                        try (InputStream content = store.getContent(entity)) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        Assert.assertEquals(entity.getContentLen(), 0);

						assertThat(gridFsTemplate.getResource(resourceLocation).exists(), is(false));

						//rendition
                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        Assert.assertEquals(entity.getContentLen(), 0);
                    });
                });

				Context("when content is unset but kept", () -> {
					BeforeEach(() -> {
						resourceLocation = entity.getContentId().toString();
						entity = store.unsetContent(entity, PropertyPath.from("content"), UnsetContentParams.builder().disposition(UnsetContentParams.Disposition.Keep).build());
						entity = repo.save(entity);
					});

					It("should have no content", () -> {
						//content
						try (InputStream content = store.getContent(entity)) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getContentId(), is(Matchers.nullValue()));
						Assert.assertEquals(entity.getContentLen(), 0);

						assertThat(gridFsTemplate.getResource(resourceLocation).exists(), is(true));
					});
				});

                Context("when an invalid property path is used to setContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                    });
                });

                Context("when an invalid property path is used to getContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.getContent(entity, PropertyPath.from("does.not.exist"));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
                    });
                });

                Context("when an invalid property path is used to unsetContent", () -> {
                    It("should throw an error", () -> {
                        try {
                            store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                        } catch (Exception sae) {
                            this.e = sae;
                        }
                        assertThat(e, is(instanceOf(StoreAccessException.class)));
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

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	@Getter
	@Setter
	public static class TestEntity implements ContentProperty {

		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
		}
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {}
	public interface TestEntityStore extends ContentStore<TestEntity, String> {}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@jakarta.persistence.Id
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
}
