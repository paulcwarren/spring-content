package it.internal.org.springframework.content.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;
import internal.org.springframework.content.rest.support.*;
import org.apache.commons.io.IOUtils;
import org.hamcrest.beans.HasPropertyWithValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class,
	  HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
public class RestResourceMappedRestEndpointsIT {

   @Autowired private TestEntity11Repository repo;
   @Autowired private TestEntity11Store store;

   private TestEntity11 testEntity11;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   private MockMvc mvc;

   {
      Describe("RestResource mapped REST Endpoints", () -> {
		BeforeEach(() -> {
		  mvc = MockMvcBuilders.webAppContextSetup(context).build();
		});
		Context("given an Entity with a simple content property", () -> {
		  BeforeEach(() -> {
			  testEntity11 = repo.save(new TestEntity11());
		  });

		  Context("given a request to a non-existent entity", () -> {
              It("should return 404", () -> {
                  mvc.perform(
                          get("/testEntity11s/9999999/package/content"))
                          .andExpect(status().isNotFound());
              });
		  });

          Context("given a request to a non-existent content property", () -> {
              It("should return 404", () -> {
                  mvc.perform(
                          get("/testEntity11s/" + testEntity11.getId() + "/doesnotexist"))
                          .andExpect(status().isNotFound());
              });
          });

		  Context("given that is has no content", () -> {
			  Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
				  It("should return 404", () -> {
					  mvc.perform(
							  get("/testEntity11s/" + testEntity11.getId() + "/package/content"))
							  .andExpect(status().isNotFound());
				  });
			  });
			  Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
				  It("should create the content", () -> {

					  mvc.perform(
							  put("/testEntity11s/" + testEntity11.getId() + "/package/content")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().get_package().contentId,is(not(nullValue())));
					  assertThat(fetched.get().get_package().contentLen, is(31L));
					  assertThat(fetched.get().get_package().contentMimeType, is("text/plain"));
					  try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("_package/content")).getInputStream()) {
					      IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
					  }
				  });
			  });
			  Context("a PUT to /{store}/{id} with json content", () -> {
			      It("should set the content and return 201", () -> {
			          String content = "{\"content\":\"Hello New Spring Content World!\"}";
			          mvc.perform(
                            put("/testEntity11s/" + testEntity11.getId() + "/package/content")
                            .content(content)
                            .contentType("application/json"))
			          .andExpect(status().isCreated());

			          Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
			          assertThat(fetched.isPresent(), is(true));
			          assertThat(fetched.get().get_package().getContentId(), is(not(nullValue())));
			          assertThat(fetched.get().get_package().getContentLen(), is(45L));
			          assertThat(fetched.get().get_package().getContentMimeType(), is("application/json"));
                      try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("_package/content")).getInputStream()) {
                          IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                      }
			      });
			  });
		  });
		  Context("given that it has content", () -> {
			  BeforeEach(() -> {
				  String content = "Hello Spring Content World!";

				  testEntity11.get_package().contentMimeType = "text/plain";
				  UUID contentId = UUID.randomUUID();
				  store.associate(testEntity11, PropertyPath.from("_package/content"), contentId);
				  WritableResource r = (WritableResource)store.getResource(testEntity11, PropertyPath.from("_package/content"));
				  try (OutputStream out = r.getOutputStream()) {
				      out.write(content.getBytes());
				  }
				  testEntity11 = repo.save(testEntity11);

				  versionTests.setMvc(mvc);
				  versionTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
				  versionTests.setCollectionUrl("/testEntity11s");
				  versionTests.setContentLinkRel("package/content");
				  versionTests.setRepo(repo);
				  versionTests.setStore(store);
				  versionTests.setEtag(format("\"%s\"", testEntity11.getVersion()));

				  lastModifiedDateTests.setMvc(mvc);
				  lastModifiedDateTests.setUrl("/testEntity11s/" + testEntity11.getId() + "/package/content");
				  lastModifiedDateTests.setLastModifiedDate(testEntity11.getModifiedDate());
				  lastModifiedDateTests.setEtag(testEntity11.getVersion().toString());
				  lastModifiedDateTests.setContent(content);
			  });
			  Context("a GET to /{repository}/{id} for the entity json", () -> {
				  It("should return the mapped content links", () -> {
					  MockHttpServletResponse res = mvc.perform(
									  get("/testEntity11s/" + testEntity11.getId())
											  .accept("application/hal+json"))
							  .andExpect(status().is2xxSuccessful())
							  .andReturn().getResponse();

					  ObjectMapper mapper = new ObjectMapper();
					  Map<String,Object> obj = mapper.readValue(res.getContentAsString(), Map.class);

					  Object val = parse(obj, "_links", "package/content", "href");
					  assertThat(val, is(not(nullValue())));
					  assertThat(val.toString(), matchesPattern("http://localhost/testEntity11s/.*/package/content"));
				  });
			  });
			  Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
				  It("should return the content", () -> {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity11s/" + testEntity11.getId() + "/package/content")
									  .accept("text/plain"))
							  .andExpect(status().isOk())
							  .andExpect(header().string("etag", is("\"1\"")))
							  .andExpect(header().string("last-modified", LastModifiedDate
									  .isWithinASecond(testEntity11.getModifiedDate())))
							  .andReturn().getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
				  });
			  });
			  Context("a GET to /{repository}/{id}/{contentProperty} with a mime type that matches a renderer", () -> {
				  It("should return the rendition and 200", () -> {
					  MockHttpServletResponse response = mvc
							  .perform(get(
									  "/testEntity11s/" + testEntity11.getId() + "/package/content")
									  .accept("text/html"))
							  .andExpect(status().isOk()).andReturn()
							  .getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(), is(
							  "<html><body>Hello Spring Content World!</body></html>"));
				  });
			  });
			  Context("a GET to /{repository}/{id}/{contentProperty} with multiple mime types the last of which matches the content", () -> {
				  It("should return the original content and 200", () -> {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity11s/" + testEntity11.getId() + "/package/content").accept(
									  new String[] {"text/xml",
											  "text/plain"}))
							  .andExpect(status().isOk()).andReturn()
							  .getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(),
							  is("Hello Spring Content World!"));
				  });
			  });
			  Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
				  It("should create the content", () -> {
					  mvc.perform(
							  put("/testEntity11s/" + testEntity11.getId() + "/package/content")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().get_package().contentId,is(not(nullValue())));
					  assertThat(fetched.get().get_package().contentLen, is(31L));
					  assertThat(fetched.get().get_package().contentMimeType, is("text/plain"));
				  });
			  });
			  Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
				  It("should delete the content", () -> {
					  mvc.perform(delete(
							  "/testEntity11s/" + testEntity11.getId() + "/package/content"))
							  .andExpect(status().isNoContent());

					  Optional<TestEntity11> fetched = repo.findById(testEntity11.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().get_package().contentId, is(nullValue()));
					  assertThat(fetched.get().get_package().contentLen, is(nullValue()));
                      assertThat(fetched.get().get_package().contentMimeType, is(nullValue()));
				  });
			  });

			  versionTests = Version.tests();
			  lastModifiedDateTests = LastModifiedDate.tests();
		  });
		});
      });
   }

	private Object parse(Map<String, Object> obj, String... path) {
		Object current = null;
		current = obj.get(path[0]);
		if (current instanceof Map) {
			current = parse((Map<String,Object>)current, Arrays.copyOfRange(path, 1, path.length));
		}
		return current;
	}

	@Test
	public void noop() {
	}
}
