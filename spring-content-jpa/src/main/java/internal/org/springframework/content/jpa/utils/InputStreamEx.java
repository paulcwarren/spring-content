package internal.org.springframework.content.jpa.utils;

import java.io.IOException;
import java.io.InputStream;

public final class InputStreamEx extends InputStream {
	
	private InputStream in;
	
	private int len;
	
	public InputStreamEx(InputStream in) {
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		int read = in.read();
		if (read != -1)
			this.len++;
		return read;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int read = in.read(b);
		if (read != -1)
			this.len += read;
		return read;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int read = in.read(b, off, len);
		if (read != -1)
			this.len += read;
		return read;
	}
	
	public int getLength() {
		return this.len;
	}
	
}
