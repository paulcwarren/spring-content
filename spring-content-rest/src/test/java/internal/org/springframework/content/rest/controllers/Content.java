package internal.org.springframework.content.rest.controllers;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import internal.org.springframework.content.rest.support.ContentEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@Setter
public class Content {

	private MockMvc mvc;
	private String url;
	private String contextPath = "";
	private ContentEntity entity;
	private CrudRepository repository;
	private ContentStore store;

	public static Content tests() {
		return new Content();
	}

	{
		Context("a GET to /{store}/{id} accepting a content mime-type", () -> {
			It("should return 404", () -> {
				mvc.perform(get(url)
						.accept("text/plain"))
						.andExpect(status().isNotFound());
			});
		});
		Context("a PUT to /{store}/{id} with a content body", () -> {
			It("should set the content and return 201", () -> {
				String content = "Hello New Spring Content World!";
				mvc.perform(
						put(url)
								.contextPath(contextPath)
								.content(content)
								.contentType("text/plain"))
						.andExpect(status().isCreated());

				Optional<ContentEntity> fetched = repository.findById(entity.getId());
				assertThat(fetched.isPresent(), is(true));
				assertThat(fetched.get().getContentId(), is(not(nullValue())));
				assertThat(fetched.get().getLen(), is(31L));
				assertThat(fetched.get().getMimeType(), is("text/plain"));
				assertThat(IOUtils.toString(store.getContent(fetched.get())), is(content));
			});
		});
		Context("a DELETE to /{store}/{id} with a mime-type", () -> {
			It("should return 404", () -> {
				mvc.perform(delete(url)
						.contextPath(contextPath)
						.accept("text/plain")).andExpect(status().isNotFound());
			});
		});
		Context("a POST to /{store}/{id} with a multi-part form-data request", () -> {
			It("should set the content and return 200", () -> {
				String content = "This is Spring Content!";

				mvc.perform(multipart(url)
						.file(new MockMultipartFile("file", "tests-file.txt", "text/plain", content.getBytes()))
						.contextPath(contextPath)
					)
					.andExpect(status().isCreated());

				Optional<ContentEntity> fetched = repository.findById(entity.getId());
				assertThat(fetched.isPresent(), is(true));
				assertThat(fetched.get().getContentId(), is(not(nullValue())));
				assertThat(fetched.get().getOriginalFileName(), is("tests-file.txt"));
				assertThat(fetched.get().getMimeType(), is("text/plain"));
				assertThat(fetched.get().getLen(), is(new Long(content.length())));
			});
		});

		Context("given the Entity has content", () -> {
			BeforeEach(() -> {
				String content = "Hello Spring Content World!";
				store.setContent(entity, new ByteArrayInputStream(content.getBytes()));
				entity.setMimeType("text/plain");
				entity = (ContentEntity)repository.save(entity);
			});
			Context("a GET to /{store}/{id}", () -> {
				It("should return the original content and 200", () -> {
					MockHttpServletResponse response = mvc
							.perform(get(url)
									.contextPath(contextPath)
									.accept("text/plain"))
							.andExpect(status().isOk()).andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
				});
			});
			Context("a GET to /{store}/{id} with no accept header", () -> {
				It("should return the original content", () -> {
					MockHttpServletResponse response = mvc.perform(
							get(url)
							.contextPath(contextPath)
						)
						.andExpect(status().isOk())
						.andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(),is("Hello Spring Content World!"));
				});
			});
			Context("a GET to /{store}/{id} with a mime type that matches a renderer", () -> {
				It("should return the rendition and 200", () -> {
					MockHttpServletResponse response = mvc
							.perform(get(url)
									.contextPath(contextPath)
									.accept("text/html"))
							.andExpect(status().isOk()).andReturn()
							.getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentLength(), is(-1));
					assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
				});
			});
			Context("a GET to /{store}/{id} with multiple mime types the last of which matches the content", () -> {
				It("should return the original content and 200", () -> {
					MockHttpServletResponse response = mvc.perform(get(url)
									.contextPath(contextPath)
									.accept(new String[] { "text/xml", "text/*" }))
							.andExpect(status().isOk()).andReturn()
							.getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
				});
			});
			Context("a GET to /{store}/{id} with a range header", () -> {
				It("should return the content range and 206", () -> {
					MockHttpServletResponse response = mvc
							.perform(get(url)
									.contextPath(contextPath)
									.accept("text/plain")
									.header("range", "bytes=6-19"))
							.andExpect(status().isPartialContent()).andReturn()
							.getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Spring Content"));
				});
			});
			Context("a PUT to /{store}/{id}", () -> {
				It("should overwrite the content and return 200", () -> {
					mvc.perform(put(url)
							.contextPath(contextPath)
							.content("Hello Modified Spring Content World!")
							.contentType("text/plain"))
							.andExpect(status().isOk());

					assertThat(IOUtils.toString(store.getContent(entity)), is("Hello Modified Spring Content World!"));
				});
			});
			Context("a POST to /{store}/{id} with a multi-part request", () -> {
				It("should overwrite the content and return 200", () -> {

					String content = "This is Modified Spring Content!";

					mvc.perform(multipart(url)
							.file(new MockMultipartFile("file",
									"tests-file-modified.txt",
									"text/plain", content.getBytes()))
							.contextPath(contextPath)
						)
						.andExpect(status().isOk());

					Optional<ContentEntity> fetched = repository.findById(entity.getId());
					assertThat(fetched.isPresent(), is(true));
					assertThat(fetched.get().getContentId(), is(not(nullValue())));
					assertThat(fetched.get().getOriginalFileName(), is("tests-file-modified.txt"));
					assertThat(fetched.get().getMimeType(), is("text/plain"));
					assertThat(fetched.get().getLen(), is(new Long(content.length())));
				});
			});
			Context("a DELETE to /{store}/{id} with the mimetype", () -> {
				It("should delete the content, attributes and return a 200 response", () -> {
					mvc.perform(delete(url)
							.contentType("text/plain")
							.contextPath(contextPath)
						)
						.andExpect(status().isNoContent());

					Optional<ContentEntity> fetched = repository.findById(entity.getId());
					assertThat(fetched.isPresent(), is(true));
					assertThat(fetched.get().getContentId(), is(nullValue()));
					assertThat(fetched.get().getLen(), is(0L));
					assertThat(fetched.get().getMimeType(), is(nullValue()));
					assertThat(store.getContent(entity), is(nullValue()));
				});
			});
		});
	}
}
