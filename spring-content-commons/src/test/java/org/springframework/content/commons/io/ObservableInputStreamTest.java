package org.springframework.content.commons.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.util.Observable;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class ObservableInputStreamTest {

    private ObservableInputStream ois;

    private FileInputStream fis;
    private InputStreamObserver observer;

    {
        Describe("ObservableInputStream", () -> {
            Context("when an input stream is observed", () -> {
                BeforeEach(() -> {
                    fis = mock(FileInputStream.class);
                    observer = mock(InputStreamObserver.class);

                    ois = new ObservableInputStream(fis, observer);
                });
                Context("when the input stream has listeners", () -> {
                    It("should return them", () -> {
                        assertThat(ois.getObservers(), hasItem(observer));
                    });
                });
                Context("when the input stream is read", () -> {
                    JustBeforeEach(() -> {
                        ois.read();
                    });
                    It("should delegate to the underlying input stream", () -> {
                        verify(fis).read();
                    });
                });
                Context("when the input stream is closed", () -> {
                    JustBeforeEach(() -> {
                        ois.close();
                    });
                    It("should call listeners on closed event handler", () -> {
                        verify(observer).closed();
                    });
                });
            });
        });
    }
}
