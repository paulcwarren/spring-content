package org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.Serializable;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

@RunWith(Ginkgo4jSpringRunner.class )
@Ginkgo4jConfiguration(threads=1)
@ContextConfiguration(classes = StoreTest.StoreTestConfiguration.class)
public class StoreTest {

    @Autowired
    private ApplicationContext context;

    {
        Describe("given a store definition", () -> {
        	// see TestContentRepository
        	Context("given the application context", () -> {
        		It("should have a store bean", () -> {
        			assertThat(context.getBean(TestContentRepository.class), is(not(nullValue())));
        		});
        		It("should have the core spring content service beans", () -> {
        			assertThat(context.getBean(ContentStoreService.class), is(not(nullValue())));
        			assertThat(context.getBean(PlacementService.class), is(not(nullValue())));
        			assertThat(context.getBean(RenditionService.class), is(not(nullValue())));
        		});
        		It("should have a teststore bean", () -> {
        			assertThat(context.getBean(TestStore.class), is(not(nullValue())));
        		});
        	});
        });
    }

    @Configuration
    @EnableTestStores
    public static class StoreTestConfiguration {
    }

    public interface TestContentRepository extends ContentStore<Object, Serializable> {
    }

    public interface TestStore extends Store<URI> {
    }
    
    public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
    }
    
    @Test
    public void noop() {}
}
