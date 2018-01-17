package org.springframework.content.commons.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import java.io.IOException;
import java.io.OutputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Ginkgo4jRunner.class)
public class ObservableOutputStreamTest {

    private ObservableOutputStream observable;

    private OutputStreamObserver observer1;
    private OutputStreamObserver observer2;

    private OutputStream os;

    private Exception exception;


    {
        Describe("ObservableOutputStream", () -> {
            BeforeEach(() -> {
                os = mock(OutputStream.class);
                observer1 = mock(OutputStreamObserver.class);
                observer2 = mock(OutputStreamObserver.class);
            });
            JustBeforeEach(() -> {
                observable = new ObservableOutputStream(os);
                observable.addObservers(observer1);
                observable.addObservers(observer2);
            });
            Context("when the output stream has listeners", () -> {
                It("should return them", () -> {
                    assertThat(observable.getObservers(), hasItem(observer1));
                });
            });
            Context("when the output stream is written to", () -> {
                JustBeforeEach(() -> {
                    observable.write(32);
                });
                It("should delegate to the underlying input stream", () -> {
                    verify(os).write(32);
                });
            });
            Context("when the output stream is closed", () -> {
                JustBeforeEach(() -> {
                    observable.close();
                });
                It("should call listeners on closed event handler (in order)", () -> {
                    InOrder inOrder = inOrder(observer1, observer2);
                    verify(observer1).closed();
                    verify(observer2).closed();
                    verifyNoMoreInteractions(observer1, observer2);
                });
            });
            Context("when the output stream is closed and throws an exception", () -> {
                BeforeEach(() -> {
                    doThrow(new IOException("badness")).when(os).close();
                });
                JustBeforeEach(() -> {
                    try {
                        observable.close();
                    } catch (Exception e) {
                        exception = e;
                    }
                });
                It("should call listeners on closed event handler and throw the exception", () -> {
                    verify(observer1).closed();
                    assertThat(exception, is(not(nullValue())));
                });
            });
        });
    }
}
