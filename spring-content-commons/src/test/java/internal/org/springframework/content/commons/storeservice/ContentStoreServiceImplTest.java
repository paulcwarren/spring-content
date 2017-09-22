package internal.org.springframework.content.commons.storeservice;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.storeservice.StoreFilter;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentStoreServiceImplTest {

	private ContentStoreServiceImpl contentRepoService; 
	private StoreFactory mockFactory;
	{
		Describe("ContentStoreServiceImpl", () -> {
			BeforeEach(() -> {
				contentRepoService = new ContentStoreServiceImpl();
			});
			Context("given no factories", () -> {
				It("should always return empty", () -> {
					assertThat(contentRepoService.getContentStores(), is(new ContentStoreInfo[]{}));
				});
			});
			Context("given a ContentStore factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(ContentStore.class));
					when(mockFactory.getStoreInterface()).thenAnswer(new Answer<Object>() {
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
					ContentStoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(0));
				});
				It("should return no associativestore info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getStores(AssociativeStore.class);
					assertThat(infos.length, is(0));
				});
				It("should return content store info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getContentStores();
					assertThat(infos.length, is(1));
				});
			});
			Context("given a Store factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(Store.class));
					when(mockFactory.getStoreInterface()).thenAnswer(new Answer<Object>() {
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
					ContentStoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(1));
				});
				It("should return associativestore info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getStores(AssociativeStore.class);
					assertThat(infos.length, is(0));
				});
				It("should return no content store info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getContentStores();
					assertThat(infos.length, is(0));
				});
			});
			Context("given an AssociativeStore factory", () -> {
				BeforeEach(() -> {
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(AssociativeStore.class));
					when(mockFactory.getStoreInterface()).thenAnswer(new Answer<Object>() {
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
					ContentStoreInfo[] infos = contentRepoService.getContentStores();
					assertThat(infos.length, is(0));
				});
				It("should return store info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getStores(Store.class);
					assertThat(infos.length, is(1));
				});
				It("should return associativestore info", () -> {
					ContentStoreInfo[] infos = contentRepoService.getStores(AssociativeStore.class);
					assertThat(infos.length, is(1));
				});
			});
			Context("given multiple stores", () -> {
				BeforeEach(() -> {
					List<StoreFactory> factories = new ArrayList<>();
					mockFactory = mock(StoreFactory.class);
					when(mockFactory.getStore()).thenReturn(mock(AssociativeStore.class));
					when(mockFactory.getStoreInterface()).thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return EntityStoreInterface.class;
						}
					});
					factories.add(mockFactory);
					
					StoreFactory mockFactory2 = mock(StoreFactory.class);
					when(mockFactory2.getStore()).thenReturn(mock(AssociativeStore.class));
					when(mockFactory2.getStoreInterface()).thenAnswer(new Answer<Object>() {
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
					ContentStoreInfo[] infos = contentRepoService.getStores(AssociativeStore.class, ContentStoreService.MATCH_ALL);
					assertThat(infos.length, is(2));
				});
				It("should not return stores that dont match the filter", () -> {
					ContentStoreInfo[] infos = contentRepoService.getStores(AssociativeStore.class, new StoreFilter() {
						@Override
						public boolean matches(ContentStoreInfo info) {
							return false;
						}
					});
					assertThat(infos.length, is(0));
				});
			});
		});
	}
	
	@Test
	public void test() {
	}
	
	public interface StoreInterface extends Store<String> {
	}
	
	public interface AssociativeStoreInterface extends AssociativeStore<Object,String> {
	}
	
	public interface ContentRepositoryInterface extends ContentStore<Object, String> {
	}

	public static class Entity {};
	public static class OtherEntity {};
	
	public interface EntityStoreInterface extends AssociativeStore<Entity, String> {
	}

	public interface OtherEntityStoreInterface extends AssociativeStore<OtherEntity, String> {
	}
}
