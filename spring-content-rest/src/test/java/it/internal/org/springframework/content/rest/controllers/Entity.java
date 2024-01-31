package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringRegularExpression.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.StringReader;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.ContentEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Entity {

	private MockMvc mvc;
	private String url;
	private String linkRel;
	private ContentEntity entity;
	private CrudRepository repository;

	public static Entity tests() {
		return new Entity();
	}

	{
		Context("a GET to /{store}/{id} accepting hal+json", () -> {
			It("should return the entity", () -> {
				MockHttpServletResponse response = mvc
						.perform(get(url)
								.accept("application/hal+json"))
						.andExpect(status().isOk())
						.andReturn().getResponse();

				RepresentationFactory representationFactory = new StandardRepresentationFactory();
				ReadableRepresentation halResponse = representationFactory
						.readRepresentation("application/hal+json",
								new StringReader(response.getContentAsString()));
				assertThat(halResponse.getLinksByRel(linkRel), is(not(nullValue())));
				assertThat(halResponse.getLinksByRel(linkRel).size(), is(1));
				assertThat(halResponse.getLinksByRel(linkRel).get(0).getHref(), matchesRegex("http://localhost" + url));
			});
		});
		Context("a PUT to /{store}/{id} with a json body", () -> {
			It("should set Entities data and return 200", () -> {
				entity.setTitle("Spring Content");
				mvc.perform(put(url)
						.content(new ObjectMapper().writeValueAsString(entity))
						.contentType("application/hal+json"))
						.andExpect(status().is2xxSuccessful());

				Optional<ContentEntity> fetched = repository.findById(entity.getId());
				assertThat(fetched.isPresent(), is(true));
				assertThat(fetched.get().getTitle(), is("Spring Content"));
				assertThat(fetched.get().getContentId(), is(nullValue()));
				assertThat(fetched.get().getLen(), is(nullValue()));
			});
		});
		Context("a PATCH to /{store}/{id} with a json body", () -> {
			It("should patch the entity data and return 200", () -> {
				mvc.perform(patch(url)
						.content("{\"title\":\"Spring Content Modified\"}")
						.contentType("application/hal+json"))
						.andExpect(status().is2xxSuccessful());

				Optional<ContentEntity> fetched = repository.findById(entity.getId());
				assertThat(fetched.isPresent(), is(true));
				assertThat(fetched.get().getTitle(), is("Spring Content Modified"));
				assertThat(fetched.get().getContentId(), is(nullValue()));
				assertThat(fetched.get().getLen(), is(nullValue()));
				assertThat(fetched.get().getMimeType(), is(nullValue()));
			});
		});
		Context("a HEAD to /{store}/{id} with a json body", () -> {
			It("should return 200", () -> {
				mvc.perform(head(url))
						.andExpect(status().is2xxSuccessful());
			});
		});
	}
}
