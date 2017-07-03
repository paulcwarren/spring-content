package internal.org.springframework.content.rest.utils;

import java.util.UUID;

import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.rest.StoreRestResource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;

import internal.org.springframework.content.commons.storeservice.ContentStoreInfoImpl;
import internal.org.springframework.content.rest.TestEntity;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
@RunWith(Ginkgo4jRunner.class)
public class StoreUtilsTest {
	
	private ContentStoreService stores;
	private Class<TestEntity> entityClass;
	
	private ContentStoreInfo result, info;
	private String storePath;
	
	{
		Describe("StoreUtilsTest", () -> {
			Context("#findContentStore (by entity class)", () -> {
				BeforeEach(() -> {
					stores = mock(ContentStoreService.class);
					entityClass = TestEntity.class;
				});
				JustBeforeEach(() -> {
					result = ContentStoreUtils.findContentStore(stores, entityClass);
				});
				Context("given a content store service with a store that has the deprecated ContentStoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
						info = new ContentStoreInfoImpl(ContentStoreWithDeprecatedAnnotation.class, TestEntity.class, storeImpl);
						when(stores.getStores(eq(ContentStore.class))).thenReturn(new ContentStoreInfo[]{info});
					});
					It("should find the content store", () -> {
						assertThat(result, is(info));
					});
				});
				Context("given a content store service with a store that has the StoreRestResource annotation", () -> {
					BeforeEach(() -> {
						ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
						info = new ContentStoreInfoImpl(ContentStoreWithAnnotation.class, TestEntity.class, storeImpl);
						when(stores.getStores(eq(ContentStore.class))).thenReturn(new ContentStoreInfo[]{info});
					});
					It("should find the content store", () -> {
						assertThat(result, is(info));
					});
				});
			});
		});
		Context("#findContentStore (by path name)", () -> {
			BeforeEach(() -> {
				stores = mock(ContentStoreService.class);
				storePath = "testEntities";
			});
			JustBeforeEach(() -> {
				result = ContentStoreUtils.findContentStore(stores, storePath);
			});
			Context("given a stores service with a content store with no annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
					info = new ContentStoreInfoImpl(ContentStoreWithAnnotation.class, TestEntity.class, storeImpl);
					when(stores.getStores(eq(ContentStore.class))).thenReturn(new ContentStoreInfo[]{info});
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
			Context("given a content store with a deprecated ContentStoreRestResource annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
					info = new ContentStoreInfoImpl(ContentStoreWithDeprecatedAnnotation.class, TestEntity.class, storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("testEntities"));
				});
			});
			Context("given a content store with a deprecated ContentStoreRestResource annotation that specifies a path", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithPath.class);
					info = new ContentStoreInfoImpl(ContentStoreWithPath.class, TestEntity.class, storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("some-path"));
				});
			});
			Context("given a content store with a StoreRestResource annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
					info = new ContentStoreInfoImpl(ContentStoreWithAnnotation.class, TestEntity.class, storeImpl);
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
		});
	}
	
	@ContentStoreRestResource
	public static interface ContentStoreWithDeprecatedAnnotation extends ContentStore<TestEntity, UUID> {
		//
	}

	@ContentStoreRestResource(path="some-path")
	public static interface ContentStoreWithPath extends ContentStore<TestEntity, UUID> {
		//
	}

	@StoreRestResource
	public static interface ContentStoreWithAnnotation extends ContentStore<TestEntity, UUID> {
		//
	}

	@StoreRestResource(path="some-other-path")
	public static interface ContentStoreWithAnotherPath extends ContentStore<TestEntity, UUID> {
		//
	}
}
