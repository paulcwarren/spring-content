package internal.org.springframework.content.rest.events;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

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
import internal.org.springframework.content.rest.support.TestEntity8;
import internal.org.springframework.content.rest.support.TestEntity8Repository;
import internal.org.springframework.content.rest.support.TestEntity8Store;
import lombok.Getter;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
      StoreRestEventsIT.EventsConfig.class,
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class StoreRestEventsIT {

    @Autowired private TestEntity8Repository repo;
    @Autowired private TestEntity8Store store;

    private TestEntity8 testEntity8;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private TestEventHandler eventHandler;

	private MockMvc mvc;

	{
      Describe("Store Rest Events", () -> {
		BeforeEach(() -> {
		  mvc = MockMvcBuilders.webAppContextSetup(context).build();
		});
		Context("given an Entity with a content property", () -> {
		  BeforeEach(() -> {
			  testEntity8 = repo.save(new TestEntity8());
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
				  testEntity8 = repo.save(testEntity8);
			  });

			  Context("given the content property is accessed via the fully-qualified URL", () -> {
				  Context("a GET to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
				      It("should return the content", () -> {
				          assertThat(eventHandler.isBeforeGetContentEvent(), is(false));
                          assertThat(eventHandler.isAfterGetContentEvent(), is(false));

						  mvc.perform(get("/testEntity8s/" + testEntity8.getId() + "/child/content")
								  .accept("text/plain"))
								  .andExpect(status().isOk())
								  .andExpect(header().string("etag", is("\"1\"")))
								  .andExpect(content().string(is("Hello Spring Content World!")));

                          assertThat(eventHandler.isBeforeGetContentEvent(), is(true));
                          assertThat(eventHandler.isAfterGetContentEvent(), is(true));
					  });
				  });
				  Context("a PUT to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
					  It("should overwrite the content", () -> {
                          assertThat(eventHandler.isBeforeSetContentEvent(), is(false));
                          assertThat(eventHandler.isAfterSetContentEvent(), is(false));

						  mvc.perform(put("/testEntity8s/"
								  + testEntity8.getId() + "/child/content")
						          .content("Hello Modified Spring Content World!")
								  .contentType("text/plain"))
								  .andExpect(status().isOk());


						  Resource r = store.getResource(testEntity8, PropertyPath.from("child.content"));
						  try (InputStream actual = r.getInputStream()) {
						      assertThat(IOUtils.toString(actual),
    				              is("Hello Modified Spring Content World!"));
						  }

                          assertThat(eventHandler.isBeforeSetContentEvent(), is(true));
                          assertThat(eventHandler.isAfterSetContentEvent(), is(true));
					  });
				  });
				  Context("a DELETE to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
					  It("should delete the content", () -> {
                          assertThat(eventHandler.isBeforeUnsetContentEvent(), is(false));
                          assertThat(eventHandler.isAfterUnsetContentEvent(), is(false));

						  mvc.perform(delete("/testEntity8s/"
								  + testEntity8.getId() + "/child/content"))
								  .andExpect(status().isNoContent());

                          Optional<TestEntity8> fetched = repo.findById(testEntity8.getId());
                          assertThat(fetched.isPresent(), is(true));
                          assertThat(fetched.get().getChild().contentId, is(nullValue()));
                          assertThat(fetched.get().getChild().contentLen, is(0L));
                          assertThat(fetched.get().getChild().contentMimeType, is(nullValue()));

                          assertThat(eventHandler.isBeforeUnsetContentEvent(), is(true));
                          assertThat(eventHandler.isAfterUnsetContentEvent(), is(true));
					  });
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
	public static class EventsConfig {

	    @Bean
	    public TestEventHandler eventHandler() {
	        return new TestEventHandler();
	    }
    }

	@Getter
	@StoreRestEventHandler
	public static class TestEventHandler {

	    private boolean beforeGetContentEvent = false;
        private boolean afterGetContentEvent = false;

        private boolean beforeSetContentEvent = false;
        private boolean afterSetContentEvent = false;

        private boolean beforeUnsetContentEvent = false;
        private boolean afterUnsetContentEvent = false;

	    @HandleBeforeGetContent
	    public void onBeforeGetContent(BeforeGetContentEvent event) {
	        beforeGetContentEvent = true;
	    }

        @HandleAfterGetContent
        public void onAfterGetContent(AfterGetContentEvent event) {
            afterGetContentEvent = true;
        }

        @HandleBeforeSetContent
        public void onBeforeSetContent(BeforeSetContentEvent event) {
            beforeSetContentEvent = true;
        }

        @HandleAfterSetContent
        public void onAfterSetContent(AfterSetContentEvent event) {
            afterSetContentEvent = true;
        }

        @HandleBeforeUnsetContent
        public void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
            beforeUnsetContentEvent = true;
        }

        @HandleAfterUnsetContent
        public void onAfterUnsetContent(AfterUnsetContentEvent event) {
            afterUnsetContentEvent = true;
        }
	}
}
