package internal.org.springframework.content.jpa;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static internal.org.springframework.content.jpa.StoreIT.H2Config;
import static internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import static internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import static internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import static internal.org.springframework.content.jpa.StoreIT.SqlServerConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.UUID;

import org.junit.runner.RunWith;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class AssociativeStoreIT {

	private static Class<?>[] CONFIG_CLASSES = new Class[]{
			H2Config.class, 
			HSQLConfig.class, 
			MySqlConfig.class, 
			PostgresConfig.class, 
			SqlServerConfig.class
		};

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
	
    private DocumentRepository repo;
    private DocumentAssociativeStore store;

    private Document document;
    private Resource resource;
    private String resourceId;

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
		                       });
		                        It("should be recorded as such on the entity's @ContentId", () -> {
		                            assertThat(document.getContentId(), is(resourceId));
		                        });
		                        Context("when the resource is unassociated", () -> {
		                            BeforeEach(() -> {
		                                store.unassociate(document);
		                            });
		                            It("should reset the entity's @ContentId", () -> {
		                                assertThat(document.getContentId(), is(nullValue()));
		                            });
		                        });
		                    });
		                });
		            });
	        	});
			}
        });
    }
}
