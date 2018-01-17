package org.springframework.content.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObservableInputStream extends InputStream {

    private final InputStream is;
    private final CloseableObserver observer;

    public ObservableInputStream(InputStream is, CloseableObserver observer) {
        this.is = is;
        this.observer = observer;
    }

    @Override
    public int read() throws IOException {
        return this.is.read();
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.observer.closed();
    }

    public List<CloseableObserver> getObservers() {
        return Arrays.asList(new CloseableObserver[]{observer});
    }
}
