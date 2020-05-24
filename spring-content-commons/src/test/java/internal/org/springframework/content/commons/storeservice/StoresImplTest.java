package internal.org.springframework.content.commons.storeservice;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;

import java.util.ArrayList;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class StoresImplTest {

	private StoresImpl contentRepoService;
	private StoreFactory mockFactory;
	{
		Describe("StoresImpl", () -> {
			BeforeEach(() -> {
				contentRepoService = new StoresImpl();
			});
			Context("given no factories", () -> {
				It("should always return empty", () -> {
					assertThat(contentRepoService.getStores(Store.class),
							is(new StoreInfo[] {}));
				});
			});
			Context("given a ContentStore factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(ContentStore.class));
					when(mockFactory.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return ContentRepositoryInterface.class;
								}
							});
					List<StoreFactory> factories = new ArrayList<>();
					factories.add(mockFactory);
					contentRepoService.setFactories(factories);
				});
				It("should return no store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(1));
				});
				It("should return no associativestore info", () -> {
					StoreInfo[] infos = contentRepoService
							.getStores(AssociativeStore.class);
					assertThat(infos.length, is(1));
				});
				It("should return content store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
					assertThat(infos.length, is(1));
				});
			});
			Context("given a Store factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(Store.class));
					when(mockFactory.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return StoreInterface.class;
								}
							});
					List<StoreFactory> factories = new ArrayList<>();
					factories.add(mockFactory);
					contentRepoService.setFactories(factories);
				});
				It("should return store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(1));
				});
				It("should return associativestore info", () -> {
					StoreInfo[] infos = contentRepoService
							.getStores(AssociativeStore.class);
					assertThat(infos.length, is(0));
				});
				It("should return no content store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
					assertThat(infos.length, is(0));
				});
			});
			Context("given an AssociativeStore factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(AssociativeStore.class));
					when(mockFactory.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return AssociativeStoreInterface.class;
								}
							});
					List<StoreFactory> factories = new ArrayList<>();
					factories.add(mockFactory);
					contentRepoService.setFactories(factories);
				});
				It("should return no content store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
					assertThat(infos.length, is(0));
				});
				It("should return store info", () -> {
					StoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(1));
				});
				It("should return associativestore info", () -> {
					StoreInfo[] infos = contentRepoService
							.getStores(AssociativeStore.class);
					assertThat(infos.length, is(1));
				});
			});
			Context("given multiple stores", () -> {
				BeforeEach(() -> {
					List<StoreFactory> factories = new ArrayList<>();
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(AssociativeStore.class));
					when(mockFactory.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return EntityStoreInterface.class;
								}
							});
					factories.add(mockFactory);

					StoreFactory mockFactory2 = mock(StoreFactory.class);
					when(mockFactory2.getStore())
							.thenReturn(mock(AssociativeStore.class));
					when(mockFactory2.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return OtherEntityStoreInterface.class;
								}
							});
					factories.add(mockFactory2);

					contentRepoService.setFactories(factories);
				});
				It("should return stores that match the filter", () -> {
					StoreInfo[] infos = contentRepoService.getStores(
							AssociativeStore.class, Stores.MATCH_ALL);
					assertThat(infos.length, is(2));
				});
				It("should not return stores that dont match the filter", () -> {
					StoreInfo[] infos = contentRepoService
							.getStores(AssociativeStore.class, new StoreFilter() {
								@Override
								public String name() {
									return "test";
								}

								@Override
								public boolean matches(StoreInfo info) {
									return false;
								}
							});
					assertThat(infos.length, is(0));
				});
			});
			Context("given multiple stores for the same Entity", () -> {
				BeforeEach(() -> {
					List<StoreFactory> factories = new ArrayList<>();
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(ContentStore.class));
					when(mockFactory.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return FsEntityStoreInterface.class;
								}
							});
					factories.add(mockFactory);

					StoreFactory mockFactory2 = mock(StoreFactory.class);
					when(mockFactory2.getStore())
							.thenReturn(mock(ContentStore.class));
					when(mockFactory2.getStoreInterface())
							.thenAnswer(new Answer<Object>() {
								@Override
								public Object answer(InvocationOnMock invocation)
										throws Throwable {
									return JpaEntityStoreInterface.class;
								}
							});
					factories.add(mockFactory2);

					contentRepoService.setFactories(factories);
				});

				It("should return stores that match the filter", () -> {
					StoreInfo[] infos = contentRepoService.getStores(ContentStore.class, Stores.MATCH_ALL);
					assertThat(infos.length, is(2));
				});
			});
		});
	}

	@Test
	public void test() {
	}

	public interface StoreInterface extends Store<String> {
	}

	public interface AssociativeStoreInterface extends AssociativeStore<Object, String> {
	}

	public interface ContentRepositoryInterface extends ContentStore<Object, String> {
	}

	public static class Entity {
	};

	public static class OtherEntity {
	};

	public interface EntityStoreInterface extends AssociativeStore<Entity, String> {
	}

	public interface OtherEntityStoreInterface
			extends AssociativeStore<OtherEntity, String> {
	}

	public interface FsEntityStoreInterface extends ContentStore<Entity, String> {
	}

	public interface JpaEntityStoreInterface extends ContentStore<Entity, String> {
	}
}
