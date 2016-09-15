package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
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

import internal.org.springframework.content.rest.TestConfig;
import internal.org.springframework.content.rest.TestEntity2;
import internal.org.springframework.content.rest.TestEntity2Repository;
import internal.org.springframework.content.rest.TestEntityChild;
import internal.org.springframework.content.rest.TestEntityChildContentRepository;
import internal.org.springframework.content.rest.TestEntityContentRepository;
import internal.org.springframework.content.rest.TestEntityRepository;
import internal.org.springframework.content.rest.config.ContentRestConfiguration;
import internal.org.springframework.content.rest.controllers.ContentPropertyCollectionRestController;
import internal.org.springframework.content.rest.controllers.ContentPropertyRestController;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, ContentRestConfiguration.class})
//@WebIntegrationTest
@Transactional
public class ContentPropertyCollectionRestConrtollerIntegrationTest {
	
	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;
	
	@Autowired TestEntityRepository repository;
	@Autowired TestEntityContentRepository contentRepository;
	@Autowired TestEntity2Repository repository2;
	@Autowired TestEntityChildContentRepository contentRepository2;
	
	@Autowired ContentPropertyCollectionRestController collectionCtrlr;
	@Autowired ContentPropertyRestController propCtrlr;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity2 testEntity2;
	private TestEntityChild child1;
	private TestEntityChild child2;

	{
		Describe("ContentPropertyCollectionRestController", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
			Context("given an Entity with a content collection property", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
				});
				Context("a GET to /{repository}/{id}/{contentCollectionProperty}", () -> {
					It("should return an empty collection", () -> {
						MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/children")
								.accept("application/hal+json"))
								.andExpect(status().isOk())
								.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse.getResources().size(), is(0));
					});
				});
				Context("a PUT to /{repository}/{id}/{contentCollectionProperty}", () -> {
					It("should return 404", () -> {
						mvc.perform(put("/files/" + testEntity2.id.toString() + "/children"))
						.andExpect(status().is4xxClientError());
					});
				});
				Context("a POST to /{repository}/{id}/{contentCollectionProperty}", () -> {
					It("should append to the collection and return 201", () -> { 
						mvc.perform(post("/files/" + testEntity2.id.toString() + "/children")
								.content("Hello Spring Content World!"))								
						.andExpect(status().is2xxSuccessful())
						.andReturn();
					
						TestEntity2 fetched = repository2.findOne(testEntity2.id);
						assertThat(fetched.children.size(), is(1));
					});
				});
				Context("a DELETE to /{repository}/{id}/{contentCollectionProperty}", () -> {
					It("should return 404", () -> {
						mvc.perform(delete("/files/" + testEntity2.id.toString() + "/children"))
						.andExpect(status().is4xxClientError());
					});
				});
				Context("given the Entity has content in its content collection", () -> {
					BeforeEach(() -> {
						child1 = new TestEntityChild();
						child1.mimeType = "text/plain";
						contentRepository2.setContent(child1, new ByteArrayInputStream("Hello".getBytes()));

						child2 = new TestEntityChild();
						child2.mimeType = "text/plain";
						contentRepository2.setContent(child2, new ByteArrayInputStream("Spring Content World!".getBytes()));

						testEntity2.children = new ArrayList<TestEntityChild>();
						testEntity2.children.add(child1);
						testEntity2.children.add(child2);
						
						repository2.save(testEntity2);
					});
					Context("a GET to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should return a populated collection", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/children")
								.accept("application/hal+json"))
								.andExpect(status().isOk())
								.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));
							
							RepresentationFactory representationFactory = new StandardRepresentationFactory();
							ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
							assertThat(halResponse.getResources().size(), is(2));
							
							for (Entry<String, ReadableRepresentation> entry : halResponse.getResources()) {
								assertThat(entry.getValue().getLinkByRel("self").getHref(), 
										anyOf(
												 is("http://localhost/files/"+testEntity2.id+"/children/"+child1.contentId), 
												 is("http://localhost/files/"+testEntity2.id+"/children/"+child2.contentId)
										));
							}
						});
					});
					Context("a PUT to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should still return 404", () -> {
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/children"))
								.andExpect(status().is4xxClientError());
						});
					});
					Context("a POST to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should append to the collection and return 201", () -> {
							mvc.perform(post("/files/" + testEntity2.id.toString() + "/children")
											.content("Hello Spring Content World!"))								
								.andExpect(status().is2xxSuccessful())
								.andReturn();
							
							TestEntity2 fetched = repository2.findOne(testEntity2.id);
							assertThat(fetched.children.size(), is(3));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should still return 404", () -> {
							mvc.perform(delete("/files/" + testEntity2.id.toString() + "/children"))
							.andExpect(status().is4xxClientError());
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
