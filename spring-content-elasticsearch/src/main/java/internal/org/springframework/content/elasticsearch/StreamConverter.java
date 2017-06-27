package internal.org.springframework.content.elasticsearch;

import java.io.IOException;
import java.io.InputStream;

public interface StreamConverter {
	byte[] convert(InputStream stream) throws IOException;
}
