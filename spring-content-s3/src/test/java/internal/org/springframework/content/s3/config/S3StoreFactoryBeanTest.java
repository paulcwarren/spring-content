package internal.org.springframework.content.s3.config;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.utils.PlacementService;

import java.io.Serializable;

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

@RunWith(Ginkgo4jRunner.class)
public class S3StoreFactoryBeanTest {

	private S3StoreFactoryBean factory;

	private AmazonS3 client;
	private PlacementService placer;

	private Store store;

	{
		Describe("S3StoreFactoryBean", () -> {
			BeforeEach(() -> {
				client = mock(AmazonS3.class);
				placer = mock(PlacementService.class);

				factory = new S3StoreFactoryBean(client, placer);
			});
			Context("#getStore", () -> {
				BeforeEach(() -> {
					factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
				});
				JustBeforeEach(() -> {
					store = factory.getStore();
				});
				Context("given a Store", () -> {
					BeforeEach(() -> {
						factory.setStoreInterface(S3StoreFactoryBeanTest.TestStore.class);
					});
					It("should return a store implementation", () -> {
						assertThat(store, is(not(nullValue())));
					});
				});
				Context("given an AssociativeStore", () -> {
					BeforeEach(() -> {
						factory.setStoreInterface(S3StoreFactoryBeanTest.TestAssociativeStore.class);
					});
					It("should return a store implementation", () -> {
						assertThat(store, is(not(nullValue())));
					});
				});
//				Context("given a ContentStore", () -> {
//					BeforeEach(() -> {
//						factory.setStoreInterface(S3StoreFactoryBeanTest.TestContentStore.class);
//					});
//					It("should return a store implementation", () -> {
//						assertThat(store, is(not(nullValue())));
//					});
//				});
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
