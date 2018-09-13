package org.springframework.content.commons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ObservableOutputStream extends OutputStream {

    private final OutputStream delegate;
    private List<CloseableObserver> observers;

    public ObservableOutputStream(OutputStream delegate) {
        this.delegate = delegate;
        this.observers = new ArrayList<>();
    }

    @Override
    public void write(int b) throws IOException {
        this.delegate.write(b);
    }

    @Override
    public void close() throws IOException {
        try {
            this.delegate.close();
        } finally {
            for (CloseableObserver observer : this.observers) {
                observer.closed();
            }
        }
    }

    public void addObservers(OutputStreamObserver... observers) {
        this.observers.addAll(Arrays.asList(observers));
    }

    public List<CloseableObserver> getObservers() {
        return Collections.unmodifiableList(observers);
    }
}
