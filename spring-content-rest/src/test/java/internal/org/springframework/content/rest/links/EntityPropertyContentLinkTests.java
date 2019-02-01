package internal.org.springframework.content.rest.links;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import lombok.Setter;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import java.io.StringReader;

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
public class EntityPropertyContentLinkTests {

	private MockMvc mvc;

	private CrudRepository repository;
	private ContentStore store;

	private Object testEntity;
	private String url;
	private String linkRel;
	private String expectedLinkRegex;

	{
		Context("a GET to /{api?}/{repository}/{id}", () -> {
			It("should return the content link", () -> {
				MockHttpServletResponse response = mvc
						.perform(get(url)
								.accept("application/hal+json"))
						.andExpect(status().isOk()).andReturn().getResponse();
				assertThat(response, is(not(nullValue())));

				RepresentationFactory representationFactory = new StandardRepresentationFactory();
				ReadableRepresentation halResponse = representationFactory
						.readRepresentation("application/hal+json",
								new StringReader(
										response.getContentAsString()));
				assertThat(halResponse, is(not(nullValue())));
				assertThat(halResponse.getLinksByRel(linkRel),
						is(not(nullValue())));
				assertThat(halResponse.getLinksByRel(linkRel).size(), is(1));
				assertThat(halResponse.getLinksByRel(linkRel).get(0).getHref(),
						matchesPattern(expectedLinkRegex));
			});
		});
		Context("a GET to /repository/id", () -> {
			It("should return the content collection link", () -> {
				MockHttpServletResponse response = mvc
						.perform(get(url)
								.accept("application/hal+json"))
						.andExpect(status().isOk()).andReturn()
						.getResponse();
				assertThat(response, is(not(nullValue())));

				RepresentationFactory representationFactory = new StandardRepresentationFactory();
				ReadableRepresentation halResponse = representationFactory
						.readRepresentation("application/hal+json",
								new StringReader(response
										.getContentAsString()));
				assertThat(halResponse, is(not(nullValue())));
				assertThat(halResponse.getLinksByRel("children"), is(not(nullValue())));
				assertThat(halResponse.getLinksByRel("children").size(), is(2));
			});
		});
	}
}
