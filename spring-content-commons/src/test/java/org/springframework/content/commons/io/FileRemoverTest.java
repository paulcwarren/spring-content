package org.springframework.content.commons.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;

import java.io.File;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class FileRemoverTest {

    private File file;

    private FileRemover observer;

    {
        Describe("FileRemover", () -> {
            Context("when a file input stream observer's closed is called", () -> {
                BeforeEach(() -> {
                    file = mock(File.class);
                    observer = new FileRemover(file);
                });
                JustBeforeEach(() -> {
                    observer.closed();
                });
                It("should delete the underlying file", () -> {
                    verify(file).delete();
                });
            });
        });
    }
}
