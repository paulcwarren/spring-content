package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import internal.org.springframework.content.rest.support.ContentEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Content {

    private MockMvc mvc;
    private String url;
    private String contextPath = "";
    private ContentEntity entity;
    private CrudRepository repository;
    private Store store;

    public static Content tests() {
        return new Content();
    }

    {
        Context("a GET to /{store}/{id} accepting */*", () -> {
            It("should return 404", () -> {
                mvc.perform(get(url)
                        .accept("*/*"))
                .andExpect(status().isNotFound());
            });
        });
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
                assertThat(IOUtils.toString(((ContentStore)store).getContent(fetched.get()), Charset.defaultCharset()), is(content));
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
                        .file(new MockMultipartFile("file", "表单ID及字段.txt", "text/plain", content.getBytes()))
                        .contextPath(contextPath)
                        )
                .andExpect(status().isCreated());

                Optional<ContentEntity> fetched = repository.findById(entity.getId());
                assertThat(fetched.isPresent(), is(true));
                assertThat(fetched.get().getContentId(), is(not(nullValue())));
                assertThat(fetched.get().getOriginalFileName(), is("表单ID及字段.txt"));
                assertThat(fetched.get().getMimeType(), is("text/plain"));
                assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
            });
        });

        Context("given the Entity has text/plain content", () -> {
            BeforeEach(() -> {
                String content = "Hello Spring Content World!";
                entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream(content.getBytes()));
                entity.setMimeType("text/plain");
                entity.setOriginalFileName("表单ID及字段.txt");
                entity = (ContentEntity) repository.save(entity);
            });
            Context("a GET to /{store}/{id}", () -> {
                It("should return the original content, filename and 200", () -> {

                    assertThat(Charset.defaultCharset(), is(Charset.forName("UTF-8")));

                    MockHttpServletResponse response = mvc
                            .perform(get(url)
                                    .contextPath(contextPath)
                                    .accept("text/plain"))
                            .andExpect(status().isOk()).andReturn().getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getHeader("Content-Disposition"), containsString("filename*=UTF-8''" + URLEncoder.encode("表单ID及字段.txt")));
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
                    assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
                    assertThat(response.getContentType(), is("text/html"));
                });
            });
            Context("a GET to /{store}/{id} with a mime type that matches a renderer and the original content-type", () -> {

                BeforeEach(() -> {
                    entity = (ContentEntity) ((ContentStore)store).setContent(entity, new ByteArrayInputStream("<html><body>original content</body></html>".getBytes()));
                    entity.setMimeType("text/html");
                    entity = (ContentEntity) repository.save(entity);
                });

                It("should return the rendition and 200", () -> {
                    MockHttpServletResponse response = mvc
                            .perform(get(url)
                                    .contextPath(contextPath)
                                    .accept("text/html"))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("<html><body>original content</body></html>"));
                    assertThat(response.getContentType(), is("text/html"));
                });
            });
            Context("a GET to /{store}/{id} with multiple mime types the last of which matches the content", () -> {
                It("should return the original content and 200", () -> {
                    MockHttpServletResponse response = mvc.perform(get(url)
                            .contextPath(contextPath)
                            .accept(new String[] { "text/xml", "text/plain" }))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
                    assertThat(response.getContentType(), is("text/plain"));
                });
            });
            Context("a GET to /{store}/{id} with multiple mime types the middle of which matches the content", () -> {
                It("should return the original content and 200", () -> {
                    MockHttpServletResponse response = mvc.perform(get(url)
                            .contextPath(contextPath)
                            .accept(new String[] { "text/xml", "text/html", "*/*" }))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
                    assertThat(response.getContentType(), is("text/html"));
                });
            });
            Context("a GET to /{store}/{id} with just an accept all mime type", () -> {
                It("should return the original content and 200", () -> {
                    MockHttpServletResponse response = mvc.perform(get(url)
                            .contextPath(contextPath)
                            .accept(new String[] { "*/*" }))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
                    assertThat(response.getContentType(), is("text/plain"));
                });
            });
            Context("a GET to /{store}/{id} with a mime type specifying charset", () -> {
                It("should return the original content and 200", () -> {
                    MockHttpServletResponse response = mvc.perform(get(url)
                            .contextPath(contextPath)
                            .accept(new String[] { "text/html;charset=ISO-8859-1" }))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
                    assertThat(response.getContentType(), is("text/html;charset=ISO-8859-1"));
                });
            });
            Context("a GET to /{store}/{id} when the original mime type has a charset", () -> {
                BeforeEach(() -> {
                    entity.setMimeType("text/plain;charset=ISO-8859-1");
                    entity = (ContentEntity) repository.save(entity);
                });
                It("should return the original content and 200", () -> {
                    MockHttpServletResponse response = mvc.perform(get(url)
                            .contextPath(contextPath)
                            .accept(new String[] { "text/plain" }))
                            .andExpect(status().isOk()).andReturn()
                            .getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
                    assertThat(response.getContentType(), is("text/plain;charset=ISO-8859-1"));
                });
            });
            Context("a GET to /{store}/{id} with a mime type that does not match a renderer or the original content", () -> {
                It("should return the original content and 200", () -> {
                    mvc.perform(get(url)
                        .contextPath(contextPath)
                        .accept("text/css"))
                    .andExpect(status().isNotFound());
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

                    assertThat(IOUtils.toString(((ContentStore)store).getContent(entity), Charset.defaultCharset()), is("Hello Modified Spring Content World!"));
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
                    assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
                });
            });
            Context("a PUT to /{store}/{id} with a multi-part request", () -> {
                It("should overwrite the content and return 200", () -> {

                    String content = "This is Modified Spring Content!";

                    mvc.perform(multipart(HttpMethod.PUT, url)
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
                    assertThat(fetched.get().getLen(), is(Long.valueOf(content.length())));
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
                    assertThat(((ContentStore)store).getContent(entity), is(nullValue()));
                });
            });
        });
    }
}
