package internal.org.springframework.content.gcs.config;
/*package internal.org.springframework.content.s3.config;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.s3.store.DefaultS3StoreImpl;
import org.junit.runner.RunWith;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.content.s3.config.S3ObjectIdResolvers;
import org.springframework.core.convert.ConversionService;

import java.io.Serializable;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
public class GCSStoreFactoryBeanTest {

	private S3StoreFactoryBean factory;

	private AmazonS3 client;
	private SimpleStorageResourceLoader loader;
	private ConversionService converter;
	private GCSObjectIdResolvers resolvers;

	private S3ObjectIdResolver resolver;

	{
		Describe("S3StoreFactoryBean", () -> {
			BeforeEach(() -> {
				client = mock(AmazonS3.class);
				loader = mock(SimpleStorageResourceLoader.class);
				converter = mock(ConversionService.class);
				resolvers = new GCSObjectIdResolvers();

				factory = new S3StoreFactoryBean(client, loader, converter, resolvers);
			});
			Context("given a Store", () -> {
				BeforeEach(() -> {
					factory.setStoreInterface(GCSStoreFactoryBeanTest.TestStore.class);
				});
				Context("given no resolvers", () -> {
					It("should use the DEFAULT_S3OBJECTID_RESOLVER_STORE", () -> {
						DefaultS3StoreImpl store = (DefaultS3StoreImpl) factory
								.getContentStoreImpl();
						assertThat(store.getS3ObjectIdResolver(),
								is(S3StoreFactoryBean.DEFAULT_S3OBJECTID_RESOLVER_STORE));
					});
				});
				Context("given a resolver", () -> {
					BeforeEach(() -> {
						resolver = new SerializableResolver();
						resolvers.add(resolver);
					});
					It("should instantiate a store that uses resolver", () -> {
						DefaultS3StoreImpl store = (DefaultS3StoreImpl) factory
								.getContentStoreImpl();
						assertThat(store.getS3ObjectIdResolver(), is(resolver));
					});
				});
			});
			Context("given an AssociativeStore", () -> {
				BeforeEach(() -> {
					factory.setStoreInterface(
							GCSStoreFactoryBeanTest.TestAssociativeStore.class);
				});
				Context("given no resolvers", () -> {
					It("should use an instance of DefaultAssociativeStoreS3ObjectIdResolver",
							() -> {
								DefaultS3StoreImpl store = (DefaultS3StoreImpl) factory
										.getContentStoreImpl();
								assertThat(store.getS3ObjectIdResolver(), is(instanceOf(
										DefaultAssociativeStoreS3ObjectIdResolver.class)));
							});
				});
				Context("given an id resolver", () -> {
					BeforeEach(() -> {
						resolver = new SerializableResolver();
						resolvers.add(resolver);
					});
					It("should instantiate a store that uses the id resolver", () -> {
						DefaultS3StoreImpl store = (DefaultS3StoreImpl) factory
								.getContentStoreImpl();
						assertThat(store.getS3ObjectIdResolver(), is(resolver));
					});
				});
				Context("given an id and an entity resolver", () -> {
					BeforeEach(() -> {
						resolvers.add(new SerializableResolver());
						resolver = new ObjectResolver();
						resolvers.add(resolver);
					});
					It("should instantiate a store that uses the entity resolver", () -> {
						DefaultS3StoreImpl store = (DefaultS3StoreImpl) factory
								.getContentStoreImpl();
						assertThat(store.getS3ObjectIdResolver(), is(resolver));
					});
				});
			});
		});
	}

	public interface TestStore extends Store<Serializable> {
	}

	private interface TestAssociativeStore
			extends AssociativeStore<Object, Serializable> {
	}

	public class SerializableResolver implements S3ObjectIdResolver<Serializable> {
	}

	public class ObjectResolver implements S3ObjectIdResolver<Object> {
	}
}
*/