package internal.org.springframework.content.rest.links;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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

import com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.TestConfig;
import internal.org.springframework.content.rest.TestEntity;
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
public class ContentLinksResourceProcessorIntegrationTest {
	
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

	private TestEntity testEntity;
	private TestEntity2 testEntity2;
	private TestEntityChild child2;

	{
		Describe("ContentLinksResourceProcessor", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
			
			// TODO: Add tests for content entity

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
							assertThat(halResponse.getLinksByRel("children").size(), is(1));
							assertThat(halResponse.getLinksByRel("children").get(0).getHref(), is("http://localhost/files/"+testEntity2.id+"/children"));
						});
//						Context("a GET to /repository/id/collectionProperty", () -> {
//							Ginkgo4jDSL.FIt("should return content collection links", () -> {
//								MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id + "/children")
//										.accept("application/hal+json"))
//										.andExpect(status().isOk())
//										.andReturn().getResponse();
//								assertThat(response, is(not(nullValue())));
//								
//								RepresentationFactory representationFactory = new StandardRepresentationFactory();
//								ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
//								assertThat(halResponse, is(not(nullValue())));
//								assertThat(halResponse.getResources().size(), is(2));
//								for (Entry<String, ReadableRepresentation> entry : halResponse.getResources()) {
//									
//									// attempting to assert links
//									
//									assertThat(entry.getValue().getLinkByRel("self"), startsWith("http://localhost/files/"+testEntity2.id+"/children/"));
//								}
//							});
//						});
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
