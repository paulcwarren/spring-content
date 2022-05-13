package internal.org.springframework.content.rest.storedrenditions;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.HandleAfterGetContent;
import org.springframework.content.commons.annotations.HandleAfterSetContent;
import org.springframework.content.commons.annotations.HandleAfterUnsetContent;
import org.springframework.content.commons.annotations.HandleBeforeGetContent;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.HandleBeforeUnsetContent;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.AfterGetContentEvent;
import org.springframework.content.rest.AfterSetContentEvent;
import org.springframework.content.rest.AfterUnsetContentEvent;
import org.springframework.content.rest.BeforeGetContentEvent;
import org.springframework.content.rest.BeforeSetContentEvent;
import org.springframework.content.rest.BeforeUnsetContentEvent;
import org.springframework.content.rest.StoreRestEventHandler;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
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
@ContextConfiguration(classes = {
      StoredRenditionsRestIT.StoredRenditionsConfig.class,
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class StoredRenditionsRestIT {

    @Autowired private TestEntity5Repository repo;
    @Autowired private TestEntity5Store store;

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
                  mvc.perform(put("/testEntity5s/"
                          + testEntity5.getId() + "/content")
                          .content("Hello Spring Content World!")
                          .contentType("text/plain"))
                          .andExpect(status().is2xxSuccessful());
		      });

			  It("should store the rendition", () -> {

			      testEntity5 = repo.findById(testEntity5.getId()).get();
				  Resource r = store.getResource(testEntity5, PropertyPath.from("content"));
				  try (InputStream actual = r.getInputStream()) {
				      assertThat(IOUtils.toString(actual),
			              is("Hello Spring Content World!"));
				  }

                  r = store.getResource(testEntity5, PropertyPath.from("rendition"));
                  try (InputStream actual = r.getInputStream()) {
                      assertThat(IOUtils.toString(actual),
                          is("HELLO SPRING CONTENT WORLD!"));
                  }
			  });

              Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {

                  It("should also delete the rendition", () -> {

                      Long id = testEntity5.getId();

                      mvc.perform(delete("/testEntity5s/"
                              + testEntity5.getId() + "/content"))
                              .andExpect(status().isNoContent());

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
    }

	@Getter
	@StoreRestEventHandler
	public static class TestEventHandler {

	    @Autowired
	    private TestEntity5Repository repo;

	    @Autowired
	    private TestEntity5Store store;

	    @HandleBeforeGetContent
	    public void onBeforeGetContent(BeforeGetContentEvent event) {
	    }

        @HandleAfterGetContent
        public void onAfterGetContent(AfterGetContentEvent event) {
        }

        @HandleBeforeSetContent
        public void onBeforeSetContent(BeforeSetContentEvent event) {
        }

        @HandleAfterSetContent
        public void onAfterSetContent(AfterSetContentEvent event) throws IOException {
            String content = IOUtils.toString(event.getResource().getInputStream());
            TestEntity5 entity = (TestEntity5) event.getActualSource();

            entity = store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream(content.toUpperCase().getBytes()));
            entity = repo.save(entity);
        }

        @HandleBeforeUnsetContent
        public void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
            TestEntity5 entity = (TestEntity5) event.getActualSource();

            store.unsetContent(entity, PropertyPath.from("rendition"));
        }

        @HandleAfterUnsetContent
        public void onAfterUnsetContent(AfterUnsetContentEvent event) {
        }
	}
}
