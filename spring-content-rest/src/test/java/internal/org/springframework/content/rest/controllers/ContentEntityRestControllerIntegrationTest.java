package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.TestConfig;
import internal.org.springframework.content.rest.TestEntity;
import internal.org.springframework.content.rest.TestEntityContentRepository;
import internal.org.springframework.content.rest.TestEntityRepository;
import internal.org.springframework.content.rest.config.ContentRestConfiguration;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, ContentRestConfiguration.class})
//@WebIntegrationTest
@Transactional
public class ContentEntityRestControllerIntegrationTest {
	
	@Autowired TestEntityRepository repository;
	@Autowired TestEntityContentRepository contentRepository;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	
	{
		Describe("ContentEntityRestController", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
			
			Context("given an Entity is a ContentRepository", () -> {
				BeforeEach(() -> {
					testEntity = repository.save(new TestEntity());
				});
				Context("a GET to /{repository}/{id} accepting a content mime-type", () -> {
					It("should return 404", () -> {
						mvc.perform(get("/testEntities/" + testEntity.id)
						.accept("text/plain"))
						.andExpect(status().isNotFound());
					});
				});
				Context("a GET to /{repository}/{id} accepting hal+json", () -> {
					It("should return the entity", () -> {
						MockHttpServletResponse response = mvc.perform(get("/testEntities/" + testEntity.id)
								.accept("application/hal+json"))
								.andExpect(status().isOk())
								.andReturn().getResponse();
							
						RepresentationFactory representationFactory = new StandardRepresentationFactory();
						ReadableRepresentation halResponse = representationFactory.readRepresentation("application/hal+json", new StringReader(response.getContentAsString()));
						assertThat(halResponse.getLinks().size(), is(2));
						assertThat(halResponse.getLinksByRel("testEntity"), is(not(nullValue())));
					});
				});
				Context("a PUT to /{repository}/{id} with a content body", () -> {
					Ginkgo4jDSL.It("should set the content and return 200", () -> {
						mvc.perform(put("/testEntities/" + testEntity.id.toString())
						.content("Hello New Spring Content World!")
						.contentType("text/plain"))
						.andExpect(status().isOk());

						TestEntity fetched = repository.findOne(testEntity.id);
						assertThat(fetched.contentId, is(not(nullValue())));
						assertThat(fetched.len, is(31L));
						assertThat(fetched.mimeType, is("text/plain"));
						assertThat(IOUtils.toString(contentRepository.getContent(fetched)), is("Hello New Spring Content World!"));
					});
				});
				Context("a PUT to /{repository}/{id} with a json body", () -> {
					Ginkgo4jDSL.FIt("should set Entities data", () -> {
						mvc.perform(put("/testEntities/" + testEntity.id.toString())
						.content("{\"name\":\"Spring Content\"}")
						.contentType("application/hal+json"))
						.andExpect(status().is2xxSuccessful());

						TestEntity fetched = repository.findOne(testEntity.id);
						assertThat(fetched.name, is("Spring Content"));
						assertThat(fetched.contentId, is(nullValue()));
						assertThat(fetched.len, is(nullValue()));
						assertThat(fetched.mimeType, is(nullValue()));
						assertThat(contentRepository.getContent(fetched), is(nullValue()));
					});
				});

				Context("given the Entity has content", () -> {
					BeforeEach(() -> {
						contentRepository.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						testEntity.mimeType = "text/plain";
						testEntity = repository.save(testEntity);
					});
					Context("a GET to /{repository}/{id}", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity.id)
									.accept("text/plain"))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
						});
					});
					Context("a PUT to /{repository}/{id}", () -> {
						It("should overwrite the content", () -> {
							mvc.perform(put("/files/" + testEntity.id)
									.content("Hello Modified Spring Content World!")
									.contentType("text/plain"))
									.andExpect(status().isOk());

							assertThat(IOUtils.toString(contentRepository.getContent(testEntity)), is("Hello Modified Spring Content World!"));
						});
					});
				});
			});
		});
	}

	@Test
	public void noop() {}
}
