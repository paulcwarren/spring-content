package it.internal.org.springframework.content.rest.controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import internal.org.springframework.content.rest.support.ContentEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity4;
import lombok.Setter;
import org.apache.commons.io.IOUtils;

import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.data.repository.CrudRepository;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Setter
public class Version {

    private MockMvc mvc;
    private String url;
    private String collectionUrl;
    private String contentLinkRel;
    private CrudRepository repo;
    private Store store;
    private String etag;
    private ContentEntity entity;

    public static Version tests() {
        return new Version();
    }

    {
        Context("#Issue 1975", () -> {
            It("should always evaluate if-match header even after content deletion", () -> {
                String entityUrl = mvc.perform(post(collectionUrl).content("{}"))
                        .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getHeader("Location");
                assertThat(entityUrl, is(not(nullValue())));

                String body = mvc.perform(get(entityUrl).accept("application/json"))
                        .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();

                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseJson = objectMapper.readTree(body);
                JsonNode linksNode = responseJson.get("_links");
                JsonNode contentNode = linksNode.get(contentLinkRel);
                String contentHref = contentNode.get("href").asText();

                mvc.perform(put(contentHref).header("If-Match", "\"0\"").contentType("text/plain").content("Hello world!"))
                        .andExpect(status().is2xxSuccessful());  // Content created
                mvc.perform(delete(contentHref).header("If-Match", "\"1\""))
                        .andExpect(status().is2xxSuccessful());  // User A
                mvc.perform(put(contentHref).header("If-Match", "\"1\"").contentType("text/plain").content("foo bar"))
                        .andExpect(status().isPreconditionFailed());  // User B
            });
        });

        Context("a GET request to /{store}/{id}", () -> {
            It("should return an etag header", () -> {
                MockHttpServletResponse response = mvc
                        .perform(get(url)
                                .accept("text/plain"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("etag", is(etag)))
                        .andReturn().getResponse();

                assertThat(response, is(not(nullValue())));
                assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
            });
        });
        Context("a GET request to /{store}/{id} with a matching If-None-Match header", () -> {
            It("should respond with a 304 Not Modified", () -> {
                mvc.perform(get(url)
                        .accept("text/plain")
                        .header("if-none-match", etag))
                        .andExpect(status().isNotModified());
            });
        });
        Context("a GET request to /{store}/{id} with an non-matching If-None-Match header", () -> {
            It("should respond with the content", () -> {
                MockHttpServletResponse response = mvc
                        .perform(get(url)
                                .accept("text/plain")
                                .header("if-none-match", "\"999\""))
                        .andExpect(status().isOk())
                        .andExpect(header().string("etag", is(etag)))
                        .andReturn().getResponse();

                assertThat(response, is(not(nullValue())));
                assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
            });
        });
        Context("a PUT to /{store}/{id} with a matching If-Match header", () -> {
            It("should update the content", () -> {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/other")
                        .header("if-match", etag))
                        .andExpect(status().isOk());
            });
            It("should update the content attributes", () -> {
                mvc.perform(multipart(url)
                        .file(new MockMultipartFile("file",
                                "test-file-modified.txt",
                                "text/other", "Hello Modified Spring Content World!".getBytes()))
                        .header("if-match", etag))
                        .andExpect(status().isOk());

                if (entity != null) {
                    Optional<ContentEntity> fetched = repo.findById(entity.getId());
                    assertThat(fetched.isPresent(), is(true));
                    assertThat(fetched.get().getLen(), is(36L));
                    assertThat(fetched.get().getMimeType(), is("text/other"));
                    assertThat(fetched.get().getOriginalFileName(), is("test-file-modified.txt"));
                }
            });
        });
        Context("a DELETE to /{store}/{id} with a matching If-Match header", () -> {
            It("should delete the content, attributes and return a 200 response", () -> {
                mvc.perform(delete(url)
                        .contentType("text/plain"))
                        .andExpect(status().isNoContent());

                if (entity != null) {
                    Optional<ContentEntity> fetched = repo.findById(entity.getId());
                    assertThat(fetched.isPresent(), is(true));
                    assertThat(fetched.get().getContentId(), is(nullValue()));
                    assertThat(fetched.get().getLen(), is(nullValue()));
                    assertThat(fetched.get().getMimeType(), is(nullValue()));
                    assertThat(((ContentStore)store).getContent(fetched.get()), is(nullValue()));
                }
            });
        });
        Context("a PUT to /{store}/{id} with a non-matching If-Match header", () -> {
            It("should respond with 412 Precondition Failed", () -> {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-match", "\"999\""))
                        .andExpect(status().isPreconditionFailed());
            });
        });
        Context("a PUT to /{store}/{id} with a matching If-None-Match header", () -> {
            It("should respond with a 412 Precondition Failed", () -> {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-none-match", etag))
                        .andExpect(status().isPreconditionFailed());
            });
        });
        Context("a PUT to /{store}/{id} with a non-matching If-None-Match header", () -> {
            It("should respond with 200 OK and set the content", () -> {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-none-match", "\"999\""))
                        .andExpect(status().isOk());
            });
        });
        Context("a PUT to /{store}/{id} with a matching If-Match header and a matching if-none-match header", () -> {
            It("should respond with a 412 Precondition Failed", () -> {
                mvc.perform(put(url)
                        .content("Hello Modified Spring Content World!")
                        .contentType("text/plain")
                        .header("if-match", etag)
                        .header("if-none-match", etag))
                        .andExpect(status().isPreconditionFailed());
            });
        });
        Context("a POST to /{store}/{id} with a non-matching If-Match header", () -> {
            It("should respond with 412 Precondition Failed", () -> {
                mvc.perform(multipart(url)
                        .file(new MockMultipartFile("file",
                                "tests-file-modified.txt",
                                "text/plain", "Hello Spring Content World!".getBytes()))
                        .header("if-match", "\"999\""))
                        .andExpect(status().isPreconditionFailed());
            });
        });
        Context("a DELETE to /{store}/{id} with a non-matching If-Match header", () -> {
            It("should respond with 412 Precondition Failed", () -> {
                mvc.perform(delete(url)
                        .header("if-match", "\"999\""))
                        .andExpect(status().isPreconditionFailed());
            });
        });
    }
}
