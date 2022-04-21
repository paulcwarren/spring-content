package internal.org.springframework.content.rest.storedrenditions;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
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
import internal.org.springframework.content.rest.support.TestEntity5;
import internal.org.springframework.content.rest.support.TestEntity5Repository;
import internal.org.springframework.content.rest.support.TestEntity5Store;
import lombok.Getter;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = { StoredRenditionsRestIT.StoredRenditionsConfig.class, StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class StoredRenditionsRestIT {

    @Autowired
    private TestEntity5Repository repo;
    @Autowired
    private TestEntity5Store store;

    private TestEntity5 testEntity5;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestEventHandler eventHandler;

    private MockMvc mvc;

    {
        Describe("Stored Renditions", () -> {

            BeforeEach(() -> {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            });

            Context("given an Entity with a content property", () -> {

                BeforeEach(() -> {
                    testEntity5 = repo.save(new TestEntity5());
                });

                Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {

                    BeforeEach(() -> {
                        mvc.perform(put("/testEntity5s/" + testEntity5.getId() + "/content").content("foo").contentType("text/plain")).andExpect(status().is2xxSuccessful());
                    });

                    It("should store the rendition", () -> {

                        testEntity5 = repo.findById(testEntity5.getId()).get();
                        Resource r = store.getResource(testEntity5, PropertyPath.from("content"));
                        try (InputStream actual = r.getInputStream()) {
                            assertThat(IOUtils.toString(actual), is("foo"));
                        }

                        r = store.getResource(testEntity5, PropertyPath.from("rendition"));
                        try (InputStream actual = r.getInputStream()) {
                            assertThat(IOUtils.toString(actual), is("<html><head><title>Stored Rendition</title></head><body>foo</body></html>"));
                        }
                    });

                    Context("a GET to /{repository}/{id}/{contentProperty}", () -> {

                        It("should get the stored rendition", () -> {

                            Long id = testEntity5.getId();

                            mvc.perform(
                                    get("/testEntity5s/" + testEntity5.getId() + "/content")
                                    .accept("text/html"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(not("<html><head><title>Dynamic Rendition</title></head><body>foo</body></html>")))
                                .andExpect(content().string("<html><head><title>Stored Rendition</title></head><body>foo</body></html>"));
                        });
                    });

                    Context("a GET to /{repository}/{id}/{renditionProperty}", () -> {

                        It("should return a 405", () -> {

                            Long id = testEntity5.getId();

                            mvc.perform(
                                    get("/testEntity5s/" + testEntity5.getId() + "/rendition")
                                    .accept("text/html"))
                                .andExpect(status().isMethodNotAllowed());
                        });
                    });

                    Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {

                        It("should also delete the rendition", () -> {

                            Long id = testEntity5.getId();

                            mvc.perform(delete("/testEntity5s/" + testEntity5.getId() + "/content")).andExpect(status().isNoContent());

                            Optional<TestEntity5> fetched = repo.findById(id);
                            assertThat(fetched.isPresent(), is(true));
                            assertThat(fetched.get().getContentId(), is(nullValue()));
                            assertThat(fetched.get().getContentLen(), is(0L));
                            assertThat(fetched.get().getContentMimeType(), is(nullValue()));

                            assertThat(fetched.get().getRenditionId(), is(nullValue()));
                            assertThat(fetched.get().getRenditionLen(), is(0L));
                            assertThat(fetched.get().getRenditionMimeType(), is(nullValue()));
                        });
                    });
                });
            });
        });
    }

    @Test
    public void noop() {
    }

    @Configuration
    public static class StoredRenditionsConfig {

        @Bean
        public TestEventHandler eventHandler() {
            return new TestEventHandler();
        }

        // a dynamic renderer helps prove the stored rendition was returned
        @Bean
        public RenditionProvider textToHtml() {
            return new RenditionProvider() {

                @Override
                public String consumes() {
                    return "text/plain";
                }

                @Override
                public String[] produces() {
                    return new String[] { "text/html" };
                }

                @Override
                public InputStream convert(InputStream fromInputSource, String toMimeType) {
                    String input = null;
                    try {
                        input = IOUtils.toString(fromInputSource);
                    }
                    catch (IOException e) {
                    }
                    return new ByteArrayInputStream(
                        String.format("\"<html><head><title>Dynamic Rendition</title></head><body>%s</body></html>\"", input).getBytes());
                }
            };
        }
    }

    @Getter
    @StoreEventHandler
    public static class TestEventHandler {

        @Autowired
        private TestEntity5Repository repo;

        @Autowired
        private TestEntity5Store store;

        @HandleAfterSetContent
        public void onAfterSetContent(AfterSetContentEvent event)
                throws IOException {

            TestEntity5 entity = (TestEntity5)event.getSource();

            long renderedLength = 0;
            try (InputStream inputStream = store.getContent(entity, PropertyPath.from("content"))) {

                String renderedContent = IOUtils.toString(inputStream);

                try (OutputStream stream = ((WritableResource)store.getResource(entity, PropertyPath.from("rendition"))).getOutputStream()) {
                    renderedLength = IOUtils.copyLarge(
                            new ByteArrayInputStream(String.format("<html><head><title>Stored Rendition</title></head><body>%s</body></html>", renderedContent).getBytes()),
                            stream);
                }
            }

            entity.setRenditionLen(renderedLength);
            entity.setRenditionMimeType("text/html");
        }

        @HandleBeforeUnsetContent
        public void onBeforeUnsetContent(BeforeUnsetContentEvent event) throws IOException {

            TestEntity5 entity = (TestEntity5) event.getSource();

            ((DeletableResource)store.getResource(entity, PropertyPath.from("rendition"))).delete();

            entity.setRenditionId(null);
            entity.setRenditionLen(0L);
            entity.setRenditionMimeType(null);
        }
    }
}
