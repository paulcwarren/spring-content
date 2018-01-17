package org.springframework.content.jpa.io;

import org.springframework.core.io.ResourceLoader;

public interface BlobResourceLoader extends ResourceLoader {

    String getDatabaseName();

}
