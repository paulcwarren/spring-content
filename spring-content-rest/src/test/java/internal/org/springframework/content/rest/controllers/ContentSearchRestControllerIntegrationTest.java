package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.webmvc.ContentSearchRestController;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@RunWith(Ginkgo4jSpringRunner.class)
// because the controller bean is shared and we need to instruct the reflection service
// to behave differently in each test
@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {ContentSearchRestControllerIntegrationTest.TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class})
@Transactional
@ActiveProfiles("search")
public class ContentSearchRestControllerIntegrationTest {
	
	@Autowired TestEntityWithSharedIdsRepository repository;
	@Autowired TestEntityWithSeparateIdsRepository entityWithSeparateRepository;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;
	private RepresentationFactory representationFactory = new StandardRepresentationFactory();
	
	private TestEntityWithSharedId entity;
	private TestEntityWithSharedId entity2;
	private List<UUID> sharedIds;
	
	private TestEntityWithSeparateId entity3;
	private TestEntityWithSeparateId entity4;
	private List<UUID> contentIds;
	
	// mocks
	private static ReflectionService reflectionService;
	
	{
		Describe("ContentSearchRestController", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
//			JustBeforeEach(() -> {
//				try {
//					result = controller.searchContent(resourceInfo, entityAssembler, repository, searchMethod, keywords);
//				} catch (Exception e) {
//					resultException = e;
//				}
//			});
			Context("given an entity has no content associations", () -> {
				It("should throw an exception", () -> {
					MvcResult result = mvc.perform(get("/testEntityNoContents/searchContent/findKeyword?keyword=one")
						.accept("application/hal+json"))
						.andExpect(status().isNotFound()).andReturn();
					
					assertThat(result.getResolvedException().getMessage(), containsString("no content"));
				});
			});
			Context("given a store that is not Searchable", () -> {
				It("should throw a ResourceNotFoundException", () -> {
					MvcResult result = mvc.perform(get("/testEntityNotSearchables/searchContent/findKeyword?keyword=one")
							.accept("application/hal+json"))
							.andExpect(status().isNotFound()).andReturn();
					
					assertThat(result.getResolvedException().getMessage(), containsString("not searchable"));
				});
			});
			Context("given the search method is invalid", () -> {
				It("should return a ResourceNotFoundException", () ->{
					MvcResult result = mvc.perform(get("/testEntityWithSharedIds/searchContent/invalidSearchMethod?keyword=one")
							.accept("application/hal+json"))
							.andExpect(status().isNotFound()).andReturn();
					
					assertThat(result.getResolvedException().getMessage(), containsString("Invalid search: invalidSearchMethod"));
				});
			});
			Context("given no keywords are specified", () -> {
				It("should return a BadRequestException", () -> {
					mvc.perform(get("/testEntityWithSharedIds/searchContent/findKeyword")
							.accept("application/hal+json"))
							.andExpect(status().isBadRequest());
				});
			});
			Context("given an entity with a shared Id/ContentId field", () -> {
				BeforeEach(() ->{
					reflectionService = mock(ReflectionService.class);
					ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
					controller.setReflectionService(reflectionService);
				});
				Context("given no results are found", () ->{
					BeforeEach(() ->{
						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("one"))).thenReturn(Collections.EMPTY_LIST);
					});
					It("should return an empty response entity", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSharedIds/searchContent/findKeyword?keyword=one")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();

						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResources().size(), is(0));
					});
				});
				Context("given results are found", () ->{
					BeforeEach(() ->{
						entity = new TestEntityWithSharedId();
						repository.save((TestEntityWithSharedId)entity);
						
						entity2 = new TestEntityWithSharedId();
						repository.save((TestEntityWithSharedId)entity2);
						
						sharedIds = new ArrayList<>();
						sharedIds.add(entity.getId());
						sharedIds.add(entity2.getId());
						
						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("two"))).thenReturn(sharedIds);
					});
					It("should return a response entity with the entity", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSharedIds/searchContent/findKeyword?keyword=two")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();

						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResourcesByRel("testEntityWithSharedIds").size(), is(2));
						String id1 = halResponse.getResourcesByRel("testEntityWithSharedIds").get(0).getValue("contentId").toString();
						String id2 = halResponse.getResourcesByRel("testEntityWithSharedIds").get(1).getValue("contentId").toString();
						assertThat(sharedIds, hasItem(UUID.fromString(id1)));
						assertThat(sharedIds, hasItem(UUID.fromString(id2)));
						assertThat(id1, is(not(id2)));
					});
				});
				Context("given results contain invalid IDs", () -> {
					BeforeEach(() -> {
						entity2 = new TestEntityWithSharedId();
						repository.save((TestEntityWithSharedId)entity2);

						contentIds = new ArrayList<>();
						contentIds.add(UUID.randomUUID());			// invalid id
						contentIds.add(entity2.getContentId());

						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("else"))).thenReturn(contentIds);
					});
					It("should filter out invalid IDs", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSharedIds/searchContent/findKeyword?keyword=else")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();

						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResourcesByRel("testEntityWithSharedIds").size(), is(1));
						String id1 = halResponse.getResourcesByRel("testEntityWithSharedIds").get(0).getValue("contentId").toString();
						assertThat(contentIds, hasItem(UUID.fromString(id1)));
					});
				});
			});
			Context("given an entity with separate Id/ContentId fields", () -> {
				BeforeEach(() ->{
					reflectionService = mock(ReflectionService.class);
					ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
					controller.setReflectionService(reflectionService);
				});
				Context("given no results are found", () ->{
					BeforeEach(() ->{
						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("something"))).thenReturn(Collections.EMPTY_LIST);
					});
					It("should return an empty response entity", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSeparateIds/searchContent/findKeyword?keyword=something")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();
						
						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResources().size(), is(0));
					});
				});
				Context("given results are found", () ->{
					BeforeEach(() ->{
						entity3 = new TestEntityWithSeparateId();
						entityWithSeparateRepository.save(entity3);

						entity4 = new TestEntityWithSeparateId();
						entityWithSeparateRepository.save(entity4);

						contentIds = new ArrayList<>();
						contentIds.add(entity3.getContentId());
						contentIds.add(entity4.getContentId());
						
						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("else"))).thenReturn(contentIds);
					});
					It("should return a response entity with the entity", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSeparateIds/searchContent/findKeyword?keyword=else")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();

						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResourcesByRel("testEntityWithSeparateIds").size(), is(2));
						String id1 = halResponse.getResourcesByRel("testEntityWithSeparateIds").get(0).getValue("contentId").toString();
						String id2 = halResponse.getResourcesByRel("testEntityWithSeparateIds").get(1).getValue("contentId").toString();
						assertThat(contentIds, hasItem(UUID.fromString(id1)));
						assertThat(contentIds, hasItem(UUID.fromString(id2)));
						assertThat(id1, is(not(id2)));
					});
				});
				Context("given results contain invalid IDs", () -> {
					BeforeEach(() -> {
						entity3 = new TestEntityWithSeparateId();
						entityWithSeparateRepository.save(entity3);

						contentIds = new ArrayList<>();
						contentIds.add(UUID.randomUUID());			// invalid id
						contentIds.add(entity3.getContentId());

						when(reflectionService.invokeMethod(anyObject(), anyObject(), eq("else"))).thenReturn(contentIds);
					});
					It("should filter out invalid IDs", () -> {
						MvcResult result = mvc.perform(get("/testEntityWithSeparateIds/searchContent/findKeyword?keyword=else")
								.accept("application/hal+json"))
								.andExpect(status().isOk()).andReturn();

						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(result.getResponse().getContentAsString()));
						assertThat(halResponse.getResourcesByRel("testEntityWithSeparateIds").size(), is(1));
						String id1 = halResponse.getResourcesByRel("testEntityWithSeparateIds").get(0).getValue("contentId").toString();
						assertThat(contentIds, hasItem(UUID.fromString(id1)));
					});
				});
			});
//			Context("given an entity with a content property", () -> {});
		});
	}

	@Test
	public void noop() {}
	
	@Configuration
	@EnableJpaRepositories(basePackages="internal.org.springframework.content.rest.controllers", considerNestedRepositories=true)
	@EnableTransactionManagement
	@EnableFilesystemStores(basePackages="internal.org.springframework.content.rest.controllers")
	@Profile("search")
	public static class TestConfig extends JpaInfrastructureConfig {
		
		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
		}

		@Bean
		public File filesystemRoot() {
			File baseDir = new File(System.getProperty("java.io.tmpdir"));
			File filesystemRoot = new File(baseDir, "spring-content-search-controller-tests");
			filesystemRoot.mkdirs();
			return filesystemRoot;
		}
		
		protected String packagesToScan() {
			return "internal.org.springframework.content.rest.controllers";
		}
	}

	@MappedSuperclass
	public static class AbstractTestEntity {
		@Id
		@ContentId
		private UUID id = UUID.randomUUID();

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public UUID getContentId() {
			return id;
		}

		public void setContentId(UUID id) {
			this.id = id;
		}
	}

	@Entity(name="testentitynocontent")
	public static class TestEntityNoContent extends AbstractTestEntity {}
	public interface TestEntityNoContentRepository extends CrudRepository<TestEntityNoContent,UUID> {}
	
	@Entity(name="testentitynotsearchable")
	public static class TestEntityNotSearchable extends AbstractTestEntity {}
	public interface TestEntityNotSearchableRepository extends CrudRepository<TestEntityNotSearchable,UUID> {}
	public interface TestEntityNotSearchableStore extends ContentStore<TestEntityNotSearchable,UUID> {}
	
	@Entity(name="testentitysharedid")
	public static class TestEntityWithSharedId extends AbstractTestEntity {}
	public interface TestEntityWithSharedIdsRepository extends CrudRepository<TestEntityWithSharedId,UUID> {}
	public interface TestEntityWithSharedIdsSearchableStore extends ContentStore<TestEntityWithSharedId,UUID>, Searchable<UUID> {}
	
	@Entity(name="testentityseparateid")
	public static class TestEntityWithSeparateId {
		@Id
		private UUID id = UUID.randomUUID();
		@ContentId
		private UUID contentId = UUID.randomUUID();

		public UUID getId() {
			return id;
		}

		public void setId(UUID id) {
			this.id = id;
		}

		public UUID getContentId() {
			return contentId;
		}

		public void setContentId(UUID id) {
			this.contentId = id;
		}
	}
	public interface TestEntityWithSeparateIdsRepository extends CrudRepository<TestEntityWithSeparateId,UUID> {}
	public interface TestEntityWithSeparateIdsSearchableStore extends ContentStore<TestEntityWithSeparateId,UUID>, Searchable<UUID> {}
}
