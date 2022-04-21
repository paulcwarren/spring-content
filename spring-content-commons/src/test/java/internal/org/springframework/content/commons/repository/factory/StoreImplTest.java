package internal.org.springframework.content.commons.repository.factory;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.runner.RunWith;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.ApplicationEventPublisher;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
public class StoreImplTest {

    private StoreImpl stores;

    private ContentStore store;
    private ApplicationEventPublisher publisher;
    private String tmpDir;
    private Path contentCopyPathRoot;

    {
        Describe("StoreImpl", () -> {

            BeforeEach(() -> {
                store = mock(ContentStore.class);
                publisher = mock(ApplicationEventPublisher.class);

            });
            JustBeforeEach(() -> {
                contentCopyPathRoot = Files.createTempDirectory("storeimpltest");

                for (File f : contentCopyPathRoot.toFile().listFiles()) {
                    if (f.getName().endsWith(".tmp")) {
                        f.delete(); // may fail mysteriously - returns boolean you may want to check
                    }
                }

                stores = new StoreImpl(store, publisher, contentCopyPathRoot);
            });

            Context("#setContent - inputstream", () -> {

                BeforeEach(() -> {
                    when(store.setContent(anyObject(), any(InputStream.class))).thenReturn(new Object());
                });

                JustBeforeEach(() -> {
                    stores.setContent(new Object(), new ByteArrayInputStream("foo".getBytes()));
                });

                It("should delete the content copy file", () -> {
                    for (File f : contentCopyPathRoot.toFile().listFiles()) {
                        if (f.getName().endsWith(".tmp")) {
                            fail("Found orphaned content copy path");
                        }
                    }
                });
            });
        });
    }
}
