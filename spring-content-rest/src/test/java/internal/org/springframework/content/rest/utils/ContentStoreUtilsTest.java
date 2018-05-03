package internal.org.springframework.content.rest.utils;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.commons.storeservice.ContentStoreInfoImpl;
import internal.org.springframework.content.rest.StoreRestResource;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.support.TestEntity;

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

	{
		Describe("ContentStoreUtilsTest", () -> {
			Context("#findContentStore (by entity class)", () -> {
				BeforeEach(() -> {
					stores = mock(ContentStoreService.class);
					entityClass = TestEntity.class;
				});
				JustBeforeEach(() -> {
					result = ContentStoreUtils.findContentStore(stores, entityClass);
				});
				Context("given a content store service with a store that has the deprecated ContentStoreRestResource annotation",
						() -> {
							BeforeEach(() -> {
								ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
								info = new ContentStoreInfoImpl(ContentStoreWithDeprecatedAnnotation.class,
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
						ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
						info = new ContentStoreInfoImpl(ContentStoreWithAnnotation.class, TestEntity.class, storeImpl);
						when(stores.getStores(eq(ContentStore.class))).thenReturn(new ContentStoreInfo[] { info });
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
					when(stores.getStores(eq(ContentStore.class))).thenReturn(new ContentStoreInfo[] { info });
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
					info = new ContentStoreInfoImpl(ContentStoreWithDeprecatedAnnotation.class, TestEntity.class,
							storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("testEntities"));
				});
			});
			Context("given a content store with a deprecated ContentStoreRestResource annotation that specifies a path",
					() -> {
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
		Context("#getContent", () -> {
			JustBeforeEach(() -> {
				headers = new HttpHeaders();
				ContentStoreUtils.getContent((ContentStore) store, entity, mimeTypes, headers);
			});
			BeforeEach(() -> {
				store = mock(StoreWithRenderable.class);
				entity = new TestEntity();
				entity.setMimeType("application/word");
				entity.setLen(100L);
				mimeTypes = Arrays.asList(new MediaType[] { MediaType.valueOf("application/word") });
			});
			It("should get the content from the store and set type and length headers", () -> {
				verify(store).getContent(anyObject());
				assertThat(headers.getContentType().toString(), is("application/word"));
				assertThat(headers.getContentLength(), is(100L));
			});
			Context("given an application/word entity and mime types text/html", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("application/word");
					mimeTypes = Arrays.asList(new MediaType[] { MediaType.valueOf("text/html") });
				});
				It("should not attempt to get content from store", () -> {
					verify(store, never()).getContent(anyObject());
				});
			});
			Context("given an audio/basic entity and mime types audio/*", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("audio/basic");
					mimeTypes = Arrays.asList(new MediaType[] { MediaType.valueOf("audio/*") });
				});
				It("should get the content from the store", () -> {
					verify(store).getContent(anyObject());
					verify(store, never()).getRendition(anyObject(), anyObject());
				});
			});
			Context("given an audio/basic entity and mime types */*", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("audio/basic");
					entity.setLen(1000L);
					mimeTypes = Arrays.asList(new MediaType[] { MediaType.valueOf("*/*") });
				});
				It("should get the content from the store", () -> {
					verify(store).getContent(anyObject());
					assertThat(headers.getContentType().toString(), is("audio/basic"));
					assertThat(headers.getContentLength(), is(1000L));

					verify(store, never()).getRendition(anyObject(), anyObject());
				});
			});
			Context("given an application/word entity, accept mime type text/html and a renderer", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("application/word");
					mimeTypes = Arrays.asList(new MediaType[] { MediaType.valueOf("text/html") });
				});
				It("should get the content from the renderer", () -> {
					verify(store, never()).getContent(anyObject());
					verify(store).getRendition(eq(entity), eq("text/html"));
				});
			});
			Context("given an application/word entity, accept mime types text/xml, text/html", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("application/word");
					mimeTypes = Arrays
							.asList(new MediaType[] { MediaType.valueOf("text/xml"), MediaType.valueOf("text/html") });

					when(store.getRendition(eq(entity), eq("text/html")))
							.thenReturn(new InputStreamResource(new ByteArrayInputStream(new byte[] {})));
				});
				It("should sort the mimetypes and get the content from the renderer", () -> {
					InOrder inOrder = inOrder(store);
					inOrder.verify(store, times(1)).getRendition(eq(entity), eq("text/xml"));
					inOrder.verify(store, times(1)).getRendition(eq(entity), eq("text/html"));
					inOrder.verifyNoMoreInteractions();
				});
			});
			Context("given an application/word entity, accept mime types text/xml, text/*", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("application/word");
					mimeTypes = Arrays
							.asList(new MediaType[] { MediaType.valueOf("text/*"), MediaType.valueOf("text/xml") });

					when(store.getRendition(eq(entity), eq("text/*")))
							.thenReturn(new InputStreamResource(new ByteArrayInputStream(new byte[] {})));
				});
				It("should sort the mimetypes and get the content from text/*", () -> {
					InOrder inOrder = inOrder(store);
					inOrder.verify(store, times(1)).getRendition(eq(entity), eq("text/xml"));
					inOrder.verify(store, times(1)).getRendition(eq(entity), eq("text/*"));
					inOrder.verifyNoMoreInteractions();
				});
			});
			Context("given an application/word entity, accept mime types text/xml, */*", () -> {
				BeforeEach(() -> {
					store = mock(StoreWithRenderable.class);
					entity = new TestEntity();
					entity.setMimeType("application/word");
					entity.setLen(100L);
					mimeTypes = Arrays
							.asList(new MediaType[] { MediaType.valueOf("*/*"), MediaType.valueOf("text/xml") });

					when(store.getRendition(eq(entity), eq("text/html")))
							.thenReturn(new InputStreamResource(new ByteArrayInputStream(new byte[] {})));
				});
				It("should sort the mimetypes and get the original content", () -> {
					InOrder inOrder = inOrder(store);
					inOrder.verify(store, times(1)).getRendition(eq(entity), eq("text/xml"));
					inOrder.verify(store, times(1)).getContent(eq(entity));
					inOrder.verifyNoMoreInteractions();

					assertThat(headers.getContentType().toString(), is("application/word"));
					assertThat(headers.getContentLength(), is(100L));
				});
			});
		});
	}

	@ContentStoreRestResource
	public static interface ContentStoreWithDeprecatedAnnotation extends ContentStore<TestEntity, UUID> {
		//
	}

	@ContentStoreRestResource(path = "some-path")
	public static interface ContentStoreWithPath extends ContentStore<TestEntity, UUID> {
		//
	}

	@StoreRestResource
	public static interface ContentStoreWithAnnotation extends ContentStore<TestEntity, UUID> {
		//
	}

	@StoreRestResource(path = "some-other-path")
	public static interface ContentStoreWithAnotherPath extends ContentStore<TestEntity, UUID> {
		//
	}

	public static interface StoreWithRenderable extends ContentStore<TestEntity, UUID>, Renderable<TestEntity> {
		//
	}
}
