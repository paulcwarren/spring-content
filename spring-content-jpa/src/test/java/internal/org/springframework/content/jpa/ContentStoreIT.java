package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.GenericGenerator;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.store.ValueGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.StoreIT.SqlServerConfig;
import internal.org.springframework.content.jpa.StoreIT.TestConfig;
import internal.org.springframework.content.jpa.testsupport.models.Claim;
import internal.org.springframework.content.jpa.testsupport.models.ClaimForm;
import internal.org.springframework.content.jpa.testsupport.repositories.ClaimRepository;
import internal.org.springframework.content.jpa.testsupport.stores.ClaimFormStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentStoreIT {

	private static Class<?>[] CONFIG_CLASSES = new Class[]{
			H2Config.class,
			HSQLConfig.class,
			MySqlConfig.class,
			PostgresConfig.class,
			SqlServerConfig.class
		};

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	// for postgres (large object api operations must be in a transaction)
	private PlatformTransactionManager ptm;

	protected ClaimRepository claimRepo;
	protected ClaimFormStore claimFormStore;

	protected Claim claim;
    protected Object id;

	{
		Describe("ContentStore", () -> {

			for (Class<?> configClass : CONFIG_CLASSES) {

				Context(getContextName(configClass), () -> {

					BeforeEach(() -> {
						context = new AnnotationConfigApplicationContext();
						context.register(TestConfig.class);
						context.register(configClass);
						context.refresh();

						ptm = context.getBean(PlatformTransactionManager.class);
						claimRepo = context.getBean(ClaimRepository.class);
						claimFormStore = context.getBean(ClaimFormStore.class);

						if (ptm == null) {
							ptm = mock(PlatformTransactionManager.class);
						}
					});

					AfterEach(() -> {
						deleteAllClaimFormsContent();
						deleteAllClaims();
					});

					Context("given an Entity with content", () -> {

						BeforeEach(() -> {
							claim = new Claim();
							claim.setFirstName("John");
							claim.setLastName("Smith");
							claim.setClaimForm(new ClaimForm());
							claim = claimRepo.save(claim);

							claimFormStore.setContent(claim.getClaimForm(), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						});

						It("should be able to store new content", () -> {
							doInTransaction(ptm, () -> {
								try (InputStream content = claimFormStore.getContent(claim.getClaimForm())) {
									assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
								} catch (IOException ioe) {}
								return null;
							});
						});

						It("should have content metadata", () -> {
							Assert.assertThat(claim.getClaimForm().getContentId(), is(notNullValue()));
							Assert.assertThat(claim.getClaimForm().getContentId().trim().length(), greaterThan(0));
							Assert.assertEquals(claim.getClaimForm().getContentLength(), 27L);
						});

						Context("when content is updated", () -> {
							BeforeEach(() ->{
								claimFormStore.setContent(claim.getClaimForm(), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
								claim = claimRepo.save(claim);
							});

							It("should have the updated content", () -> {
								doInTransaction(ptm, () -> {
									boolean matches = false;
									try (InputStream content = claimFormStore.getContent(claim.getClaimForm())) {
										matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
										assertThat(matches, is(true));
									} catch (IOException e) {
									}
									return null;
								});
							});
						});

						Context("when content is updated with shorter content", () -> {
							BeforeEach(() -> {
								claimFormStore.setContent(claim.getClaimForm(), new ByteArrayInputStream("Hello Spring World!".getBytes()));
								claim = claimRepo.save(claim);
							});
							It("should store only the new content", () -> {
								doInTransaction(ptm, () -> {
									boolean matches = false;
									try (InputStream content = claimFormStore.getContent(claim.getClaimForm())) {
										matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
										assertThat(matches, is(true));
									} catch (IOException e) {
									}
									return null;
								});
							});
						});

						Context("when content is deleted", () -> {
						    BeforeEach(() -> {
		                        id = claim.getClaimForm().getContentId();
								claimFormStore.unsetContent(claim.getClaimForm());
								claim = claimRepo.save(claim);
							});

						    AfterEach(() -> {
						        claimRepo.delete(claim);
		                    });

							It("should have no content", () -> {
		                        ClaimForm deletedClaimForm = new ClaimForm();
		                        deletedClaimForm.setContentId((String)id);

		                        doInTransaction(ptm, () -> {
		                        	try (InputStream content = claimFormStore.getContent(deletedClaimForm)) {
				                        Assert.assertThat(content, is(nullValue()));
		                        	} catch (IOException e) {
									}
									return null;
		                        });

								Assert.assertThat(claim.getClaimForm().getContentId(), is(nullValue()));
								Assert.assertEquals(claim.getClaimForm().getContentLength(), 0);
							});
						});

		                Context("when content is updated and the content id field is computed from a custom value generator", () -> {

		                    It("should assign a new content Id", () -> {

		                        TEntityWithGenRepository repoWithGen = context.getBean(TEntityWithGenRepository.class);
		                        TEntityWithGenStore storeWithGen = context.getBean(TEntityWithGenStore.class);

		                        ContentStoreIT.TEntityWithGenerator entity = new ContentStoreIT.TEntityWithGenerator();
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

	                            claim = new Claim();
	                            claim.setClaimForm(new ClaimForm());
                                claim = claimRepo.save(claim);
	                            claimFormStore.setContent(claim.getClaimForm(), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                claim = claimRepo.save(claim);
		                        String firstContentId = claim.getClaimForm().getContentId();

                                claimFormStore.setContent(claim.getClaimForm(), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                                claim = claimRepo.save(claim);
		                        String secondContentId = claim.getClaimForm().getContentId();

		                        assertThat(firstContentId, is(secondContentId));
		                    });
		                });
					});
				});
			}
		});
    }

	public static <T> T doInTransaction(PlatformTransactionManager ptm, Supplier<T> block) {
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());

		try {
			T result = block.get();
			ptm.commit(status);
			return result;
		} catch (Exception e) {
			ptm.rollback(status);
		}

		return null;
	}

	protected boolean hasContent(ClaimForm claimForm) {

		if (claimForm == null) {
			return false;
		}

		boolean exists = doInTransaction(ptm, () -> {
			try (InputStream content = claimFormStore.getContent(claimForm)) {
				if (content != null) {
					return true;
				}
			} catch (IOException e) {
			}
			return false;
		});

		return exists;
	}

	protected void deleteAllClaims() {
		claimRepo.deleteAll();
	}

	protected void deleteAllClaimFormsContent() {
		Iterable<Claim> existingClaims = claimRepo.findAll();
		for (Claim existingClaim : existingClaims) {
			if (existingClaim.getClaimForm() != null && hasContent(existingClaim.getClaimForm())) {
				String contentId = existingClaim.getClaimForm().getContentId();
				claimFormStore.unsetContent(existingClaim.getClaimForm());
				if (existingClaim.getClaimForm() != null) {
					Assert.assertThat(existingClaim.getClaimForm().getContentId(), is(nullValue()));
					Assert.assertEquals(existingClaim.getClaimForm().getContentLength(), 0);

					// double check the content got removed
					ClaimForm deletedClaimForm = new ClaimForm();
					deletedClaimForm.setContentId(contentId);
					InputStream content = doInTransaction(ptm, () -> claimFormStore.getContent(deletedClaimForm));
					try {
						Assert.assertThat(content, is(nullValue()));
					}
					finally {
						IOUtils.closeQuietly(content);
					}
				}
			}
		}
	}

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    @Table(name="tentity_with_generator")
    public static class TEntityWithGenerator {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @ContentId
        @GenericGenerator(strategy=ContentStoreIT.TestContentIdGenerator.class)
        private String contentId;

        @ContentLength
        private long contentLen;
    }

    public interface TEntityWithGenRepository extends JpaRepository<TEntityWithGenerator, String> {}
    public interface TEntityWithGenStore extends ContentStore<TEntityWithGenerator, String> {}

    public static class TestContentIdGenerator implements ValueGenerator<ContentStoreIT.TEntityWithGenerator, String> {

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