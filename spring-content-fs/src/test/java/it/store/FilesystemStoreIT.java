package it.store;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.repository.DefaultFilesystemStoreImpl;
import net.bytebuddy.utility.RandomString;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class FilesystemStoreIT {
	private DefaultFilesystemStoreImpl<Object, String> mongoContentRepoImpl;
	private FilesystemStoreIT.TEntity property;
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
		Describe("DefaultFilesystemStoreImpl", () -> {

			BeforeEach(() -> {
				context = new AnnotationConfigApplicationContext();
				context.register(FilesystemStoreIT.TestConfig.class);
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
						property = new FilesystemStoreIT.TEntity();
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
					property = new FilesystemStoreIT.TEntity();
					property = repo.save(property);

					store.setContent(property, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				});

				It("should be able to store new content", () -> {
					try (InputStream content = store.getContent(property)) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {}
				});

				It("should have content metadata", () -> {
					assertThat(property.getContentId(), is(notNullValue()));
					assertThat(property.getContentId().trim().length(), greaterThan(0));
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
						resourceLocation = property.getContentId().toString();
						property = store.unsetContent(property);
						property = repo.save(property);
					});

					It("should have no content", () -> {
						try (InputStream content = store.getContent(property)) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(property.getContentId(), is(Matchers.nullValue()));
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
			});
		});
	}

	@Test
	public void test() {
		// noop
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@EnableFilesystemStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	public static class InfrastructureConfig {

	    @Bean
	    File filesystemRoot() {
	        try {
	            return Files.createTempDirectory("").toFile();
	        } catch (IOException ioe) {}
	        return null;
	    }

	    @Bean
	    FileSystemResourceLoader fileSystemResourceLoader() {
	        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
	    }

	    @Bean
	    public DataSource dataSource() {
	        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
	        return builder.setType(EmbeddedDatabaseType.H2).build();
	    }

	    @Bean
	    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

	        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
	        vendorAdapter.setDatabase(Database.H2);
	        vendorAdapter.setGenerateDdl(true);

	        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
	        factory.setJpaVendorAdapter(vendorAdapter);
	        factory.setPackagesToScan("it.store");
	        factory.setDataSource(dataSource());

	        return factory;
	    }

	    @Bean
	    public PlatformTransactionManager transactionManager() {

	        JpaTransactionManager txManager = new JpaTransactionManager();
	        txManager.setEntityManagerFactory(entityManagerFactory().getObject());
	        return txManager;
	    }
	}

	public interface ContentProperty {
	    String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	@Entity
	public static class TEntity implements ContentProperty {

	    @Id
        private String id = UUID.randomUUID().toString();

		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public TEntity() {
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

	public interface TestEntityRepository extends JpaRepository<TEntity, String> {}
	public interface TestEntityStore extends ContentStore<TEntity, String> {}

	@Entity
    @Table(name="shared_id_entity")
	public static class SharedIdContentIdEntity implements ContentProperty {

		@Id
		@ContentId
		private String contentId = UUID.randomUUID().toString();

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
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

	public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {}
	public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}
}
