package internal.org.springframework.content.rest.io;

import java.io.InputStream;

public class InputStreamResource extends org.springframework.core.io.InputStreamResource {

    private final String filename;

    public InputStreamResource(InputStream inputStream, String originalFilename) {

        super(inputStream);

        this.filename = originalFilename;
    }

    @Override
    public String getFilename() {
        return filename;
    }
}
