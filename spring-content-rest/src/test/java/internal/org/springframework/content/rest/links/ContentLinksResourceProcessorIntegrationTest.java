package internal.org.springframework.content.rest.links;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.rest.controllers.ContentPropertyCollectionRestController;
import internal.org.springframework.content.rest.controllers.ContentPropertyRestController;
import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import internal.org.springframework.content.rest.support.TestEntityChildContentRepository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class, HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
public class ContentLinksResourceProcessorIntegrationTest {
	
	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;
	
	@Autowired TestEntityRepository repository;
	@Autowired TestEntityContentRepository contentRepository;
	@Autowired TestEntity2Repository repository2;
	@Autowired TestEntityChildContentRepository contentRepository2;
	@Autowired TestEntity3Repository repository3;
	@Autowired TestEntity3ContentRepository contentRepository3;
	
	@Autowired ContentPropertyCollectionRestController collectionCtrlr;
	@Autowired ContentPropertyRestController propCtrlr;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity2 testEntity2;
	private TestEntity3 testEntity3;

	{
		Describe("ContentLinksResourceProcessor", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
			
			Context("given an Entity and a Store with a default store path", () -> {
				BeforeEach(() -> {
					testEntity3 = repository3.save(new TestEntity3());
				});
				Context("given content is associated", () -> {
					BeforeEach(() -> {
						contentRepository3.setContent(testEntity3, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						repository3.save(testEntity3);
					});
					Context("a GET to /repository/id", () -> {
						It("should provide a response with a content link", () -> {
							MockHttpServletResponse response = mvc.perform(get("/testEntity3s/" + testEntity3.id)
									.accept("application/hal+json"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse, is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("testEntity3s"), is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("testEntity3s").size(), is(1));
							assertThat(halResponse.getLinksByRel("testEntity3s").get(0).getHref(), is("http://localhost/testEntity3s/"+testEntity3.contentId));
						});
					});
				});
				Context("a GET to /repository/id", () -> {
					It("should provide a response without a content link", () -> {
						MockHttpServletResponse response = mvc.perform(get("/testEntity3s/" + testEntity3.id)
								.accept("application/hal+json"))
								.andExpect(status().isOk())
								.andReturn().getResponse();
						assertThat(response, is(not(nullValue())));

						RepresentationFactory representationFactory = new StandardRepresentationFactory();
						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
						assertThat(halResponse, is(not(nullValue())));
						assertThat(halResponse.getLinksByRel("testEntity3s"), is(not(nullValue())));
						assertThat(halResponse.getLinksByRel("testEntity3s").size(), is(0));
					});
				});
			});

			Context("given an Entity with associated content and a Store specifying a store path", () -> {
				BeforeEach(() -> {
					testEntity = repository.save(new TestEntity());
				});
				Context("given content is associated", () -> {
					BeforeEach(() -> {
						contentRepository.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						repository.save(testEntity);
					});
					Context("a GET to /repository/id", () -> {
						It("should provide a response with a content link", () -> {
							MockHttpServletResponse response = mvc.perform(get("/testEntities/" + testEntity.id)
									.accept("application/hal+json"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse, is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("testEntitiesContent"), is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("testEntitiesContent").size(), is(1));
							assertThat(halResponse.getLinksByRel("testEntitiesContent").get(0).getHref(), is("http://localhost/testEntitiesContent/"+testEntity.contentId));
						});
					});
				});
			});

			Context("given an Entity with content properties", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
					repository2.save(testEntity2);
				});
				Context("given a single content property with content", () -> {
					BeforeEach(() -> {
						TestEntityChild child = new TestEntityChild();
						child.mimeType = "text/plain";
						contentRepository2.setContent(child, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						
						testEntity2.child = child;
						repository2.save(testEntity2);
					});
					Context("a GET to /repository/id", () -> {
						It("should return the content link", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id)
									.accept("application/hal+json"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse, is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("child"), is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("child").size(), is(1));
							assertThat(halResponse.getLinksByRel("child").get(0).getHref(), is("http://localhost/files/"+testEntity2.id+"/child/"+testEntity2.child.contentId));
						});
					});
				});
				
				Context("given a collection content property with several contents", () -> {
					BeforeEach(() -> {
						TestEntityChild child1 = new TestEntityChild();
						child1.mimeType = "text/plain";
						contentRepository2.setContent(child1, new ByteArrayInputStream("Content 1".getBytes()));

						TestEntityChild child2 = new TestEntityChild();
						child2.mimeType = "text/plain";
						contentRepository2.setContent(child2, new ByteArrayInputStream("Content 2".getBytes()));
						
						List<TestEntityChild> children = new ArrayList<>();
						children.add(child1);
						children.add(child2);
						
						testEntity2.children = children;
						repository2.save(testEntity2);
					});
					Context("a GET to /repository/id", () -> {
						It("should return the content collection link", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id)
									.accept("application/hal+json"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse, is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("children"), is(not(nullValue())));
							assertThat(halResponse.getLinksByRel("children").size(), is(2));
						});
					});
				});
			});
		});
	}

	@Test
	public void noop() {}
	
	protected RootResourceInformation getResourceInformation(Class<?> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainType);

		return new RootResourceInformation(mappings.getMetadataFor(domainType), entity,
				invokerFactory.getInvokerFor(domainType));
	}
}
