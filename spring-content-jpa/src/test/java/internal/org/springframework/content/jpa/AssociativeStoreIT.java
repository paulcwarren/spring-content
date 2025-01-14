package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.StoreIT.SqlServerConfig;
import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AssociativeStoreIT {

	private static Class<?>[] CONFIG_CLASSES = new Class[]{
			H2Config.class,
			HSQLConfig.class,
			MySqlConfig.class,
			PostgresConfig.class
//			SqlServerConfig.class,
//			StoreIT.OracleConfig.class
	};

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private DocumentRepository repo;
    private DocumentAssociativeStore store;

	private PlatformTransactionManager txn;

	private Document document;
    private Resource resource;
    private String resourceId;

    private Exception e;

    {
        Describe("AssociativeStore", () -> {

			for (Class<?> configClass : CONFIG_CLASSES) {

	        	Context(getContextName(configClass), () -> {

	        		BeforeEach(() -> {
						context = new AnnotationConfigApplicationContext();
						context.register(StoreIT.TestConfig.class);
						context.register(configClass);
						context.refresh();

	        			repo = context.getBean(DocumentRepository.class);
	        			store = context.getBean(DocumentAssociativeStore.class);
						txn = context.getBean(PlatformTransactionManager.class);
	        		});

		            Context("given a new entity", () -> {

		                BeforeEach(() -> {
		                    document = new Document();
		                    document = repo.save(document);
		                });
		                It("should not have an associated resource", () -> {
		                    assertThat(document.getContentId(), is(nullValue()));
		                    assertThat(store.getResource(document), is(nullValue()));
		                });
		                Context("given a resource", () -> {
		                    BeforeEach(() -> {
		                        resourceId = UUID.randomUUID().toString();
		                        resource = store.getResource(resourceId);
		                    });
		                    Context("when the resource is associated", () -> {
		                       BeforeEach(() -> {
		                           store.associate(document, resourceId);
	                               store.associate(document, PropertyPath.from("rendition"), resourceId);
		                       });
		                        It("should be recorded as such on the entity's @ContentId", () -> {
		                            assertThat(document.getContentId(), is(resourceId));
                                    assertThat(document.getRenditionId(), is(resourceId));
		                        });
								Context("when the resource has content", () -> {
									BeforeEach(() -> {
										TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

										Resource r = store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().build());
										try (OutputStream os = ((WritableResource)resource).getOutputStream()) {
											os.write("Hello Client-side World!".getBytes());
										}

										txn.commit(status);
									});
									It("should not honor byte ranges", () -> {
										// relies on REST-layer to serve byte range
										Resource r = store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());
										try (InputStream is = r.getInputStream()) {
											assertThat(IOUtils.toString(is), is("Hello Client-side World!"));
										}
									});
								});
		                        Context("when the resource is unassociated", () -> {
		                            BeforeEach(() -> {
		                                store.unassociate(document);
	                                    store.unassociate(document, PropertyPath.from("rendition"));
		                            });
		                            It("should reset the entity's @ContentId", () -> {
		                                assertThat(document.getContentId(), is(nullValue()));
                                        assertThat(document.getRenditionId(), is(nullValue()));
		                            });
		                        });
		                    });

                            Context("when a invalid property path is used to associate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.associate(document, PropertyPath.from("does.not.exist"), resourceId);
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to load a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.getResource(document, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });

                            Context("when a invalid property path is used to unassociate a resource", () -> {
                                It("should throw an error", () -> {
                                    try {
                                        store.unassociate(document, PropertyPath.from("does.not.exist"));
                                    } catch (Exception sae) {
                                        this.e = sae;
                                    }
                                    assertThat(e, is(instanceOf(StoreAccessException.class)));
                                });
                            });
		                });
		            });
	        	});
			}
        });
    }
}
