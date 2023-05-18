package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
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
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
import internal.org.springframework.content.jpa.testsupport.stores.ClaimStore;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentStoreIT {

	private static Class<?>[] CONFIG_CLASSES = new Class[]{
			H2Config.class,
			HSQLConfig.class,
			MySqlConfig.class,
			PostgresConfig.class,
			SqlServerConfig.class,
			StoreIT.OracleConfig.class
		};

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	// for postgres (large object api operations must be in a transaction)
	private PlatformTransactionManager ptm;

	protected ClaimRepository claimRepo;
	protected ClaimStore claimFormStore;

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
						claimFormStore = context.getBean(ClaimStore.class);

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

							claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                            claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
						});

						It("should be able to store new content", () -> {
						    // content
							doInTransaction(ptm, () -> {
								try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
									assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
								} catch (IOException ioe) {}
								return null;
							});

	                        // rendition
                            doInTransaction(ptm, () -> {
                                try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
                                } catch (IOException ioe) {}
                                return null;
                            });

						});

						It("should have content metadata", () -> {
						    // content
							Assert.assertThat(claim.getClaimForm().getContentId(), is(notNullValue()));
							Assert.assertThat(claim.getClaimForm().getContentId().trim().length(), greaterThan(0));
							Assert.assertEquals(claim.getClaimForm().getContentLength(), 27L);

							// renditoin
                            Assert.assertThat(claim.getClaimForm().getRenditionId(), is(notNullValue()));
                            Assert.assertThat(claim.getClaimForm().getRenditionId().trim().length(), greaterThan(0));
                            Assert.assertEquals(claim.getClaimForm().getRenditionLen(), 40L);
						});

						Context("when content is updated", () -> {
							BeforeEach(() ->{
								claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
								claim = claimRepo.save(claim);
							});

							It("should have the updated content", () -> {
							    // content
								doInTransaction(ptm, () -> {
									boolean matches = false;
									try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
										matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
										assertThat(matches, is(true));
									} catch (IOException e) {
									}
									return null;
								});

                                // rendition
                                doInTransaction(ptm, () -> {
                                    boolean matches = false;
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException e) {
                                    }
                                    return null;
                                });
							});
						});

						Context("when content is updated with shorter content", () -> {
							BeforeEach(() -> {
								claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                                claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
								claim = claimRepo.save(claim);
							});
							It("should store only the new content", () -> {
							    // content
								doInTransaction(ptm, () -> {
									boolean matches = false;
									try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
										matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
										assertThat(matches, is(true));
									} catch (IOException e) {
									}
									return null;
								});

                                // rendition
                                doInTransaction(ptm, () -> {
                                    boolean matches = false;
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                                        assertThat(matches, is(true));
                                    } catch (IOException e) {
                                    }
                                    return null;
                                });
							});
						});

						Context("when content is updated and not overwritten", () -> {
							It("should have the updated content", () -> {
								String contentId = claim.getClaimForm().getContentId();

								claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().overwriteExistingContent(false).build());
								claim = claimRepo.save(claim);

								boolean matches = false;
								try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
									matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
									assertThat(matches, is(true));
								}

								assertThat(claim.getClaimForm().getContentId(), is(not(contentId)));
							});
						});

						Context("when content is deleted", () -> {
						    BeforeEach(() -> {
		                        id = claim.getClaimForm().getContentId();
								claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
                                claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));
								claim = claimRepo.save(claim);
							});

						    AfterEach(() -> {
						        claimRepo.delete(claim);
		                    });

							It("should have no content", () -> {
		                        ClaimForm deletedClaimForm = new ClaimForm();
		                        deletedClaimForm.setContentId((String)id);

		                        // content
		                        doInTransaction(ptm, () -> {
		                        	try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
				                        Assert.assertThat(content, is(nullValue()));
		                        	} catch (IOException e) {
									}
									return null;
		                        });

                                Assert.assertThat(claim.getClaimForm().getContentId(), is(nullValue()));
                                Assert.assertEquals(claim.getClaimForm().getContentLength(), 0);

	                            // rendition
                                doInTransaction(ptm, () -> {
                                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                                        Assert.assertThat(content, is(nullValue()));
                                    } catch (IOException e) {
                                    }
                                    return null;
                                });

								Assert.assertThat(claim.getClaimForm().getRenditionId(), is(nullValue()));
								Assert.assertEquals(claim.getClaimForm().getRenditionLen(), 0);
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

	protected boolean hasContent(Claim claim, PropertyPath path) {

		if (claim == null) {
			return false;
		}

		boolean exists = doInTransaction(ptm, () -> {
			try (InputStream content = claimFormStore.getContent(claim, path)) {
				if (content != null) {
					return true;
				}
			} catch (Exception e) {
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
			if (existingClaim.getClaimForm() != null && (hasContent(existingClaim, PropertyPath.from("claimForm/content")) || hasContent(existingClaim, PropertyPath.from("claimForm/rendition")))) {
				String contentId = existingClaim.getClaimForm().getContentId();
                String renditionId = existingClaim.getClaimForm().getRenditionId();
				claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/content"));
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/rendition"));
				if (existingClaim.getClaimForm() != null) {
					Assert.assertThat(existingClaim.getClaimForm().getContentId(), is(nullValue()));
					Assert.assertEquals(existingClaim.getClaimForm().getContentLength(), 0);
                    Assert.assertThat(existingClaim.getClaimForm().getRenditionId(), is(nullValue()));
                    Assert.assertEquals(existingClaim.getClaimForm().getRenditionLen(), 0);

					// double check the content got removed
					InputStream content = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/content")));
                    InputStream renditionContent = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/rendition")));
					try {
						Assert.assertThat(content, is(nullValue()));
                        Assert.assertThat(renditionContent, is(nullValue()));
					}
					finally {
						IOUtils.closeQuietly(content);
					}
				}
			}
		}
	}
}
