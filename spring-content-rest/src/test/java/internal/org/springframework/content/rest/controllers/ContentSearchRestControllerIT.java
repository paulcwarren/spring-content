package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
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
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.commons.utils.ReflectionService;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.FulltextEntityLookupQuery;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.extensions.contentsearch.ContentSearchRestController;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import internal.org.springframework.data.rest.extensions.contentsearch.DefaultEntityLookupStrategy;
import internal.org.springframework.data.rest.extensions.contentsearch.QueryMethodsEntityLookupStrategy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
// because the controller bean is shared and we need to instruct the reflection service
// to behave differently in each tests
@Ginkgo4jConfiguration(threads = 1)
@WebAppConfiguration
@ContextConfiguration(classes = {
        ContentSearchRestControllerIT.TestConfig.class,
        DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class,
        RestConfiguration.class })
@Transactional
@ActiveProfiles("search")
public class ContentSearchRestControllerIT {

    @Autowired
    TestEntityWithSharedIdsRepository repository;

    @Autowired
    TestEntityWithSeparateIdsRepository entityWithSeparateRepository;

    @Autowired
    RepositoryWithNoLookupStrategy repoWithNoLookupStrategy;

    @Autowired
    TestEntityWithSeparateIdsSearchableStore entityWithSeparateStore;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private RepresentationFactory representationFactory = new StandardRepresentationFactory();

    private TestEntityWithSharedId entity;
    private TestEntityWithSharedId entity2;
    private List<UUID> sharedIds;

    private TestEntityWithSeparateId entity3;
    private TestEntityWithSeparateId entity4;
    private List<UUID> contentIds;

    private TestEntity2 entity5;
    private TestEntity2 entity6;

    // mocks/spys
    private static ReflectionService reflectionService;
    private static DefaultEntityLookupStrategy defaultLookupStrategy;
    private static QueryMethodsEntityLookupStrategy queryMethodsLookupStrategy;

    {
        Describe("ContentSearchRestController", () -> {

            BeforeEach(() -> {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();

                reflectionService = mock(ReflectionService.class);
                ContentSearchRestController controller = context.getBean(ContentSearchRestController.class);
                controller.setReflectionService(reflectionService);

                defaultLookupStrategy = spy(new DefaultEntityLookupStrategy());
                controller.setDefaultEntityLookupStrategy(defaultLookupStrategy);
                queryMethodsLookupStrategy = spy(new QueryMethodsEntityLookupStrategy());
                controller.setQueryMethodsEntityLookupStrategy(queryMethodsLookupStrategy);
            });

            Describe("#search endpoint", () -> {

                Context("given an entity has no content associations", () -> {

                    It("should throw an exception", () -> {
                        MvcResult result = mvc.perform(get(
                                "/testEntityNoContents/searchContent?queryString=one")
                                .accept("application/hal+json"))
                                .andExpect(status().isNotFound()).andReturn();

                        assertThat(result.getResolvedException().getMessage(), containsString("no content"));
                    });
                });

                Context("given a store that is not Searchable", () -> {

                    It("should throw a ResourceNotFoundException", () -> {
                        MvcResult result = mvc.perform(get(
                                "/testEntityNotSearchables/searchContent?queryString=one")
                                .accept("application/hal+json"))
                                .andExpect(status().isNotFound()).andReturn();

                        assertThat(result.getResolvedException().getMessage(), containsString("not searchable"));
                    });
                });

                Context("given the search method is invalid", () -> {

                    It("should return a ResourceNotFoundException", () -> {
                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSharedIds/searchContent/invalidSearchMethod?keyword=one")
                                .accept("application/hal+json"))
                                .andExpect(status().isNotFound()).andReturn();
                    });
                });

                Context("given no keywords are specified", () -> {

                    It("should return a BadRequestException", () -> {
                        mvc.perform(get("/testEntityWithSharedIds/searchContent")
                                .accept("application/hal+json"))
                        .andExpect(status().isBadRequest());
                    });
                });

                Context("given paged results are requested", () -> {

                    It("should invoke search with the page request", () -> {

                        MvcResult result = mvc.perform(get(
                                "/testEntityWithSeparateIds/searchContent?queryString=else&page=1&size=1")
                                .accept("application/hal+json"))
                                .andExpect(status().isOk()).andReturn();

                        Method m = ReflectionUtils.findMethod(Searchable.class,"search", new Class<?>[] { String.class, Pageable.class });
                        PageRequest pageable = PageRequest.of(1, 1);

                        verify(reflectionService).invokeMethod(eq(m), any(), eq("else"), eq(pageable), eq(UUID.class));
                    });
                });

                Context("given an entity with a shared Id/ContentId field", () -> {

                    Context("given no results are found", () -> {

                        BeforeEach(() -> {
                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("one"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(Collections.EMPTY_LIST);
                        });

                        It("should return an empty response entity", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSharedIds/searchContent?queryString=one")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse.getResources().size(), is(0));
                        });
                    });

                    Context("given results are found", () -> {

                        BeforeEach(() -> {
                            entity = new TestEntityWithSharedId();
                            repository.save(entity);

                            entity2 = new TestEntityWithSharedId();
                            repository.save(entity2);

                            sharedIds = new ArrayList<>();
                            sharedIds.add(entity.getId());
                            sharedIds.add(entity2.getId());

                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("two"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(sharedIds);
                        });

                        It("should return a response entity with the entity", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSharedIds/searchContent?queryString=two")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse
                                    .getResourcesByRel("testEntityWithSharedIds").size(),
                                    is(2));
                            String id1 = halResponse
                                    .getResourcesByRel("testEntityWithSharedIds").get(0)
                                    .getValue("contentId").toString();
                            String id2 = halResponse
                                    .getResourcesByRel("testEntityWithSharedIds").get(1)
                                    .getValue("contentId").toString();
                            assertThat(sharedIds, hasItem(UUID.fromString(id1)));
                            assertThat(sharedIds, hasItem(UUID.fromString(id2)));
                            assertThat(id1, is(not(id2)));
                        });


                    });

                    Context("given results contain orphaned fulltext documents", () -> {

                        BeforeEach(() -> {
                            entity2 = new TestEntityWithSharedId();
                            repository.save(entity2);

                            contentIds = new ArrayList<>();
                            contentIds.add(UUID.randomUUID()); // invalid id
                            contentIds.add(entity2.getContentId());

                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("else"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(contentIds);
                        });

                        It("should filter out invalid IDs", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSharedIds/searchContent?queryString=else")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse
                                    .getResourcesByRel("testEntityWithSharedIds").size(),
                                    is(1));
                            String id1 = halResponse
                                    .getResourcesByRel("testEntityWithSharedIds").get(0)
                                    .getValue("contentId").toString();
                            assertThat(contentIds, hasItem(UUID.fromString(id1)));
                        });
                    });
                });

                Context("given an entity with separate Id/ContentId fields", () -> {

                    Context("given no results are found", () -> {

                        BeforeEach(() -> {
                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("something"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(Collections.EMPTY_LIST);
                        });

                        It("should return an empty response entity", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSeparateIds/searchContent?queryString=something")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse.getResources().size(), is(0));
                        });
                    });

                    Context("given results are found", () -> {

                        BeforeEach(() -> {
                            entity3 = new TestEntityWithSeparateId();
                            entityWithSeparateRepository.save(entity3);

                            entity4 = new TestEntityWithSeparateId();
                            entityWithSeparateRepository.save(entity4);

                            contentIds = new ArrayList<>();
                            contentIds.add(entity3.getContentId());
                            contentIds.add(entity4.getContentId());

                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("else"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(contentIds);
                        });

                        It("should return a response entity with the entity", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSeparateIds/searchContent?queryString=else")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            verify(defaultLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));
                            verify(queryMethodsLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse
                                    .getResourcesByRel("testEntityWithSeparateIds").size(),
                                    is(2));
                            String id1 = halResponse
                                    .getResourcesByRel("testEntityWithSeparateIds").get(0)
                                    .getValue("contentId").toString();
                            String id2 = halResponse
                                    .getResourcesByRel("testEntityWithSeparateIds").get(1)
                                    .getValue("contentId").toString();
                            assertThat(contentIds, hasItem(UUID.fromString(id1)));
                            assertThat(contentIds, hasItem(UUID.fromString(id2)));
                            assertThat(id1, is(not(id2)));
                        });
                    });

                    Context("given results contain orphaned fulltext documents", () -> {

                        BeforeEach(() -> {
                            entity3 = new TestEntityWithSeparateId();
                            entityWithSeparateRepository.save(entity3);

                            contentIds = new ArrayList<>();
                            contentIds.add(UUID.randomUUID()); // invalid id
                            contentIds.add(entity3.getContentId());

                            when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                    eq("else"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(contentIds);
                        });

                        It("should filter out invalid IDs", () -> {
                            MvcResult result = mvc.perform(get(
                                    "/testEntityWithSeparateIds/searchContent?queryString=else")
                                    .accept("application/hal+json"))
                                    .andExpect(status().isOk()).andReturn();

                            ReadableRepresentation halResponse = representationFactory
                                    .readRepresentation("application/hal+json",
                                            new StringReader(result.getResponse()
                                                    .getContentAsString()));
                            assertThat(halResponse
                                    .getResourcesByRel("testEntityWithSeparateIds").size(),
                                    is(1));
                            String id1 = halResponse
                                    .getResourcesByRel("testEntityWithSeparateIds").get(0)
                                    .getValue("contentId").toString();
                            assertThat(contentIds, hasItem(UUID.fromString(id1)));
                        });
                    });
                });

                Context("given results are found returning a custom result type", () -> {

                    BeforeEach(() -> {
                        List<CustomResult> results = new ArrayList<>();

                        results.add(new CustomResult("12345", "<em>something else</em>", "foo1", "bar1"));
                        results.add(new CustomResult("67890", "<em>else altogether</em>", "foo2", "bar2"));

                        when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                eq("else"), argThat(instanceOf(Pageable.class)), eq(CustomResult.class))).thenReturn(results);
                    });

                    It("should return a response entity with the entity", () -> {
                        MvcResult result = mvc.perform(get(
                                "/repoWithCustomSearchReturnType/searchContent?queryString=else")
                                .accept("application/hal+json"))
                                .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                                .readRepresentation("application/hal+json",
                                        new StringReader(result.getResponse()
                                                .getContentAsString()));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").size(),
                                is(2));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("highlight").toString(), is("<em>something else</em>"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("foo").toString(), is("foo1"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("bar").toString(), is("bar1"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(1)
                                .getValue("highlight").toString(), is("<em>else altogether</em>"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(1)
                                .getValue("foo").toString(), is("foo2"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(1)
                                .getValue("bar").toString(), is("bar2"));
                    });
                });

                Context("given paged results are found returning a custom result type", () -> {

                    BeforeEach(() -> {
                        List<CustomResult> results = new ArrayList<>();

                        results.add(new CustomResult("12345", "<em>something else</em>", "foo1", "bar1"));

                        when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                eq("else"), argThat(instanceOf(Pageable.class)), eq(CustomResult.class))).thenReturn(results);
                    });

                    It("should return a response entity with the entity", () -> {
                        MvcResult result = mvc.perform(get(
                                "/repoWithCustomSearchReturnType/searchContent?queryString=else&page=1&size=1")
                                .accept("application/hal+json"))
                                .andExpect(status().isOk()).andReturn();

                        ReadableRepresentation halResponse = representationFactory
                                .readRepresentation("application/hal+json",
                                        new StringReader(result.getResponse()
                                                .getContentAsString()));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").size(),
                                is(1));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("highlight").toString(), is("<em>something else</em>"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("foo").toString(), is("foo1"));
                        assertThat(halResponse
                                .getResourcesByRel("customResults").get(0)
                                .getValue("bar").toString(), is("bar1"));
                    });
                });

                Context("given a repository with no lookup strategy", () -> {

                    BeforeEach(() -> {
                        entity5 = new TestEntity2();
                        repoWithNoLookupStrategy.save(entity5);

                        entity6 = new TestEntity2();
                        repoWithNoLookupStrategy.save(entity6);

                        contentIds = new ArrayList<>();
                        contentIds.add(entity5.getContentId());
                        contentIds.add(entity6.getContentId());

                        when(reflectionService.invokeMethod(anyObject(), anyObject(),
                                eq("else"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(contentIds);
                    });

                    It("should return a response entity with the entity", () -> {
                        MvcResult result = mvc.perform(get(
                                "/repoWithNoLookupStrategy/searchContent?queryString=else")
                                .accept("application/hal+json"))
                                .andExpect(status().isOk()).andReturn();

                        verify(defaultLookupStrategy).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));
                        verify(queryMethodsLookupStrategy, never()).lookup(any(RootResourceInformation.class), any(RepositoryInformation.class), any(List.class), any(List.class));

                        ReadableRepresentation halResponse = representationFactory
                                .readRepresentation("application/hal+json",
                                        new StringReader(result.getResponse()
                                                .getContentAsString()));
                        assertThat(halResponse
                                .getResourcesByRel("testEntity2s").size(),
                                is(2));
                        String id1 = halResponse
                                .getResourcesByRel("testEntity2s").get(0)
                                .getValue("contentId").toString();
                        String id2 = halResponse
                                .getResourcesByRel("testEntity2s").get(1)
                                .getValue("contentId").toString();
                        assertThat(contentIds, hasItem(UUID.fromString(id1)));
                        assertThat(contentIds, hasItem(UUID.fromString(id2)));
                        assertThat(id1, is(not(id2)));
                    });
                });
            });

            Describe("#findKeyword endpoint", () -> {

                BeforeEach(() -> {
                    entity3 = new TestEntityWithSeparateId();
                    entityWithSeparateRepository.save(entity3);

                    entity4 = new TestEntityWithSeparateId();
                    entityWithSeparateRepository.save(entity4);

                    contentIds = new ArrayList<>();
                    contentIds.add(entity3.getContentId());
                    contentIds.add(entity4.getContentId());

                    when(reflectionService.invokeMethod(anyObject(), anyObject(),
                            eq("else"), argThat(instanceOf(Pageable.class)), eq(UUID.class))).thenReturn(contentIds);
                });

                It("should return a response entity with the entity", () -> {

                    MvcResult result = mvc.perform(get(
                            "/testEntityWithSeparateIds/searchContent/findKeyword?keyword=else")
                            .accept("application/hal+json"))
                            .andExpect(status().isOk()).andReturn();

                    ReadableRepresentation halResponse = representationFactory
                            .readRepresentation("application/hal+json",
                                    new StringReader(result.getResponse()
                                            .getContentAsString()));

                    assertThat(halResponse.getResourcesByRel("testEntityWithSeparateIds").size(), is(2));

                    String id1 = halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").get(0)
                            .getValue("contentId").toString();

                    String id2 = halResponse
                            .getResourcesByRel("testEntityWithSeparateIds").get(1)
                            .getValue("contentId").toString();

                    assertThat(contentIds, hasItem(UUID.fromString(id1)));
                    assertThat(contentIds, hasItem(UUID.fromString(id2)));
                    assertThat(id1, is(not(id2)));
                });
            });
        });
    }

    @Test
    public void noop() {
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableTransactionManagement
    @EnableFilesystemStores
    @Profile("search")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir,
                    "spring-content-search-controller-tests");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }

        @Override
        protected String[] packagesToScan() {
            return new String[]{
                    "internal.org.springframework.content.rest.controllers",
                    "internal.org.springframework.content.rest.support"
            };
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

    @Entity(name = "testentitynocontent")
    public static class TestEntityNoContent extends AbstractTestEntity {
    }

    public interface TestEntityNoContentRepository
    extends CrudRepository<TestEntityNoContent, UUID> {
    }

    @Entity(name = "testentitynotsearchable")
    public static class TestEntityNotSearchable extends AbstractTestEntity {
    }

    public interface TestEntityNotSearchableRepository
    extends CrudRepository<TestEntityNotSearchable, UUID> {
    }

    public interface TestEntityNotSearchableStore
    extends FilesystemContentStore<TestEntityNotSearchable, UUID> {
    }

    @Entity(name = "testentitysharedid")
    public static class TestEntityWithSharedId extends AbstractTestEntity {
    }

    public interface TestEntityWithSharedIdsRepository
    extends CrudRepository<TestEntityWithSharedId, UUID> {
    }

    public interface TestEntityWithSharedIdsSearchableStore
    extends FilesystemContentStore<TestEntityWithSharedId, UUID>, Searchable<UUID> {
    }

    // stub out a Searchable implementation so that the content store can be instantiated
    // this wont actually get called because the test intercepts calls to the Searchable by mocking the reflection
    // service
    public static class SearchableImpl implements Searchable<UUID> {

        @Override
        public Iterable<UUID> search(String queryString) {
            return null;
        }

        @Override
        public List<UUID> search(String queryString, Pageable pageable) {
            return null;
        }

        @Override
        public List<UUID> search(String queryString, Pageable pageable, Class<? extends UUID> searchType) {
            return null;
        }

        @Override
        public Iterable<UUID> findKeyword(String query) {
            return null;
        }

        @Override
        public Iterable<UUID> findAllKeywords(String... terms) {
            return null;
        }

        @Override
        public Iterable<UUID> findAnyKeywords(String... terms) {
            return null;
        }

        @Override
        public Iterable<UUID> findKeywordsNear(int proximity, String... terms) {
            return null;
        }

        @Override
        public Iterable<UUID> findKeywordStartsWith(String term) {
            return null;
        }

        @Override
        public Iterable<UUID> findKeywordStartsWithAndEndsWith(String a, String b) {
            return null;
        }

        @Override
        public Iterable<UUID> findAllKeywordsWithWeights(String[] terms, double[] weights) {
            return null;
        }
    }

    @Entity(name = "testentityseparateid")
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

    public interface TestEntityWithSeparateIdsRepository
        extends CrudRepository<TestEntityWithSeparateId, UUID> {

        @Query("select e from internal.org.springframework.content.rest.controllers.ContentSearchRestControllerIT$TestEntityWithSeparateId e")
        List<TestEntityWithSeparateId> randomQueryMethod();

        @FulltextEntityLookupQuery
        List<TestEntityWithSeparateId> findAllByContentIdIn(@Param("contentIds") List<UUID> contentIds);
    }

    public interface TestEntityWithSeparateIdsSearchableStore
        extends FilesystemContentStore<TestEntityWithSeparateId, UUID>, Searchable<UUID> {
    }

    @Entity
    @Getter
    @Setter
    public static class TestEntity2 {
        @Id
        private UUID id = UUID.randomUUID();
        @ContentId
        private UUID contentId = UUID.randomUUID();
    }

    @RepositoryRestResource(path="repoWithNoLookupStrategy")
    public interface RepositoryWithNoLookupStrategy
        extends CrudRepository<TestEntity2, UUID> {
    }

    public interface TestEntity2SearchableStore
        extends FilesystemContentStore<TestEntity2, UUID>, Searchable<UUID> {
    }

    @Entity
    @Getter
    @Setter
    public static class TestEntity3 {
        @Id
        private UUID id = UUID.randomUUID();
        @ContentId
        private UUID contentId = UUID.randomUUID();
    }

    @RepositoryRestResource(path="repoWithCustomSearchReturnType")
    public interface RepositoryWithCustomSearchReturnType
        extends CrudRepository<TestEntity3, UUID> {
    }

    public interface TestEntity3SearchableStore
        extends FilesystemContentStore<TestEntity3, UUID>, Searchable<CustomResult> {
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public class CustomResult {

        @ContentId
        private String contentId;

        @Highlight
        private String highlight;

        @Attribute(name="foo")
        private String foo;

        @Attribute(name="bar")
        private String bar;
    }
}
