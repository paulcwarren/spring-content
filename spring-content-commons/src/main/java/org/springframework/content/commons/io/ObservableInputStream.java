package org.springframework.content.commons.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ObservableInputStream extends InputStream {

    private final InputStream is;
    private final InputStreamObserver observer;

    public ObservableInputStream(InputStream is, InputStreamObserver observer) {
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

    public List<InputStreamObserver> getObservers() {
        return Arrays.asList(new InputStreamObserver[]{observer});
    }
}
