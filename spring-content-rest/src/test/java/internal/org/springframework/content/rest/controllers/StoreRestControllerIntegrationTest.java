package internal.org.springframework.content.rest.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.Resource;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import internal.org.springframework.content.rest.support.TestStore;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class})
@Transactional
@ActiveProfiles("store")
public class StoreRestControllerIntegrationTest {
	
	@Autowired TestEntityRepository repository;
	@Autowired TestEntityContentRepository contentRepository;
	@Autowired TestStore store;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;

	private String path;
	private String request;
	
	{
		Describe("StoreRestController", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});

			Context("given a root resource", () -> {
				BeforeEach(() -> {
					path = "/" + UUID.randomUUID() + ".txt";
					request = "/teststore" + path;
					Resource r = store.getResource(path);
					if (r instanceof WritableResource) {
						IOUtils.copy(new ByteArrayInputStream("Existing content".getBytes()), ((WritableResource) r).getOutputStream());
					}
				});
				It("should return the resource's content", () -> {
					MockHttpServletResponse response = mvc.perform(get(request))
							.andExpect(status().isOk())
							.andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Existing content"));

				});
				It("should return a byte range when requested", () -> {
					MockHttpServletResponse response = mvc.perform(get(request)
							.header("range", "bytes=9-12"))
							.andExpect(status().isPartialContent())
							.andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("cont"));
				});
				It("should overwrite the resource's content", () -> {
					mvc.perform(put(request)
							.content("New Existing content")
							.contentType("text/plain"))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(IOUtils.contentEquals(new ByteArrayInputStream("New Existing content".getBytes()), r.getInputStream()), is(true));
				});
				Context("a POST to /{store}/{path} with multi-part form-data ", () -> {
					It("should overwrite the content and return 200", () -> {

						String content = "New multi-part content";

						mvc.perform(fileUpload(request)
								.file(new MockMultipartFile("file", "test-file.txt", "text/plain", content.getBytes())))
								.andExpect(status().isOk());

						Resource r = store.getResource(path);
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("New multi-part content".getBytes()), r.getInputStream()), is(true));
					});
				});
				It("should delete the resource", () -> {
					mvc.perform(delete(request))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(r.exists(), is(false));
				});
			});

			Context("given a nested resource", () -> {
				BeforeEach(() -> {
					path = "/a/b/" + UUID.randomUUID() + ".txt";
					request = "/teststore" + path;
					Resource r = store.getResource(path);
					if (r instanceof WritableResource) {
						IOUtils.copy(new ByteArrayInputStream("Existing content".getBytes()), ((WritableResource) r).getOutputStream());
					}
				});
				It("should return the resource's content", () -> {
					MockHttpServletResponse response = mvc.perform(get(request))
							.andExpect(status().isOk())
							.andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Existing content"));

				});
				It("should return a byte range when requested", () -> {
					MockHttpServletResponse response = mvc.perform(get(request)
							.header("range", "bytes=9-12"))
							.andExpect(status().isPartialContent())
							.andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("cont"));
				});
				Context("given a typical browser request", () -> {
					It("should return the resource's content", () -> {
						MockHttpServletResponse response = mvc.perform(get(request).accept(new String[] {"text/html","application/xhtml+xml","application/xml;q=0.9","image/webp","image/apng","*/*;q=0.8"}))
								.andExpect(status().isOk())
								.andReturn().getResponse();

						assertThat(response, is(not(nullValue())));
						assertThat(response.getContentAsString(), is("Existing content"));
					});
				});
				It("should overwrite the resource's content", () -> {
					mvc.perform(put(request)
							.content("New Existing content")
							.contentType("text/plain"))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(IOUtils.contentEquals(new ByteArrayInputStream("New Existing content".getBytes()), r.getInputStream()), is(true));
				});
				Context("a POST to /{store}/{path} with multi-part form-data ", () -> {
					It("should overwrite the content and return 200", () -> {

						String content = "New multi-part content";

						mvc.perform(fileUpload(request)
								.file(new MockMultipartFile("file", "test-file.txt", "text/plain", content.getBytes())))
								.andExpect(status().isOk());

						Resource r = store.getResource(path);
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("New multi-part content".getBytes()), r.getInputStream()), is(true));
					});
				});
				It("should delete the resource", () -> {
					mvc.perform(delete(request))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(r.exists(), is(false));
				});
			});
		});
	}

	@Test
	public void noop() {}
}
