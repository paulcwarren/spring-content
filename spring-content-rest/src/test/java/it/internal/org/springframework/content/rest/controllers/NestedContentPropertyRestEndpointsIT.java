package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity8;
import internal.org.springframework.content.rest.support.TestEntity8Repository;
import internal.org.springframework.content.rest.support.TestEntity8Store;

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
public class NestedContentPropertyRestEndpointsIT {

   @Autowired private TestEntity8Repository repository2;
   @Autowired private TestEntity8Store store;

   private TestEntity8 testEntity8;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   private MockMvc mvc;

   {
      Describe("Nested Content Property REST Endpoints", () -> {
		BeforeEach(() -> {
		  mvc = MockMvcBuilders.webAppContextSetup(context).build();
		});
		Context("given an Entity with a simple content property", () -> {
		  BeforeEach(() -> {
			  testEntity8 = repository2.save(new TestEntity8());
		  });

		  Context("given a request to a non-existent entity", () -> {
              It("should return 404", () -> {
                  mvc.perform(
                          get("/testEntity8s/9999999/foo"))
                          .andExpect(status().isNotFound());
              });
		  });

		  Context("given a POST to the entity endpoint with a multipart/form request", () -> {
			  It("should create a new entity and its content and respond with a 201 Created", () -> {
				  // assert content does not exist
				  String newContent = "This is some new content";

				  MockMultipartFile file = new MockMultipartFile("child", "filename.txt", "text/plain", newContent.getBytes());

				  // POST the new content
				  MockHttpServletResponse response = mvc.perform(multipart("/testEntity8s")
						  .file(file)
						  .param("name", "foo")
						  .param("hidden", "bar"))
						  .andExpect(status().isCreated())
						  .andReturn().getResponse();

				  String location = response.getHeader("Location");

				  Optional<TestEntity8> fetchedEntity = repository2.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
				  assertThat(fetchedEntity.get().getHidden(), is(nullValue()));

				  // assert that it now exists
				  response = mvc.perform(get(location + "/child")
						  .accept("text/plain"))
						  .andExpect(status().isOk())
						  .andReturn().getResponse();

				  assertThat(response.getContentAsString(), is(newContent));
			  });
		  });

          Context("given a request to a non-existent content property", () -> {
              It("should return 404", () -> {
                  mvc.perform(
                          get("/testEntity8s/" + testEntity8.getId() + "/doesnotexist"))
                          .andExpect(status().isNotFound());
              });
          });

		  Context("given that is has no content", () -> {
			  Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
				  It("should return 404", () -> {
					  mvc.perform(
							  get("/testEntity8s/" + testEntity8.getId() + "/child"))
							  .andExpect(status().isNotFound());
				  });
			  });
			  Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
				  It("should create the content", () -> {

					  mvc.perform(
							  put("/testEntity8s/" + testEntity8.getId() + "/child")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId,is(not(nullValue())));
					  assertThat(fetched.get().getChild().contentLen, is(31L));
					  assertThat(fetched.get().getChild().contentMimeType, is("text/plain"));
					  try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
					      IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
					  }
				  });
			  });
			  Context("a PUT to /{store}/{id} with json content", () -> {
			      It("should set the content and return 201", () -> {
			          String content = "{\"content\":\"Hello New Spring Content World!\"}";
			          mvc.perform(
                            put("/testEntity8s/" + testEntity8.getId() + "/child")
                            .content(content)
                            .contentType("application/json"))
			          .andExpect(status().isCreated());

			          Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
			          assertThat(fetched.isPresent(), is(true));
			          assertThat(fetched.get().getChild().getContentId(), is(not(nullValue())));
			          assertThat(fetched.get().getChild().getContentLen(), is(45L));
			          assertThat(fetched.get().getChild().getContentMimeType(), is("application/json"));
                      try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
                          IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                      }
			      });
			  });
		  });
		  Context("given that it has content", () -> {
			  BeforeEach(() -> {
				  String content = "Hello Spring Content World!";

				  testEntity8.getChild().contentMimeType = "text/plain";
				  UUID contentId = UUID.randomUUID();
				  store.associate(testEntity8, PropertyPath.from("child"), contentId);
				  WritableResource r = (WritableResource)store.getResource(testEntity8, PropertyPath.from("child"));
				  try (OutputStream out = r.getOutputStream()) {
				      out.write(content.getBytes());
				  }
				  testEntity8 = repository2.save(testEntity8);

				  versionTests.setMvc(mvc);
				  versionTests.setUrl("/testEntity8s/" + testEntity8.getId() + "/child");
				  versionTests.setCollectionUrl("/testEntity8s");
				  versionTests.setContentLinkRel("child");
				  versionTests.setRepo(repository2);
				  versionTests.setStore(store);
				  versionTests.setEtag(format("\"%s\"", testEntity8.getVersion()));

				  lastModifiedDateTests.setMvc(mvc);
				  lastModifiedDateTests.setUrl("/testEntity8s/" + testEntity8.getId() + "/child");
				  lastModifiedDateTests.setLastModifiedDate(testEntity8.getModifiedDate());
				  lastModifiedDateTests.setEtag(testEntity8.getVersion().toString());
				  lastModifiedDateTests.setContent(content);
			  });
			  Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
				  It("should return the content", () -> {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity8s/" + testEntity8.getId() + "/child")
									  .accept("text/plain"))
							  .andExpect(status().isOk())
							  .andExpect(header().string("etag", is("\"1\"")))
							  .andExpect(header().string("last-modified", LastModifiedDate
									  .isWithinASecond(testEntity8.getModifiedDate())))
							  .andReturn().getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
				  });
			  });
			  Context("a GET to /{repository}/{id}/{contentProperty} with a mime type that matches a renderer", () -> {
				  It("should return the rendition and 200", () -> {
					  MockHttpServletResponse response = mvc
							  .perform(get(
									  "/testEntity8s/" + testEntity8.getId()
											  + "/child")
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
							  .perform(get("/testEntity8s/"
									  + testEntity8.getId()
									  + "/child").accept(
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
							  put("/testEntity8s/" + testEntity8.getId() + "/child")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity8> fetched = repository2
							  .findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId,is(not(nullValue())));
					  assertThat(fetched.get().getChild().contentLen, is(31L));
					  assertThat(fetched.get().getChild().contentMimeType, is("text/plain"));
				  });
			  });
			  Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
				  It("should delete the content", () -> {
					  mvc.perform(delete(
							  "/testEntity8s/" + testEntity8.getId() + "/child"))
							  .andExpect(status().isNoContent());

					  Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId, is(nullValue()));
					  assertThat(fetched.get().getChild().contentLen, is(nullValue()));
                      assertThat(fetched.get().getChild().contentMimeType, is(nullValue()));
				  });
			  });

			  versionTests = Version.tests();
			  lastModifiedDateTests = LastModifiedDate.tests();
		  });
		});
      });
   }

	@Test
   public void noop() {
   }

   private static String toHeaderDateFormat(Date dt) {
      SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
      format.setTimeZone(TimeZone.getTimeZone("GMT"));
      return format.format(dt);
   }

}
