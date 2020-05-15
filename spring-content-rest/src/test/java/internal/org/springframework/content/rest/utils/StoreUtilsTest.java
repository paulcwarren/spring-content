package internal.org.springframework.content.rest.utils;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.commons.storeservice.StoreInfoImpl;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.support.TestEntity;
import org.junit.runner.RunWith;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
@RunWith(Ginkgo4jRunner.class)
public class StoreUtilsTest {

	private StoreInfo info;
	private String storePath;

	{
		Context("#storePath", () -> {
			JustBeforeEach(() -> {
				storePath = StoreUtils.storePath(info);
			});
			Context("given a content store with no annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(TestContentStore.class);
					info = new StoreInfoImpl(TestContentStore.class, TestEntity.class, storeImpl);
				});
				It("should return return 'testEntities'", () -> {
					assertThat(storePath, is("testEntities"));
				});
			});
			Context("given a content store with a deprecated ContentStoreRestResource annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
					info = new StoreInfoImpl(
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
					info = new StoreInfoImpl(ContentStoreWithPath.class,
							TestEntity.class, storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("some-path"));
				});
			});
			Context("given a content store with a StoreRestResource annotation", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
					info = new StoreInfoImpl(ContentStoreWithAnnotation.class,
							TestEntity.class, storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("testEntities"));
				});
			});
			Context("given a content store with a StoreRestResource annotation that specifies a path", () -> {
				BeforeEach(() -> {
					ContentStore storeImpl = mock(ContentStoreWithAnotherPath.class);
					info = new StoreInfoImpl(ContentStoreWithAnotherPath.class, TestEntity.class, storeImpl);
				});
				It("should return return the specified path", () -> {
					assertThat(storePath, is("some-other-path"));
				});
			});
			Context("given a Store with no annotations", () -> {
				BeforeEach(() -> {
					Store storeImpl = mock(TestStore.class);
					info = new StoreInfoImpl(TestStore.class, null, storeImpl);
				});
				It("should return 'tests'", () -> {
					assertThat(storePath, is("tests"));
				});
			});
			Context("given a Store with a StoreRestResource annotation", () -> {
				BeforeEach(() -> {
					Store storeImpl = mock(TestStoreWithAnnotation.class);
					info = new StoreInfoImpl(TestStoreWithAnnotation.class, null, storeImpl);
				});
				It("should return 'tests'", () -> {
					assertThat(storePath, is("testWithAnnotations"));
				});
			});
			Context("given a Store with a StoreRestResource annotation with a path of 'foo'", () -> {
				BeforeEach(() -> {
					Store storeImpl = mock(TestStoreWithPath.class);
					info = new StoreInfoImpl(TestStoreWithPath.class, null, storeImpl);
				});
				It("should return 'tests'", () -> {
					assertThat(storePath, is("foo"));
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
