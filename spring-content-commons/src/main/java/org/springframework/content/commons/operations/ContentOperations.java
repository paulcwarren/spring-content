package org.springframework.content.commons.operations;

import java.io.InputStream;

/**
 * Collection of operations to store and read content from a content store.
 */
public interface ContentOperations {
	<T> void setContent(T metadata, InputStream content);
	<T> void unsetContent(T property);
	<T> InputStream getContent(T property);
}
