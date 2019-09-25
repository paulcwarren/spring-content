package org.springframework.content.commons.repository.factory.stores;

import java.net.URI;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@ActiveProfiles(profiles = "c")
@ContextConfiguration(classes = StoreCandidateComponentProviderEnvironmentTest.StoreTestConfiguration.class)
public class StoreCandidateComponentProviderEnvironmentTest {

	@Autowired(required=false)
	private TestStore store;

	@Autowired(required=false)
	private TestAssociativeStore associativeStore;

	@Autowired(required=false)
	private TestContentStore contentStore;

	{
		Describe("given two stores with profiles", () -> {

				It("should have a store bean", () -> {
					assertThat(store, is(not(nullValue())));
					assertThat(associativeStore, is(nullValue()));
					assertThat(contentStore, is(not(nullValue())));
				});

		});
	}

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {
	}

	public interface TestStore extends Store<URI> {
	}

	@Profile("b")
	public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
	}

	@Profile("c")
	public interface TestContentStore extends ContentStore<Object, URI> {
	}

	@Test
	public void noop() {
	}
}
