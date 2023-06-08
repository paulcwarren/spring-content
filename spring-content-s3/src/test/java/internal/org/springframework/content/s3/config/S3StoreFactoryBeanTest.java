package internal.org.springframework.content.s3.config;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.support.GenericApplicationContext;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import software.amazon.awssdk.services.s3.S3Client;

@RunWith(Ginkgo4jRunner.class)
public class S3StoreFactoryBeanTest {

	private S3StoreFactoryBean factory;

	private GenericApplicationContext context = new GenericApplicationContext();
	private S3Client client;
	private PlacementService placer;

	private Store store;

	{
		Describe("S3StoreFactoryBean", () -> {
			BeforeEach(() -> {
				client = mock(S3Client.class);
				placer = mock(PlacementService.class);

				context.registerBean("amazonS3", S3Client.class, () -> client);
				context.refresh();

				factory = new S3StoreFactoryBean(S3StoreFactoryBeanTest.TestStore.class/*, context, client, placer*/);
				factory.setContext(context);
				factory.setClient(client);
				factory.setS3StorePlacementService(placer);
			});
			Context("#getStore", () -> {
				BeforeEach(() -> {
					factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
				});
				JustBeforeEach(() -> {
					store = factory.getStore();
				});
				Context("given a Store", () -> {
					It("should return a store implementation", () -> {
						assertThat(store, is(not(nullValue())));
					});
				});
				Context("given an AssociativeStore", () -> {
					It("should return a store implementation", () -> {
						assertThat(store, is(not(nullValue())));
					});
				});
			});
		});
	}

	public interface TestStore extends Store<Serializable> {
	}

	private interface TestAssociativeStore extends AssociativeStore<Object, Serializable> {
	}

	private interface TestContentStore extends ContentStore<Object, Serializable> {
	}
}
