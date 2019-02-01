package internal.org.springframework.content.rest.links;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import lombok.Setter;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Setter
public class EntityContentLinkTests {

	private MockMvc mvc;

	private CrudRepository repository;
	private ContentStore store;

	private Object testEntity;
	private String url;
	private String linkRel;
	private String expectedLinkRegex;

	{
		Context("a GET to /{api}?/{repository}/{id}", () -> {
			It("should provide a response without a content link", () -> {
				MockHttpServletResponse response = mvc
						.perform(get(url)
								.accept("application/hal+json"))
						.andExpect(status().isOk()).andReturn().getResponse();
				assertThat(response, is(not(nullValue())));

				RepresentationFactory representationFactory = new StandardRepresentationFactory();
				ReadableRepresentation halResponse = representationFactory
						.readRepresentation("application/hal+json",
								new StringReader(response.getContentAsString()));
				assertThat(halResponse, is(not(nullValue())));
				assertThat(halResponse.getLinksByRel(linkRel),is(not(nullValue())));
				assertThat(halResponse.getLinksByRel(linkRel).size(), is(0));
			});
		});

		Context("given content is associated", () -> {
			BeforeEach(() -> {
				store.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				repository.save(testEntity);
			});
			Context("a GET to /{api}?/{repository}/{id}", () -> {
				It("should provide a response with a content link", () -> {
					MockHttpServletResponse response = mvc.perform(get(url)
									.accept("application/hal+json"))
							.andExpect(status().isOk()).andReturn().getResponse();
					assertThat(response, is(not(nullValue())));

					RepresentationFactory representationFactory = new StandardRepresentationFactory();
					ReadableRepresentation halResponse = representationFactory
							.readRepresentation("application/hal+json",
									new StringReader(response.getContentAsString()));

					assertThat(halResponse, is(not(nullValue())));
					assertThat(halResponse.getLinksByRel(linkRel), is(not(nullValue())));
					assertThat(halResponse.getLinksByRel(linkRel).size(), is(1));
					assertThat(halResponse.getLinksByRel(linkRel).get(0).getHref(), matchesPattern(expectedLinkRegex));
				});
			});
		});

//			Context("given an Entity with associated content and a Store specifying a store path", () -> {
//				BeforeEach(() -> {
//					testEntity = repository.save(new TestEntity());
//				});
//				Context("given content is associated", () -> {
//					BeforeEach(() -> {
//						contentRepository.setContent(testEntity,
//								new ByteArrayInputStream(
//										"Hello Spring Content World!"
//												.getBytes()));
//						repository.save(testEntity);
//					});
//					Context("a GET to /repository/id", () -> {
//						It("should provide a response with a content link", () -> {
//							MockHttpServletResponse response = mvc
//									.perform(get("/api/testEntities/" + testEntity.id).accept(
//											"application/hal+json"))
//									.andExpect(status().isOk())
//									.andReturn().getResponse();
//							assertThat(response, is(not(nullValue())));
//
//							RepresentationFactory representationFactory = new StandardRepresentationFactory();
//							ReadableRepresentation halResponse = representationFactory
//									.readRepresentation("application/hal+json",
//											new StringReader(response
//													.getContentAsString()));
//							assertThat(halResponse, is(not(nullValue())));
//							assertThat(halResponse.getLinksByRel("testEntitiesContent"), is(not(nullValue())));
//							assertThat(halResponse .getLinksByRel("testEntitiesContent") .size(), is(1));
//							assertThat(halResponse.getLinksByRel("testEntitiesContent").get(0).getHref(),
//									is("http://localhost/contentApi/testEntitiesContent/"
//											+ testEntity.contentId));
//						});
//					});
//				});
//			});
	}
}
