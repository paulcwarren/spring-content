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

import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
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
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.repository.DefaultFilesystemStoreImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;

import javax.sql.DataSource;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class FilesystemStorePropertyPathAccessorsIT {

    private DefaultFilesystemStoreImpl<Object, String> mongoContentRepoImpl;
	private FilesystemStorePropertyPathAccessorsIT.TEntity entity;
	private Resource genericResource;

	private InputStream content;
	private InputStream result;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private String resourceLocation;

	{
		Describe("DefaultFilesystemStoreImpl PropertyPath Accessors", () -> {

			BeforeEach(() -> {
				context = new AnnotationConfigApplicationContext();
				context.register(FilesystemStorePropertyPathAccessorsIT.TestConfig.class);
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
						entity = new FilesystemStorePropertyPathAccessorsIT.TEntity();
						entity = repo.save(entity);
					});

					It("should not have an associated resource", () -> {
						assertThat(entity.getContent().getId(), is(nullValue()));
						assertThat(store.getResource(entity, PropertyPath.from("content")), is(nullValue()));
					});

					Context("given a resource", () -> {

						BeforeEach(() -> {
							genericResource = store.getResource(resourceLocation);
						});

						Context("when the resource is associated", () -> {

							BeforeEach(() -> {
								store.associate(entity, PropertyPath.from("content"), resourceLocation);
							});

							It("should be recorded as such on the entity's @ContentId", () -> {
								assertThat(entity.getContent().getId(), is(resourceLocation));
							});

							Context("when the resource is unassociated", () -> {

								BeforeEach(() -> {
                                    store.unassociate(entity, PropertyPath.from("content"));
								});

								It("should reset the entity's @ContentId", () -> {
									assertThat(entity.getContent().getId(), is(nullValue()));
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
					entity = new FilesystemStorePropertyPathAccessorsIT.TEntity();
					entity = repo.save(entity);

					store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				});

				It("should be able to store new content", () -> {
				    // content
					try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {}
				});

				It("should have content metadata", () -> {
				    // content
					assertThat(entity.getContent().getId(), is(notNullValue()));
					assertThat(entity.getContent().getId().trim().length(), greaterThan(0));
					Assert.assertEquals(entity.getContent().getLength(), 27L);
				});

				Context("when content is updated", () -> {
					BeforeEach(() ->{
						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
						entity = repo.save(entity);
					});

					It("should have the updated content", () -> {
					    //content
						boolean matches = false;
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						}
					});
				});

				Context("when content is updated with shorter content", () -> {
					BeforeEach(() -> {
						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
						entity = repo.save(entity);
					});
					It("should store only the new content", () -> {
					    //content
						boolean matches = false;
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
							assertThat(matches, is(true));
						}
					});
				});

				Context("when content is deleted", () -> {
					BeforeEach(() -> {
						resourceLocation = entity.getContent().getId().toString();
						entity = store.unsetContent(entity, PropertyPath.from("content"));
						entity = repo.save(entity);
					});

					It("should have no content", () -> {
					    //content
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getContent().getId(), is(Matchers.nullValue()));
						Assert.assertEquals(entity.getContent().getLength(), 0);
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
			});
		});
	}

	@Test
	public void test() {
		// noop
	}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@EntityScan(basePackageClasses = TEntity.class)
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
	        return builder.setType(EmbeddedDatabaseType.HSQL).build();
	    }

	    @Bean
	    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

	        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
				vendorAdapter.setDatabase(Database.HSQL);
	        vendorAdapter.setGenerateDdl(true);

			EntityManagerFactoryBuilder builder = createEntityManagerFactoryBuilder(new JpaProperties());
			return builder.dataSource(dataSource).packages(TEntity.class).persistenceUnit("firstDs").build();
		}

		private EntityManagerFactoryBuilder createEntityManagerFactoryBuilder(JpaProperties jpaProperties) {

			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			return new EntityManagerFactoryBuilder(vendorAdapter, jpaProperties.getProperties(), null);
		}

	    @Bean
	    public PlatformTransactionManager transactionManager(DataSource dataSource) {

	        JpaTransactionManager txManager = new JpaTransactionManager();
	        txManager.setEntityManagerFactory(entityManagerFactory(dataSource).getObject());
	        return txManager;
	    }
	}

	@Entity
	@NoArgsConstructor
	@Getter
	@Setter
	@Table(name = "tentity_content")
	public static class TEntity {

	    @Id
	    @GeneratedValue(strategy = GenerationType.AUTO)
	    private UUID id;

	    private String number;

	    @Embedded
	    @AttributeOverride(name="id", column = @Column(name = "content__id"))
	    private EmbeddedContent content = new EmbeddedContent();
	}

	@Embeddable
	@NoArgsConstructor
	@Getter
	@Setter
	public static class EmbeddedContent {
	    @ContentId
	    private String id;

	    @ContentLength
	    private long length;

	    @MimeType
	    private String mimetype;

	    @OriginalFileName
	    private String filename;
	}

	public interface TestEntityRepository extends JpaRepository<TEntity, UUID> {}
	public interface TestEntityStore extends ContentStore<TEntity, String> {}
}
