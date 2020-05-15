package org.springframework.content.commons.repository.factory.stores;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.io.Serializable;
import java.net.URI;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@ContextConfiguration(classes = StoreTest.StoreTestConfiguration.class)
public class StoreTest {

	@Autowired
	private ApplicationContext context;

	{
		Describe("given a store definition", () -> {

			Context("given the application context", () -> {

				It("should have a store bean", () -> {
					assertThat(context.getBean(TestContentRepository.class), is(not(nullValue())));
				});

				It("should have the core spring content service beans", () -> {
					assertThat(context.getBean(Stores.class), is(not(nullValue())));
					assertThat(context.getBean(AnnotatedStoreEventInvoker.class), is(not(nullValue())));
				});

				It("should have a TestStore bean", () -> {
					assertThat(context.getBean(TestStore.class), is(not(nullValue())));
				});

				It("should have an TestAssociativeStore bean", () -> {
					assertThat(context.getBean(TestAssociativeStore.class), is(not(nullValue())));
				});

				It("should have an TestAssociativeAndContentStore bean", () -> {
					assertThat(context.getBean(TestAssociativeAndContentStore.class), is(not(nullValue())));
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

	public interface TestAssociativeAndContentStore extends ContentStore<Object, URI> {
	}

	@Test
	public void noop() {
	}
}
