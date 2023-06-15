package it.events;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jSpringRunner.class)
@ContextConfiguration(classes = { BeforeSetEventIT.TestConfig.class })
public class BeforeSetEventIT {

    static TestConfig.ExampleAnnotatedEventHandler ev;

    @Autowired
    private TestEntityContentStore store;

    {
        Describe("BeforeSetEvent InputStream Access", () -> {

            Context("when the content input stream is consumed by a beforesetevent", () -> {

                It("should still set the content in the store", () -> {
                    TestEntity te = new TestEntity();
                    ByteArrayInputStream bais = new ByteArrayInputStream("Still here!".getBytes());
                    te = store.setContent(te, bais);
                    IOUtils.closeQuietly(bais);
                    try (InputStream foo = store.getContent(te)) {
                        assertThat(IOUtils.toString(foo), is("Still here!"));
                    }

                    verify(ev, times(1));
                });
            });
        });
    }

    @Configuration
    @EnableFilesystemStores
    public static class TestConfig {

        @Bean
        FileSystemResourceLoader fs() throws IOException {
            return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
        }

        @Bean
        public ExampleAnnotatedEventHandler eventHandler() {
            ev = spy(new ExampleAnnotatedEventHandler());
            return ev;
        }

        @StoreEventHandler
        public static class ExampleAnnotatedEventHandler {

            @HandleBeforeSetContent
            public void handleBeforeSetContentEvent(BeforeSetContentEvent event) throws IOException {
                InputStream is = event.getInputStream();
                IOUtils.copy(is, new ByteArrayOutputStream());
            }
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public class TestEntity {
        @ContentId
        private String contentId;
    }

    public interface TestEntityContentStore extends ContentStore<TestEntity, String> {
    }

    @Test
    public void noop() {}
}
