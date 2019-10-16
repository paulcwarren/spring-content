package internal.org.springframework.content.rest.utils;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.commons.storeservice.ContentStoreInfoImpl;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.support.TestEntity;
import org.junit.runner.RunWith;

import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
@RunWith(Ginkgo4jRunner.class)
public class ContentStoreUtilsTest {

	private ContentStoreService stores;
	private Class<TestEntity> entityClass;

	private ContentStoreInfo result, info;
	private String storePath;

	private StoreWithRenderable store;
	private TestEntity entity;
	private List<MediaType> mimeTypes;
	private InputStream content;
	private HttpHeaders headers;

	private ContentStoreUtils.ResourcePlan plan;

	{
		Describe("ContentStoreUtilsTest", () -> {
			Context("#findContentStore by entity class", () -> {
				BeforeEach(() -> {
					stores = mock(ContentStoreService.class);
					entityClass = TestEntity.class;
				});
				JustBeforeEach(() -> {
					result = ContentStoreUtils.findContentStore(stores, entityClass);
				});
				Context("given a content store service with a store that has the deprecated ContentStoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(
								ContentStoreWithDeprecatedAnnotation.class);
						info = new ContentStoreInfoImpl(
								ContentStoreWithDeprecatedAnnotation.class,
								TestEntity.class, storeImpl);
						when(stores.getStores(eq(ContentStore.class)))
								.thenReturn(new ContentStoreInfo[] { info });
					});
					It("should find the content store", () -> {
						assertThat(result, is(info));
					});
				});
				Context("given a content store service with a store that has the StoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(
								ContentStoreWithAnnotation.class);
						info = new ContentStoreInfoImpl(
								ContentStoreWithAnnotation.class,
								TestEntity.class, storeImpl);
						when(stores.getStores(eq(ContentStore.class)))
								.thenReturn(new ContentStoreInfo[] { info });
					});
					It("should find the content store", () -> {
						assertThat(result, is(info));
					});
				});
			});
			Context("#findContentStore by path name", () -> {
				BeforeEach(() -> {
					stores = mock(ContentStoreService.class);
					storePath = "testEntities";
				});
				JustBeforeEach(() -> {
					result = ContentStoreUtils.findContentStore(stores, storePath);
				});
				Context("given a stores service with a content store with no annotation",
						() -> {
							BeforeEach(() -> {
								ContentStore storeImpl = mock(
										ContentStoreWithAnnotation.class);
								info = new ContentStoreInfoImpl(
										ContentStoreWithAnnotation.class, TestEntity.class,
										storeImpl);
								when(stores.getStores(eq(ContentStore.class)))
										.thenReturn(new ContentStoreInfo[] { info });
							});
							It("should return the content store", () -> {
								assertThat(result, is(info));
							});
						});
			});
			Context("#storePath", () -> {
				JustBeforeEach(() -> {
					storePath = ContentStoreUtils.storePath(info);
				});
				Context("given a content store with no annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(TestContentStore.class);
						info = new ContentStoreInfoImpl(TestContentStore.class, TestEntity.class, storeImpl);
					});
					It("should return return 'testEntities'", () -> {
						assertThat(storePath, is("testEntities"));
					});
				});
				Context("given a content store with a deprecated ContentStoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
						info = new ContentStoreInfoImpl(
								ContentStoreWithDeprecatedAnnotation.class,
								TestEntity.class, storeImpl);
					});
					It("should return return the specified path", () -> {
						assertThat(storePath, is("testEntities"));
					});
				});
				Context("given a content store with a deprecated ContentStoreRestResource annotation that specifies a path", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithPath.class);
						info = new ContentStoreInfoImpl(ContentStoreWithPath.class,
								TestEntity.class, storeImpl);
					});
					It("should return return the specified path", () -> {
						assertThat(storePath, is("some-path"));
					});
				});
				Context("given a content store with a StoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
						info = new ContentStoreInfoImpl(ContentStoreWithAnnotation.class,
								TestEntity.class, storeImpl);
					});
					It("should return return the specified path", () -> {
						assertThat(storePath, is("testEntities"));
					});
				});
				Context("given a content store with a StoreRestResource annotation that specifies a path", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithAnotherPath.class);
						info = new ContentStoreInfoImpl(ContentStoreWithAnotherPath.class, TestEntity.class, storeImpl);
					});
					It("should return return the specified path", () -> {
						assertThat(storePath, is("some-other-path"));
					});
				});
				Context("given a Store with no annotations", () -> {
					BeforeEach(() -> {
						Store storeImpl = mock(TestStore.class);
						info = new ContentStoreInfoImpl(TestStore.class, null, storeImpl);
					});
					It("should return 'tests'", () -> {
						assertThat(storePath, is("tests"));
					});
				});
				Context("given a Store with a StoreRestResource annotation", () -> {
					BeforeEach(() -> {
						Store storeImpl = mock(TestStoreWithAnnotation.class);
						info = new ContentStoreInfoImpl(TestStoreWithAnnotation.class, null, storeImpl);
					});
					It("should return 'tests'", () -> {
						assertThat(storePath, is("testWithAnnotations"));
					});
				});
				Context("given a Store with a StoreRestResource annotation with a path of 'foo'", () -> {
					BeforeEach(() -> {
						Store storeImpl = mock(TestStoreWithPath.class);
						info = new ContentStoreInfoImpl(TestStoreWithPath.class, null, storeImpl);
					});
					It("should return 'tests'", () -> {
						assertThat(storePath, is("foo"));
					});
				});
			});
		});
	}

	public interface TestStore extends Store<String> {}

	@StoreRestResource
	public interface TestStoreWithAnnotation extends Store<String> {}

	@StoreRestResource(path="foo")
	public interface TestStoreWithPath extends Store<String> {}

	public interface TestContentStore extends ContentStore<TestEntity, UUID> {}

	@ContentStoreRestResource
	public interface ContentStoreWithDeprecatedAnnotation extends ContentStore<TestEntity, UUID> {}

	@ContentStoreRestResource(path = "some-path")
	public interface ContentStoreWithPath extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource
	public interface ContentStoreWithAnnotation extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource(path = "some-other-path")
	public interface ContentStoreWithAnotherPath extends ContentStore<TestEntity, UUID> {}

	public interface StoreWithRenderable extends ContentStore<TestEntity, UUID>, Store<UUID>, Renderable<TestEntity> {}
}
